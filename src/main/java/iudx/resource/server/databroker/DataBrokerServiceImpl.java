package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;

/**
 * The Data Broker Service Implementation.
 * <h1>Data Broker Service Implementation</h1>
 * <p>
 * The Data Broker Service implementation in the IUDX Resource Server implements the definitions of
 * the {@link iudx.resource.server.databroker.DataBrokerService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DataBrokerServiceImpl implements DataBrokerService {

  private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceImpl.class);
  private RabbitMQClient client;
  private String url;
  private WebClient webClient;
  JsonObject requestBody = new JsonObject();
  JsonObject finalResponse = new JsonObject();
  private String user;
  private String password;
  private String vhost;
  private int totalBindCount;
  private int totalBindSuccess;
  private int totalUnBindCount;
  private int totalUnBindSuccess;
  private boolean bindingSuccessful;
  private PgPool pgclient;

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param clientInstance which is a RabbitMQ client
   * @param webClientInstance which is a Vertx Web client
   */

  public DataBrokerServiceImpl(RabbitMQClient clientInstance, WebClient webClientInstance,
      JsonObject propObj, PgPool pgclientinstance) {

    logger.info("Got the RabbitMQ Client instance");
    client = clientInstance;
    pgclient = pgclientinstance;


    client.start(resultHandler -> {

      if (resultHandler.succeeded()) {
        logger.info("RabbitMQ Client Connected");

      } else {
        logger.info("RabbitMQ Client Not Connected");
      }

    });

    if (propObj != null && !propObj.isEmpty()) {

      user = propObj.getString("userName");
      password = propObj.getString("password");
      vhost = propObj.getString("vHost");

    }
    webClient = webClientInstance;

  }

  /**
   * This method creates user, declares exchange and bind with predefined queues.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService registerAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    System.out.println(request.toString());
    /* Get the ID and userName from the request */
    String id = request.getString("resourceGroup");
    String resourceServer = request.getString("resourceServer");
    String userName = request.getString(Constants.CONSUMER);

    logger.info("Resource Group Name given by user is : " + id);
    logger.info("Resource Server Name by user is : " + resourceServer);
    logger.info("User Name is : " + userName);

    /* Construct a response object */
    JsonObject registerResponse = new JsonObject();

    /* Validate the request object */
    if (request != null && !request.isEmpty()) {
      /* Goto Create user if ID is not empty */
      if (id != null && !id.isEmpty() && !id.isBlank()) {
        /* Validate the ID for special characters */
        if (validateID(id)) {
          /* Validate the userName */
          if (userName != null && !userName.isBlank() && !userName.isEmpty()) {
            /* Create a new user, if it does not exists */
            Future<JsonObject> userCreationFuture = createUserIfNotExist(userName, vhost);
            /* On completion of user creation, handle the result */
            userCreationFuture.onComplete(rh -> {
              if (rh.succeeded()) {
                /* Obtain the result of user creation */
                JsonObject result = rh.result();
                logger.info("Response of createUserIfNotExist is : " + result);

                /* Construct the domain, userNameSHA, userID and adaptorID */
                String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
                String userNameSha = getSha(userName);
                String userID = domain + "/" + userNameSha;
                String adaptorID = userID + "/" + resourceServer + "/" + id;

                logger.info("userID is : " + userID);
                logger.info("adaptorID is : " + adaptorID);

                if (adaptorID != null && !adaptorID.isBlank() && !adaptorID.isEmpty()) {
                  JsonObject json = new JsonObject();
                  json.put(Constants.EXCHANGE_NAME, adaptorID);
                  /* Create an exchange if it does not exists */
                  Future<JsonObject> exchangeDeclareFuture = createExchange(json);
                  /* On completion of exchange creation, handle the result */
                  exchangeDeclareFuture.onComplete(ar -> {
                    if (ar.succeeded()) {
                      /* Obtain the result of exchange creation */
                      JsonObject obj = ar.result();

                      logger.info("Response of createExchange is : " + obj);
                      logger.info("exchange name provided : " + adaptorID);
                      logger.info("exchange name received : " + obj.getString("exchange"));

                      // if exchange just registered then set topic permission and bind with
                      // queues
                      if (!obj.containsKey("detail")) {

                        Future<JsonObject> topicPermissionFuture = setTopicPermissions(vhost,
                            domain + "/" + userNameSha + "/" + resourceServer + "/" + id, userID);
                        topicPermissionFuture.onComplete(topicHandler -> {
                          if (topicHandler.succeeded()) {
                            logger.info("Write permission set on topic for exchange "
                                + obj.getString("exchange"));
                            /* Bind the exchange with the database and adaptorLogs queue */
                            Future<JsonObject> queueBindFuture = queueBinding(
                                domain + "/" + userNameSha + "/" + resourceServer + "/" + id);
                            queueBindFuture.onComplete(res -> {
                              if (res.succeeded()) {
                                logger.info("Queue_Database, Queue_adaptorLogs binding done with "
                                    + obj.getString("exchange") + " exchange");
                                /* Construct the response for registration of adaptor */
                                registerResponse.put(Constants.USER_NAME,
                                    domain + "/" + userNameSha);
                                /*
                                 * APIKEY should be equal to password generated. For testing use
                                 * Constants.APIKEY_TEST_EXAMPLE
                                 */
                                registerResponse.put(Constants.APIKEY,
                                    Constants.APIKEY_TEST_EXAMPLE);
                                registerResponse.put(Constants.ID,
                                    domain + "/" + userNameSha + "/" + resourceServer + "/" + id);
                                registerResponse.put(Constants.VHOST, Constants.VHOST_IUDX);

                                logger.info("registerResponse : " + registerResponse);
                                handler.handle(Future.succeededFuture(registerResponse));

                              } else {
                                /* Handle Queue Error */
                                logger.error(
                                    "error in queue binding with adaptor - cause : " + res.cause());
                                registerResponse.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
                                handler.handle(Future.failedFuture(registerResponse.toString()));

                              }
                            });

                          } else {
                            /* Handle Topic Permission Error */
                            logger.info("topic permissions not set for exchange "
                                + obj.getString("exchange") + " - cause : "
                                + topicHandler.cause().getMessage());

                            registerResponse.put(Constants.ERROR,
                                Constants.TOPIC_PERMISSION_SET_ERROR);
                            handler.handle(Future.failedFuture(registerResponse.toString()));

                          }
                        });

                      } else if (obj.getString("detail") != null
                          && !obj.getString("detail").isEmpty()
                          && obj.getString("detail").equalsIgnoreCase("Exchange already exists")) {
                        /* Handle Exchange Error */
                        logger.error("something wrong in exchange declaration : " + ar.cause());
                        registerResponse.put(Constants.ERROR, Constants.EXCHANGE_EXISTS);
                        handler.handle(Future.failedFuture(registerResponse.toString()));
                      }

                    } else {
                      /* Handle Exchange Error */
                      logger.error("something wrong in exchange declaration : " + ar.cause());
                      registerResponse.put(Constants.ERROR, Constants.EXCHANGE_DECLARATION_ERROR);
                      handler.handle(Future.failedFuture(registerResponse.toString()));

                    }

                  });

                } else {
                  /* Handle Request Error */
                  logger.error("AdaptorID / Exchange not provided in request");
                  registerResponse.put(Constants.ERROR, Constants.ADAPTOR_ID_NOT_PROVIDED);
                  handler.handle(Future.failedFuture(registerResponse.toString()));

                }

              } else if (rh.failed()) {
                /* Handle User Creation Error */
                logger.error("User creation failed. " + rh.cause());
                registerResponse.put(Constants.ERROR, Constants.USER_CREATION_ERROR);
                handler.handle(Future.failedFuture(registerResponse.toString()));
              } else {
                /* Handle User Creation Error */
                logger.error("User creation failed. " + rh.cause());
                registerResponse.put(Constants.ERROR, Constants.USER_CREATION_ERROR);
                handler.handle(Future.failedFuture(registerResponse.toString()));

              }

            });

          } else {
            /* Handle Request Error */
            logger.error("user not provided in adaptor registration");
            registerResponse.put(Constants.ERROR, Constants.USER_NAME_NOT_PROVIDED);
            handler.handle(Future.failedFuture(registerResponse.toString()));
          }
        } else {
          /* Handle Invalid ID Error */
          registerResponse.put(Constants.ERROR, Constants.INVALID_ID);
          handler
              .handle(Future.failedFuture(new JsonObject().put("error", "invalid id").toString()));
          logger.error("id not provided in adaptor registration");
        }
      } else {
        /* Handle Request Error */
        logger.error("id not provided in adaptor registration");
        registerResponse.put(Constants.ERROR, Constants.ID_NOT_PROVIDED);
        handler.handle(Future.failedFuture(registerResponse.toString()));
      }
    } else {
      /* Handle Request Error */
      logger.error("Bad Request");
      registerResponse.put(Constants.ERROR, Constants.BAD_REQUEST);
      handler.handle(Future.failedFuture(registerResponse.toString()));
    }

    return null;

  }

  /*
   * helper method which bind registered exchange with predefined queues
   * 
   * @param adaptorID which is a String object
   * 
   * @return response which is a Future object of promise of Json type
   */
  Future<JsonObject> queueBinding(String adaptorID) {
    Promise<JsonObject> promise = Promise.promise();
    /* Create a response object */
    JsonObject response = new JsonObject();
    // Now bind newly created adaptor with queues i.e. adaptorLogs,database
    String topics = adaptorID + "/.*";
    /* Bind to database queue */
    client.queueBind(Constants.QUEUE_DATA, adaptorID, topics, result -> {
      if (result.succeeded()) {
        /* On success bind to adaptorLogs queue */
        response.put("Queue_Database", Constants.QUEUE_DATA + " queue bound to " + adaptorID);

        client.queueBind(Constants.QUEUE_ADAPTOR_LOGS, adaptorID, adaptorID + Constants.HEARTBEAT,
            bindingheartBeatResult -> {
              if (bindingheartBeatResult.succeeded()) {
                client.queueBind(Constants.QUEUE_ADAPTOR_LOGS, adaptorID,
                    adaptorID + Constants.DATA_ISSUE, bindingdataIssueResult -> {
                      if (bindingdataIssueResult.succeeded()) {
                        client.queueBind(Constants.QUEUE_ADAPTOR_LOGS, adaptorID,
                            adaptorID + Constants.DOWNSTREAM_ISSUE,
                            bindingdownstreamIssueResult -> {
                              if (bindingdownstreamIssueResult.succeeded()) {

                                promise.complete(response);

                              } else {
                                /* Handle bind to adaptorLogs queue error */
                                logger.error(" Queue_adaptorLogs binding error : "
                                    + bindingdownstreamIssueResult.cause());
                                response.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
                                promise.fail(response.toString());
                              }
                            });
                      } else {
                        /* Handle bind to adaptorLogs queue error */
                        logger.error(
                            " Queue_adaptorLogs binding error : " + bindingdataIssueResult.cause());
                        response.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
                        promise.fail(response.toString());
                      }
                    });
              } else {
                /* Handle bind to adaptorLogs queue error */
                logger
                    .error(" Queue_adaptorLogs binding error : " + bindingheartBeatResult.cause());
                response.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
                promise.fail(response.toString());
              }
            });
      } else {
        /* Handle bind to database queue error */
        logger.error(" Queue_Database binding error : " + result.cause());
        response.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
        promise.fail(response.toString());
      }
    });

    return promise.future();
  }

  /**
   * The createUserIfNotExist implements the create user if does not exist.
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUserIfNotExist(String userName, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    /* Create a response object */
    JsonObject response = new JsonObject();
    Future<JsonObject> future = createUserIfNotPresent(userName, vhost);
    future.onComplete(handler -> {
      /* On successful response handle the result */
      if (handler.succeeded()) {
        /* Respond to the requestor */
        JsonObject result = handler.result();
        response.put(Constants.SHA_USER_NAME, result.getString("shaUsername"));
        response.put(Constants.APIKEY, result.getString("password"));
        response.put(Constants.TYPE, result.getString("type"));
        response.put(Constants.TITLE, result.getString("title"));
        response.put(Constants.DETAILS, result.getString("detail"));
        response.put(Constants.VHOST_PERMISSIONS, result.getString("vhostPermissions"));
        promise.complete(response);
      } else {
        logger.info("Something went wrong - Cause: " + handler.cause());
        response.put(Constants.ERROR, Constants.USER_CREATION_ERROR);
        promise.fail(response.toString());
      }

    });

    return promise.future();

  }

  /**
   * createUserIfNotExist helper method which check user existence. Create user if not present
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUserIfNotPresent(String userName, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    /* Get domain, shaUsername from userName */
    String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
    String shaUsername = domain + "/" + getSha(userName);
    // This API requires user name in path parameter. Encode the username as it
    // contains a "/"
    String url = "/api/users/" + encodedValue(shaUsername);
    /* Check if user exists */
    HttpRequest<Buffer> request = webClient.get(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();
    request.send(reply -> {
      if (reply.succeeded()) {
        /* Check if user not found */
        if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
          logger.info(
              "createUserIfNotExist success method : User not found. So creating user .........");
          /* Create new user */
          Future<JsonObject> userCreated = createUser(shaUsername, vhost, url);
          userCreated.onComplete(handler -> {
            if (handler.succeeded()) {
              /* Handle the response */
              JsonObject result = handler.result();
              response.put(Constants.SHA_USER_NAME, result.getString("shaUsername"));
              response.put(Constants.APIKEY, result.getString("password"));
              response.put(Constants.TYPE, result.getString("type"));
              response.put(Constants.TITLE, result.getString("title"));
              response.put(Constants.DETAILS, result.getString("detail"));
              response.put(Constants.VHOST_PERMISSIONS, result.getString("vhostPermissions"));
              promise.complete(response);
            } else {
              logger.error("createUser method onComplete() - Error in user creation. Cause : "
                  + handler.cause());
            }
          });

        } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
          // user exists , So something useful can be done here
          // TODO : Need to get the "apiKey"
          /* Handle the response if a user exists */
          JsonObject result = reply.result().bodyAsJsonObject();
          response.put(Constants.SHA_USER_NAME, result.getString("shaUsername"));
          response.put(Constants.TYPE, Constants.USER_EXISTS);
          response.put(Constants.TITLE, Constants.SUCCESS);
          response.put(Constants.DETAILS, Constants.USER_ALREADY_EXISTS);
          promise.complete(response);
        }

      } else {
        /* Handle API error */
        logger.info("Something went wrong while finding user using mgmt API: " + reply.cause());
        promise.fail(reply.cause().toString());
      }

    });

    return promise.future();

  }

  /**
   * CreateUserIfNotPresent's helper method which creates user if not present.
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUser(String shaUsername, String vhost, String url) {

    Promise<JsonObject> promise = Promise.promise();
    // now creating user using same url with method put
    HttpRequest<Buffer> createUserRequest = webClient.put(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put(Constants.PASSWORD, generateRandomPassword());
    arg.put(Constants.TAGS, Constants.NONE);

    createUserRequest.sendJsonObject(arg, ar -> {
      if (ar.succeeded()) {
        /* Check if user is created */
        if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
          logger.info("createUserRequest success");
          response.put(Constants.SHA_USER_NAME, shaUsername);
          response.put(Constants.PASSWORD, arg.getString("password"));
          response.put(Constants.TITLE, Constants.SUCCESS);
          response.put(Constants.TYPE, "" + ar.result().statusCode());
          response.put(Constants.DETAILS, Constants.USER_CREATED);
          logger.info("createUser method : given user created successfully");
          // set permissions to vhost for newly created user
          Future<JsonObject> vhostPermission = setVhostPermissions(shaUsername, vhost);
          vhostPermission.onComplete(handler -> {
            if (handler.succeeded()) {
              response.put(Constants.VHOST_PERMISSIONS,
                  handler.result().getString("vhostPermissions"));
              promise.complete(response);
            } else {
              /* Handle error */
              logger.error("Error in setting vhostPermissions. Cause : " + handler.cause());
              response.put(Constants.VHOST_PERMISSIONS, Constants.VHOST_PERMISSIONS_FAILURE);
              promise.complete(response);
            }
          });

        } else {
          /* Handle error */
          logger.error("createUser method - Some network error. cause" + ar.cause());
          response.put(Constants.FAILURE, Constants.NETWORK_ISSUE);
          promise.fail(response.toString());
        }
      } else {
        /* Handle error */
        logger.info("Something went wrong while creating user using mgmt API :" + ar.cause());
        response.put(Constants.FAILURE, Constants.CHECK_CREDENTIALS);
        promise.fail(response.toString());
      }
    });

    return promise.future();
  }

  /**
   * set topic permissions.
   * 
   * @param vhost which is a String
   * @param adaptorID which is a String
   * @param shaUsername which is a String
   * @return response which is a Future object of promise of Json type
   **/
  private Future<JsonObject> setTopicPermissions(String vhost, String adaptorID, String userID) {
    // now set write permission to user for this adaptor(exchange)
    String url = "/api/topic-permissions/" + vhost + "/" + encodedValue(userID);

    JsonObject param = new JsonObject();
    // set all mandatory fields
    param.put(Constants.EXCHANGE, adaptorID);
    param.put(Constants.WRITE, Constants.ALLOW);
    param.put(Constants.READ, Constants.DENY);
    param.put(Constants.CONFIGURE, Constants.DENY);

    Promise<JsonObject> promise = Promise.promise();
    HttpRequest<Buffer> request = webClient.put(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();

    request.sendJsonObject(param, result -> {
      if (result.succeeded()) {
        /* Check if request was a success */
        if (result.result().statusCode() == HttpStatus.SC_CREATED) {
          response.put(Constants.TOPIC_PERMISSION, Constants.TOPIC_PERMISSION_SET_SUCCESS);
          logger.info("Topic permission set");
          promise.complete(response);
        } else if (result.result()
            .statusCode() == HttpStatus.SC_NO_CONTENT) { /* Check if request was already served */
          response.put(Constants.TOPIC_PERMISSION, Constants.TOPIC_PERMISSION_ALREADY_SET);
          promise.complete(response);
        } else { /* Check if request has an error */
          logger.error("Error in setting topic permissions" + result.result().statusMessage());
          response.put(Constants.TOPIC_PERMISSION, Constants.TOPIC_PERMISSION_SET_ERROR);
          promise.fail(response.toString());
        }
      } else { /* Check if request has an error */
        logger.error("Error in setting topic permission : " + result.cause());
        response.put(Constants.TOPIC_PERMISSION, Constants.TOPIC_PERMISSION_SET_ERROR);
        promise.fail(response.toString());
      }
    });

    return promise.future();
  }

  /**
   * set vhost permissions for given userName.
   * 
   * @param shaUsername which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  private Future<JsonObject> setVhostPermissions(String shaUsername, String vhost) {
    // set permissions for this user
    /* Construct URL to use */
    String url = "/api/permissions/" + vhost + "/" + encodedValue(shaUsername);
    JsonObject vhostPermissions = new JsonObject();

    // all keys are mandatory. empty strings used for configure,read as not
    // permitted.
    vhostPermissions.put(Constants.CONFIGURE, Constants.DENY);
    vhostPermissions.put(Constants.WRITE, Constants.ALLOW);
    vhostPermissions.put(Constants.READ, Constants.ALLOW);

    Promise<JsonObject> promise = Promise.promise();
    // now set all mandatory permissions for given vhost for newly created user
    HttpRequest<Buffer> vhostPermissionRequest =
        webClient.put(url).basicAuthentication(user, password);
    /* Construct a response object */
    JsonObject vhostPermissionResponse = new JsonObject();

    vhostPermissionRequest.sendJsonObject(vhostPermissions, handler -> {
      if (handler.succeeded()) {
        /* Check if permission was set */
        if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
          logger.info("vhostPermissionRequest success");
          vhostPermissionResponse.put(Constants.VHOST_PERMISSIONS,
              Constants.VHOST_PERMISSIONS_WRITE);
          logger.info(
              "write permission set for user [ " + shaUsername + " ] in vHost [ " + vhost + "]");
          promise.complete(vhostPermissionResponse);
        } else {
          logger.error("Error in write permission set for user [ " + shaUsername + " ] in vHost [ "
              + vhost + " ]");
          vhostPermissionResponse.put(Constants.VHOST_PERMISSIONS,
              Constants.VHOST_PERMISSION_SET_ERROR);
          promise.fail(vhostPermissions.toString());
        }
      } else {
        /* Check if request has an error */
        logger.error("Error in write permission set for user [ " + shaUsername + " ] in vHost [ "
            + vhost + " ]");
        vhostPermissionResponse.put(Constants.VHOST_PERMISSIONS,
            Constants.VHOST_PERMISSION_SET_ERROR);
        promise.fail(vhostPermissions.toString());
      }
    });

    return promise.future();
  }

  /**
   * encode string using URLEncoder's encode method.
   * 
   * @param vhost which is a String
   * @return encoded_vhost which is a String
   **/
  private String encodedValue(String vhost) {
    String encodedVhost = null;
    try {
      encodedVhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      logger.error("Error in encode vhost name :" + ex.getCause());
    }
    return encodedVhost;
  }

  /**
   * This method is as simple as but it can have more sophisticated encryption logic.
   * 
   * @param plainUserName which is a String
   * @return encodedValue which is a String
   **/
  private String getSha(String plainUserName) {

    String encodedValue = null;
    try {
      encodedValue = DigestUtils.md5Hex(plainUserName);
    } catch (Exception e) {
      // throw new RuntimeException(e);
      logger.error("Unable to encode username using SHA" + e.getLocalizedMessage());
    }
    return encodedValue;
  }

  /**
   * This method generate random alphanumeric password of given PASSWORD_LENGTH.
   **/
  private String generateRandomPassword() {
    // It is simple one. here we may have strong algorithm for password generation.
    return org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(Constants.PASSWORD_LENGTH);
  }

  /**
   * TODO This method checks the if for special characters other than hyphen, A-Z, a-z and 0-9.
   **/

  private boolean validateID(String id) {
    /* Check if id contains any special character */
    Pattern allowedPattern = Pattern.compile("[^-_.a-z0-9 ]", Pattern.CASE_INSENSITIVE);
    Matcher isInvalid = allowedPattern.matcher(id);
    
    if (isInvalid.find()) {
      logger.info("Invalid ID" + id);
      return false;
    } else {
      logger.info("Valid ID" + id);
      return true;
    }
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /*
   * It retrieves exchange is exist
   * 
   * @param request which is of type JsonObject
   * 
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService getExchange(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = getExchange(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("getExchange resultHandler failed : " + resultHandler.cause());
        }
      });
    }
    return this;
  }

  /*
   * overridden method
   */
  Future<JsonObject> getExchange(JsonObject request) {
    JsonObject response = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("id");
      exchangeName = exchangeName.replace("/", "%2F");
      if (vhost.contains("/")) {
        url = "/api/exchanges/" + encodedValue(vhost) + "/" + exchangeName;
      } else {
        url = "/api/exchanges/" + vhost + "/" + exchangeName;
      }

      HttpRequest<Buffer> isExchangeExist = webClient.get(url).basicAuthentication(user, password);
      isExchangeExist.send(result -> {
        if (result.succeeded()) {
          int status = result.result().statusCode();
          response.put(Constants.TYPE, status);
          if (status == HttpStatus.SC_OK) {
            response.put(Constants.TITLE, Constants.SUCCESS);
            response.put(Constants.DETAIL, Constants.EXCHANGE_FOUND);
          } else if (status == HttpStatus.SC_NOT_FOUND) {
            response.put(Constants.TITLE, Constants.FAILURE);
            response.put(Constants.DETAIL, Constants.EXCHANGE_NOT_FOUND);
          } else {
            response.put("getExchange_status", status);
            promise.fail("getExchange_status" + result.cause());
          }
        } else {
          response.put("getExchange_error", result.cause());
          promise.fail("getExchange_error" + result.cause());
        }
        logger.info("getExchange method response : " + response);
        promise.complete(response);
      });

    } else {
      promise.fail("exchangeName not provided");
    }

    return promise.future();

  }

  /**
   * The deleteAdaptor implements deletion feature for an adaptor(exchange).
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService deleteAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject finalResponse = new JsonObject();
    System.out.println(request.toString());
    Future<JsonObject> result = getExchange(request);
    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        int status = resultHandler.result().getInteger("type");
        // exchange found
        if (status == 200) {
          String exchangeID = request.getString("id");
          client.exchangeDelete(exchangeID, rh -> {
            if (rh.succeeded()) {
              logger.info(exchangeID + " adaptor deleted successfully");
              finalResponse.put("id", exchangeID);
              finalResponse.put(Constants.TYPE, "adaptor deletion");
              finalResponse.put(Constants.TITLE, "success");
              finalResponse.put(Constants.DETAIL, "adaptor deleted");
            } else if (rh.failed()) {
              finalResponse.put(Constants.TYPE, "adaptor delete");
              finalResponse.put(Constants.TITLE, "Error in adaptor deletion");
              finalResponse.put(Constants.DETAIL, rh.cause());
              handler.handle(Future.failedFuture("Bad request : nothing to delete"));
            } else {
              logger.error("Something wrong in deleting adaptor" + rh.cause());
              handler.handle(Future.failedFuture("Bad request : nothing to delete"));
            }
            handler.handle(Future.succeededFuture(finalResponse));
          });

        } else if (status == 404) { // exchange not found
          finalResponse.put(Constants.TYPE, status);
          finalResponse.put(Constants.TITLE, resultHandler.result().getString("title"));
          finalResponse.put(Constants.DETAIL, resultHandler.result().getString("detail"));
        } else { // some other issue
          handler.handle(Future.failedFuture("Bad request : nothing to delete"));
        }

      }

      if (resultHandler.failed()) {
        logger.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
        handler.handle(Future.failedFuture("Bad request : nothing to delete"));
      }

    });

    return this;

  }

  /**
   * The listAdaptor implements the list of bindings for an exchange (source). This method has
   * similar functionality as listExchangeSubscribers(JsonObject) method
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService listAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listExchangeSubscribers(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("listAdaptor - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });

    } else {
      finalResponse.put(Constants.ERROR, Constants.EXCHANGE_LIST_ERROR);
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = domain + "/" + getSha(userName) + "/" + request.getString("name");

      Future<JsonObject> resultCreateUser = createUserIfNotExist(userName, Constants.VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {

          // For testing instead of generateRandomPassword() use password = 1234
          String streamingUrl = "amqp://" + userName + ":" + "1234" // generateRandomPassword()
              + "@" + Constants.BROKER_IP + ":" + Constants.BROKER_PORT + "/" + Constants.VHOST_IUDX
              + "/" + queueName;
          logger.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(Constants.ENTITIES);
          logger.info("Request Access for " + entitites);
          logger.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(Constants.QUEUE_NAME, queueName);

          Future<JsonObject> resultqueue = createQueue(requestjson);
          resultqueue.onComplete(resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {

              logger.info("sucess :: Create Queue " + resultHandlerqueue.result());
              JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

              if (createQueueResponse.containsKey(Constants.TITLE) && createQueueResponse
                  .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                logger.error("failed ::" + resultHandlerqueue.cause());
                handler.handle(Future.failedFuture(
                    new JsonObject().put(Constants.ERROR, "Queue Creation Failed").toString()));
              } else {

                logger.info("Success Queue Created");

                for (Object currentEntity : entitites) {
                  String routingKey = (String) currentEntity;
                  logger.info("routingKey is " + routingKey);
                  if (routingKey != null) {
                    if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                        || routingKey.split("/").length != 5) {
                      logger.error("failed :: Invalid (or) NULL routingKey");

                      Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                      resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                        if (resultHandlerDeletequeue.succeeded()) {

                          handler.handle(Future.failedFuture(new JsonObject()
                              .put(Constants.ERROR, "Invalid routingKey").toString()));

                        }
                      });
                    } else {

                      String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                      JsonArray array = new JsonArray();
                      array.add(currentEntity);
                      JsonObject json = new JsonObject();
                      json.put(Constants.EXCHANGE_NAME, exchangeName);
                      json.put(Constants.QUEUE_NAME, queueName);
                      json.put(Constants.ENTITIES, array);

                      Future<JsonObject> resultbind = bindQueue(json);
                      resultbind.onComplete(resultHandlerbind -> {
                        if (resultHandlerbind.succeeded()) {
                          // count++
                          totalBindSuccess += 1;
                          logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                              + resultHandlerbind.result());

                          JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                          if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                              .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                            logger.error("failed ::" + resultHandlerbind.cause());
                            Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                            resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                              if (resultHandlerDeletequeue.succeeded()) {
                                handler.handle(Future.failedFuture(new JsonObject()
                                    .put(Constants.ERROR, "Binding Failed").toString()));
                              }
                            });
                          } else if (totalBindSuccess == totalBindCount) {
                            registerStreamingSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
                                queueName);
                            registerStreamingSubscriptionResponse.put(Constants.STREAMING_URL,
                                streamingUrl);
                            handler.handle(
                                Future.succeededFuture(registerStreamingSubscriptionResponse));
                          }
                        } else if (resultHandlerbind.failed()) {
                          logger.error("failed ::" + resultHandlerbind.cause());
                          Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              handler.handle(Future.failedFuture(new JsonObject()
                                  .put(Constants.ERROR, "Binding Failed").toString()));
                            }
                          });
                        }
                      });
                    }
                  } else {
                    logger.error("failed :: Invalid (or) NULL routingKey");
                    Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                    resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                      if (resultHandlerDeletequeue.succeeded()) {
                        handler.handle(Future.failedFuture(new JsonObject()
                            .put(Constants.ERROR, "Invalid routingKey").toString()));
                      }
                    });
                  }
                }
              }
            } else if (resultHandlerqueue.failed()) {
              logger.error("failed ::" + resultHandlerqueue.cause());
              handler.handle(Future.failedFuture("Queue Creation Failed"));
            }
          });
        }
      });
    } else {
      logger.error("Error in payload");
      handler.handle(Future
          .failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject updateStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = domain + "/" + getSha(userName) + "/" + request.getString("name");

      Future<JsonObject> resultCreateUser = createUserIfNotExist(userName, Constants.VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {

          // For testing instead of generateRandomPassword() use password = 1234
          String streamingUrl = "amqp://" + userName + ":" + "1234" // generateRandomPassword()
              + "@" + Constants.BROKER_IP + ":" + Constants.BROKER_PORT + "/" + Constants.VHOST_IUDX
              + "/" + queueName;
          logger.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(Constants.ENTITIES);
          logger.info("Request Access for " + entitites);
          logger.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(Constants.QUEUE_NAME, queueName);

          Future<JsonObject> deleteQueue = deleteQueue(requestjson);
          deleteQueue.onComplete(deleteQueuehandler -> {
            if (deleteQueuehandler.succeeded()) {
              logger.info("sucess :: Deleted Queue " + deleteQueuehandler.result());

              Future<JsonObject> resultqueue = createQueue(requestjson);
              resultqueue.onComplete(resultHandlerqueue -> {
                if (resultHandlerqueue.succeeded()) {

                  logger.info("sucess :: Create Queue " + resultHandlerqueue.result());
                  JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

                  if (createQueueResponse.containsKey(Constants.TITLE) && createQueueResponse
                      .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                    logger.error("failed ::" + resultHandlerqueue.cause());
                    handler.handle(Future.failedFuture(
                        new JsonObject().put(Constants.ERROR, "Queue Creation Failed").toString()));
                  } else {

                    logger.info("Success Queue Created");

                    for (Object currentEntity : entitites) {
                      String routingKey = (String) currentEntity;
                      logger.info("routingKey is " + routingKey);
                      if (routingKey != null) {
                        if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                            || routingKey.split("/").length != 5) {
                          logger.error("failed :: Invalid (or) NULL routingKey");

                          Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {

                              handler.handle(Future.failedFuture(new JsonObject()
                                  .put(Constants.ERROR, "Invalid routingKey").toString()));

                            }
                          });
                        } else {

                          String exchangeName =
                              routingKey.substring(0, routingKey.lastIndexOf("/"));
                          JsonArray array = new JsonArray();
                          array.add(currentEntity);
                          JsonObject json = new JsonObject();
                          json.put(Constants.EXCHANGE_NAME, exchangeName);
                          json.put(Constants.QUEUE_NAME, queueName);
                          json.put(Constants.ENTITIES, array);

                          Future<JsonObject> resultbind = bindQueue(json);
                          resultbind.onComplete(resultHandlerbind -> {
                            if (resultHandlerbind.succeeded()) {
                              // count++
                              totalBindSuccess += 1;
                              logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                                  + resultHandlerbind.result());

                              JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                              if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                                  .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                                logger.error("failed ::" + resultHandlerbind.cause());
                                Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    handler.handle(Future.failedFuture(new JsonObject()
                                        .put(Constants.ERROR, "Binding Failed").toString()));
                                  }
                                });
                              } else if (totalBindSuccess == totalBindCount) {
                                updateStreamingSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
                                    queueName);
                                updateStreamingSubscriptionResponse.put(Constants.STREAMING_URL,
                                    streamingUrl);
                                handler.handle(
                                    Future.succeededFuture(updateStreamingSubscriptionResponse));
                              }
                            } else if (resultHandlerbind.failed()) {
                              logger.error("failed ::" + resultHandlerbind.cause());
                              Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                              resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                if (resultHandlerDeletequeue.succeeded()) {
                                  handler.handle(Future.failedFuture(new JsonObject()
                                      .put(Constants.ERROR, "Binding Failed").toString()));
                                }
                              });
                            }
                          });
                        }
                      } else {
                        logger.error("failed :: Invalid (or) NULL routingKey");
                        Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                        resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                          if (resultHandlerDeletequeue.succeeded()) {
                            handler.handle(Future.failedFuture(new JsonObject()
                                .put(Constants.ERROR, "Invalid routingKey").toString()));
                          }
                        });
                      }
                    }
                  }
                } else if (resultHandlerqueue.failed()) {
                  logger.error("failed ::" + resultHandlerqueue.cause());
                  handler.handle(Future.failedFuture("Queue Creation Failed"));
                }
              });
            } else if (deleteQueuehandler.failed()) {
              logger.error("failed ::" + deleteQueuehandler.cause());
              handler.handle(Future.failedFuture("Queue Deletion Failed"));
            }
          });
        }
      });
    } else {
      logger.error("Error in payload");
      handler.handle(Future
          .failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService appendStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject appendStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonArray entitites = request.getJsonArray(Constants.ENTITIES);
      logger.info("Request Access for " + entitites);
      logger.info("No of bindings to do : " + entitites.size());

      totalBindCount = entitites.size();
      totalBindSuccess = 0;

      String queueName = request.getString(Constants.SUBSCRIPTION_ID);
      requestjson.put(Constants.QUEUE_NAME, queueName);
      Future<JsonObject> result = listQueueSubscribers(requestjson);
      result.onComplete(resultHandlerqueue -> {
        if (resultHandlerqueue.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandlerqueue.result();
          logger.info(listQueueResponse);
          if (listQueueResponse.containsKey(Constants.TITLE)
              && listQueueResponse.getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
            handler.handle(Future.failedFuture(
                new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
          } else {
            for (Object currentEntity : entitites) {
              String routingKey = (String) currentEntity;
              logger.info("routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                    || routingKey.split("/").length != 5) {
                  logger.error("failed :: Invalid (or) NULL routingKey");

                  handler.handle(Future.failedFuture(
                      new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString()));

                } else {

                  String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                  JsonArray array = new JsonArray();
                  array.add(currentEntity);
                  JsonObject json = new JsonObject();
                  json.put(Constants.EXCHANGE_NAME, exchangeName);
                  json.put(Constants.QUEUE_NAME, queueName);
                  json.put(Constants.ENTITIES, array);

                  Future<JsonObject> resultbind = bindQueue(json);
                  resultbind.onComplete(resultHandlerbind -> {
                    if (resultHandlerbind.succeeded()) {
                      // count++
                      totalBindSuccess += 1;
                      logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());

                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                          .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                        logger.error("failed ::" + resultHandlerbind.cause());
                        handler.handle(Future.failedFuture(
                            new JsonObject().put(Constants.ERROR, "Binding Failed").toString()));
                      } else if (totalBindSuccess == totalBindCount) {
                        appendStreamingSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
                            queueName);
                        appendStreamingSubscriptionResponse.put(Constants.ENTITIES, entitites);
                        handler.handle(Future.succeededFuture(appendStreamingSubscriptionResponse));
                      }
                    } else if (resultHandlerbind.failed()) {
                      logger.error("failed ::" + resultHandlerbind.cause());
                      handler.handle(Future.failedFuture(
                          new JsonObject().put(Constants.ERROR, "Binding Failed").toString()));
                    }
                  });
                }
              } else {
                logger.error("failed :: Invalid (or) NULL routingKey");
                Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                  if (resultHandlerDeletequeue.succeeded()) {
                    handler.handle(Future.failedFuture(
                        new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString()));
                  }
                });
              }
            }
          }
        } else {
          logger.error("Error in payload");
          handler.handle(Future
              .failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
        }
      });
    } else {
      logger.error("Error in payload");
      handler.handle(Future
          .failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject deleteStreamingSubscription = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(Constants.SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(Constants.QUEUE_NAME, queueName);
      Future<JsonObject> result = deleteQueue(requestBody);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();

          if (deleteQueueResponse.containsKey(Constants.TITLE) && deleteQueueResponse
              .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
            logger.info("failed :: Response is " + deleteQueueResponse);
            handler.handle(Future.failedFuture(deleteQueueResponse.toString()));
          } else {
            deleteStreamingSubscription.put(Constants.SUBSCRIPTION_ID, queueName);
            handler.handle(Future.succeededFuture(deleteStreamingSubscription));
          }
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(
              new JsonObject().put(Constants.ERROR, Constants.QUEUE_DELETE_ERROR).toString()));
        }
      });
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(Constants.SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(Constants.QUEUE_NAME, queueName);
      Future<JsonObject> result = listQueueSubscribers(requestBody);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          JsonObject listQueueResponse = (JsonObject) resultHandler.result();

          if (listQueueResponse.containsKey(Constants.TITLE)
              && listQueueResponse.getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
            logger.info("failed :: Response is " + listQueueResponse);
            handler.handle(Future.failedFuture(listQueueResponse.toString()));
          } else {
            logger.info(listQueueResponse);
            handler.handle(Future.succeededFuture(listQueueResponse));
          }
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(
              new JsonObject().put(Constants.ERROR, Constants.QUEUE_LIST_ERROR).toString()));
        }
      });
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject registerCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      
      JsonObject requestjson = new JsonObject();
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID = domain + "/" + getSha(userName) + "/" + request.getString("name");
      JsonObject publishjson = new JsonObject();
      publishjson.put("subscriptionID", subscriptionID);
      publishjson.put("operation", "create");

      OffsetDateTime dateTime = OffsetDateTime.now();
      String callbackURL = request.getString("callbackURL");
      String queueName = request.getString("queue");
      JsonArray entitites = request.getJsonArray("entities");
      totalBindCount = entitites.size();
      totalBindSuccess = 0;
      requestjson.put(Constants.QUEUE_NAME, queueName);

      for (Object currentEntity : entitites) {
        String routingKey = (String) currentEntity;
        logger.info("routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            logger.error("failed :: Invalid (or) NULL routingKey");

            registerCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
            handler.handle(Future.succeededFuture(registerCallbackSubscriptionResponse));

          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(currentEntity);
            JsonObject json = new JsonObject();
            json.put(Constants.EXCHANGE_NAME, exchangeName);
            json.put(Constants.QUEUE_NAME, queueName);
            json.put(Constants.ENTITIES, array);

            Future<JsonObject> resultbind = bindQueue(json);
            resultbind.onComplete(resultHandlerbind -> {
              if (resultHandlerbind.succeeded()) {
                // count++
                totalBindSuccess += 1;
                logger.info(
                    "sucess :: totalBindSuccess " + totalBindSuccess + resultHandlerbind.result());

                JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                    .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {

                  logger.error("failed ::" + resultHandlerbind.cause());
                  pgclient.preparedQuery("Delete from registercallback WHERE subscriptionID = $1 ")
                      .execute(Tuple.of(subscriptionID), resulhandlerdel -> {
                        if (resulhandlerdel.succeeded()) {
                          registerCallbackSubscriptionResponse.put(Constants.ERROR,
                              "Binding Failed");
                          handler
                              .handle(Future.succeededFuture(registerCallbackSubscriptionResponse));
                          // handler.handle(Future.failedFuture(new JsonObject() ---not working
                          // .put(Constants.ERROR, "Binding Failed").toString()));
                        }
                      });
                } else if (totalBindSuccess == totalBindCount) {
                  pgclient.preparedQuery(
                      "INSERT INTO registercallback (subscriptionID  ,callbackURL ,entities ,start_time , end_time , frequency ) VALUES ($1, $2, $3, $4, $5, $6)")
                      .execute(Tuple.of(subscriptionID, callbackURL, entitites, dateTime, dateTime,
                          dateTime), ar -> {
                            if (ar.succeeded()) {
                              String exchangename = "callback.notification";
                              String routing_key = "create";

                              JsonObject jsonpg = new JsonObject();
                              jsonpg.put("body", publishjson.toString());

                              client.basicPublish(exchangename, routing_key, jsonpg,
                                  resultHandler -> {
                                    if (resultHandler.succeeded()) {
                                      registerCallbackSubscriptionResponse.put("subscriptionID",
                                          subscriptionID);
                                      logger.info("Message published to queue");
                                    } else {
                                      pgclient.preparedQuery(
                                          "Delete from registercallback WHERE subscriptionID = $1 ")
                                          .execute(Tuple.of(subscriptionID), deletepg -> {
                                            if (deletepg.succeeded()) {
                                              registerCallbackSubscriptionResponse
                                                  .put("messagePublished", "failed");
                                            }
                                          });
                                    }
                                    handler.handle(Future
                                        .succeededFuture(registerCallbackSubscriptionResponse));
                                  });

                            } else {
                              logger.error("failed ::" + ar.cause().getMessage());

                              pgclient
                                  .preparedQuery(
                                      "Delete from registercallback WHERE subscriptionID = $1 ")
                                  .execute(Tuple.of(subscriptionID), resultHandlerDeletequeuepg -> {
                                    if (resultHandlerDeletequeuepg.succeeded()) {
                                      registerCallbackSubscriptionResponse.put(Constants.ERROR,
                                          "duplicate key value violates unique constraint");
                                      handler.handle(Future
                                          .succeededFuture(registerCallbackSubscriptionResponse));
                                      // handler.handle(Future.failedFuture(new JsonObject()
                                      // --not working
                                      // .put(Constants.ERROR, ar.cause()).toString()));
                                    }
                                  });
                            }
                            pgclient.close();
                          });
                }
              } else if (resultHandlerbind.failed()) {
                logger.error("failed ::" + resultHandlerbind.cause());

                pgclient.preparedQuery("Delete from registercallback WHERE subscriptionID = $1 ")
                    .execute(Tuple.of(subscriptionID), resultDeletequeuepg -> {
                      if (resultDeletequeuepg.succeeded()) {

                        registerCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                        handler
                            .handle(Future.succeededFuture(registerCallbackSubscriptionResponse));
                        // handler.handle(Future.failedFuture(
                        // new JsonObject().put(Constants.ERROR, "Binding Failed").toString()));
                        // --not working
                      }
                    });
              }
            });
          }
        } else {
          logger.error("failed :: Invalid (or) NULL routingKey");

          pgclient.preparedQuery("Delete from registercallback WHERE subscriptionID = $1 ")
              .execute(Tuple.of(subscriptionID), resultHandlerDeletequeuepg -> {
                if (resultHandlerDeletequeuepg.succeeded()) {

                  // handler.handle(Future.failedFuture(
                  // new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString()));
                  // --not working
                  registerCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
                  handler.handle(Future.succeededFuture(registerCallbackSubscriptionResponse));
                }
              });
        }
      }

    } else {
      logger.error("Error in payload");
      // handler.handle(
      // Future.failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
      // -- not working
      registerCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.succeededFuture(registerCallbackSubscriptionResponse));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject updateCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonObject requestjson = new JsonObject();
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID = domain + "/" + getSha(userName) + "/" + request.getString("name");
      JsonObject publishjson = new JsonObject();
      publishjson.put("subscriptionID", subscriptionID);
      publishjson.put("operation", "update");
      String queueName = request.getString("queue");
      JsonArray entitites = request.getJsonArray("entities");
      totalBindCount = entitites.size();
      totalBindSuccess = 0;
      requestjson.put(Constants.QUEUE_NAME, queueName);

      for (Object currentEntity : entitites) {
        String routingKey = (String) currentEntity;
        logger.info("routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            logger.error("failed :: Invalid (or) NULL routingKey");

            Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
            resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
              if (resultHandlerDeletequeue.succeeded()) {
                // handler.handle(Future.failedFuture(
                // new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString())); --not
                // working
                updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
                handler.handle(Future.succeededFuture(updateCallbackSubscriptionResponse));
              }
            });
          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(currentEntity);
            JsonObject json = new JsonObject();
            json.put(Constants.EXCHANGE_NAME, exchangeName);
            json.put(Constants.QUEUE_NAME, queueName);
            json.put(Constants.ENTITIES, array);
            Future<JsonObject> resultbind = bindQueue(json);
            resultbind.onComplete(resultHandlerbind -> {
              if (resultHandlerbind.succeeded()) {
                // count++
                totalBindSuccess += 1;
                logger.info(
                    "sucess :: totalBindSuccess " + totalBindSuccess + resultHandlerbind.result());
                JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                    .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                  logger.error("failed ::" + resultHandlerbind.cause());
                  Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                  resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                    if (resultHandlerDeletequeue.succeeded()) {
                      updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                      handler.handle(Future.succeededFuture(updateCallbackSubscriptionResponse));
                      // handler.handle(Future.failedFuture(new JsonObject() ---not working
                      // .put(Constants.ERROR, "Binding Failed").toString()));

                    }
                  });
                } else if (totalBindSuccess == totalBindCount) {
                  pgclient
                      .preparedQuery(
                          " UPDATE registercallback SET entities = $1 WHERE subscriptionID = $2")
                      .execute(Tuple.of(entitites, subscriptionID), ar -> {
                        if (ar.succeeded()) {
                          String exchangename = "callback.notification";
                          String routing_key = "update";

                          JsonObject jsonpg = new JsonObject();
                          jsonpg.put("body", publishjson.toString());

                          client.basicPublish(exchangename, routing_key, jsonpg, resultHandler -> {

                            if (resultHandler.succeeded()) {

                              updateCallbackSubscriptionResponse.put("subscriptionID",
                                  subscriptionID);
                              logger.info("Message published to queue");

                            } else {
                              logger.info("Message published failed");
                              Future<JsonObject> resultDeletequeuepub = deleteQueue(requestjson);
                              resultDeletequeuepub.onComplete(resultHandlerDeletequeuepub -> {
                                if (resultHandlerDeletequeuepub.succeeded()) {
                                }
                              });
                              updateCallbackSubscriptionResponse.put("messagePublished", "failed");

                            }
                            handler
                                .handle(Future.succeededFuture(updateCallbackSubscriptionResponse));
                          });

                        } else {
                          logger.error("failed ::" + ar.cause().getMessage());
                          Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {

                              updateCallbackSubscriptionResponse.put(Constants.ERROR,
                                  "duplicate key value violates unique constraint");
                              handler.handle(
                                  Future.succeededFuture(updateCallbackSubscriptionResponse));
                              // handler.handle(Future.failedFuture(new JsonObject() --not
                              // working
                              // .put(Constants.ERROR, ar.cause()).toString()));

                            }
                          });

                        }
                        pgclient.close();
                      });
                }
              } else if (resultHandlerbind.failed()) {
                logger.error("failed ::" + resultHandlerbind.cause());
                Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                  if (resultHandlerDeletequeue.succeeded()) {
                    updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                    handler.handle(Future.succeededFuture(updateCallbackSubscriptionResponse));
                    // handler.handle(Future.failedFuture(
                    // new JsonObject().put(Constants.ERROR, "Binding Failed").toString())); --not
                    // working

                  }
                });
              }
            });
          }
        } else {
          logger.error("failed :: Invalid (or) NULL routingKey");
          Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
            if (resultHandlerDeletequeue.succeeded()) {
              // handler.handle(Future.failedFuture(
              // new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString())); --not
              // working
              updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
              handler.handle(Future.succeededFuture(updateCallbackSubscriptionResponse));

            }
          });
        }
      }

    } else {
      logger.error("Error in payload");
      // handler.handle(
      // Future.failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
      // -- not working
      updateCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.succeededFuture(updateCallbackSubscriptionResponse));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject deleteCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonObject requestjson = new JsonObject();
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID = domain + "/" + getSha(userName) + "/" + request.getString("name");
      JsonObject publishjson = new JsonObject();
      publishjson.put("subscriptionID", subscriptionID);
      publishjson.put("operation", "delete");
      String queueName = request.getString("queue");
      JsonArray entitites = request.getJsonArray("entities");
      totalUnBindCount = entitites.size();
      totalUnBindSuccess = 0;
      requestjson.put(Constants.QUEUE_NAME, queueName);

      for (Object currentEntity : entitites) {
        String routingKey = (String) currentEntity;
        logger.info("routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            logger.error("failed :: Invalid (or) NULL routingKey");
            // handler.handle(Future.failedFuture(
            // new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString())); --not
            // working
            deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
            handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));

          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(currentEntity);
            JsonObject json = new JsonObject();
            json.put(Constants.EXCHANGE_NAME, exchangeName);
            json.put(Constants.QUEUE_NAME, queueName);
            json.put(Constants.ENTITIES, array);
            Future<JsonObject> resultbind = unbindQueue(json);
            resultbind.onComplete(resultHandlerbind -> {
              if (resultHandlerbind.succeeded()) {
                // count++
                totalUnBindSuccess += 1;
                logger.info("sucess :: totalUnBindSuccess " + totalUnBindSuccess
                    + resultHandlerbind.result());
                JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                    .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                  logger.error("failed ::" + resultHandlerbind.cause());
                  deleteCallbackSubscriptionResponse.put(Constants.ERROR, "UnBinding Failed");
                  handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
                  // handler.handle(Future.failedFuture(new JsonObject() ---not working
                  // .put(Constants.ERROR, "Binding Failed").toString()));

                } else if (totalUnBindSuccess == totalUnBindCount) {
                  pgclient.preparedQuery("Delete from registercallback WHERE subscriptionID = $1 ")
                      .execute(Tuple.of(subscriptionID), ar -> {
                        if (ar.succeeded()) {
                          String exchangename = "callback.notification";
                          String routing_key = "delete";
                          JsonObject jsonpg = new JsonObject();
                          jsonpg.put("body", publishjson.toString());
                          client.basicPublish(exchangename, routing_key, jsonpg, resultHandler -> {
                            if (resultHandler.succeeded()) {
                              deleteCallbackSubscriptionResponse.put("subscriptionID",
                                  subscriptionID);
                              logger.info("Message published to queue");
                            } else {
                              logger.info("Message published failed");
                              deleteCallbackSubscriptionResponse.put("messagePublished", "failed");
                            }
                            handler
                                .handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
                          });
                        } else {
                          logger.error("failed ::" + ar.cause().getMessage());
                          deleteCallbackSubscriptionResponse.put(Constants.ERROR,
                              "duplicate key value violates unique constraint");
                          handler
                              .handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
                          // handler.handle(Future.failedFuture(new JsonObject() --not
                          // working
                          // .put(Constants.ERROR, ar.cause()).toString()));
                        }
                        pgclient.close();
                      });
                }
              } else if (resultHandlerbind.failed()) {
                logger.error("failed ::" + resultHandlerbind.cause());

                deleteCallbackSubscriptionResponse.put(Constants.ERROR, "UnBinding Failed");
                handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
                // handler.handle(Future.failedFuture(
                // new JsonObject().put(Constants.ERROR, "Binding Failed").toString())); --not
                // working
              }
            });
          }
        } else {
          logger.error("failed :: Invalid (or) NULL routingKey");
          Future<JsonObject> resultDeletequeue = deleteQueue(requestjson);
          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
            if (resultHandlerDeletequeue.succeeded()) {
              // handler.handle(Future.failedFuture(
              // new JsonObject().put(Constants.ERROR, "Invalid routingKey").toString())); --not
              // working
              deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
              handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
            }
          });
        }
      }

    } else {
      logger.error("Error in payload");
      // handler.handle(
      // Future.failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
      // -- not working
      deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject listCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID = domain + "/" + getSha(userName) + "/" + request.getString("name");
      pgclient.preparedQuery("SELECT * FROM registercallback WHERE  subscriptionID = $1 ")
          .execute(Tuple.of(subscriptionID), ar -> {
            if (ar.succeeded()) {
              RowSet<Row> result = ar.result();
              /* Iterating Rows for getting entity, callbackurl, username and password */
              for (Row row : result) {

                String subscriptionIDdb = row.getString(0);
                String callBackUrl = row.getString(1);
                JsonArray entities = (JsonArray) row.getValue(2);
                String userNamedb = row.getString(6);
                String passworddb = row.getString(7);
                listCallbackSubscriptionResponse.put("subscriptionID", subscriptionIDdb);
                listCallbackSubscriptionResponse.put("callbackURL", callBackUrl);
                listCallbackSubscriptionResponse.put("entities", entities);
                listCallbackSubscriptionResponse.put("username", userNamedb);
                listCallbackSubscriptionResponse.put("password", passworddb);
              }
              handler.handle(Future.succeededFuture(listCallbackSubscriptionResponse));

            } else {
              listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
              handler.handle(Future.succeededFuture(listCallbackSubscriptionResponse));
            }
          });

    } else {
      logger.error("Error in payload");
      // handler.handle(
      // Future.failedFuture(new JsonObject().put(Constants.ERROR, "Error in payload").toString()));
      // -- not working
      listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.succeededFuture(listCallbackSubscriptionResponse));
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createExchange(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      // calls the common create exchange method
      Future<JsonObject> result = createExchange(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return this;
  }

  /**
   * The createExchange implements the create exchange.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   **/

  Future<JsonObject> createExchange(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      url = "/api/exchanges/" + vhost + "/" + encodedValue(exchangeName);
      JsonObject obj = new JsonObject();
      obj.put(Constants.TYPE, Constants.EXCHANGE_TYPE);
      obj.put(Constants.AUTO_DELETE, false);
      obj.put(Constants.DURABLE, true);
      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.sendJsonObject(obj, ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.EXCHANGE, exchangeName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.EXCHANGE_EXISTS);
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL,
                  Constants.EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES);
            }

            promise.complete(finalResponse);
          }

        } else {
          logger.error("Creation of Exchange failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.EXCHANGE_CREATE_ERROR);
          promise.fail(finalResponse.toString());
        }

      });
    }

    return promise.future();

  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateExchange(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteExchange(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = deleteExchange(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The deleteExchange implements the delete exchange operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deleteExchange(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      url = "/api/exchanges/" + vhost + "/" + encodedValue(exchangeName);
      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.EXCHANGE, exchangeName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.EXCHANGE_NOT_FOUND);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of Exchange failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.EXCHANGE_DELETE_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();

  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listExchangeSubscribers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listExchangeSubscribers(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The listExchangeSubscribers implements the list of bindings for an exchange (source).
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listExchangeSubscribers(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("id");
      // exchangeName = exchangeName.replace("/", "%2F");

      url = "/api/exchanges/" + vhost + "/" + encodedValue(exchangeName) + "/bindings/source";
      HttpRequest<Buffer> webRequest = webClient.get(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();
              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());

                jsonBody.forEach(current -> {
                  JsonObject currentJson = new JsonObject(current.toString());
                  String okey = currentJson.getString("destination");
                  if (finalResponse.containsKey(okey)) {
                    JsonArray obj = (JsonArray) finalResponse.getValue(okey);
                    obj.add(currentJson.getString("routing_key"));
                    finalResponse.put(okey, obj);
                  } else {
                    ArrayList<String> temp = new ArrayList<String>();
                    temp.add(currentJson.getString("routing_key"));
                    finalResponse.put(okey, temp);
                  }
                });
                if (finalResponse.isEmpty()) {
                  finalResponse.put(Constants.TYPE, HttpStatus.SC_NOT_FOUND);
                  finalResponse.put(Constants.TITLE, Constants.FAILURE);
                  finalResponse.put(Constants.DETAIL, Constants.EXCHANGE_NOT_FOUND);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.EXCHANGE_NOT_FOUND);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Exchange failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.EXCHANGE_LIST_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }

    return promise.future();

  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = createQueue(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The createQueue implements the create queue operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> createQueue(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      url = "/api/queues/" + vhost + "/" + encodedValue(queueName);
      JsonObject configProp = new JsonObject();
      configProp.put(Constants.X_MESSAGE_TTL_NAME, Constants.X_MESSAGE_TTL_VALUE);
      configProp.put(Constants.X_MAXLENGTH_NAME, Constants.X_MAXLENGTH_VALUE);
      configProp.put(Constants.X_QUEUE_MODE_NAME, Constants.X_QUEUE_MODE_VALUE);
      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.sendJsonObject(configProp, ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {

            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.QUEUE_ALREADY_EXISTS);
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL,
                  Constants.QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Creation of Queue failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.QUEUE_CREATE_ERROR);
          promise.fail(finalResponse.toString());
        }

      });
    }

    return promise.future();

  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = deleteQueue(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The deleteQueue implements the delete queue operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deleteQueue(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      url = "/api/queues/" + vhost + "/" + encodedValue(queueName);

      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();

            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of Queue failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.QUEUE_DELETE_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService bindQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = bindQueue(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }

    return null;
  }

  /**
   * The bindQueue implements the bind queue to exchange by routing key.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> bindQueue(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    JsonObject requestBody = new JsonObject();

    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      String url = "/api/bindings/" + vhost + "/e/" + encodedValue(exchangeName) + "/q/"
          + encodedValue(queueName);
      for (Object rkey : entities) {
        requestBody.put("routing_key", rkey.toString());
        HttpRequest<Buffer> webRequest = webClient.post(url).basicAuthentication(user, password);
        webRequest.sendJsonObject(requestBody, ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();
              logger.info("Binding " + rkey.toString() + "Success. Status is " + status);
              if (status == HttpStatus.SC_CREATED) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.put(Constants.TYPE, status);
                finalResponse.put(Constants.TITLE, Constants.FAILURE);
                finalResponse.put(Constants.DETAIL, Constants.QUEUE_EXCHANGE_NOT_FOUND);
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            logger.error("Binding of Queue failed" + ar.cause());
            finalResponse.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
            promise.fail(finalResponse.toString());
          }
        });
      }

    }
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService unbindQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = unbindQueue(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });

    }

    return null;
  }

  /**
   * The unbindQueue implements the unbind queue to exchange by routing key.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> unbindQueue(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      for (Object rkey : entities) {

        url = "/api/bindings/" + vhost + "/e/" + encodedValue(exchangeName) + "/q/"
            + encodedValue(queueName) + "/" + encodedValue((String) rkey);

        HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
        webRequest.send(ar -> {

          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();

              if (status == HttpStatus.SC_NO_CONTENT) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.put(Constants.TYPE, status);
                finalResponse.put(Constants.TITLE, Constants.FAILURE);
                finalResponse.put(Constants.DETAIL, Constants.ALL_NOT_FOUND);
              }
            }

            if (rkey == entities.getValue(arrayPos)) {

              promise.complete(finalResponse);
            }
            /*
             * else { responseArray.add(reponse); }
             */
          } else {
            logger.error("Unbinding of Queue failed" + ar.cause());
            finalResponse.put(Constants.ERROR, Constants.QUEUE_BIND_ERROR);
            promise.fail(finalResponse.toString());
          }
        });
      }
    }

    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createvHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = createvHost(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }

    return null;
  }

  /**
   * The createvHost implements the create virtual host operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> createvHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String vhost = request.getString("vHost");
      url = "/api/vhosts/" + encodedValue(vhost);

      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.VHOST_ALREADY_EXISTS);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Creation of vHost failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.VHOST_CREATE_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updatevHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deletevHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = deletevHost(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }

    return null;
  }

  /**
   * The deletevHost implements the delete virtual host operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deletevHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String vhost = request.getString("vHost");
      url = "/api/vhosts/" + encodedValue(vhost);
      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {

        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();

            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.VHOST_NOT_FOUND);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of vHost failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.VHOST_DELETE_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }

    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null) {
      Future<JsonObject> result = listvHost(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The listvHost implements the list of virtual hosts .
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listvHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null) {
      JsonArray vhostList = new JsonArray();
      url = "/api/vhosts";

      HttpRequest<Buffer> webRequest = webClient.get(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();

            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();

              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());
                jsonBody.forEach(current -> {
                  JsonObject currentJson = new JsonObject(current.toString());
                  String vhostName = currentJson.getString("name");

                  vhostList.add(vhostName);

                });
                if (vhostList != null && !vhostList.isEmpty()) {
                  finalResponse.put(Constants.VHOST, vhostList);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.VHOST_NOT_FOUND);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of vHost failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.VHOST_LIST_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();

  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {
    JsonObject dataBrokerQuery = new JsonObject();
    return dataBrokerQuery;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listQueueSubscribers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listQueueSubscribers(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.result().toString()));
        }
      });
    }
    return null;
  }

  /**
   * The listQueueSubscribers implements the list of bindings for a queue.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listQueueSubscribers(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      JsonArray oroutingKeys = new JsonArray();
      url = "/api/queues/" + vhost + "/" + encodedValue(queueName) + "/bindings";
      HttpRequest<Buffer> webRequest = webClient.get(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_OK) {
              Buffer body = response.body();
              if (body != null) {
                JsonArray jsonBody = new JsonArray(body.toString());
                jsonBody.forEach(current -> {
                  JsonObject currentJson = new JsonObject(current.toString());
                  String rkeys = currentJson.getString("routing_key");
                  if (rkeys != null && !rkeys.equalsIgnoreCase(queueName)) {
                    oroutingKeys.add(rkeys);
                  }
                });
                if (oroutingKeys != null && !oroutingKeys.isEmpty()) {
                  finalResponse.put(Constants.ENTITIES, oroutingKeys);
                } else {
                  finalResponse.put(Constants.TYPE, HttpStatus.SC_NOT_FOUND);
                  finalResponse.put(Constants.TITLE, Constants.FAILURE);
                  finalResponse.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put(Constants.TYPE, status);
              finalResponse.put(Constants.TITLE, Constants.FAILURE);
              finalResponse.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Queue failed" + ar.cause());
          finalResponse.put(Constants.ERROR, Constants.QUEUE_LIST_ERROR);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService publishFromAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    JsonObject json = new JsonObject();
    if (request != null && !request.isEmpty()) {
      json.put("body", request.toString());
      String resourceGroupId = request.getString("id");
      String routingKey = resourceGroupId;
      if (resourceGroupId != null && !resourceGroupId.isBlank()) {
        resourceGroupId = resourceGroupId.substring(0, resourceGroupId.lastIndexOf("/"));

        client.basicPublish(resourceGroupId, routingKey, json, resultHandler -> {
          if (resultHandler.succeeded()) {
            finalResponse.put(Constants.STATUS, HttpStatus.SC_OK);
            handler.handle(Future.succeededFuture(finalResponse));
            logger.info("Message published to queue");
          } else {
            finalResponse.put(Constants.TYPE, HttpStatus.SC_BAD_REQUEST);
            logger.error("Message publishing failed");
            resultHandler.cause().printStackTrace();
            handler.handle(Future.failedFuture(resultHandler.result().toString()));
          }
        });
      }
    }
    return null;
  }

  @Override
  public DataBrokerService publishHeartbeat(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject response = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String adaptor = request.getString("id");
      String routingKey = request.getString("status");
      if (adaptor != null && !adaptor.isEmpty() && routingKey != null && !routingKey.isEmpty()) {
        JsonObject json = new JsonObject();
        Future<JsonObject> future1 = getExchange(json.put("id", adaptor));
        future1.onComplete(ar -> {
          if (ar.result().getInteger("type") == HttpStatus.SC_OK) {
            json.put("exchangeName", adaptor);
            // exchange found, now get list of all queues which are bound with this exchange
            Future<JsonObject> future2 = listExchangeSubscribers(json);
            future2.onComplete(rh -> {
              JsonObject queueList = rh.result();
              if (queueList != null && queueList.size() > 0) {
                // now find queues which are bound with given routingKey and publish message
                queueList.forEach(queue -> {
                  // JsonObject obj = new JsonObject();
                  Map.Entry<String, Object> map = queue;
                  String queueName = map.getKey();
                  JsonArray array = (JsonArray) map.getValue();
                  array.forEach(rk -> {
                    if ((rk.toString()).contains(routingKey)) {
                      // routingKey matched. now publish message
                      JsonObject message = new JsonObject();
                      message.put("body", request.toString());
                      client.basicPublish(adaptor, routingKey, message, resultHandler -> {
                        if (resultHandler.succeeded()) {
                          logger.info("publishHeartbeat - message published to queue [ " + queueName
                              + " ] for routingKey [ " + routingKey + " ]");
                          response.put("type", "success");
                          response.put("queueName", queueName);
                          response.put("routingKey", rk.toString());
                          response.put("detail", "routingKey matched");
                        } else {
                          logger.error(
                              "publishHeartbeat - some error in publishing message to queue [ "
                                  + queueName + " ]. cause : " + resultHandler.cause());
                          response.put("messagePublished", "failed");
                          response.put("type", "error");
                          response.put("detail", "routingKey not matched");
                        }
                        handler.handle(Future.succeededFuture(response));
                      });
                    } else {
                      logger.error(
                          "publishHeartbeat - routingKey [ " + routingKey + " ] not matched with [ "
                              + rk.toString() + " ] for queue [ " + queueName + " ]");
                    }
                  });

                });

              } else {
                logger.error(
                    "publishHeartbeat method - Oops !! None queue bound with given exchange");
                handler.handle(Future.failedFuture(
                    "publishHeartbeat method - Oops !! None queue bound with given exchange"));
              }

            });

          } else {
            logger.error("Either adaptor does not exist or some other error to publish message");
            handler.handle(Future.failedFuture(
                "Either adaptor does not exist or some other error to publish message"));
          }

        });
      } else {
        logger.error("publishHeartbeat - adaptor and routingKey not provided to publish message");
        handler.handle(Future.failedFuture(
            "publishHeartbeat - adaptor and routingKey not provided to publish message"));
      }

    } else {
      logger.error("publishHeartbeat - request is null to publish message");
      handler.handle(Future.failedFuture("publishHeartbeat - request is null to publish message"));
    }

    return null;

  }

}

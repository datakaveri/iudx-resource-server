package iudx.resource.server.databroker;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
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
import io.vertx.rabbitmq.RabbitMQClient;

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

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param clientInstance which is a RabbitMQ client
   * @param webClientInstance which is a Vertx Web client
   */

  public DataBrokerServiceImpl(RabbitMQClient clientInstance, WebClient webClientInstance,
      JsonObject propObj) {

    logger.info("Got the RabbitMQ Client instance");
    client = clientInstance;

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
      vhost = URLEncoder.encode(propObj.getString("vHost"));

    }
    webClient = webClientInstance;

  }

  /**
   * This method creates user, declares exchange and bind with predefined queues
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService registerAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject registerResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      // user creation if user does not exist
      String userName = request.getString("consumer");
      if (vhost.equalsIgnoreCase("/")) {
        vhost = encodedValue(vhost);
      }

      if (userName != null && !userName.isBlank() && !userName.isEmpty()) {
        Future<JsonObject> userCreationFuture = createUserIfNotExist(userName, vhost);

        userCreationFuture.onComplete(rh -> {
          if (rh.succeeded()) {
            // createUserIfNotExist_onComplete result set to registerResponse
            JsonObject result = rh.result();
            registerResponse.put("username", result.getString("shaUsername"));
            registerResponse.put("apiKey", "123456");
            // registerResponse.put("apiKey", result.getString("apiKey"));
            // registerResponse.put("type", result.getString("type"));
            // registerResponse.put("title", result.getString("title"));
            // registerResponse.put("detail", result.getString("detail"));
            // registerResponse.put("vhostPermissions", result.getString("vhostPermissions"));

            String uname = rh.result().getString("shaUsername");
            // now declare exchange and bind it with queues
            String adaptorID = request.getString("id");
            if (adaptorID != null && !adaptorID.isBlank() && !adaptorID.isEmpty()) {
              JsonObject json = new JsonObject();
              json.put("exchangeName", adaptorID);

              Future<JsonObject> exchangeDeclareFuture = createExchange(json);
              exchangeDeclareFuture.onComplete(ar -> {
                if (ar.succeeded()) {
                  // ar.result() set to registerResponse after
                  // exchangeDeclareFuture_onComplete
                  JsonObject obj = ar.result();
                  registerResponse.put("id", obj.getString("exchange"));
                  // registerResponse.put("exchangename", obj.getString("exchange"));
                  registerResponse.put("vHost", vhost);
                  // if exchange just registered then set topic permission and bind with queues
                  if (obj.getString("exchange") != null && !obj.getString("exchange").isEmpty()) {
                    Future<JsonObject> topicPermissionFuture =
                        setTopicPermissions(vhost, obj.getString("exchange"), uname);
                    topicPermissionFuture.onComplete(topicHandler -> {
                      if (topicHandler.succeeded()) {
                        logger.info("Write permission set on topic for exchange "
                            + obj.getString("exchange"));

                        /*
                         * registerResponse.put("topic_permissions",
                         * topicHandler.result().getString("topic_permissions"));
                         */

                      } else {
                        logger.info(
                            "topic permissions not set for exchange " + obj.getString("exchange")
                                + " - cause : " + topicHandler.cause().getMessage());
                      }
                    });

                    Future<JsonObject> queueBindFuture = queueBinding(obj.getString("exchange"));
                    queueBindFuture.onComplete(res -> {
                      if (res.succeeded()) {
                        logger.info("Queue_Database, Queue_adaptorLogs binding done with "
                            + obj.getString("exchange"));

                        /*
                         * registerResponse.put("Queue_Database", "Queue_Database" + " bound with "
                         * + obj.getString("exchange")); registerResponse.put("Queue_adaptorLogs",
                         * "Queue_adaptorLogs" + " bound with " + obj.getString("exchange"));
                         */

                      } else {
                        logger
                            .error("error in queue binding with adaptor - cause : " + res.cause());
                      }
                    });

                  } else if (obj.getString("detail") != null && !obj.getString("detail").isEmpty()
                      && obj.getString("detail").equalsIgnoreCase("Exchange already exists")) {
                    // registerResponse.put("exchange_detail", "Exchange already exists");
                    registerResponse.put("id", adaptorID);
                    // registerResponse.put("exchangename", adaptorID);
                  }

                  logger.info("registerResponse : " + registerResponse);
                  handler.handle(Future.succeededFuture(registerResponse));

                } else {
                  handler.handle(Future
                      .failedFuture("something wrong in exchange declaration : " + ar.cause()));
                  logger.error("something wrong in exchange declaration : " + ar.cause());
                }

              });

            } else {
              handler.handle(Future.failedFuture("AdaptorID / Exchange not provided in request"));
              logger.error("AdaptorID / Exchange not provided in request");
            }

          } else if (rh.failed()) {
            handler
                .handle(Future.failedFuture("Something went wrong in user creation " + rh.cause()));
            logger.error("Something went wrong in user creation. " + rh.cause());
          } else {
            handler.handle(Future.failedFuture("User creation failed. " + rh.cause()));
            logger.error("User creation failed. " + rh.cause());
          }

        });

      } else {
        handler.handle(Future.failedFuture("userName not provided in adaptor registration"));
        logger.error("user not provided in adaptor registration");
      }

    } else {
      registerResponse.put("status", "Bad request : insufficient request data to register adaptor");
      handler.handle(
          Future.failedFuture("Bad request : insufficient request data to register adaptor"));
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
    JsonObject response = new JsonObject();
    // Now bind newly created adaptor with queues i.e. adaptorLogs,database
    client.queueBind(Constants.QUEUE_DATA, adaptorID, adaptorID, result -> {
      if (result.succeeded()) {
        response.put("Queue_Database", Constants.QUEUE_DATA + " queue bound to " + adaptorID);
      } else {
        logger.error(" Queue_Database binding error : " + result.cause());
      }
    });

    for (String routingKey : getRoutingKeys()) {
      String rk = adaptorID + routingKey;
      client.queueBind(Constants.QUEUE_ADAPTOR_LOGS, adaptorID, rk, result -> {
        if (result.succeeded()) {
          response.put("Queue_adaptorLogs", " queue bound to " + adaptorID + " with key " + rk);
        } else {
          logger.error(" Queue_adaptorLogs binding error : " + result.cause());
        }
      });
    }

    promise.complete(response);
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
    JsonObject response = new JsonObject();
    Future<JsonObject> future = CreateUserIfNotPresent(userName, vhost);
    future.onComplete(handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result();
        response.put("shaUsername", result.getString("shaUsername"));
        response.put("apiKey", result.getString("apiKey"));
        response.put("type", "" + result.getString("type"));
        response.put("title", result.getString("title"));
        response.put("detail", result.getString("detail"));
        response.put("vhostPermissions", result.getString("vhostPermissions"));
        promise.complete(response);
      } else {
        logger.info("Something went wrong - Cause: " + handler.cause());
        promise.fail(handler.cause().toString());
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
  Future<JsonObject> CreateUserIfNotPresent(String userName, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
    String shaUsername = domain + encodedValue("/") + getSHA(userName);
    url = "/api/users/" + shaUsername;
    HttpRequest<Buffer> request = webClient.get(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();
    request.send(reply -> {
      if (reply.succeeded()) {
        if (reply.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
          logger.info("createUserIfNotExist method : User not found. So creating user .........");
          Future<JsonObject> userCreated = createUser(shaUsername, vhost);
          userCreated.onComplete(handler -> {
            if (handler.succeeded()) {
              JsonObject result = handler.result();
              response.put("shaUsername", result.getString("shaUsername"));
              response.put("apiKey", result.getString("password"));
              response.put("type", "" + result.getString("type"));
              response.put("title", result.getString("title"));
              response.put("detail", result.getString("detail"));
              response.put("vhostPermissions", result.getString("vhostPermissions"));
              promise.complete(response);
            } else {
              logger.error("createUser method onComplete() - Error in user creation. Cause : "
                  + handler.cause());
            }
          });

        } else if (reply.result().statusCode() == HttpStatus.SC_OK) {
          // user exists , So something useful can be done here
          response.put("shaUsername", shaUsername);
          response.put("type", "UserExists");
          response.put("title", "success");
          response.put("detail", "User already exists");
          promise.complete(response);
        }

      } else {
        logger.info("Something went wrong while finding user using mgmt API: " + reply.cause());
        promise.fail(reply.cause().toString());
      }

    });

    return promise.future();

  }

  /**
   * CreateUserIfNotPresent's helper method which creates user if not present
   * 
   * @param userName which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  Future<JsonObject> createUser(String shaUsername, String vhost) {

    Promise<JsonObject> promise = Promise.promise();
    // now creating user using same url with method put
    HttpRequest<Buffer> createUserRequest = webClient.put(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();
    JsonObject arg = new JsonObject();
    arg.put("password", generateRandomPassword());
    arg.put("tags", "None");

    createUserRequest.sendJsonObject(arg, ar -> {
      if (ar.succeeded()) {
        if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
          response.put("shaUsername", shaUsername);
          response.put("password", arg.getString("password"));
          response.put("title", "success");
          response.put("type", "" + ar.result().statusCode());
          response.put("detail", "UserCreated");
          logger.info("createUser method : given user created successfully");
          // set permissions to vhost for newly created user
          Future<JsonObject> vhostPermission = setVhostPermissions(shaUsername, vhost);
          vhostPermission.onComplete(handler -> {
            if (handler.succeeded()) {
              response.put("vhostPermissions", handler.result().getString("vhostPermissions"));
              promise.complete(response);
            } else {
              logger.error("Error in setting vhostPermissions. Cause : " + handler.cause());
            }
          });

        } else {
          promise.fail("createUser method - Some newtork error. cause" + ar.cause());
          logger.error("createUser method - Some newtork error. cause" + ar.cause());
        }
      } else {
        promise.fail(ar.cause().toString());
        logger.info("Something went wrong while creating user using mgmt API :" + ar.cause());
      }
    });

    return promise.future();
  }

  /**
   * set topic permissions
   * 
   * @param vhost which is a String
   * @param adaptorID which is a String
   * @param shaUsername which is a String
   * @return response which is a Future object of promise of Json type
   **/
  private Future<JsonObject> setTopicPermissions(String vhost, String adaptorID,
      String shaUsername) {
    Promise<JsonObject> promise = Promise.promise();
    // now set write permission to user for this adaptor(exchange)
    url = "/api/topic-permissions/" + vhost + "/" + shaUsername;
    HttpRequest<Buffer> request = webClient.put(url).basicAuthentication(user, password);
    JsonObject response = new JsonObject();
    JsonObject param = new JsonObject();
    // set all mandatory fields
    param.put("exchange", adaptorID);
    param.put("write", ".*");
    param.put("read", "");
    request.sendJsonObject(param, result -> {
      if (result.succeeded()) {
        if (result.result().statusCode() == HttpStatus.SC_CREATED) {
          response.put("topic_permissions", "topic permission set");
          promise.complete(response);
        } else if (result.result().statusCode() == HttpStatus.SC_NO_CONTENT) {
          response.put("topic_permissions", "already set");
        } else {
          logger.error("Error in setting Topic permissions" + result.result().statusMessage());
          promise.fail(result.result().statusMessage());
        }
      } else {
        logger.error("Error in setting topic permission : " + result.cause());
        promise.fail("Error in setting topic permission. cause : " + result.cause());
      }
    });

    return promise.future();
  }

  /**
   * set vhost permissions for given userName
   * 
   * @param shaUsername which is a String
   * @param vhost which is a String
   * @return response which is a Future object of promise of Json type
   **/
  private Future<JsonObject> setVhostPermissions(String shaUsername, String vhost) {
    // set permissions for this user
    Promise<JsonObject> promise = Promise.promise();
    JsonObject vhostPermissionResponse = new JsonObject();
    url = "/api/permissions/" + vhost + "/" + shaUsername;
    // now set all mandatory permissions for given vhost for newly created user
    HttpRequest<Buffer> vhostPermissionRequest =
        webClient.put(url).basicAuthentication(user, password);
    JsonObject vhostPermissions = new JsonObject();
    // all keys are mandatory. empty strings used for configure,read as not permitted.
    vhostPermissions.put("configure", "");
    vhostPermissions.put("write", ".*");
    vhostPermissions.put("read", "");
    vhostPermissionRequest.sendJsonObject(vhostPermissions, handler -> {
      if (handler.succeeded()) {
        if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
          vhostPermissionResponse.put("vhostPermissions", "write permission set");
          logger.info(
              "write permission set for user [ " + shaUsername + " ] in vHost [ " + vhost + "]");
          promise.complete(vhostPermissionResponse);
        } else {
          promise.fail(handler.cause().toString());
          logger.error("Error in write permission set for user [ " + shaUsername + " ] in vHost [ "
              + vhost + " ]");
        }
      } else {
        promise.fail(handler.cause().toString());
        logger.error("Error in setting permission to vhost : " + handler.cause());
      }
    });

    return promise.future();
  }

  /**
   * encode string using URLEncoder's encode method
   * 
   * @param vhost which is a String
   * @return encoded_vhost which is a String
   **/
  private String encodedValue(String vhost) {
    String encoded_vhost = null;
    try {
      encoded_vhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      logger.error("Error in encode vhost name :" + ex.getCause());
    }
    return encoded_vhost;
  }

  /**
   * This method is as simple as but it can have more sophisticated encryption logic.
   * 
   * @param plainUserName which is a String
   * @return encodedValue which is a String
   **/
  private String getSHA(String plainUserName) {

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
   * This method generate random alphanumeric password of given PASSWORD_LENGTH
   **/
  private String generateRandomPassword() {
    // It is simple one. here we may more strong algorith for password generation.
    return org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(Constants.PASSWORD_LENGTH);
  }

  /**
   * read routingKeys values from properties file
   * 
   * @return arrayList of routingKeys
   **/
  private ArrayList<String> getRoutingKeys() {
    ArrayList<String> arrayList = new ArrayList<String>();
    // get routingKeys from properties file
    try {
      Properties properties = new Properties();
      properties.load(new FileInputStream("adaptorRoutingKeys.properties"));
      for (int i = 1; i <= 10; i++) {
        String rk = properties.getProperty("routingKey_" + i);
        if (rk != null && !rk.isEmpty() && !rk.isBlank())
          arrayList.add(rk);
      }
    } catch (Exception ex) {
      logger.info("Eception in getting routingKeys" + ex.toString());
    }

    return arrayList;
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
    return null;
  }

  /*
   * overridden method
   */
  Future<JsonObject> getExchange(JsonObject request) {
    JsonObject response = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("id");
      if (vhost.contains("/")) {
        url = "/api/exchanges/" + encodedValue(vhost) + "/" + exchangeName;
      } else {
        url = "/api/exchanges/" + vhost + "/" + exchangeName;
      }

      HttpRequest<Buffer> isExchangeExist = webClient.get(url).basicAuthentication(user, password);
      isExchangeExist.send(result -> {
        if (result.succeeded()) {
          int status = result.result().statusCode();
          response.put("type", status);
          if (status == HttpStatus.SC_OK) {
            response.put("title", "success");
            response.put("detail", "Exchange found");
          } else if (status == HttpStatus.SC_NOT_FOUND) {
            response.put("title", "Failure");
            response.put("detail", "Exchange not found");
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

    JsonObject deleteResponse = new JsonObject();

    Future<JsonObject> result = getExchange(request);
    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        int status = resultHandler.result().getInteger("type");
        if (status == 200) {// exchange found
          String exchangeID = request.getString("id");
          client.exchangeDelete(exchangeID, rh -> {
            if (rh.succeeded()) {
              logger.info(exchangeID + " adaptor deleted successfully");
              deleteResponse.put("id", exchangeID);
              deleteResponse.put("type", "adaptor deletion");
              deleteResponse.put("title", "success");
              deleteResponse.put("detail", "adaptor deleted");
            } else if (rh.failed()) {
              deleteResponse.put("type", "adaptor delete");
              deleteResponse.put("title", "Error in adaptor deletion");
              deleteResponse.put("detail", rh.cause());
              handler.handle(Future.failedFuture("Bad request : nothing to delete"));
            } else {
              logger.error("Something wrong in deleting adaptor" + rh.cause());
              handler.handle(Future.failedFuture("Bad request : nothing to delete"));
            }
            handler.handle(Future.succeededFuture(deleteResponse));
          });

        } else if (status == 404) { // exchange not found
          deleteResponse.put("type", status);
          deleteResponse.put("title", resultHandler.result().getString("title"));
          deleteResponse.put("detail", resultHandler.result().getString("detail"));
        } else { // some other issue
          handler.handle(Future.failedFuture("Bad request : nothing to delete"));
        }

      }

      if (resultHandler.failed()) {
        logger.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
        handler.handle(Future.failedFuture("Bad request : nothing to delete"));
      }

    });

    return null;

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
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listExchangeSubscribers(request);

      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("listAdaptor - resultHandler failed : " + resultHandler.cause());
        }
      });

    } else {
      handler.handle(Future.failedFuture("listAdaptor - Exchange not provided in request"));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

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
        }
      });
    }
    return null;
  }


  /**
   * The createExchange implements the create exchange.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   **/

  Future<JsonObject> createExchange(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      url = "/api/exchanges/" + vhost + "/" + exchangeName;
      JsonObject obj = new JsonObject();
      obj.put("type", Constants.EXCHANGE_TYPE);
      obj.put("auto_delete", false);
      obj.put("durable", true);
      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.sendJsonObject(obj, ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put("exchange", exchangeName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Exchange already exists");
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Exchange already exists with different properties");
            }

            promise.complete(finalResponse);
          }

        } else {
          logger.error("Creation of Exchange failed" + ar.cause());
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
    finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      url = "/api/exchanges/" + vhost + "/" + exchangeName;
      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("exchange", exchangeName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Exchange does not exist");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of Exchange failed" + ar.cause());
          promise.fail(ar.cause().toString());
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
    finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");

      url = "/api/exchanges/" + vhost + "/" + exchangeName + "/bindings/source";
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
                  finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
                  finalResponse.put("title", "Failure");
                  finalResponse.put("detail", "Exchange does not exist");
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Exchange does not exist");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Exchange failed" + ar.cause());
          promise.fail(ar.cause().toString());
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
    finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      url = "/api/queues/" + vhost + "/" + queueName;
      JsonObject configProp = new JsonObject();
      configProp.put("x-message-ttl", Constants.X_MESSAGE_TTL);
      configProp.put("x-max-length", Constants.X_MAXLENGTH);
      configProp.put("x-queue-mode", Constants.X_QUEQUE_MODE);
      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.sendJsonObject(configProp, ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {

            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put("queue", queueName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue already exists");
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue already exists with different properties");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Creation of Queue failed" + ar.cause());
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
    finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      url = "/api/queues/" + vhost + "/" + queueName;

      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();

            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("queue", queueName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue does not exist");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of Queue failed" + ar.cause());
          promise.fail(ar.cause().toString());
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
    finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      url = "/api/bindings/" + vhost + "/e/" + exchangeName + "/q/" + queueName;

      for (Object rkey : entities) {
        requestBody.put("routing_key", rkey.toString());
        HttpRequest<Buffer> webRequest = webClient.post(url).basicAuthentication(user, password);
        webRequest.sendJsonObject(requestBody, ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();

              if (status == HttpStatus.SC_CREATED) {
                finalResponse.put("exchange", exchangeName);
                finalResponse.put("queue", queueName);
                finalResponse.put("entities", entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.put("type", status);
                finalResponse.put("title", "Failure");
                finalResponse.put("detail", "Queue/Exchange does not exist");
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            logger.error("Binding of Queue failed" + ar.cause());
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
    finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      for (Object rkey : entities) {

        finalResponse = new JsonObject();
        url = "/api/bindings/" + vhost + "/e/" + exchangeName + "/q/" + queueName + "/" + rkey;
        HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
        webRequest.send(ar -> {

          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();

              if (status == HttpStatus.SC_NO_CONTENT) {
                finalResponse.put("exchange", exchangeName);
                finalResponse.put("queue", queueName);
                finalResponse.put("entities", entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.put("type", status);
                finalResponse.put("title", "Failure");
                finalResponse.put("detail", "Queue/Exchange/Routing Key does not exist");
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
            promise.fail(ar.cause().toString());
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
    finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String vhost = request.getString("vHost");
      url = "/api/vhosts/" + vhost;

      HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
      webRequest.send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put("vHost", vhost);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "vHost already exists");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Creation of vHost failed" + ar.cause());
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
    finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String vhost = request.getString("vHost");
      url = "/api/vhosts/" + vhost;
      HttpRequest<Buffer> webRequest = webClient.delete(url).basicAuthentication(user, password);
      webRequest.send(ar -> {

        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();

            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("vHost", vhost);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue does not exist");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Deletion of Queue failed" + ar.cause());
          promise.fail(ar.cause().toString());
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
    finalResponse = new JsonObject();
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
                  finalResponse.put("vHost", vhostList);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "No vhosts found");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Queue failed" + ar.cause());
          promise.fail(ar.cause());
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
    finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      JsonArray oroutingKeys = new JsonArray();
      url = "/api/queues/" + vhost + "/" + queueName + "/bindings";
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
                  finalResponse.put("entities", oroutingKeys);
                } else {
                  finalResponse.put("type", HttpStatus.SC_NOT_FOUND);
                  finalResponse.put("title", "Failure");
                  finalResponse.put("detail", "Queue does not exist");
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.put("type", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue does not exist");
            }
          }
          promise.complete(finalResponse);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Queue failed" + ar.cause());
          promise.fail(ar.cause());
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
    finalResponse = new JsonObject();
    JsonObject json = new JsonObject();
    if (request != null && !request.isEmpty()) {
      json.put("body", request.toString());
      String resourceGroupId = request.getString("id");
      String routingKey = resourceGroupId;
      if (resourceGroupId != null && !resourceGroupId.isBlank()) {
        resourceGroupId = resourceGroupId.substring(0, resourceGroupId.lastIndexOf("/"));

        client.basicPublish(resourceGroupId, routingKey, json, resultHandler -> {
          if (resultHandler.succeeded()) {
            finalResponse.put("status", HttpStatus.SC_OK);
            handler.handle(Future.succeededFuture(finalResponse));
            logger.info("Message published to queue");
          } else {
            finalResponse.put("type", HttpStatus.SC_BAD_REQUEST);
            logger.error("Message publishing failed");
            resultHandler.cause().printStackTrace();
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

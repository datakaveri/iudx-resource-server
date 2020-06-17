package iudx.resource.server.databroker;

import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpRequest;
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
  private RabbitMQClient rabbitmqClient;
  private String uri;
  private WebClient webClient;
  private JsonObject jsonResponse;
  private String user;
  private String password;
  private String vhost;
  private Properties properties;
  private String encoded_vhost;

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param clientInstance which is a RabbitMQ client
   */

  public DataBrokerServiceImpl(RabbitMQClient clientInstance, WebClient webClientInstance,
      JsonObject propObj) {

    logger.info("Got the RabbitMQ Client instance");
    rabbitmqClient = clientInstance;

    rabbitmqClient.start(resultHandler -> {
      if (resultHandler.succeeded()) {
        logger.info("RabbitMQ Client Connected");

      } else {
        logger.info("RabbitMQ Client Not Connected");
      }
    });

    webClient = webClientInstance;
    user = propObj.getString("userName");
    password = propObj.getString("password");
    vhost = propObj.getString("vHost");

  }

  /**
  *
  */
  @Override
  public DataBrokerService registerAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {
      jsonResponse = new JsonObject();
      String userName = request.getString("consumer");
      if (userName != null && !userName.isBlank() && !userName.isEmpty()) {
        createUserIfNotExist(userName);
      } else {
        logger.error("user not provided in adaptor registration");
      }

      String adaptorID = request.getString("id");
      // stage 1.1) and 1.2 implementation. stage 1.3 is already handled in APIServerVerticle.
      rabbitmqClient.exchangeDeclare(adaptorID, Constants.EXCHANGE_TYPE,
          Constants.EXCHANGE_DURABLE_TRUE, Constants.EXCHANGE_AUTODELETE_FALSE, ar -> {
            StringBuilder status = new StringBuilder();
            if (ar.succeeded()) {
              logger.info(adaptorID + " adaptor declared successfully");
              jsonResponse.put("id", adaptorID);
              jsonResponse.put("vHost", vhost);
              status.append("AdaptorID : " + adaptorID);
              // stage 2.Now bind newly created adaptor with queues i.e. adaptorLogs,database
              rabbitmqClient.queueBind(Constants.QUEUE_DATA, adaptorID, adaptorID, result -> {
                if (result.succeeded()) {
                  logger.info(Constants.QUEUE_DATA + " queue bound to " + adaptorID);
                  status.append(", " + Constants.QUEUE_DATA + " queue bound to " + adaptorID);
                } else {
                  logger.error(Constants.QUEUE_DATA + " queue not bound to " + adaptorID);
                  status.append(", " + Constants.QUEUE_DATA + " queue not bound to " + adaptorID);
                }
              });

              for (String routingKey : getRoutingKeys()) {
                String rk = adaptorID + routingKey;
                rabbitmqClient.queueBind(Constants.QUEUE_ADAPTOR_LOGS, adaptorID, rk, result -> {
                  if (result.succeeded()) {
                    logger.info(Constants.QUEUE_ADAPTOR_LOGS + " queue bound to " + adaptorID
                        + " with key " + rk);
                    status.append(", " + Constants.QUEUE_ADAPTOR_LOGS + " queue bound to "
                        + adaptorID + " with key " + rk);
                  } else {
                    logger.error(Constants.QUEUE_ADAPTOR_LOGS + " queue not bound to " + adaptorID
                        + " with key " + rk);
                    status.append(", " + Constants.QUEUE_ADAPTOR_LOGS + " queue not bound to "
                        + adaptorID + " with key " + rk);
                  }
                });
              }


            } else {
              logger.error("Adaptor registration failed" + ar.cause().getLocalizedMessage());
              status.append("AdaptorID : " + "Failure");
            }

            jsonResponse.put("status", status);
            logger.info("Status at end of registerAdaptor method : " + status);
            handler.handle(Future.succeededFuture(jsonResponse));
          });

    } else {
      jsonResponse.put("status", "Bad request : insufficient data to register adaptor");
      handler.handle(Future.failedFuture("Bad request : insufficient data to register adaptor"));
    }

    return null;

  }

  private void createUserIfNotExist(String userName) {
    uri = "/api/users/" + userName;
    HttpRequest<Buffer> isUserExistsRequest =
        webClient.get(uri).basicAuthentication(user, password);
    isUserExistsRequest.send(result -> {
      if (result.succeeded()) {
        if (result.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
          logger.info("createUserIfNotExist method : User not found. So creating user .........");
          // now creating user using same uri with method put
          HttpRequest<Buffer> createUserRequest =
              webClient.put(uri).basicAuthentication(user, password);
          JsonObject requestBody = new JsonObject();
          requestBody.put("password", generateRandomPassword());
          requestBody.put("tags", "None");

          createUserRequest.sendJsonObject(requestBody, ar -> {
            if (ar.succeeded()) {
              if (ar.result().statusCode() == HttpStatus.SC_CREATED) {
                jsonResponse.put("apiKey", requestBody.getString("password"));
                jsonResponse.put("type", "UserCreated");
                jsonResponse.put("title", "success");
                logger.info("createUserIfNotExist method : given user created successfully");
                // set permissions for this user
                if (vhost.equalsIgnoreCase("/")) {
                  encoded_vhost = encode_vhost(vhost);
                  uri = "/api/permissions/" + encoded_vhost + "/" + userName;
                } else {
                  uri = "/api/permissions/" + vhost + "/" + userName;
                }
                // now set all mandatory permissions for given vhost for newly created user
                HttpRequest<Buffer> vhostPermissionRequest =
                    webClient.put(uri).basicAuthentication(user, password);
                JsonObject vhostPermissions = new JsonObject();
                // all keys are mandatory. empty strings used for configure,read as not permitted.
                vhostPermissions.put("configure", "");
                vhostPermissions.put("write", ".*");
                vhostPermissions.put("read", "");
                vhostPermissionRequest.sendJsonObject(vhostPermissions, handler -> {
                  if (handler.succeeded()) {
                    if (handler.result().statusCode() == HttpStatus.SC_CREATED) {
                      logger
                          .info("permission set for newly created user in vHost [ " + vhost + "]");
                    } else {
                      logger.error("Error in permission set for newly created user in vHost ["
                          + vhost + "]");
                    }
                  } else {
                    logger.error("Error in setting permission handler: " + handler.cause());
                  }
                });


                // now set write permission to user for this adaptor(exchange)
                if (vhost.equalsIgnoreCase("/")) {
                  encoded_vhost = encode_vhost(vhost);
                  uri = "/api/topic-permissions/" + encoded_vhost + "/" + userName;
                } else {
                  uri = "/api/topic-permissions/" + vhost + "/" + userName;
                }
                HttpRequest<Buffer> exchangePermissionRequest =
                    webClient.put(uri).basicAuthentication(user, password);
                JsonObject obj = new JsonObject();
                if (jsonResponse.getString("id") != null
                    && !jsonResponse.getString("id").isEmpty()) {
                  // set all mandatory fields
                  obj.put("exchange", jsonResponse.getString("id"));
                  obj.put("write", ".*");
                  obj.put("read", "");
                  exchangePermissionRequest.sendJsonObject(obj, result1 -> {
                    if (result1.result().statusCode() == HttpStatus.SC_CREATED) {
                      logger.info("permissions set for newly created exchange,user :"
                          + jsonResponse.getString("id"));
                    } else {
                      logger.error("Error in setting permissions for newly created exchange,user :"
                          + jsonResponse.getString("id"));
                    }
                  });
                }


              } else {
                logger.info("Some error in user creation");
              }
            } else {
              logger.info("Something went wrong while finding user :" + ar.cause());
            }
          });

        } else if (result.result().statusCode() == HttpStatus.SC_OK) {
          // user exists , So something useful can be done here
          jsonResponse.put("type", "UserExists");
          jsonResponse.put("title", "success");
          jsonResponse.put("detail", "User already exists");
          logger.info(
              "user is already exists with properties : [type : UserExists, title : success, detail : User already exists ]");
        }

        String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
        jsonResponse.put("username", domain + "/" + getSHA(userName));

      } else {
        logger.info("Something went wrong while finding user :" + result.cause());
      }
    });

  }

  private String encode_vhost(String vhost) {
    String encoded_vhost = null;
    try {
      encoded_vhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      logger.error("Error in encode vhost name :" + ex.getCause());
    }
    return encoded_vhost;
  }

  /*
   * This method is as simple as but it can have more sophisticated encryption logic.
   */
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

  private String generateRandomPassword() {
    // It is simple one. here we may more strong algorith for password generation.
    return org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(Constants.PASSWORD_LENGTH);
  }

  private ArrayList<String> getRoutingKeys() {
    ArrayList<String> arrayList = new ArrayList<String>();
    // get routingKeys from properties file
    try {
      properties = new Properties();
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

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    jsonResponse = new JsonObject();
    if (request != null && !request.isEmpty() && request.getString("id") != null
        && !request.getString("id").isEmpty() && !request.getString("id").isBlank()) {
      String adaptorID = request.getString("id");
      if (vhost.equalsIgnoreCase("/")) {
        encoded_vhost = encode_vhost(vhost);
        uri = "/api/exchanges/" + encoded_vhost + "/" + adaptorID;
      } else {
        uri = "/api/exchanges/" + vhost + "/" + adaptorID;
      }

      HttpRequest<Buffer> isAdaptorExistRequest =
          webClient.get(uri).basicAuthentication(user, password);
      isAdaptorExistRequest.send(result -> {
        if (result.succeeded()) {
          if (result.result().statusCode() == HttpStatus.SC_NOT_FOUND) {
            jsonResponse.put("type", "status");
            jsonResponse.put("title", "Failure");
            jsonResponse.put("detail", "Adaptor does not exists");
          } else if (result.result().statusCode() == HttpStatus.SC_OK) {
            logger.info(adaptorID + " found for deletion. Now deleting ....");
            rabbitmqClient.exchangeDelete(adaptorID, rh -> {
              if (rh.succeeded()) {
                logger.info(adaptorID + " adaptor deleted successfully");
                jsonResponse.put("id", adaptorID);
                jsonResponse.put("type", "status");
                jsonResponse.put("title", "success");
                jsonResponse.put("detail", "adaptor deleted");
              } else if (rh.failed()) {
                logger.info("adaptor deletion failed. Cause :" + rh.cause());
                jsonResponse.put("type", "status");
                jsonResponse.put("title", "error");
                jsonResponse.put("detail", "Error in adaptor deletion");
              } else {
                System.out.println("Something else in deleting adaptor");
              }
            });
          } else {
            System.out.println("Something fatal in finding adaptor");
          }
        } else {
          System.out.println("Something wrong in finding adaptor" + result.cause());
        }
        logger
            .info("deleteAdaptor method final response : [ " + jsonResponse.getString("type") + ", "
                + jsonResponse.getString("title") + ", " + jsonResponse.getString("detail") + " ]");
      });
    } else {
      jsonResponse.put("status", "Bad request : nothing to delete");
      handler.handle(Future.failedFuture("Bad request : nothing to delete"));
    }

    handler.handle(Future.succeededFuture(jsonResponse));

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

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

    return null;
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

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listExchangeSubscribers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
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

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService bindQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService unbindQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createvHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
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

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
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
}

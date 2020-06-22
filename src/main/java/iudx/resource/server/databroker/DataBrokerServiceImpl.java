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
import io.vertx.rabbitmq.RabbitMQClient;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;

/**
 * The Data Broker Service Implementation.
 *
 * <h1>Data Broker Service Implementation</h1>
 *
 * <p>The Data Broker Service implementation in the IUDX Resource Server implements the definitions
 * of the {@link iudx.resource.server.databroker.DataBrokerService}.
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
  JsonObject finalResponse;
  private String user;
  private String password;
  private String vhost;
  JsonObject registerStreamingSubscriptionResponse;
  private String ip;
  private int port;

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   *
   * @param clientInstance which is a RabbitMQ client
   * @param webClientInstance which is a Vertx Web client
   */
  public DataBrokerServiceImpl(
      RabbitMQClient clientInstance, WebClient webClientInstance, JsonObject propObj) {
    logger.info("Got the RabbitMQ Client instance");
    client = clientInstance;
    client.start(
        resultHandler -> {
          if (resultHandler.succeeded()) {
            logger.info("Client Connected");
          } else {
            logger.info("Client Not Connected");
          }
        });

    if (propObj != null && !propObj.isEmpty()) {

      user = propObj.getString("userName");
      password = propObj.getString("password");
      vhost = URLEncoder.encode(propObj.getString("vHost"));
      ip = propObj.getString("ip");
      port = propObj.getInteger("port");
    }
  }

  /**
   * {@inheritDoc}
   */
  
  @Override
  public DataBrokerService registerAdaptor(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  
  @Override
  public DataBrokerService updateAdaptor(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteAdaptor(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listAdaptor(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      Base64.Encoder encoder = Base64.getEncoder();
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String encodequeue = domain + "/" + getSha(userName) + "/" + request.getString("name");
      String queueName = encoder.encodeToString(encodequeue.getBytes());
      String streamingUrl =
          "amqp://"
              + userName
              + ":"
              + generateRandomPassword() // for testing use "1234"
              + "@"
              + ip
              + ":"
              + port
              + "/"
              + "iudx"
              + "/"
              + encodequeue;

      JsonArray entitites = request.getJsonArray("entities");
      requestjson.put("queueName", queueName);
      // FOR cration of new user
      // Future<JsonObject> resultuser = createUserIfNotExist(userName);
      // resultuser.onComplete(resultHandleruser -> {
      // if (resultHandleruser.succeeded()) {

      Future<JsonObject> resultqueue = createQueue(requestjson);
      resultqueue.onComplete(
          resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {
              JsonObject exchangeBindResponse = new JsonObject();

              String exchangeName = "";
              for (Object currentEntity : entitites) {
                String routingKey = (String) currentEntity;
                if (routingKey != null) {
                  exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                  if (exchangeBindResponse.containsKey(exchangeName)) {
                    JsonArray obj = (JsonArray) exchangeBindResponse.getValue(exchangeName);
                    obj.add(currentEntity);
                    exchangeBindResponse.put(exchangeName, obj);
                  } else {
                    ArrayList<String> temp = new ArrayList<String>();
                    temp.add(routingKey);
                    exchangeBindResponse.put(exchangeName, temp);
                  }
                }
              }

              exchangeBindResponse.forEach(
                  action -> {
                    JsonObject json = new JsonObject();
                    json.put("exchangeName", encoder.encodeToString(action.getKey().getBytes()));
                    json.put("queueName", queueName);
                    JsonArray array = new JsonArray();
                    array.add(action.getValue());
                    json.put("entities", array);
                    Future<JsonObject> resultbind = bindQueue(json);
                    resultbind.onComplete(
                        resultHandlerbind -> {
                          if (resultHandlerbind.succeeded()) {
                            // count++
                            logger.debug("sucess ::" + resultHandlerbind.result());
                          }

                          if (resultHandlerbind.failed()) {
                            logger.error("failed ::" + resultHandlerbind.cause());
                          }
                        });
                  });

              registerStreamingSubscriptionResponse.put("subscriptionID", queueName);
              registerStreamingSubscriptionResponse.put("streamingURL", streamingUrl);
              handler.handle(Future.succeededFuture(registerStreamingSubscriptionResponse));
            }
            if (resultHandlerqueue.failed()) {
              logger.error("failed ::" + resultHandlerqueue.cause());
            }
          });

      // }
      // if (resultHandleruser.failed()) {
      // logger.error("failed ::" + resultHandleruser.cause());
      // }
      // });

    } else {
      logger.error("Error in payload");
    }
    return null;
  }

  /*
   * This method is as simple as but it can have more sophisticated encryption logic.
   */
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

  private String generateRandomPassword() {
    // It is simple one. here we may more strong algorith for password generation.
    return org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric(Constants.PASSWORD_LENGTH);
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject deleteStreamingSubscription = new JsonObject();
    if (request != null && !request.isEmpty()) {
      Base64.Encoder encoder = Base64.getEncoder();
      String queueNameencode = request.getString("subscriptionID");
      String queueName = encoder.encodeToString(queueNameencode.getBytes());
      JsonObject requestBody = new JsonObject();
      requestBody.put("queueName", queueName);
      Future<JsonObject> result = deleteQueue(requestBody);
      result.onComplete(
          resultHandler -> {
            if (resultHandler.succeeded()) {

              deleteStreamingSubscription.put("subscriptionID", queueName);
              handler.handle(Future.succeededFuture(deleteStreamingSubscription));
            }
            if (resultHandler.failed()) {
              logger.error("failed ::" + resultHandler.cause());
            }
          });
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listStreamingSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Base64.Encoder encoder = Base64.getEncoder();
      String queueNameencode = request.getString("subscriptionID");
      String queueName = encoder.encodeToString(queueNameencode.getBytes());
      JsonObject requestBody = new JsonObject();
      requestBody.put("queueName", queueName);
      Future<JsonObject> result = listQueueSubscribers(requestBody);
      result.onComplete(
          resultHandler -> {
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerCallbackSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateCallbackSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteCallbackSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listCallbackSubscription(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createExchange(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      // calls the common create exchange method
      Future<JsonObject> result = createExchange(request);
      result.onComplete(
          resultHandler -> {
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
   */
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
      webRequest.sendJsonObject(
          obj,
          ar -> {
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
  public DataBrokerService updateExchange(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteExchange(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = deleteExchange(request);
      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
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
  public DataBrokerService listExchangeSubscribers(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listExchangeSubscribers(request);
      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              if (response != null && !response.equals(" ")) {
                int status = response.statusCode();
                if (status == HttpStatus.SC_OK) {
                  Buffer body = response.body();
                  if (body != null) {
                    JsonArray jsonBody = new JsonArray(body.toString());
                    jsonBody.forEach(
                        current -> {
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
  public DataBrokerService createQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = createQueue(request);
      result.onComplete(
          resultHandler -> {
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
      webRequest.sendJsonObject(
          configProp,
          ar -> {
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
  public DataBrokerService updateQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = deleteQueue(request);
      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
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

      result.onComplete(
          resultHandler -> {
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
        webRequest.sendJsonObject(
            requestBody,
            ar -> {
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
  public DataBrokerService unbindQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = unbindQueue(request);

      result.onComplete(
          resultHandler -> {
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
        webRequest.send(
            ar -> {
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
  public DataBrokerService createvHost(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = createvHost(request);

      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
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
  public DataBrokerService updatevHost(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deletevHost(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = deletevHost(request);

      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
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
      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              if (response != null && !response.equals(" ")) {
                int status = response.statusCode();

                if (status == HttpStatus.SC_OK) {
                  Buffer body = response.body();

                  if (body != null) {
                    JsonArray jsonBody = new JsonArray(body.toString());
                    jsonBody.forEach(
                        current -> {
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
  public DataBrokerService listQueueSubscribers(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listQueueSubscribers(request);
      result.onComplete(
          resultHandler -> {
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
      webRequest.send(
          ar -> {
            if (ar.succeeded()) {
              HttpResponse<Buffer> response = ar.result();
              if (response != null && !response.equals(" ")) {
                int status = response.statusCode();
                if (status == HttpStatus.SC_OK) {
                  Buffer body = response.body();
                  if (body != null) {
                    JsonArray jsonBody = new JsonArray(body.toString());
                    jsonBody.forEach(
                        current -> {
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
  public DataBrokerService publishFromAdaptor(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    finalResponse = new JsonObject();
    JsonObject json = new JsonObject();
    if (request != null && !request.isEmpty()) {
      json.put("body", request.toString());
      String resourceGroupId = request.getString("id");
      String routingKey = resourceGroupId;
      if (resourceGroupId != null && !resourceGroupId.isBlank()) {
        resourceGroupId = resourceGroupId.substring(0, resourceGroupId.lastIndexOf("/"));

        client.basicPublish(
            resourceGroupId,
            routingKey,
            json,
            resultHandler -> {
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
}

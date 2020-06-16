package iudx.resource.server.databroker;

import io.vertx.core.AbstractVerticle;
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

  /**
   * This is a constructor which is used by the DataBroker Verticle to instantiate a RabbitMQ
   * client.
   * 
   * @param clientInstance which is a RabbitMQ client
   */

  public DataBrokerServiceImpl(RabbitMQClient clientInstance, WebClient webClientInstance,
      JsonObject propObj) {

    logger.info("Got the RabbitMQ Client instance");
    client = clientInstance;

    client.start(resultHandler -> {
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
    }

    webClient = webClientInstance;

  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
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
    if (request != null && !request.isEmpty()) {
      // calls the common create exchange method
      Future<JsonObject> result = createExchangeCommon(request);
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
      Future<JsonObject> result = deleteExchangeCommon(request);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listExchangeSubscribers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listExchangeSubscribersCommon(request);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = createQueueCommon(request);
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
      Future<JsonObject> result = deleteQueueCommon(request);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService bindQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = bindQueueCommon(request);

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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService unbindQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    if (request != null && !request.isEmpty()) {


      Future<JsonObject> result = unbindQueueCommon(request);


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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createvHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {

      Future<JsonObject> result = createvHostCommon(request);

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

      Future<JsonObject> result = deletevHostCommon(request);

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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = listvHostCommon(request);
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
      Future<JsonObject> result = listQueueSubscribersCommon(request);
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

  @Override
  public DataBrokerService publishFromAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
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
            logger.error("Message publishing failed");
            resultHandler.cause().printStackTrace();
          }
        });
      }
    }
    return null;
  }

  /**
   * The createExchangeCommon implements the create exchange.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   **/

  Future<JsonObject> createExchangeCommon(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
              finalResponse.put("Exchange", exchangeName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put("status", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Exchange already exists");
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put("status", status);
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
   * The deleteExchangeCommon implements the delete exchange operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deleteExchangeCommon(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
              finalResponse.put("status", status);
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
   * The listExchangeSubscribersCommon implements the list of bindings for an exchange (source).
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listExchangeSubscribersCommon(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String exchangeName = request.getString("exchangeName");
      JsonObject output = new JsonObject();
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
                  if (output.containsKey(okey)) {
                    JsonArray obj = (JsonArray) output.getValue(okey);
                    obj.add(currentJson.getString("routing_key"));
                    output.put(okey, obj);
                  } else {
                    ArrayList<String> temp = new ArrayList<String>();
                    temp.add(currentJson.getString("routing_key"));
                    output.put(okey, temp);
                  }
                });
                if (output.isEmpty()) {
                  output.put("status", HttpStatus.SC_NOT_FOUND);
                  output.put("title", "Failure");
                  output.put("detail", "Exchange does not exist");
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              output.put("status", status);
              output.put("title", "Failure");
              output.put("detail", "Exchange does not exist");
            }
          }
          promise.complete(output);
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
   * The createQueueCommon implements the create queue operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> createQueueCommon(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
              finalResponse.put("status", status);
              finalResponse.put("title", "Failure");
              finalResponse.put("detail", "Queue already exists");
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.put("status", status);
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
   * The deleteQueueCommon implements the delete queue operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deleteQueueCommon(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
              finalResponse.put("status", status);
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
   * The listQueueSubscribersCommon implements the list of bindings for a queue.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listQueueSubscribersCommon(JsonObject request) {

    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      String queueName = request.getString("queueName");
      JsonObject output = new JsonObject();
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
                  output.put("entities", oroutingKeys);
                } else {
                  output.put("status", HttpStatus.SC_NOT_FOUND);
                  output.put("title", "Failure");
                  output.put("detail", "Queue does not exist");
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              output.put("status", status);
              output.put("title", "Failure");
              output.put("detail", "Queue does not exist");
            }
          }
          promise.complete(output);
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
   * The bindQueueCommon implements the bind queue to exchange by routing key.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> bindQueueCommon(JsonObject request) {

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
                finalResponse.put("status", status);
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
   * The unbindQueueCommon implements the unbind queue to exchange by routing key.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> unbindQueueCommon(JsonObject request) {

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
                finalResponse.put("status", status);
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
   * The createvHostCommon implements the create virtual host operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> createvHostCommon(JsonObject request) {

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
              finalResponse.put("status", status);
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
   * The deletevHostCommon implements the delete virtual host operation.
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> deletevHostCommon(JsonObject request) {

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
              finalResponse.put("status", status);
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
   * The listvHostCommon implements the list of virtual hosts .
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  private Future<JsonObject> listvHostCommon(JsonObject request) {

    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {

      JsonObject output = new JsonObject();
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
                  output.put("vHost", vhostList);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              output.put("status", status);
              output.put("title", "Failure");
              output.put("detail", "No vhosts found");
            }
          }
          promise.complete(output);
          logger.info(finalResponse);
        } else {
          logger.error("Listing of Queue failed" + ar.cause());
          promise.fail(ar.cause());
        }
      });
    }
    return promise.future();
  }
}

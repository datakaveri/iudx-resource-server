package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.BAD_REQUEST_CODE;
import static iudx.resource.server.databroker.util.Constants.BAD_REQUEST_DATA;
import static iudx.resource.server.databroker.util.Constants.ID;
import static iudx.resource.server.databroker.util.Constants.STATUS;
import static iudx.resource.server.databroker.util.Constants.TYPE;
import static iudx.resource.server.databroker.util.Constants.USER_ID;

import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.databroker.util.Util;

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

  private static final Logger LOGGER = LogManager.getLogger(DataBrokerServiceImpl.class);
  private String url;
  JsonObject requestBody = new JsonObject();
  JsonObject finalResponse =
      Util.getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, BAD_REQUEST_DATA);
  private String user;
  private String password;
  private String vhost;
  private int totalBindCount;
  private int totalBindSuccess;
  private int totalUnBindCount;
  private int totalUnBindSuccess;
  private boolean bindingSuccessful;
  private RabbitClient webClient;
  private PostgresClient pgClient;
  private SubscriptionService subscriptionService;


  public DataBrokerServiceImpl(RabbitClient webClient, PostgresClient pgClient, JsonObject config) {
    this.webClient = webClient;
    this.pgClient = pgClient;
    this.vhost = config.getString("dataBrokerVhost");
    this.subscriptionService = new SubscriptionService(this.webClient, pgClient, config);

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
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.registerAdapter(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("registerAdaptor resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    }
    return this;
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
      Future<JsonObject> result = webClient.getExchange(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("getExchange resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    }
    return this;
  }

  /*
   * overridden method
   */

  /**
   * The deleteAdaptor implements deletion feature for an adaptor(exchange).
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService deleteAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.deleteAdapter(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("getExchange resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    }
    return this;

  }

  /**
   * The listAdaptor implements the list of bindings for an exchange (source). This method has similar
   * functionality as listExchangeSubscribers(JsonObject) method
   * 
   * @param request which is a Json object
   * @return response which is a Future object of promise of Json type
   */
  @Override
  public DataBrokerService listAdaptor(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.listExchangeSubscribers(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
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
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.registerStreamingSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "registerStreamingSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.updateStreamingSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "updateStreamingSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }

    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService appendStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.appendStreamingSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "appendStreamingSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteStreamingSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.deleteStreamingSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "deleteStreamingSubscription - resultHandler failed : "
                  + resultHandler.cause().getMessage());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
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
      Future<JsonObject> result = subscriptionService.listStreamingSubscriptions(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER
              .error("listStreamingSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));

        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService registerCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.registerCallbackSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "registerCallbackSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService updateCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.updateCallbackSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "updateCallbackSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService deleteCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.deleteCallbackSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error(
              "deleteCallbackSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listCallbackSubscription(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = subscriptionService.listCallbackSubscription(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER
              .error("listCallbackSubscription - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    } else {
      handler.handle(Future.failedFuture(finalResponse.toString()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createExchange(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = webClient.createExchange(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    }
    return this;
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
      Future<JsonObject> result = webClient.deleteExchange(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.listExchangeSubscribers(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.createQueue(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.deleteQueue(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.bindQueue(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.unbindQueue(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.createvHost(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.deletevHost(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
    if (request != null) {
      Future<JsonObject> result = webClient.listvHost(request);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
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
      Future<JsonObject> result = webClient.listQueueSubscribers(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
        }
      });
    }
    return null;
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
      LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
      String routingKey = resourceGroupId;
      if (resourceGroupId != null && !resourceGroupId.isBlank()) {
        resourceGroupId = resourceGroupId.substring(0, resourceGroupId.lastIndexOf("/"));
        LOGGER.debug("Info : resourceGroupId  " + resourceGroupId);
        LOGGER.debug("Info : routingKey  " + routingKey);
        Buffer buffer = Buffer.buffer(json.toString());
        webClient.getRabbitMQClient().basicPublish(resourceGroupId, routingKey, buffer,
            resultHandler -> {
              if (resultHandler.succeeded()) {
                finalResponse.put(STATUS, HttpStatus.SC_OK);
                LOGGER.info("Success : Message published to queue");
                handler.handle(Future.succeededFuture(finalResponse));
              } else {
                finalResponse.put(TYPE, HttpStatus.SC_BAD_REQUEST);
                LOGGER.error("Fail : " + resultHandler.cause().toString());
                handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
              }
            });
      }
    }
    return this;
  }

  @Override
  public DataBrokerService publishHeartbeat(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    JsonObject response = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String adaptor = request.getString(ID);
      String routingKey = request.getString("status");
      if (adaptor != null && !adaptor.isEmpty() && routingKey != null && !routingKey.isEmpty()) {
        JsonObject json = new JsonObject();
        Future<JsonObject> future1 =
            webClient.getExchange(json.put("id", adaptor), vhost);
        future1.onComplete(ar -> {
          if (ar.result().getInteger("type") == HttpStatus.SC_OK) {
            json.put("exchangeName", adaptor);
            // exchange found, now get list of all queues which are bound with this exchange
            Future<JsonObject> future2 =
                webClient.listExchangeSubscribers(json, vhost);
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
                      Buffer buffer = Buffer.buffer(message.toString());
                      webClient.getRabbitMQClient().basicPublish(adaptor, routingKey, buffer,
                          resultHandler -> {
                            if (resultHandler.succeeded()) {
                              LOGGER.info("publishHeartbeat - message published to queue [ " + queueName
                                  + " ] for routingKey [ " + routingKey + " ]");
                              response.put("type", "success");
                              response.put("queueName", queueName);
                              response.put("routingKey", rk.toString());
                              response.put("detail", "routingKey matched");
                              handler.handle(Future.succeededFuture(response));
                            } else {
                              LOGGER.error(
                                  "publishHeartbeat - some error in publishing message to queue [ "
                                      + queueName + " ]. cause : " + resultHandler.cause());
                              response.put("messagePublished", "failed");
                              response.put("type", "error");
                              response.put("detail", "routingKey not matched");
                              handler.handle(Future.failedFuture(response.toString()));
                            }
                          });
                    } else {
                      LOGGER.error(
                          "publishHeartbeat - routingKey [ " + routingKey + " ] not matched with [ "
                              + rk.toString() + " ] for queue [ " + queueName + " ]");
                      handler.handle(Future.failedFuture(
                          "publishHeartbeat - routingKey [ " + routingKey + " ] not matched with [ "
                              + rk.toString() + " ] for queue [ " + queueName + " ]"));
                    }
                  });

                });

              } else {
                LOGGER.error(
                    "publishHeartbeat method - Oops !! None queue bound with given exchange");
                handler.handle(Future.failedFuture(
                    "publishHeartbeat method - Oops !! None queue bound with given exchange"));
              }

            });

          } else {
            LOGGER.error("Either adaptor does not exist or some other error to publish message");
            handler.handle(Future.failedFuture(
                "Either adaptor does not exist or some other error to publish message"));
          }

        });
      } else {
        LOGGER.error("publishHeartbeat - adaptor and routingKey not provided to publish message");
        handler.handle(Future.failedFuture(
            "publishHeartbeat - adaptor and routingKey not provided to publish message"));
      }

    } else {
      LOGGER.error("publishHeartbeat - request is null to publish message");
      handler.handle(Future.failedFuture("publishHeartbeat - request is null to publish message"));
    }

    return null;

  }

  @Override
  public DataBrokerService resetPassword(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    JsonObject response = new JsonObject();
    String password = Util.randomPassword.get();
    String userid = request.getString(USER_ID);
    Future<JsonObject> userFuture = webClient.getUserInDb(userid);

    userFuture.compose(checkUserFut -> {
      return webClient.resetPasswordInRMQ(userid, password);
    }).compose(rmqResetFut -> {
      return webClient.resetPwdInDb(userid, Util.getSha(password));
    }).onSuccess(successHandler -> {
      response.put("password", password);
      handler.handle(Future.succeededFuture(response));
    }).onFailure(failurehandler -> {
      JsonObject failureResponse = new JsonObject();
      failureResponse.put("type", 401).put("title", "not authorized").put("detail", "not authorized");
      handler.handle(Future.failedFuture(failureResponse.toString()));
    });

    return this;
  }

  @Override
  // TODO : complete implementation
  public DataBrokerService publishMessage(JsonObject request, String toExchange, String routingKey,
      Handler<AsyncResult<JsonObject>> handler) {

    Future<Void> rabbitMqClientStartFuture;

    JsonObject message = new JsonObject();
    message.put("body", request.toString());
    Buffer buffer = Buffer.buffer(message.toString());

    RabbitMQClient client = webClient.getRabbitMQClient();
    if (!client.isConnected())
      rabbitMqClientStartFuture = client.start();
    else
      rabbitMqClientStartFuture = Future.succeededFuture();

    rabbitMqClientStartFuture.compose(rabbitstartupFuture -> {
      return client.basicPublish(toExchange, routingKey, buffer);
    }).onSuccess(successHandler -> {
      JsonObject json = new JsonObject();
      json.put("type", ResponseUrn.SUCCESS_URN.getUrn());
      handler.handle(Future.succeededFuture(json));
    }).onFailure(failureHandler -> {
      LOGGER.error(failureHandler);
      Response response = new Response.Builder()
          .withType(ResponseUrn.QUEUE_ERROR_URN.getUrn())
          .withStatus(HttpStatus.SC_BAD_REQUEST)
          .withDetail(failureHandler.getLocalizedMessage()).build();
      handler.handle(Future.failedFuture(response.toJson().toString()));
    });
    return this;
  }
}

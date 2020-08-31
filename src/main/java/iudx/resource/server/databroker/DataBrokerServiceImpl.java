package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.*;
import java.util.Map;
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
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import iudx.resource.server.databroker.util.Constants;
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

  private RabbitMQStreamingClient rabbitMQStreamingClient;
  private PostgresQLClient pgClient;


  public DataBrokerServiceImpl(RabbitMQStreamingClient rabbitMQStreamingClient,
      PostgresQLClient pgClient, String vhost) {
    this.rabbitMQStreamingClient = rabbitMQStreamingClient;
    this.pgClient = pgClient;
    this.vhost = vhost;
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
      Future<JsonObject> result = rabbitMQStreamingClient.registerAdaptor(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("registerAdaptor resultHandler failed : " + resultHandler.cause());
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
      Future<JsonObject> result = rabbitMQStreamingClient.getExchange(request, vhost);
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
      Future<JsonObject> result = rabbitMQStreamingClient.getExchange(request, vhost);
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
      Future<JsonObject> result = rabbitMQStreamingClient.deleteAdapter(request, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          handler.handle(Future.succeededFuture(resultHandler.result()));
        }
        if (resultHandler.failed()) {
          logger.error("deleteAdaptor - resultHandler failed : " + resultHandler.cause());
          handler.handle(Future.failedFuture(resultHandler.cause().toString()));
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
    JsonObject registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = domain + "/" + Util.getSha(userName) + "/" + request.getString("name");

      Future<JsonObject> resultCreateUser =
          rabbitMQStreamingClient.createUserIfNotExist(userName, VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {

          // For testing instead of generateRandomPassword() use password = 1234
          String streamingUrl = "amqp://" + userName + ":" + "1234" // generateRandomPassword()
              + "@" + BROKER_IP + ":" + BROKER_PORT + "/" + VHOST_IUDX + "/" + queueName;
          logger.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(ENTITIES);
          logger.info("Request Access for " + entitites);
          logger.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(QUEUE_NAME, queueName);

          Future<JsonObject> resultqueue = rabbitMQStreamingClient.createQueue(requestjson, vhost);
          resultqueue.onComplete(resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {

              logger.info("sucess :: Create Queue " + resultHandlerqueue.result());
              JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

              if (createQueueResponse.containsKey(TITLE)
                  && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                logger.error("failed ::" + resultHandlerqueue.cause());
                handler.handle(Future
                    .failedFuture(new JsonObject().put(ERROR, "Queue Creation Failed").toString()));
              } else {

                logger.info("Success Queue Created");

                for (Object currentEntity : entitites) {
                  String routingKey = (String) currentEntity;
                  logger.info("routingKey is " + routingKey);
                  if (routingKey != null) {
                    if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                        || routingKey.split("/").length != 5) {
                      logger.error("failed :: Invalid (or) NULL routingKey");

                      Future<JsonObject> resultDeletequeue =
                          rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                      resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                        if (resultHandlerDeletequeue.succeeded()) {

                          handler.handle(Future.failedFuture(
                              new JsonObject().put(ERROR, "Invalid routingKey").toString()));

                        }
                      });
                    } else {

                      String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                      JsonArray array = new JsonArray();
                      array.add(currentEntity);
                      JsonObject json = new JsonObject();
                      json.put(EXCHANGE_NAME, exchangeName);
                      json.put(QUEUE_NAME, queueName);
                      json.put(ENTITIES, array);

                      Future<JsonObject> resultbind =
                          rabbitMQStreamingClient.bindQueue(json, vhost);
                      resultbind.onComplete(resultHandlerbind -> {
                        if (resultHandlerbind.succeeded()) {
                          // count++
                          totalBindSuccess += 1;
                          logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                              + resultHandlerbind.result());

                          JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                          if (bindResponse.containsKey(TITLE)
                              && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                            logger.error("failed ::" + resultHandlerbind.cause());
                            Future<JsonObject> resultDeletequeue =
                                rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                            resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                              if (resultHandlerDeletequeue.succeeded()) {
                                handler.handle(Future.failedFuture(
                                    new JsonObject().put(ERROR, "Binding Failed").toString()));
                              }
                            });
                          } else if (totalBindSuccess == totalBindCount) {
                            registerStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                            registerStreamingSubscriptionResponse.put(STREAMING_URL, streamingUrl);
                            handler.handle(
                                Future.succeededFuture(registerStreamingSubscriptionResponse));
                          }
                        } else if (resultHandlerbind.failed()) {
                          logger.error("failed ::" + resultHandlerbind.cause());
                          Future<JsonObject> resultDeletequeue =
                              rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              handler.handle(Future.failedFuture(
                                  new JsonObject().put(ERROR, "Binding Failed").toString()));
                            }
                          });
                        }
                      });
                    }
                  } else {
                    logger.error("failed :: Invalid (or) NULL routingKey");
                    Future<JsonObject> resultDeletequeue =
                        rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                    resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                      if (resultHandlerDeletequeue.succeeded()) {
                        handler.handle(Future.failedFuture(
                            new JsonObject().put(ERROR, "Invalid routingKey").toString()));
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
      handler
          .handle(Future.failedFuture(new JsonObject().put(ERROR, "Error in payload").toString()));
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
      String userName = request.getString(CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = domain + "/" + Util.getSha(userName) + "/" + request.getString("name");
      Future<JsonObject> resultCreateUser =
          rabbitMQStreamingClient.createUserIfNotExist(userName, VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {
          // For testing instead of generateRandomPassword() use password = 1234
          String streamingUrl = "amqp://" + userName + ":" + "1234" // generateRandomPassword()
              + "@" + BROKER_IP + ":" + BROKER_PORT + "/" + VHOST_IUDX + "/" + queueName;
          logger.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(ENTITIES);
          logger.info("Request Access for " + entitites);
          logger.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(QUEUE_NAME, queueName);

          Future<JsonObject> deleteQueue = rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
          deleteQueue.onComplete(deleteQueuehandler -> {
            if (deleteQueuehandler.succeeded()) {
              logger.info("sucess :: Deleted Queue " + deleteQueuehandler.result());

              Future<JsonObject> resultqueue =
                  rabbitMQStreamingClient.createQueue(requestjson, vhost);
              resultqueue.onComplete(resultHandlerqueue -> {
                if (resultHandlerqueue.succeeded()) {

                  logger.info("sucess :: Create Queue " + resultHandlerqueue.result());
                  JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

                  if (createQueueResponse.containsKey(TITLE)
                      && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                    logger.error("failed ::" + resultHandlerqueue.cause());
                    handler.handle(Future.failedFuture(
                        new JsonObject().put(ERROR, "Queue Creation Failed").toString()));
                  } else {

                    logger.info("Success Queue Created");

                    for (Object currentEntity : entitites) {
                      String routingKey = (String) currentEntity;
                      logger.info("routingKey is " + routingKey);
                      if (routingKey != null) {
                        if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                            || routingKey.split("/").length != 5) {
                          logger.error("failed :: Invalid (or) NULL routingKey");

                          Future<JsonObject> resultDeletequeue =
                              rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {

                              handler.handle(Future.failedFuture(
                                  new JsonObject().put(ERROR, "Invalid routingKey").toString()));

                            }
                          });
                        } else {

                          String exchangeName =
                              routingKey.substring(0, routingKey.lastIndexOf("/"));
                          JsonArray array = new JsonArray();
                          array.add(currentEntity);
                          JsonObject json = new JsonObject();
                          json.put(EXCHANGE_NAME, exchangeName);
                          json.put(QUEUE_NAME, queueName);
                          json.put(ENTITIES, array);

                          Future<JsonObject> resultbind =
                              rabbitMQStreamingClient.bindQueue(json, vhost);
                          resultbind.onComplete(resultHandlerbind -> {
                            if (resultHandlerbind.succeeded()) {
                              // count++
                              totalBindSuccess += 1;
                              logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                                  + resultHandlerbind.result());

                              JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                              if (bindResponse.containsKey(TITLE)
                                  && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                                logger.error("failed ::" + resultHandlerbind.cause());
                                Future<JsonObject> resultDeletequeue =
                                    rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    handler.handle(Future.failedFuture(
                                        new JsonObject().put(ERROR, "Binding Failed").toString()));
                                  }
                                });
                              } else if (totalBindSuccess == totalBindCount) {
                                updateStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                                updateStreamingSubscriptionResponse.put(STREAMING_URL,
                                    streamingUrl);
                                handler.handle(
                                    Future.succeededFuture(updateStreamingSubscriptionResponse));
                              }
                            } else if (resultHandlerbind.failed()) {
                              logger.error("failed ::" + resultHandlerbind.cause());
                              Future<JsonObject> resultDeletequeue =
                                  rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                              resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                if (resultHandlerDeletequeue.succeeded()) {
                                  handler.handle(Future.failedFuture(
                                      new JsonObject().put(ERROR, "Binding Failed").toString()));
                                }
                              });
                            }
                          });
                        }
                      } else {
                        logger.error("failed :: Invalid (or) NULL routingKey");
                        Future<JsonObject> resultDeletequeue =
                            rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                        resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                          if (resultHandlerDeletequeue.succeeded()) {
                            handler.handle(Future.failedFuture(
                                new JsonObject().put(ERROR, "Invalid routingKey").toString()));
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
      handler
          .handle(Future.failedFuture(new JsonObject().put(ERROR, "Error in payload").toString()));
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
      JsonArray entitites = request.getJsonArray(ENTITIES);
      logger.info("Request Access for " + entitites);
      logger.info("No of bindings to do : " + entitites.size());

      totalBindCount = entitites.size();
      totalBindSuccess = 0;

      String queueName = request.getString(SUBSCRIPTION_ID);
      requestjson.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitMQStreamingClient.listQueueSubscribers(requestjson, vhost);
      result.onComplete(resultHandlerqueue -> {
        if (resultHandlerqueue.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandlerqueue.result();
          logger.info(listQueueResponse);
          if (listQueueResponse.containsKey(TITLE)
              && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            handler.handle(
                Future.failedFuture(new JsonObject().put(ERROR, "Error in payload").toString()));
          } else {
            for (Object currentEntity : entitites) {
              String routingKey = (String) currentEntity;
              logger.info("routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                    || routingKey.split("/").length != 5) {
                  logger.error("failed :: Invalid (or) NULL routingKey");

                  handler.handle(Future
                      .failedFuture(new JsonObject().put(ERROR, "Invalid routingKey").toString()));

                } else {

                  String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                  JsonArray array = new JsonArray();
                  array.add(currentEntity);
                  JsonObject json = new JsonObject();
                  json.put(EXCHANGE_NAME, exchangeName);
                  json.put(QUEUE_NAME, queueName);
                  json.put(ENTITIES, array);

                  Future<JsonObject> resultbind = rabbitMQStreamingClient.bindQueue(json, vhost);
                  resultbind.onComplete(resultHandlerbind -> {
                    if (resultHandlerbind.succeeded()) {
                      // count++
                      totalBindSuccess += 1;
                      logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());

                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(TITLE)
                          && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                        logger.error("failed ::" + resultHandlerbind.cause());
                        handler.handle(Future.failedFuture(
                            new JsonObject().put(ERROR, "Binding Failed").toString()));
                      } else if (totalBindSuccess == totalBindCount) {
                        appendStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                        appendStreamingSubscriptionResponse.put(ENTITIES, entitites);
                        handler.handle(Future.succeededFuture(appendStreamingSubscriptionResponse));
                      }
                    } else if (resultHandlerbind.failed()) {
                      logger.error("failed ::" + resultHandlerbind.cause());
                      handler.handle(Future
                          .failedFuture(new JsonObject().put(ERROR, "Binding Failed").toString()));
                    }
                  });
                }
              } else {
                logger.error("failed :: Invalid (or) NULL routingKey");
                Future<JsonObject> resultDeletequeue =
                    rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                  if (resultHandlerDeletequeue.succeeded()) {
                    handler.handle(Future.failedFuture(
                        new JsonObject().put(ERROR, "Invalid routingKey").toString()));
                  }
                });
              }
            }
          }
        } else {
          logger.error("Error in payload");
          handler.handle(
              Future.failedFuture(new JsonObject().put(ERROR, "Error in payload").toString()));
        }
      });
    } else {
      logger.error("Error in payload");
      handler
          .handle(Future.failedFuture(new JsonObject().put(ERROR, "Error in payload").toString()));
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
      String queueName = request.getString(SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitMQStreamingClient.deleteQueue(requestBody, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {

          JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();

          if (deleteQueueResponse.containsKey(TITLE)
              && deleteQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            logger.info("failed :: Response is " + deleteQueueResponse);
            handler.handle(Future.failedFuture(deleteQueueResponse.toString()));
          } else {
            deleteStreamingSubscription.put(SUBSCRIPTION_ID, queueName);
            handler.handle(Future.succeededFuture(deleteStreamingSubscription));
          }
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(
              Future.failedFuture(new JsonObject().put(ERROR, QUEUE_DELETE_ERROR).toString()));
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
      String queueName = request.getString(SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitMQStreamingClient.listQueueSubscribers(requestBody, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandler.result();
          if (listQueueResponse.containsKey(TITLE)
              && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            logger.info("failed :: Response is " + listQueueResponse);
            handler.handle(Future.failedFuture(listQueueResponse.toString()));
          } else {
            logger.info(listQueueResponse);
            handler.handle(Future.succeededFuture(listQueueResponse));
          }
        }
        if (resultHandler.failed()) {
          logger.error("failed ::" + resultHandler.cause());
          handler.handle(
              Future.failedFuture(new JsonObject().put(ERROR, QUEUE_LIST_ERROR).toString()));
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
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      JsonObject publishjson = new JsonObject();
      publishjson.put(Constants.SUBSCRIPTION_ID, subscriptionID);
      publishjson.put(Constants.OPERATION, "create");
      JsonObject requestjson = new JsonObject();

      logger.info("Call Back registration ID check starts");
      pgclient.preparedQuery("Select * FROM registercallback WHERE subscriptionID = $1")
          .execute(Tuple.of(subscriptionID), resultHandlerSelectID -> {
            if (resultHandlerSelectID.succeeded()) {
              RowSet<Row> result = resultHandlerSelectID.result();
              /* Iterating Rows for getting entity, callbackurl, username and password */
              String subscriptionIDdb = null;
              for (Row row : result) {
                subscriptionIDdb = row.getString(0);
                logger.info(subscriptionIDdb);
              }

              if (subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
                logger.info("Call Back registration has duplicate ID");
                registerCallbackSubscriptionResponse.put(Constants.ERROR,
                    "duplicate key value violates unique constraint");
                handler
                    .handle(Future.failedFuture(registerCallbackSubscriptionResponse.toString()));
              } else {

                OffsetDateTime dateTime = OffsetDateTime.now();
                String callbackUrl = request.getString(Constants.CALLBACKURL);
                String queueName = request.getString(Constants.QUEUE);
                JsonArray entitites = request.getJsonArray(Constants.ENTITIES);
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
                      registerCallbackSubscriptionResponse.put(Constants.ERROR,
                          "Invalid routingKey");
                      handler.handle(
                          Future.failedFuture(registerCallbackSubscriptionResponse.toString()));

                    } else {
                      logger.info("Valid ID :: Call Back registration starts");
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
                          totalBindSuccess += 1;
                          logger.info("sucess :: totalBindSuccess " + totalBindSuccess
                              + resultHandlerbind.result());

                          JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                          if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                              .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {

                            logger.error("failed ::" + resultHandlerbind.cause());
                            pgclient
                                .preparedQuery(
                                    "Delete from registercallback WHERE subscriptionID = $1")
                                .execute(Tuple.of(subscriptionID), resulthandlerdel -> {
                                  if (resulthandlerdel.succeeded()) {
                                    registerCallbackSubscriptionResponse.put(Constants.ERROR,
                                        "Binding Failed");
                                    handler.handle(Future.failedFuture(
                                        registerCallbackSubscriptionResponse.toString()));
                                  }
                                });
                          } else if (totalBindSuccess == totalBindCount) {
                            pgclient.preparedQuery(
                                "INSERT INTO registercallback (subscriptionID  ,callbackURL ,entities ,start_time , end_time , frequency ) VALUES ($1, $2, $3, $4, $5, $6)")
                                .execute(Tuple.of(subscriptionID, callbackUrl, entitites, dateTime,
                                    dateTime, dateTime), ar -> {
                                      if (ar.succeeded()) {
                                        String exchangename = "callback.notification";
                                        String routingkey = "create";

                                        JsonObject jsonpg = new JsonObject();
                                        jsonpg.put("body", publishjson.toString());

                                        client.basicPublish(exchangename, routingkey, jsonpg,
                                            resultHandler -> {
                                              if (resultHandler.succeeded()) {
                                                registerCallbackSubscriptionResponse
                                                    .put("subscriptionID", subscriptionID);
                                                logger.info("Message published to queue");
                                                handler.handle(Future.succeededFuture(
                                                    registerCallbackSubscriptionResponse));
                                              } else {
                                                pgclient.preparedQuery(
                                                    "Delete from registercallback WHERE subscriptionID = $1")
                                                    .execute(Tuple.of(subscriptionID), deletepg -> {
                                                      if (deletepg.succeeded()) {
                                                        registerCallbackSubscriptionResponse
                                                            .put("messagePublished", "failed");
                                                        handler.handle(Future.failedFuture(
                                                            registerCallbackSubscriptionResponse
                                                                .toString()));
                                                      }
                                                    });
                                              }
                                            });
                                      } else {
                                        logger.error("failed ::" + ar.cause().getMessage());

                                        pgclient.preparedQuery(
                                            "Delete from registercallback WHERE subscriptionID = $1 ")
                                            .execute(Tuple.of(subscriptionID),
                                                resultHandlerDeletequeuepg -> {
                                                  if (resultHandlerDeletequeuepg.succeeded()) {
                                                    registerCallbackSubscriptionResponse.put(
                                                        Constants.ERROR,
                                                        "duplicate key value violates unique constraint");
                                                    handler.handle(Future.failedFuture(
                                                        registerCallbackSubscriptionResponse
                                                            .toString()));
                                                  }
                                                });
                                      }
                                    });
                          }
                        } else if (resultHandlerbind.failed()) {
                          logger.error("failed ::" + resultHandlerbind.cause());
                          registerCallbackSubscriptionResponse.put(Constants.ERROR,
                              "Binding Failed");
                          handler.handle(
                              Future.failedFuture(registerCallbackSubscriptionResponse.toString()));
                        }
                      });
                    }
                  } else {
                    logger.error("failed :: Invalid (or) NULL routingKey");
                    registerCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
                    handler.handle(Future.succeededFuture(registerCallbackSubscriptionResponse));
                  }
                }
              }
            }
          });
    } else {
      logger.error("Error in payload");
      registerCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.failedFuture(registerCallbackSubscriptionResponse.toString()));
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
      JsonObject requestjson = new JsonObject();
      requestjson.put(Constants.QUEUE_NAME, queueName);

      for (Object currentEntity : entitites) {
        String routingKey = (String) currentEntity;
        logger.info("routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            logger.error("failed :: Invalid (or) NULL routingKey");
            updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
            handler.handle(Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
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

                  updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                  handler
                      .handle(Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
                } else if (totalBindSuccess == totalBindCount) {
                  pgclient
                      .preparedQuery(
                          " UPDATE registercallback SET entities = $1 WHERE subscriptionID = $2")
                      .execute(Tuple.of(entitites, subscriptionID), ar -> {
                        if (ar.succeeded()) {
                          String exchangename = "callback.notification";
                          String routingkey = "update";

                          JsonObject jsonpg = new JsonObject();
                          jsonpg.put("body", publishjson.toString());

                          client.basicPublish(exchangename, routingkey, jsonpg, resultHandler -> {

                            if (resultHandler.succeeded()) {
                              updateCallbackSubscriptionResponse.put("subscriptionID",
                                  subscriptionID);
                              logger.info("Message published to queue");
                              handler.handle(
                                  Future.succeededFuture(updateCallbackSubscriptionResponse));
                            } else {
                              logger.info("Message published failed");
                              updateCallbackSubscriptionResponse.put("messagePublished", "failed");
                              handler.handle(Future
                                  .failedFuture(updateCallbackSubscriptionResponse.toString()));
                            }
                          });

                        } else {
                          logger.error("failed ::" + ar.cause().getMessage());
                          updateCallbackSubscriptionResponse.put(Constants.ERROR,
                              "duplicate key value violates unique constraint");
                          handler.handle(
                              Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
                        }
                      });
                }
              } else if (resultHandlerbind.failed()) {
                logger.error("failed ::" + resultHandlerbind.cause());
                updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                handler.handle(Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
              }
            });
          }
        } else {
          logger.error("failed :: Invalid (or) NULL routingKey");
          updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
          handler.handle(Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
        }
      }

    } else {
      logger.error("Error in payload");
      updateCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.failedFuture(updateCallbackSubscriptionResponse.toString()));
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
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);

      logger.info("Call Back registration ID check starts");
      pgclient.preparedQuery("Select * FROM registercallback WHERE subscriptionID = $1")
          .execute(Tuple.of(subscriptionID), resultHandlerSelectID -> {
            if (resultHandlerSelectID.succeeded()) {
              RowSet<Row> result = resultHandlerSelectID.result();
              /* Iterating Rows for getting entity, callbackurl, username and password */
              String subscriptionIDdb = null;
              for (Row row : result) {
                subscriptionIDdb = row.getString(0);
                logger.info(subscriptionIDdb);
              }

              if (!subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
                logger.info("Call Back ID not found");
                deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Call Back ID not found");
                handler.handle(Future.failedFuture(deleteCallbackSubscriptionResponse.toString()));
              } else {
                JsonObject publishjson = new JsonObject();
                publishjson.put(Constants.SUBSCRIPTION_ID, subscriptionID);
                publishjson.put(Constants.OPERATION, "delete");

                pgclient.preparedQuery("Delete from registercallback WHERE subscriptionID = $1 ")
                    .execute(Tuple.of(subscriptionID), ar -> {
                      if (ar.succeeded()) {
                        String exchangename = "callback.notification";
                        String routingkey = "delete";
                        JsonObject jsonpg = new JsonObject();
                        jsonpg.put("body", publishjson.toString());
                        client.basicPublish(exchangename, routingkey, jsonpg, resultHandler -> {
                          if (resultHandler.succeeded()) {
                            deleteCallbackSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
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
                        deleteCallbackSubscriptionResponse.put(Constants.ERROR, "delete failed");
                        handler.handle(Future.succeededFuture(deleteCallbackSubscriptionResponse));
                      }
                    });
              }
            }
          });
    } else {
      logger.error("Error in payload");
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
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      pgclient.preparedQuery("SELECT * FROM registercallback WHERE  subscriptionID = $1 ")
          .execute(Tuple.of(subscriptionID), ar -> {
            if (ar.succeeded()) {
              RowSet<Row> result = ar.result();
              logger.info(ar.result().size() + " rows");
              /* Iterating Rows for getting entity, callbackurl, username and password */
              if (ar.result().size() > 0) {
                for (Row row : result) {
                  String subscriptionIDdb = row.getString(0);
                  String callBackUrl = row.getString(1);
                  JsonArray entities = (JsonArray) row.getValue(2);
                  listCallbackSubscriptionResponse.put(Constants.SUBSCRIPTION_ID, subscriptionIDdb);
                  listCallbackSubscriptionResponse.put(Constants.CALLBACKURL, callBackUrl);
                  listCallbackSubscriptionResponse.put(Constants.ENTITIES, entities);
                }
                handler.handle(Future.succeededFuture(listCallbackSubscriptionResponse));
              } else {
                listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
                handler.handle(Future.failedFuture(listCallbackSubscriptionResponse.toString()));
              }
            } else {
              listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
              handler.handle(Future.failedFuture(listCallbackSubscriptionResponse.toString()));
            }
          });
    } else {
      logger.error("Error in payload");
      listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      handler.handle(Future.failedFuture(listCallbackSubscriptionResponse.toString()));
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
      Future<JsonObject> result = rabbitMQStreamingClient.createExchange(request, vhost);
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
      Future<JsonObject> result = rabbitMQStreamingClient.deleteExchange(request, vhost);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listExchangeSubscribers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitMQStreamingClient.listExchangeSubscribers(request, vhost);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitMQStreamingClient.createQueue(request, vhost);
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
      Future<JsonObject> result = rabbitMQStreamingClient.deleteQueue(request, vhost);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService bindQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitMQStreamingClient.bindQueue(request, vhost);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService unbindQueue(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitMQStreamingClient.unbindQueue(request, vhost);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService createvHost(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    if (request != null && !request.isEmpty()) {
      Future<JsonObject> result = rabbitMQStreamingClient.createvHost(request);
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
      Future<JsonObject> result = rabbitMQStreamingClient.deletevHost(request);
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
   * {@inheritDoc}
   */

  @Override
  public DataBrokerService listvHost(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    if (request != null) {
      Future<JsonObject> result = rabbitMQStreamingClient.listvHost(request);
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
      Future<JsonObject> result = rabbitMQStreamingClient.listQueueSubscribers(request, vhost);
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
            finalResponse.put(STATUS, HttpStatus.SC_OK);
            handler.handle(Future.succeededFuture(finalResponse));
            logger.info("Message published to queue");
          } else {
            finalResponse.put(TYPE, HttpStatus.SC_BAD_REQUEST);
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
        Future<JsonObject> future1 =
            rabbitMQStreamingClient.getExchange(json.put("id", adaptor), vhost);
        future1.onComplete(ar -> {
          if (ar.result().getInteger("type") == HttpStatus.SC_OK) {
            json.put("exchangeName", adaptor);
            // exchange found, now get list of all queues which are bound with this exchange
            Future<JsonObject> future2 =
                rabbitMQStreamingClient.listExchangeSubscribers(json, vhost);
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

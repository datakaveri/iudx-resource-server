package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.BROKER_IP;
import static iudx.resource.server.databroker.util.Constants.BROKER_PORT;
import static iudx.resource.server.databroker.util.Constants.CONSUMER;
import static iudx.resource.server.databroker.util.Constants.ENTITIES;
import static iudx.resource.server.databroker.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE_NAME;
import static iudx.resource.server.databroker.util.Constants.FAILURE;
import static iudx.resource.server.databroker.util.Constants.QUEUE_DELETE_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_LIST_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_NAME;
import static iudx.resource.server.databroker.util.Constants.STREAMING_URL;
import static iudx.resource.server.databroker.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static iudx.resource.server.databroker.util.Constants.VHOST_IUDX;
import static iudx.resource.server.databroker.util.Util.getSha;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import iudx.resource.server.databroker.util.Constants;
import iudx.resource.server.databroker.util.Util;

public class SubscriptionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionService.class);

  private String url;
  // private WebClient webClient;
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
  private PostgresQLClient pgSQLClient;

  SubscriptionService(RabbitMQStreamingClient rabbitMQStreamingClient, PostgresQLClient pgSQLClient,
      String vhost) {
    this.rabbitMQStreamingClient = rabbitMQStreamingClient;
    this.pgSQLClient = pgSQLClient;
    this.vhost = vhost;
  }

  Future<JsonObject> registerStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
          LOGGER.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(ENTITIES);
          LOGGER.info("Request Access for " + entitites);
          LOGGER.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(QUEUE_NAME, queueName);

          Future<JsonObject> resultqueue = rabbitMQStreamingClient.createQueue(requestjson, vhost);
          resultqueue.onComplete(resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {

              LOGGER.info("sucess :: Create Queue " + resultHandlerqueue.result());
              JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

              if (createQueueResponse.containsKey(TITLE)
                  && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                LOGGER.error("failed ::" + resultHandlerqueue.cause());
                promise.fail(new JsonObject().put(ERROR, "Queue Creation Failed").toString());
              } else {

                LOGGER.info("Success Queue Created");

                for (Object currentEntity : entitites) {
                  String routingKey = (String) currentEntity;
                  LOGGER.info("routingKey is " + routingKey);
                  if (routingKey != null) {
                    if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                        || routingKey.split("/").length != 5) {
                      LOGGER.error("failed :: Invalid (or) NULL routingKey");

                      Future<JsonObject> resultDeletequeue =
                          rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                      resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                        if (resultHandlerDeletequeue.succeeded()) {
                          promise
                              .fail(new JsonObject().put(ERROR, "Invalid routingKey").toString());
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
                          LOGGER.info("sucess :: totalBindSuccess " + totalBindSuccess
                              + resultHandlerbind.result());

                          JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                          if (bindResponse.containsKey(TITLE)
                              && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                            LOGGER.error("failed ::" + resultHandlerbind.cause());
                            Future<JsonObject> resultDeletequeue =
                                rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                            resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                              if (resultHandlerDeletequeue.succeeded()) {
                                promise
                                    .fail(new JsonObject().put(ERROR, "Binding Failed").toString());
                              }
                            });
                          } else if (totalBindSuccess == totalBindCount) {
                            registerStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                            registerStreamingSubscriptionResponse.put(STREAMING_URL, streamingUrl);
                            promise.complete(registerStreamingSubscriptionResponse);
                          }
                        } else if (resultHandlerbind.failed()) {
                          LOGGER.error("failed ::" + resultHandlerbind.cause());
                          Future<JsonObject> resultDeletequeue =
                              rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              promise
                                  .fail(new JsonObject().put(ERROR, "Binding Failed").toString());
                            }
                          });
                        }
                      });
                    }
                  } else {
                    LOGGER.error("failed :: Invalid (or) NULL routingKey");
                    Future<JsonObject> resultDeletequeue =
                        rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                    resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                      if (resultHandlerDeletequeue.succeeded()) {
                        promise.fail(new JsonObject().put(ERROR, "Invalid routingKey").toString());
                      }
                    });
                  }
                }
              }
            } else if (resultHandlerqueue.failed()) {
              LOGGER.error("failed ::" + resultHandlerqueue.cause());
              promise.fail("Queue Creation Failed");
            }
          });
        }
      });
    } else {
      LOGGER.error("Error in payload");
      promise.fail(new JsonObject().put(ERROR, "Error in payload").toString());
    }
    return promise.future();
  }


  Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
          LOGGER.info("Streaming URL is : " + streamingUrl);
          JsonArray entitites = request.getJsonArray(ENTITIES);
          LOGGER.info("Request Access for " + entitites);
          LOGGER.info("No of bindings to do : " + entitites.size());

          totalBindCount = entitites.size();
          totalBindSuccess = 0;

          requestjson.put(QUEUE_NAME, queueName);

          Future<JsonObject> deleteQueue = rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
          deleteQueue.onComplete(deleteQueuehandler -> {
            if (deleteQueuehandler.succeeded()) {
              LOGGER.info("sucess :: Deleted Queue " + deleteQueuehandler.result());

              Future<JsonObject> resultqueue =
                  rabbitMQStreamingClient.createQueue(requestjson, vhost);
              resultqueue.onComplete(resultHandlerqueue -> {
                if (resultHandlerqueue.succeeded()) {

                  LOGGER.info("sucess :: Create Queue " + resultHandlerqueue.result());
                  JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();

                  if (createQueueResponse.containsKey(TITLE)
                      && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                    LOGGER.error("failed ::" + resultHandlerqueue.cause());
                    promise.fail(new JsonObject().put(ERROR, "Queue Creation Failed").toString());
                  } else {

                    LOGGER.info("Success Queue Created");

                    for (Object currentEntity : entitites) {
                      String routingKey = (String) currentEntity;
                      LOGGER.info("routingKey is " + routingKey);
                      if (routingKey != null) {
                        if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                            || routingKey.split("/").length != 5) {
                          LOGGER.error("failed :: Invalid (or) NULL routingKey");

                          Future<JsonObject> resultDeletequeue =
                              rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              promise.fail(
                                  new JsonObject().put(ERROR, "Invalid routingKey").toString());
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
                              LOGGER.info("sucess :: totalBindSuccess " + totalBindSuccess
                                  + resultHandlerbind.result());

                              JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                              if (bindResponse.containsKey(TITLE)
                                  && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                                LOGGER.error("failed ::" + resultHandlerbind.cause());
                                Future<JsonObject> resultDeletequeue =
                                    rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    promise.fail(
                                        new JsonObject().put(ERROR, "Binding Failed").toString());
                                  }
                                });
                              } else if (totalBindSuccess == totalBindCount) {
                                updateStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                                updateStreamingSubscriptionResponse.put(STREAMING_URL,
                                    streamingUrl);
                                promise.complete(updateStreamingSubscriptionResponse);
                              }
                            } else if (resultHandlerbind.failed()) {
                              LOGGER.error("failed ::" + resultHandlerbind.cause());
                              Future<JsonObject> resultDeletequeue =
                                  rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                              resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                if (resultHandlerDeletequeue.succeeded()) {
                                  promise.fail(
                                      new JsonObject().put(ERROR, "Binding Failed").toString());
                                }
                              });
                            }
                          });
                        }
                      } else {
                        LOGGER.error("failed :: Invalid (or) NULL routingKey");
                        Future<JsonObject> resultDeletequeue =
                            rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                        resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                          if (resultHandlerDeletequeue.succeeded()) {
                            promise
                                .fail(new JsonObject().put(ERROR, "Invalid routingKey").toString());
                          }
                        });
                      }
                    }
                  }
                } else if (resultHandlerqueue.failed()) {
                  LOGGER.error("failed ::" + resultHandlerqueue.cause());
                  promise.fail("Queue Creation Failed");
                }
              });
            } else if (deleteQueuehandler.failed()) {
              LOGGER.error("failed ::" + deleteQueuehandler.cause());
              promise.fail("Queue Deletion Failed");
            }
          });
        }
      });
    } else {
      LOGGER.error("Error in payload");
      promise.fail(new JsonObject().put(ERROR, "Error in payload").toString());
    }
    return promise.future();
  }

  Future<JsonObject> appendStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject appendStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonArray entitites = request.getJsonArray(ENTITIES);
      LOGGER.info("Request Access for " + entitites);
      LOGGER.info("No of bindings to do : " + entitites.size());
      totalBindCount = entitites.size();
      totalBindSuccess = 0;
      String queueName = request.getString(SUBSCRIPTION_ID);
      requestjson.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitMQStreamingClient.listQueueSubscribers(requestjson, vhost);
      result.onComplete(resultHandlerqueue -> {
        if (resultHandlerqueue.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandlerqueue.result();
          LOGGER.info(listQueueResponse);
          if (listQueueResponse.containsKey(TITLE)
              && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            promise.fail(new JsonObject().put(ERROR, "Error in payload").toString());
          } else {
            for (Object currentEntity : entitites) {
              String routingKey = (String) currentEntity;
              LOGGER.info("routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                    || routingKey.split("/").length != 5) {
                  LOGGER.error("failed :: Invalid (or) NULL routingKey");
                  promise.fail(new JsonObject().put(ERROR, "Invalid routingKey").toString());
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
                      LOGGER.info("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());

                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(TITLE)
                          && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                        LOGGER.error("failed ::" + resultHandlerbind.cause());
                        promise.fail(new JsonObject().put(ERROR, "Binding Failed").toString());
                      } else if (totalBindSuccess == totalBindCount) {
                        appendStreamingSubscriptionResponse.put(SUBSCRIPTION_ID, queueName);
                        appendStreamingSubscriptionResponse.put(ENTITIES, entitites);
                        promise.complete(appendStreamingSubscriptionResponse);
                      }
                    } else if (resultHandlerbind.failed()) {
                      LOGGER.error("failed ::" + resultHandlerbind.cause());
                      promise.fail(new JsonObject().put(ERROR, "Binding Failed").toString());
                    }
                  });
                }
              } else {
                LOGGER.error("failed :: Invalid (or) NULL routingKey");
                Future<JsonObject> resultDeletequeue =
                    rabbitMQStreamingClient.deleteQueue(requestjson, vhost);
                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                  if (resultHandlerDeletequeue.succeeded()) {
                    promise.fail(new JsonObject().put(ERROR, "Invalid routingKey").toString());
                  }
                });
              }
            }
          }
        } else {
          LOGGER.error("Error in payload");
          promise.fail(new JsonObject().put(ERROR, "Error in payload").toString());
        }
      });
    } else {
      LOGGER.error("Error in payload");
      promise.fail(new JsonObject().put(ERROR, "Error in payload").toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteStreamingSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
            LOGGER.info("failed :: Response is " + deleteQueueResponse);
            promise.fail(deleteQueueResponse.toString());
          } else {
            deleteStreamingSubscription.put(SUBSCRIPTION_ID, queueName);
            promise.complete(deleteStreamingSubscription);
          }
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          promise.fail(new JsonObject().put(ERROR, QUEUE_DELETE_ERROR).toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> listStreamingSubscriptions(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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
            LOGGER.info("failed :: Response is " + listQueueResponse);
            promise.fail(listQueueResponse.toString());
          } else {
            LOGGER.info(listQueueResponse);
            promise.complete(listQueueResponse);
          }
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          promise.fail(new JsonObject().put(ERROR, QUEUE_LIST_ERROR).toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> registerCallbackSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
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

      LOGGER.info("Call Back registration ID check starts");
      String sql = "Select * FROM registercallback WHERE subscriptionID = $1";
      String deleteQuery = "Delete from registercallback WHERE subscriptionID = $1";
      List<Object> args = new ArrayList<>();
      // add to list in-according same order as in sql query parameter $1,$1.
      args.add(subscriptionID);
      pgSQLClient.executeAsync(sql, args).onComplete(resultHandlerSelectID -> {
        if (resultHandlerSelectID.succeeded()) {
          RowSet<Row> result = resultHandlerSelectID.result();
          /* Iterating Rows for getting entity, callbackurl, username and password */
          String subscriptionIDdb = null;
          for (Row row : result) {
            subscriptionIDdb = row.getString(0);
            LOGGER.info(subscriptionIDdb);
          }

          if (subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
            LOGGER.info("Call Back registration has duplicate ID");
            registerCallbackSubscriptionResponse.put(Constants.ERROR,
                "duplicate key value violates unique constraint");
            promise.fail(registerCallbackSubscriptionResponse.toString());
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
              LOGGER.info("routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                    || routingKey.split("/").length != 5) {
                  LOGGER.error("failed :: Invalid (or) NULL routingKey");
                  registerCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
                  promise.fail(registerCallbackSubscriptionResponse.toString());
                } else {
                  LOGGER.info("Valid ID :: Call Back registration starts");
                  String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                  JsonArray array = new JsonArray();
                  array.add(currentEntity);
                  JsonObject json = new JsonObject();
                  json.put(Constants.EXCHANGE_NAME, exchangeName);
                  json.put(Constants.QUEUE_NAME, queueName);
                  json.put(Constants.ENTITIES, array);

                  Future<JsonObject> resultbind = rabbitMQStreamingClient.bindQueue(json, vhost);
                  resultbind.onComplete(resultHandlerbind -> {
                    if (resultHandlerbind.succeeded()) {
                      totalBindSuccess += 1;
                      LOGGER.info("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());

                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                          .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {

                        LOGGER.error("failed ::" + resultHandlerbind.cause());
                        pgSQLClient.executeAsync(deleteQuery, subscriptionID)
                            .onComplete(resulthandlerdel -> {
                              if (resulthandlerdel.succeeded()) {
                                registerCallbackSubscriptionResponse.put(Constants.ERROR,
                                    "Binding Failed");
                                promise.fail(registerCallbackSubscriptionResponse.toString());
                              }
                            });
                      } else if (totalBindSuccess == totalBindCount) {
                        String query =
                            "INSERT INTO registercallback (subscriptionID  ,callbackURL ,entities ,start_time , end_time , frequency ) VALUES ($1, $2, $3, $4, $5, $6)";
                        pgSQLClient.executeAsync(query, subscriptionID, callbackUrl, entitites,
                            dateTime, dateTime, dateTime).onComplete(ar -> {
                              if (ar.succeeded()) {
                                String exchangename = "callback.notification";
                                String routingkey = "create";

                                JsonObject jsonpg = new JsonObject();
                                jsonpg.put("body", publishjson.toString());
                                rabbitMQStreamingClient.getRabbitMQClient().basicPublish(
                                    exchangename, routingkey, jsonpg, resultHandler -> {
                                      if (resultHandler.succeeded()) {
                                        registerCallbackSubscriptionResponse.put("subscriptionID",
                                            subscriptionID);
                                        LOGGER.info("Message published to queue");
                                        promise.complete(registerCallbackSubscriptionResponse);
                                      } else {
                                        pgSQLClient.executeAsync(deleteQuery, subscriptionID)
                                            .onComplete(deletepg -> {
                                              if (deletepg.succeeded()) {
                                                registerCallbackSubscriptionResponse
                                                    .put("messagePublished", "failed");
                                                promise.fail(registerCallbackSubscriptionResponse
                                                    .toString());
                                              }
                                            });
                                      }
                                    });
                              } else {
                                LOGGER.error("failed ::" + ar.cause().getMessage());
                                pgSQLClient.executeAsync(deleteQuery, subscriptionID)
                                    .onComplete(resultHandlerDeletequeuepg -> {
                                      if (resultHandlerDeletequeuepg.succeeded()) {
                                        registerCallbackSubscriptionResponse.put(Constants.ERROR,
                                            "duplicate key value violates unique constraint");
                                        promise
                                            .fail(registerCallbackSubscriptionResponse.toString());
                                      }
                                    });
                              }
                            });
                      }
                    } else if (resultHandlerbind.failed()) {
                      LOGGER.error("failed ::" + resultHandlerbind.cause());
                      registerCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                      promise.fail(registerCallbackSubscriptionResponse.toString());
                    }
                  });
                }
              } else {
                // TODO : DOUBT : why future passing even its handler failed ?.
                LOGGER.error("failed :: Invalid (or) NULL routingKey");
                registerCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
                promise.complete(registerCallbackSubscriptionResponse);
              }
            }
          }
        }
      });
    } else {
      LOGGER.error("Error in payload");
      registerCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      promise.fail(registerCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> updateCallbackSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject updateCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString("consumer");
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID = domain + "/" + getSha(userName) + "/" + request.getString("name");
      JsonObject publishjson = new JsonObject();
      publishjson.put("subscriptionID", subscriptionID);
      publishjson.put("operation", "update");
      String queueName = request.getString("queue");
      JsonArray entities = request.getJsonArray("entities");
      String updateQuery = " UPDATE registercallback SET entities = $1 WHERE subscriptionID = $2";
      totalBindCount = entities.size();
      totalBindSuccess = 0;
      JsonObject requestjson = new JsonObject();
      requestjson.put(Constants.QUEUE_NAME, queueName);

      for (Object currentEntity : entities) {
        String routingKey = (String) currentEntity;
        LOGGER.info("routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            LOGGER.error("failed :: Invalid (or) NULL routingKey");
            updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
            promise.fail(updateCallbackSubscriptionResponse.toString());
          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(currentEntity);
            JsonObject json = new JsonObject();
            json.put(Constants.EXCHANGE_NAME, exchangeName);
            json.put(Constants.QUEUE_NAME, queueName);
            json.put(Constants.ENTITIES, array);
            Future<JsonObject> resultbind = rabbitMQStreamingClient.bindQueue(json, vhost);
            resultbind.onComplete(resultHandlerbind -> {
              if (resultHandlerbind.succeeded()) {
                // count++
                totalBindSuccess += 1;
                LOGGER.info(
                    "sucess :: totalBindSuccess " + totalBindSuccess + resultHandlerbind.result());
                JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                    .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                  LOGGER.error("failed ::" + resultHandlerbind.cause());

                  updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                  promise.fail(updateCallbackSubscriptionResponse.toString());
                } else if (totalBindSuccess == totalBindCount) {
                  pgSQLClient.executeAsync(updateQuery, entities, subscriptionID).onComplete(ar -> {
                    if (ar.succeeded()) {
                      String exchangename = "callback.notification";
                      String routingkey = "update";

                      JsonObject jsonpg = new JsonObject();
                      jsonpg.put("body", publishjson.toString());

                      rabbitMQStreamingClient.getRabbitMQClient().basicPublish(exchangename,
                          routingkey, jsonpg, resultHandler -> {
                            if (resultHandler.succeeded()) {
                              updateCallbackSubscriptionResponse.put("subscriptionID",
                                  subscriptionID);
                              LOGGER.info("Message published to queue");
                              promise.complete(updateCallbackSubscriptionResponse);
                            } else {
                              LOGGER.info("Message published failed");
                              updateCallbackSubscriptionResponse.put("messagePublished", "failed");
                              promise.fail(updateCallbackSubscriptionResponse.toString());
                            }
                          });

                    } else {
                      LOGGER.error("failed ::" + ar.cause().getMessage());
                      updateCallbackSubscriptionResponse.put(Constants.ERROR,
                          "duplicate key value violates unique constraint");
                      promise.fail(updateCallbackSubscriptionResponse.toString());
                    }
                  });
                }
              } else if (resultHandlerbind.failed()) {
                LOGGER.error("failed ::" + resultHandlerbind.cause());
                updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                promise.fail(updateCallbackSubscriptionResponse.toString());
              }
            });
          }
        } else {
          LOGGER.error("failed :: Invalid (or) NULL routingKey");
          updateCallbackSubscriptionResponse.put(Constants.ERROR, "Invalid routingKey");
          promise.fail(updateCallbackSubscriptionResponse.toString());
        }
      }

    } else {
      LOGGER.error("Error in payload");
      updateCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      promise.fail(updateCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  // TODO : doubt in method as handler/promise always completed or passed, it never fails. why?
  Future<JsonObject> deleteCallbackSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String getQuery = "Select * FROM registercallback WHERE subscriptionID = $1";
    String deleteQuery = "Delete from registercallback WHERE subscriptionID = $1";
    JsonObject deleteCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      LOGGER.info("Call Back registration ID check starts");
      pgSQLClient.executeAsync(getQuery, subscriptionID).onComplete(resultHandlerSelectID -> {
        if (resultHandlerSelectID.succeeded()) {
          RowSet<Row> result = resultHandlerSelectID.result();
          /* Iterating Rows for getting entity, callbackurl, username and password */
          String subscriptionIDdb = null;
          for (Row row : result) {
            subscriptionIDdb = row.getString(0);
            LOGGER.info(subscriptionIDdb);
          }
          if (!subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
            LOGGER.info("Call Back ID not found");
            deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Call Back ID not found");
            promise.fail(deleteCallbackSubscriptionResponse.toString());
          } else {
            JsonObject publishjson = new JsonObject();
            publishjson.put(Constants.SUBSCRIPTION_ID, subscriptionID);
            publishjson.put(Constants.OPERATION, "delete");
            pgSQLClient.executeAsync(deleteQuery, subscriptionID).onComplete(ar -> {
              if (ar.succeeded()) {
                String exchangename = "callback.notification";
                String routingkey = "delete";
                JsonObject jsonpg = new JsonObject();
                jsonpg.put("body", publishjson.toString());
                rabbitMQStreamingClient.getRabbitMQClient().basicPublish(exchangename, routingkey,
                    jsonpg, resultHandler -> {
                      if (resultHandler.succeeded()) {
                        deleteCallbackSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
                            subscriptionID);
                        LOGGER.info("Message published to queue");
                      } else {
                        LOGGER.info("Message published failed");
                        deleteCallbackSubscriptionResponse.put("messagePublished", "failed");
                      }
                    });
              } else {
                LOGGER.error("failed ::" + ar.cause().getMessage());
                deleteCallbackSubscriptionResponse.put(Constants.ERROR, "delete failed");
                promise.complete(deleteCallbackSubscriptionResponse);
              }
            });
          }
        }
      });
    } else {
      LOGGER.error("Error in payload");
      deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      promise.complete(deleteCallbackSubscriptionResponse);
    }
    return promise.future();
  }

  Future<JsonObject> listCallbackSubscription(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject listCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      String query = "SELECT * FROM registercallback WHERE  subscriptionID = $1 ";
      pgSQLClient.executeAsync(query, subscriptionID).onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          LOGGER.info(ar.result().size() + " rows");
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
            promise.complete(listCallbackSubscriptionResponse);
          } else {
            listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
            promise.fail(listCallbackSubscriptionResponse.toString());
          }
        } else {
          listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
          promise.fail(listCallbackSubscriptionResponse.toString());
        }
      });
    } else {
      LOGGER.error("Error in payload");
      listCallbackSubscriptionResponse.put(Constants.ERROR, "Error in payload");
      promise.fail(listCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

}

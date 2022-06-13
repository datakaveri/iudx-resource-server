package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.BAD_REQUEST_CODE;
import static iudx.resource.server.databroker.util.Constants.BAD_REQUEST_DATA;
import static iudx.resource.server.databroker.util.Constants.BINDING_FAILED;
import static iudx.resource.server.databroker.util.Constants.DELETE_CALLBACK;
import static iudx.resource.server.databroker.util.Constants.DUPLICATE_KEY;
import static iudx.resource.server.databroker.util.Constants.ENTITIES;
import static iudx.resource.server.databroker.util.Constants.ERROR;
import static iudx.resource.server.databroker.util.Constants.EXCHANGE_NAME;
import static iudx.resource.server.databroker.util.Constants.FAILURE;
import static iudx.resource.server.databroker.util.Constants.INSERT_CALLBACK;
import static iudx.resource.server.databroker.util.Constants.INTERNAL_ERROR_CODE;
import static iudx.resource.server.databroker.util.Constants.INVALID_ROUTING_KEY;
import static iudx.resource.server.databroker.util.Constants.MSG_PUBLISH_FAILED;
import static iudx.resource.server.databroker.util.Constants.PAYLOAD_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_CREATE_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_DELETE_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_LIST_ERROR;
import static iudx.resource.server.databroker.util.Constants.QUEUE_NAME;
import static iudx.resource.server.databroker.util.Constants.RESULTS;
import static iudx.resource.server.databroker.util.Constants.SELECT_CALLBACK;
import static iudx.resource.server.databroker.util.Constants.SQL_ERROR;
import static iudx.resource.server.databroker.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.databroker.util.Constants.SUCCESS;
import static iudx.resource.server.databroker.util.Constants.TITLE;
import static iudx.resource.server.databroker.util.Constants.TYPE;
import static iudx.resource.server.databroker.util.Constants.UPDATE_CALLBACK;
import static iudx.resource.server.databroker.util.Constants.USER_ID;
import static iudx.resource.server.databroker.util.Constants.VHOST_IUDX;
import static iudx.resource.server.databroker.util.Util.getResponseJson;
import static iudx.resource.server.databroker.util.Util.getSha;
import java.time.OffsetDateTime;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.common.VHosts;
import iudx.resource.server.databroker.util.Constants;
import iudx.resource.server.databroker.util.PermissionOpType;

public class SubscriptionService {
  private static final Logger LOGGER = LogManager.getLogger(SubscriptionService.class);

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

  private RabbitClient rabbitClient;
  private PostgresClient pgSQLClient;

  private String amqpUrl;
  private int amqpPort;

  SubscriptionService(RabbitClient rabbitClient, PostgresClient pgSQLClient, JsonObject config) {
    this.rabbitClient = rabbitClient;
    this.pgSQLClient = pgSQLClient;
    this.vhost = config.getString(VHosts.IUDX_PROD.value);
    this.amqpUrl = config.getString("brokerAmqpIp");
    this.amqpPort = config.getInteger("brokerAmqpPort");
  }

  Future<JsonObject> registerStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#registerStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject registerStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userid = request.getString(USER_ID);
      String queueName = userid + "/" + request.getString("name");
      Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {
          JsonObject result = resultCreateUserhandler.result();
          LOGGER.debug("success :: createUserIfNotExist " + result);
          String streamingUserName = result.getString(USER_ID);
          String apiKey = result.getString("apiKey");

          JsonArray entitites = request.getJsonArray(ENTITIES);
          LOGGER.debug("Info : Request Access for " + entitites);
          LOGGER.debug("Info : No of bindings to do : " + entitites.size());
          totalBindCount = entitites.size();
          totalBindSuccess = 0;
          requestjson.put(QUEUE_NAME, queueName);
          Future<JsonObject> resultqueue = rabbitClient.createQueue(requestjson, vhost);
          resultqueue.onComplete(resultHandlerqueue -> {
            if (resultHandlerqueue.succeeded()) {
              LOGGER.debug("success :: Create Queue " + resultHandlerqueue.result());
              JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();
              if (createQueueResponse.containsKey(TITLE)
                  && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                LOGGER.error("failed ::" + resultHandlerqueue.cause());
                promise.fail(createQueueResponse.toString());
              } else {
                LOGGER.debug("Success : Success Queue Created");

                for (Object currentEntity : entitites) {
                  String routingKey = (String) currentEntity;
                  LOGGER.debug("Info : routingKey is " + routingKey);
                  if (routingKey != null) {
                    if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == "") {
                      LOGGER.error("failed :: Invalid (or) NULL routingKey");
                      Future<JsonObject> resultDeletequeue =
                          rabbitClient.deleteQueue(requestjson, vhost);
                      resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                        if (resultHandlerDeletequeue.succeeded()) {
                          promise.fail(getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA,
                              INVALID_ROUTING_KEY)
                                  .toString());
                        }
                      });
                    } else {
                      JsonArray array = new JsonArray();
                      String exchangeName;
                      if (isGroupResource(routingKey)) {
                        exchangeName = routingKey;
                        array.add(exchangeName + Constants.DATA_WILDCARD_ROUTINGKEY);
                      } else {
                        exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                        array.add(exchangeName + "/."
                            + routingKey.substring(routingKey.lastIndexOf("/") + 1));
                      }
                      JsonObject json = new JsonObject();
                      json.put(EXCHANGE_NAME, exchangeName);
                      json.put(QUEUE_NAME, queueName);
                      json.put(ENTITIES, array);
                      Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                      resultbind.onComplete(resultHandlerbind -> {
                        if (resultHandlerbind.succeeded()) {
                          // count++
                          totalBindSuccess += 1;
                          LOGGER.debug("sucess :: totalBindSuccess " + totalBindSuccess
                              + resultHandlerbind.result());

                          JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                          if (bindResponse.containsKey(TITLE)
                              && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                            LOGGER.error("failed ::" + resultHandlerbind.cause());
                            Future<JsonObject> resultDeletequeue =
                                rabbitClient.deleteQueue(requestjson, vhost);
                            resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                              if (resultHandlerDeletequeue.succeeded()) {
                                promise
                                    .fail(getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA,
                                        BINDING_FAILED)
                                            .toString());
                              }
                            });
                          } else if (totalBindSuccess == totalBindCount) {
                            rabbitClient
                                .updateUserPermissions(vhost, userid, PermissionOpType.ADD_READ,
                                    queueName)
                                .onComplete(userPermissionHandler -> {
                                  if (userPermissionHandler.succeeded()) {
                                    registerStreamingSubscriptionResponse.put(Constants.USER_NAME,
                                        streamingUserName);
                                    registerStreamingSubscriptionResponse.put(Constants.APIKEY,
                                        apiKey);
                                    registerStreamingSubscriptionResponse.put(Constants.ID,
                                        queueName);
                                    registerStreamingSubscriptionResponse.put(Constants.URL,
                                        this.amqpUrl);
                                    registerStreamingSubscriptionResponse.put(Constants.PORT,
                                        this.amqpPort);
                                    registerStreamingSubscriptionResponse.put(Constants.VHOST,
                                        this.vhost);

                                    JsonObject response = new JsonObject();
                                    response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                    response.put(TITLE, "success");
                                    response.put(RESULTS,
                                        new JsonArray().add(registerStreamingSubscriptionResponse));

                                    promise.complete(response);
                                  } else {
                                    Future<JsonObject> resultDeletequeue =
                                        rabbitClient.deleteQueue(requestjson, vhost);
                                    resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                      if (resultHandlerDeletequeue.succeeded()) {
                                        promise
                                            .fail(
                                                getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA,
                                                    BINDING_FAILED)
                                                        .toString());
                                      }
                                    });
                                  }
                                });

                          }
                        } else if (resultHandlerbind.failed()) {
                          LOGGER.error("failed ::" + resultHandlerbind.cause());
                          Future<JsonObject> resultDeletequeue =
                              rabbitClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              promise.fail(getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA,
                                  BINDING_FAILED)
                                      .toString());
                            }
                          });
                        }
                      });
                    }
                  } else {
                    LOGGER.error("failed :: Invalid (or) NULL routingKey");
                    Future<JsonObject> resultDeletequeue =
                        rabbitClient.deleteQueue(requestjson, vhost);
                    resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                      if (resultHandlerDeletequeue.succeeded()) {
                        promise.fail(
                            getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, INVALID_ROUTING_KEY)
                                .toString());
                      }
                    });
                  }
                }
              }
            } else if (resultHandlerqueue.failed()) {
              LOGGER.error("Fail ::" + resultHandlerqueue.cause());
              promise.fail(resultHandlerqueue.cause().getMessage());
            }
          });
        }
      });
    } else {
      LOGGER.error("Fail : Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, BAD_REQUEST_DATA, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> updateStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#updateStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject updateStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userid = request.getString(USER_ID);
      // String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String queueName = userid + "/" + request.getString("name");
      Future<JsonObject> resultCreateUser = rabbitClient.createUserIfNotExist(userid, VHOST_IUDX);
      resultCreateUser.onComplete(resultCreateUserhandler -> {
        if (resultCreateUserhandler.succeeded()) {
          JsonObject result = resultCreateUserhandler.result();
          LOGGER.debug("success :: createUserIfNotExist " + result);
          String streamingUserName = result.getString(USER_ID);
          String apiKey = result.getString("apiKey");

          JsonArray entitites = request.getJsonArray(ENTITIES);
          LOGGER.debug("Info : Request Access for " + entitites);
          LOGGER.debug("Info : No of bindings to do : " + entitites.size());
          totalBindCount = entitites.size();
          totalBindSuccess = 0;
          requestjson.put(QUEUE_NAME, queueName);
          Future<JsonObject> deleteQueue = rabbitClient.deleteQueue(requestjson, vhost);
          deleteQueue.onComplete(deleteQueuehandler -> {
            if (deleteQueuehandler.succeeded()) {
              LOGGER.debug("success :: Deleted Queue " + deleteQueuehandler.result());
              Future<JsonObject> resultqueue = rabbitClient.createQueue(requestjson, vhost);
              resultqueue.onComplete(resultHandlerqueue -> {
                if (resultHandlerqueue.succeeded()) {
                  LOGGER.debug("success :: Create Queue " + resultHandlerqueue.result());
                  JsonObject createQueueResponse = (JsonObject) resultHandlerqueue.result();
                  if (createQueueResponse.containsKey(TITLE)
                      && createQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                    LOGGER.error("failed ::" + resultHandlerqueue.cause());
                    promise.fail(createQueueResponse.toString());
                  } else {
                    LOGGER.debug("Success : Queue Created");
                    for (Object currentEntity : entitites) {
                      String routingKey = (String) currentEntity;
                      LOGGER.debug("Info : routingKey is " + routingKey);
                      if (routingKey != null) {
                        if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == "") {
                          LOGGER.error("failed :: Invalid (or) NULL routingKey");

                          Future<JsonObject> resultDeletequeue =
                              rabbitClient.deleteQueue(requestjson, vhost);
                          resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                            if (resultHandlerDeletequeue.succeeded()) {
                              promise.fail(
                                  getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY)
                                      .toString());
                            }
                          });
                        } else {
                          JsonArray array = new JsonArray();
                          String exchangeName;
                          if (isGroupResource(routingKey)) {
                            exchangeName = routingKey;
                            array.add(exchangeName + Constants.DATA_WILDCARD_ROUTINGKEY);
                          } else {
                            exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                            array.add(exchangeName + "/."
                                + routingKey.substring(routingKey.lastIndexOf("/") + 1));
                          }
                          JsonObject json = new JsonObject();
                          json.put(EXCHANGE_NAME, exchangeName);
                          json.put(QUEUE_NAME, queueName);
                          json.put(ENTITIES, array);

                          Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
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
                                    rabbitClient.deleteQueue(requestjson, vhost);
                                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    promise.fail(
                                        new JsonObject().put(ERROR, "Binding Failed").toString());
                                  }
                                });
                              } else if (totalBindSuccess == totalBindCount) {


                                rabbitClient
                                    .updateUserPermissions(vhost, userid, PermissionOpType.ADD_READ,
                                        queueName)
                                    .onComplete(permissionHandler -> {
                                      if (permissionHandler.succeeded()) {
                                        updateStreamingSubscriptionResponse.put(Constants.ENTITIES,
                                            entitites);
                                        
                                        JsonObject response = new JsonObject();
                                        response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                        response.put(TITLE, "success");
                                        response.put(RESULTS,
                                            new JsonArray().add(updateStreamingSubscriptionResponse));
                                        
                                        promise.complete(response);
                                      } else {
                                        LOGGER.error("failed ::" + permissionHandler.cause());
                                        Future<JsonObject> resultDeletequeue =
                                            rabbitClient.deleteQueue(requestjson, vhost);
                                        resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                          if (resultHandlerDeletequeue.succeeded()) {
                                            promise.fail(
                                                new JsonObject()
                                                    .put(ERROR, "user Permission failed")
                                                    .toString());
                                          }
                                        });
                                      }
                                    });
                              }
                            } else if (resultHandlerbind.failed()) {
                              LOGGER.error("failed ::" + resultHandlerbind.cause());
                              Future<JsonObject> resultDeletequeue =
                                  rabbitClient.deleteQueue(requestjson, vhost);
                              resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                if (resultHandlerDeletequeue.succeeded()) {
                                  promise
                                      .fail(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED)
                                          .toString());
                                }
                              });
                            }
                          });
                        }
                      } else {
                        LOGGER.error("failed :: Invalid (or) NULL routingKey");
                        Future<JsonObject> resultDeletequeue =
                            rabbitClient.deleteQueue(requestjson, vhost);
                        resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                          if (resultHandlerDeletequeue.succeeded()) {
                            promise
                                .fail(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY)
                                    .toString());
                          }
                        });
                      }
                    }
                  }
                } else if (resultHandlerqueue.failed()) {
                  LOGGER.error("failed ::" + resultHandlerqueue.cause());
                  promise.fail(
                      getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_CREATE_ERROR).toString());
                }
              });
            } else if (deleteQueuehandler.failed()) {
              LOGGER.error("failed ::" + deleteQueuehandler.cause());
              promise
                  .fail(getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR).toString());
            }
          });
        }
      });
    } else {
      LOGGER.error("Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> appendStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#appendStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject appendStreamingSubscriptionResponse = new JsonObject();
    JsonObject requestjson = new JsonObject();
    if (request != null && !request.isEmpty()) {
      JsonArray entitites = request.getJsonArray(ENTITIES);
      String userid = request.getString(USER_ID);
      LOGGER.debug("Info : Request Access for " + entitites);
      LOGGER.debug("Info : No of bindings to do : " + entitites.size());
      totalBindCount = entitites.size();
      totalBindSuccess = 0;
      String queueName = request.getString(SUBSCRIPTION_ID);
      requestjson.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.listQueueSubscribers(requestjson, vhost);
      result.onComplete(resultHandlerqueue -> {
        if (resultHandlerqueue.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandlerqueue.result();
          LOGGER.debug("Info : " + listQueueResponse);
          if (listQueueResponse.containsKey(TITLE)
              && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            promise.fail(listQueueResponse.toString());
          } else {
            for (Object currentEntity : entitites) {
              String routingKey = (String) currentEntity;
              LOGGER.debug("Info : routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == "") {
                  LOGGER.error("failed :: Invalid (or) NULL routingKey");
                  promise.fail(
                      getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY).toString());
                } else {
                  JsonArray array = new JsonArray();
                  String exchangeName;
                  if (isGroupResource(routingKey)) {
                    exchangeName = routingKey;
                    array.add(exchangeName + Constants.DATA_WILDCARD_ROUTINGKEY);
                  } else {
                    exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                    array.add(exchangeName + "/."
                        + routingKey.substring(routingKey.lastIndexOf("/") + 1));
                  }
                  JsonObject json = new JsonObject();
                  json.put(EXCHANGE_NAME, exchangeName);
                  json.put(QUEUE_NAME, queueName);
                  json.put(ENTITIES, array);

                  Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                  resultbind.onComplete(resultHandlerbind -> {
                    if (resultHandlerbind.succeeded()) {
                      // count++
                      totalBindSuccess += 1;
                      LOGGER.debug("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());

                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(TITLE)
                          && bindResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
                        LOGGER.error("failed ::" + resultHandlerbind.cause());
                        // promise.fail(new JsonObject().put(ERROR, "Binding Failed").toString());
                        promise.fail(
                            getResponseJson(BAD_REQUEST_CODE, ResponseUrn.BAD_REQUEST_URN.getUrn(),
                                BINDING_FAILED)
                                    .toString());
                      } else if (totalBindSuccess == totalBindCount) {
                        rabbitClient.updateUserPermissions(vhost, userid,
                            PermissionOpType.ADD_READ, queueName)
                            .onComplete(permissionHandler -> {
                              if (permissionHandler.succeeded()) {
                                appendStreamingSubscriptionResponse.put(Constants.ENTITIES,
                                    entitites);
                                
                                JsonObject response = new JsonObject();
                                response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
                                response.put(TITLE, "success");
                                response.put(RESULTS,
                                    new JsonArray().add(appendStreamingSubscriptionResponse));
                                
                                promise.complete(response);
                              } else {
                                LOGGER.error("failed ::" + permissionHandler.cause());
                                Future<JsonObject> resultDeletequeue =
                                    rabbitClient.deleteQueue(requestjson, vhost);
                                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                                  if (resultHandlerDeletequeue.succeeded()) {
                                    promise.fail(
                                        new JsonObject().put(ERROR, "user Permission failed")
                                            .toString());
                                  }
                                });
                              }
                            });
                      }
                    } else if (resultHandlerbind.failed()) {
                      LOGGER.error("failed ::" + resultHandlerbind.cause());
                      promise.fail(
                          getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED).toString());
                    }
                  });
                }
              } else {
                LOGGER.error("failed :: Invalid (or) NULL routingKey");
                Future<JsonObject> resultDeletequeue = rabbitClient.deleteQueue(requestjson, vhost);
                resultDeletequeue.onComplete(resultHandlerDeletequeue -> {
                  if (resultHandlerDeletequeue.succeeded()) {
                    promise.fail(
                        getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY).toString());
                  }
                });
              }
            }
          }
        } else {
          LOGGER.error("Fail : Error in payload");
          promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
        }
      });
    } else {
      LOGGER.error("Fail : Error in payload");
      promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR).toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteStreamingSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#deleteStreamingSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject deleteStreamingSubscription = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(SUBSCRIPTION_ID);
      String userid = request.getString(USER_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.deleteQueue(requestBody, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          JsonObject deleteQueueResponse = (JsonObject) resultHandler.result();
          if (deleteQueueResponse.containsKey(TITLE)
              && deleteQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            LOGGER.debug("failed :: Response is " + deleteQueueResponse);
            promise.fail(deleteQueueResponse.toString());
          } else {
            deleteStreamingSubscription.mergeIn(
                getResponseJson(ResponseUrn.SUCCESS_URN.getUrn(), HttpStatus.SC_OK, SUCCESS,
                    "Subscription deleted Successfully"));
            Future
                .future(
                    fu -> rabbitClient.updateUserPermissions(vhost, userid,
                        PermissionOpType.DELETE_READ,
                        queueName));
            promise.complete(deleteStreamingSubscription);
          }
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          promise.fail(getResponseJson(INTERNAL_ERROR_CODE, ERROR, QUEUE_DELETE_ERROR).toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> listStreamingSubscriptions(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#listStreamingSubscriptions() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString(SUBSCRIPTION_ID);
      JsonObject requestBody = new JsonObject();
      requestBody.put(QUEUE_NAME, queueName);
      Future<JsonObject> result = rabbitClient.listQueueSubscribers(requestBody, vhost);
      result.onComplete(resultHandler -> {
        if (resultHandler.succeeded()) {
          JsonObject listQueueResponse = (JsonObject) resultHandler.result();
          if (listQueueResponse.containsKey(TITLE)
              && listQueueResponse.getString(TITLE).equalsIgnoreCase(FAILURE)) {
            LOGGER.error("failed :: Response is " + listQueueResponse);
            promise.fail(listQueueResponse.toString());
          } else {
            LOGGER.debug(listQueueResponse);
            JsonObject response = new JsonObject();
            response.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
            response.put(TITLE, "success");
            response.put(RESULTS,
                new JsonArray().add(listQueueResponse));
            
            promise.complete(response);
          }
        }
        if (resultHandler.failed()) {
          LOGGER.error("failed ::" + resultHandler.cause());
          promise.fail(getResponseJson(BAD_REQUEST_CODE, ERROR, QUEUE_LIST_ERROR).toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> registerCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#registerCallbackSubscription() started");
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

      LOGGER.debug("Info : Call Back registration ID check starts");
      String query = SELECT_CALLBACK.replace("$1", subscriptionID);
      LOGGER.debug("Info : " + query);
      pgSQLClient.executeAsync(query).onComplete(resultHandlerSelectID -> {
        if (resultHandlerSelectID.succeeded()) {
          RowSet<Row> result = resultHandlerSelectID.result();
          /* Iterating Rows for getting entity, callbackurl, username and password */
          String subscriptionIDdb = null;
          for (Row row : result) {
            subscriptionIDdb = row.getString(0);
            LOGGER.debug(subscriptionIDdb);
          }
          if (subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
            LOGGER.error("error : Call Back registration has duplicate ID");
            registerCallbackSubscriptionResponse.clear()
                .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, SQL_ERROR, DUPLICATE_KEY));
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
              LOGGER.debug("routingKey is " + routingKey);
              if (routingKey != null) {
                if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
                    || routingKey.split("/").length != 5) {
                  LOGGER.error("failed :: Invalid (or) NULL routingKey");
                  registerCallbackSubscriptionResponse.clear()
                      .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, ERROR, INVALID_ROUTING_KEY));
                  promise.fail(registerCallbackSubscriptionResponse.toString());
                } else {
                  LOGGER.debug("Info : Valid ID :: Call Back registration starts");
                  String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
                  JsonArray array = new JsonArray();
                  array.add(exchangeName + Constants.DATA_WILDCARD_ROUTINGKEY);
                  JsonObject json = new JsonObject();
                  json.put(Constants.EXCHANGE_NAME, exchangeName);
                  json.put(Constants.QUEUE_NAME, queueName);
                  json.put(Constants.ENTITIES, array);

                  Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
                  resultbind.onComplete(resultHandlerbind -> {
                    if (resultHandlerbind.succeeded()) {
                      totalBindSuccess += 1;
                      LOGGER.debug("sucess :: totalBindSuccess " + totalBindSuccess
                          + resultHandlerbind.result());
                      JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                      if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                          .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                        LOGGER.error("failed ::" + resultHandlerbind.cause());
                        String deleteQuery = DELETE_CALLBACK.replace("$1", subscriptionID);
                        pgSQLClient.executeAsync(deleteQuery).onComplete(resulthandlerdel -> {
                          if (resulthandlerdel.succeeded()) {
                            registerCallbackSubscriptionResponse.clear().mergeIn(
                                getResponseJson(INTERNAL_ERROR_CODE, ERROR, BINDING_FAILED));
                            promise.fail(registerCallbackSubscriptionResponse.toString());
                          }
                        });
                      } else if (totalBindSuccess == totalBindCount) {
                        String insertQuery = INSERT_CALLBACK.replace("$1", subscriptionID)
                            .replace("$2", callbackUrl).replace("$3", entitites.toString())
                            .replace("$4", dateTime.toString()).replace("$5", dateTime.toString())
                            .replace("$6", dateTime.toString());
                        pgSQLClient.executeAsync(insertQuery).onComplete(ar -> {
                          if (ar.succeeded()) {
                            String exchangename = "callback.notification";
                            String routingkey = "create";

                            JsonObject jsonpg = new JsonObject();
                            jsonpg.put("body", publishjson.toString());
                            Buffer messageBuffer = Buffer.buffer(jsonpg.toString());
                            rabbitClient.getRabbitMQClient().basicPublish(exchangename, routingkey,
                                messageBuffer, resultHandler -> {
                                  if (resultHandler.succeeded()) {
                                    registerCallbackSubscriptionResponse.put("subscriptionID",
                                        subscriptionID);
                                    LOGGER.debug("Message published to queue");
                                    promise.complete(registerCallbackSubscriptionResponse);
                                  } else {
                                    String deleteQuery =
                                        DELETE_CALLBACK.replace("$1", subscriptionID);
                                    pgSQLClient.executeAsync(deleteQuery).onComplete(deletepg -> {
                                      if (deletepg.succeeded()) {
                                        registerCallbackSubscriptionResponse.clear()
                                            .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, ERROR,
                                                MSG_PUBLISH_FAILED));
                                        promise
                                            .fail(registerCallbackSubscriptionResponse.toString());
                                      }
                                    });
                                  }
                                });
                          } else {
                            LOGGER.error("failed ::" + ar.cause().getMessage());
                            String deleteQuery = DELETE_CALLBACK.replace("$1", subscriptionID);
                            pgSQLClient.executeAsync(deleteQuery)
                                .onComplete(resultHandlerDeletequeuepg -> {
                                  if (resultHandlerDeletequeuepg.succeeded()) {
                                    registerCallbackSubscriptionResponse.clear()
                                        .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, SQL_ERROR,
                                            DUPLICATE_KEY));
                                    promise.fail(registerCallbackSubscriptionResponse.toString());
                                  }
                                });
                          }
                        });
                      }
                    } else if (resultHandlerbind.failed()) {
                      LOGGER.error("failed ::" + resultHandlerbind.cause());
                      registerCallbackSubscriptionResponse.clear()
                          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                      promise.fail(registerCallbackSubscriptionResponse.toString());
                    }
                  });
                }
              } else {
                // TODO : DOUBT : why future passing even its handler failed ?.
                LOGGER.error("failed :: Invalid (or) NULL routingKey");
                registerCallbackSubscriptionResponse.clear()
                    .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
                promise.complete(registerCallbackSubscriptionResponse);
              }
            }
          }
        }
      });
    } else {
      LOGGER.error("Fail : Error in payload");
      registerCallbackSubscriptionResponse.clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(registerCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> updateCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#updateCallbackSubscription() started");
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
      totalBindCount = entities.size();
      totalBindSuccess = 0;
      JsonObject requestjson = new JsonObject();
      requestjson.put(Constants.QUEUE_NAME, queueName);
      for (Object currentEntity : entities) {
        String routingKey = (String) currentEntity;
        LOGGER.debug("Info : routingKey is " + routingKey);
        if (routingKey != null) {
          if (routingKey.isEmpty() || routingKey.isBlank() || routingKey == ""
              || routingKey.split("/").length != 5) {
            LOGGER.error("failed :: Invalid (or) NULL routingKey");
            updateCallbackSubscriptionResponse.clear()
                .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
            promise.fail(updateCallbackSubscriptionResponse.toString());
          } else {

            String exchangeName = routingKey.substring(0, routingKey.lastIndexOf("/"));
            JsonArray array = new JsonArray();
            array.add(exchangeName + Constants.DATA_WILDCARD_ROUTINGKEY);
            JsonObject json = new JsonObject();
            json.put(Constants.EXCHANGE_NAME, exchangeName);
            json.put(Constants.QUEUE_NAME, queueName);
            json.put(Constants.ENTITIES, array);
            Future<JsonObject> resultbind = rabbitClient.bindQueue(json, vhost);
            resultbind.onComplete(resultHandlerbind -> {
              if (resultHandlerbind.succeeded()) {
                // count++
                totalBindSuccess += 1;
                LOGGER.debug(
                    "sucess :: totalBindSuccess " + totalBindSuccess + resultHandlerbind.result());
                JsonObject bindResponse = (JsonObject) resultHandlerbind.result();
                if (bindResponse.containsKey(Constants.TITLE) && bindResponse
                    .getString(Constants.TITLE).equalsIgnoreCase(Constants.FAILURE)) {
                  LOGGER.error("failed ::" + resultHandlerbind.cause());

                  updateCallbackSubscriptionResponse.put(Constants.ERROR, "Binding Failed");
                  updateCallbackSubscriptionResponse.clear()
                      .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                  promise.fail(updateCallbackSubscriptionResponse.toString());
                } else if (totalBindSuccess == totalBindCount) {
                  String updateQuery = UPDATE_CALLBACK.replace("$1", entities.toString())
                      .replace("$2", subscriptionID);
                  pgSQLClient.executeAsync(updateQuery).onComplete(ar -> {
                    if (ar.succeeded()) {
                      String exchangename = "callback.notification";
                      String routingkey = "update";

                      JsonObject jsonpg = new JsonObject();
                      jsonpg.put("body", publishjson.toString());
                      Buffer messageBuffer = Buffer.buffer(jsonpg.toString());
                      rabbitClient.getRabbitMQClient().basicPublish(exchangename, routingkey,
                          messageBuffer, resultHandler -> {
                            if (resultHandler.succeeded()) {
                              updateCallbackSubscriptionResponse.put("subscriptionID",
                                  subscriptionID);
                              LOGGER.debug("Info : Message published to queue");
                              promise.complete(updateCallbackSubscriptionResponse);
                            } else {
                              LOGGER.error("Fail : Message published failed");
                              updateCallbackSubscriptionResponse.clear().mergeIn(
                                  getResponseJson(INTERNAL_ERROR_CODE, ERROR, MSG_PUBLISH_FAILED));
                              promise.fail(updateCallbackSubscriptionResponse.toString());
                            }
                          });
                    } else {
                      LOGGER.error("failed ::" + ar.cause().getMessage());
                      updateCallbackSubscriptionResponse.clear()
                          .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, SQL_ERROR, DUPLICATE_KEY));
                      promise.fail(updateCallbackSubscriptionResponse.toString());
                    }
                  });
                }
              } else if (resultHandlerbind.failed()) {
                LOGGER.error("failed ::" + resultHandlerbind.cause());
                updateCallbackSubscriptionResponse.clear()
                    .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, BINDING_FAILED));
                promise.fail(updateCallbackSubscriptionResponse.toString());
              }
            });
          }
        } else {
          LOGGER.error("failed :: Invalid (or) NULL routingKey");
          updateCallbackSubscriptionResponse.clear()
              .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, INVALID_ROUTING_KEY));
          promise.fail(updateCallbackSubscriptionResponse.toString());
        }
      }

    } else {
      LOGGER.error("Error in payload");
      updateCallbackSubscriptionResponse.clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(updateCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> deleteCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#deleteCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject deleteCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      LOGGER.debug("Info : Call Back registration ID check starts");
      String selectQuery = SELECT_CALLBACK.replace("$1", subscriptionID);
      pgSQLClient.executeAsync(selectQuery).onComplete(resultHandlerSelectID -> {
        if (resultHandlerSelectID.succeeded()) {
          RowSet<Row> result = resultHandlerSelectID.result();
          /* Iterating Rows for getting entity, callbackurl, username and password */
          String subscriptionIDdb = null;
          for (Row row : result) {
            subscriptionIDdb = row.getString(0);
            LOGGER.debug("Info : " + subscriptionIDdb);
          }
          if (!subscriptionID.equalsIgnoreCase(subscriptionIDdb)) {
            LOGGER.debug("Info : Call Back ID not found");
            deleteCallbackSubscriptionResponse.put(Constants.ERROR, "Call Back ID not found");
            promise.fail(deleteCallbackSubscriptionResponse.toString());
          } else {
            JsonObject publishjson = new JsonObject();
            publishjson.put(Constants.SUBSCRIPTION_ID, subscriptionID);
            publishjson.put(Constants.OPERATION, "delete");
            String deleteQuery = DELETE_CALLBACK.replace("$1", subscriptionID);
            pgSQLClient.executeAsync(deleteQuery).onComplete(ar -> {
              if (ar.succeeded()) {
                String exchangename = "callback.notification";
                String routingkey = "delete";
                JsonObject jsonpg = new JsonObject();
                jsonpg.put("body", publishjson.toString());
                Buffer messageBuffer = Buffer.buffer(jsonpg.toString());
                rabbitClient.getRabbitMQClient().basicPublish(exchangename, routingkey,
                    messageBuffer,
                    resultHandler -> {
                      if (resultHandler.succeeded()) {
                        deleteCallbackSubscriptionResponse.put(Constants.SUBSCRIPTION_ID,
                            subscriptionID);
                        LOGGER.debug("Info : Message published to queue");
                      } else {
                        LOGGER.debug("Info : Message published failed");
                        deleteCallbackSubscriptionResponse.clear().mergeIn(
                            getResponseJson(INTERNAL_ERROR_CODE, ERROR, MSG_PUBLISH_FAILED));
                      }
                      promise.complete(deleteCallbackSubscriptionResponse);
                    });
              } else {
                LOGGER.error("failed ::" + ar.cause().getMessage());
                deleteCallbackSubscriptionResponse.put(Constants.ERROR, "delete failed");
                deleteCallbackSubscriptionResponse.clear()
                    .mergeIn(getResponseJson(INTERNAL_ERROR_CODE, ERROR, FAILURE));
                promise.complete(deleteCallbackSubscriptionResponse);
              }
            });
          }
        }
      });
    } else {
      LOGGER.error("Fail : Error in payload");
      deleteCallbackSubscriptionResponse.clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(deleteCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  Future<JsonObject> listCallbackSubscription(JsonObject request) {
    LOGGER.trace("Info : SubscriptionService#listCallbackSubscription() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject listCallbackSubscriptionResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String userName = request.getString(Constants.CONSUMER);
      String domain = userName.substring(userName.indexOf("@") + 1, userName.length());
      String subscriptionID =
          domain + "/" + getSha(userName) + "/" + request.getString(Constants.NAME);
      String selectQuery = SELECT_CALLBACK.replace("$1", subscriptionID);
      pgSQLClient.executeAsync(selectQuery).onComplete(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          LOGGER.debug("Info : " + ar.result().size() + " rows");
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
            LOGGER.error("Error :payload error" + ar.cause());
            listCallbackSubscriptionResponse.clear()
                .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
            promise.fail(listCallbackSubscriptionResponse.toString());
          }
        } else {
          LOGGER.error("Error :payload error" + ar.cause());
          listCallbackSubscriptionResponse.clear()
              .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
          promise.fail(listCallbackSubscriptionResponse.toString());
        }
      });
    } else {
      LOGGER.error("Error :payload error");
      listCallbackSubscriptionResponse.clear()
          .mergeIn(getResponseJson(BAD_REQUEST_CODE, ERROR, PAYLOAD_ERROR));
      promise.fail(listCallbackSubscriptionResponse.toString());
    }
    return promise.future();
  }

  private boolean isGroupResource(String id) {
    return id.split("/").length == 4;
  }
}

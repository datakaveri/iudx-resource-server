package iudx.resource.server.callback;

import java.util.HashMap;
import org.apache.http.HttpStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
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
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

/**
 * <h1>Callback Service Service Implementation.</h1>
 * <p>
 * The Callback Service implementation in the IUDX Resource Server implements
 * the definitions of the {@link iudx.resource.server.callback.CallbackService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-07-15
 */
public class CallbackServiceImpl implements CallbackService {

  private static final Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);
  private RabbitMQClient client;
  private WebClient webClient;
  private Vertx vertx;
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int databasePoolSize;
  private PgPool pgClient;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;

  /* Cache */
  private HashMap<String, JsonObject> pgCache;

  /**
   * This is a constructor which is used by the Callback Verticle to instantiate a RabbitMQ client.
   * 
   * @param clientInstance which is a RabbitMQ client
   * @param webClientInstance which is a Vertex Web client
   * @param propObj which is a properties JsonObject
   * @param vertxInstance which is a Vertx Instance
   */
  public CallbackServiceImpl(RabbitMQClient clientInstance, WebClient webClientInstance,
      JsonObject propObj, Vertx vertxInstance) {

    logger.info("Got the RabbitMQ Client instance");
    client = clientInstance;

    JsonObject reqNotification = new JsonObject();
    reqNotification.put(Constants.QUEUE_NAME, "callback.notification");
    connectToCallbackNotificationQueue(reqNotification);

    JsonObject reqData = new JsonObject();
    reqData.put(Constants.QUEUE_NAME, "callback.data");
    connectToCallbackDataQueue(reqData);

    if (propObj != null && !propObj.isEmpty()) {
      databaseIP = propObj.getString("callbackDatabaseIP");
      databasePort = propObj.getInteger("callbackDatabasePort");
      databaseName = propObj.getString("callbackDatabaseName");
      databaseUserName = propObj.getString("callbackDatabaseUserName");
      databasePassword = propObj.getString("callbackDatabasePassword");
      databasePoolSize = propObj.getInteger("callbackpoolSize");
    }

    webClient = webClientInstance;
    vertx = vertxInstance;
  }

  /**
   * <p>
   * <h1>This method execute tasks</h1>
   * <li>Connect to RabbitMQ callback.notification Queue (callback.notification)</li>
   * <li>Create RabbitMQConsumer for consuming queue messages</li>
   * <li>Get the database operation value from message</li>
   * <li>Query Database when database operation is create|update|delete</li>
   * </p>
   *
   * @param request which is a JSON object
   * @return response which is a Future object of promise of JSON type
   * 
   */
  public Future<JsonObject> connectToCallbackNotificationQueue(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();

    if (request != null && !request.isEmpty()) {
      /* Set Queue Options */
      QueueOptions options =
          new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
      /* Get Queue Name from request */
      String queueName = request.getString(Constants.QUEUE_NAME);

      client.start(startHandler -> {
        if (startHandler.succeeded()) {
          /* Create a stream of messages from a queue */
          client.basicConsumer(queueName, options, rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
              logger.info(Constants.RABBITMQ_CONSUMER_CREATED);
              RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
              mqConsumer.handler(message -> {
                /* Message from Queue */
                Buffer body = message.body();
                logger.info(Constants.MESSAGE + Constants.COLON + message.body());
                if (body != null) {
                  JsonObject currentBodyJsonObj = null;
                  String operation = null;
                  try {
                    /* Convert message body to JsonObject */
                    currentBodyJsonObj = new JsonObject(body.toString());
                  } catch (Exception e) {
                    logger.info(Constants.JSON_PARSE_EXCEPTION + e.getCause());
                    finalResponse.put(Constants.MESSAGE, Constants.JSON_PARSE_EXCEPTION);
                    promise.fail(finalResponse.toString());
                  }

                  /* Get operation value from currentMessageJsonObj */
                  operation = currentBodyJsonObj.getString(Constants.OPERATION);

                  /* Check for operation */
                  if (operation != null && !operation.isEmpty() && !operation.isBlank()) {
                    if (operation.equals(Constants.CREATE) || operation.equals(Constants.UPDATE)
                        || operation.equals(Constants.DELETE)) {

                      /* Create request object for Query DataBase */
                      JsonObject requestObj = new JsonObject();
                      requestObj.put(Constants.TABLE_NAME, "registercallback");

                      /* Query DataBase */
                      Future<JsonObject> result = queryCallBackDataBase(requestObj);
                      result.onComplete(resultHandler -> {
                        if (resultHandler.succeeded()) {
                          logger.info(Constants.DATABASE_QUERY_RESULT + resultHandler.result());
                          finalResponse.put(Constants.DATABASE_QUERY_RESULT,
                              Constants.CACHE_UPDATE_SUCCESS);
                        } else {
                          logger.error(Constants.DATABASE_QUERY_RESULT + Constants.COLON
                              + resultHandler.cause());
                          finalResponse.put(Constants.DATABASE_QUERY_RESULT,
                              Constants.DATABASE_QUERY_FAIL);
                          promise.fail(finalResponse.toString());
                        }
                      });
                    } else {
                      logger.info(Constants.DATABASE_OPERATION_INVALID);
                      finalResponse.put(Constants.ERROR, Constants.DATABASE_OPERATION_INVALID);
                      promise.fail(finalResponse.toString());
                    }
                  } else {
                    logger.info(Constants.DATABASE_OPERATION_NOT_FOUND);
                    finalResponse.put(Constants.ERROR, Constants.DATABASE_OPERATION_NOT_FOUND);
                    promise.fail(finalResponse.toString());
                  }
                }
              });
              logger.info(Constants.QUEUE_EMPTY);
              finalResponse.put(Constants.DATABASE_QUERY_RESULT,
                  Constants.CONNECT_TO_CALLBACK_NOTIFICATION_QUEUE);
              promise.complete(finalResponse);
            } else {
              logger.error(Constants.CONSUME_QUEUE_MESSAGE_FAIL + Constants.COLON + queueName);
              logger.error(Constants.ERROR + rabbitMQConsumerAsyncResult.cause());
              finalResponse.put(Constants.ERROR,
                  Constants.CONSUME_QUEUE_MESSAGE_FAIL + Constants.COLON + queueName);
              promise.fail(finalResponse.toString());
            }
          });
        } else {
          logger.error(Constants.QUEUE_CONNECTION_FAIL + Constants.COLON + queueName);
          finalResponse.put(Constants.ERROR,
              Constants.QUEUE_CONNECTION_FAIL + Constants.COLON + queueName);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  /**
   * <p>
   * <h1>This method execute tasks</h1>
   * <li>Connect to RabbitMQ callback.data Queue (callback.data)</li>
   * <li>Create RabbitMQConsumer for consuming queue messages</li>
   * <li>Get the routing key of message</li>
   * <li>Get callbackUrl JsonObject from cache using routingKey</li>
   * <li>Send message data to callbackUrl</li>
   * </p>
   *
   * @param request which is a JSON object
   * @return response which is a Future object of promise of JSON type
   * 
   */
  public Future<JsonObject> connectToCallbackDataQueue(JsonObject request) {

    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();

    if (request != null && !request.isEmpty()) {
      /* Set Queue Options */
      QueueOptions options =
          new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);
      /* Get Queue Name from request */
      String queueName = request.getString(Constants.QUEUE_NAME);

      client.start(startHandler -> {
        if (startHandler.succeeded()) {
          /* Create a stream of messages from a queue */
          client.basicConsumer(queueName, options, rabbitMQConsumerAsyncResult -> {
            if (rabbitMQConsumerAsyncResult.succeeded()) {
              logger.info(Constants.RABBITMQ_CONSUMER_CREATED);
              RabbitMQConsumer mqConsumer = rabbitMQConsumerAsyncResult.result();
              mqConsumer.handler(message -> {
                /* Message from Queue */
                Buffer body = message.body();
                logger.info(Constants.MESSAGE + Constants.COLON + message.body());
                if (body != null) {
                  String routingKey = null;
                  JsonObject currentBodyJsonObj = null;
                  JsonObject callBackJsonObj = null;

                  /* Convert body message to JsonObject */
                  try {
                    currentBodyJsonObj = new JsonObject(body.toString());
                  } catch (Exception e) {
                    logger.error(Constants.ERROR + Constants.COLON + e.getCause());
                    finalResponse.put(Constants.ERROR, Constants.JSON_PARSE_EXCEPTION);
                    promise.fail(finalResponse.toString());
                  }

                  /* Get routingKey and currentMessageData from Message */
                  routingKey = message.envelope().routingKey();
                  currentBodyJsonObj = new JsonObject(message.body().toString());

                  /* Get callback Object from Cache */
                  callBackJsonObj = pgCache.get(routingKey);

                  logger.info(
                      Constants.ROUTING_KEY + Constants.COLON + message.envelope().routingKey());
                  logger.info(Constants.MESSAGE + Constants.COLON + currentBodyJsonObj);

                  /* Creating Request Object */
                  if (callBackJsonObj != null && !callBackJsonObj.isEmpty()) {
                    JsonObject requestObj = new JsonObject();
                    requestObj.put(Constants.CALLBACK_JSON_OBJECT, callBackJsonObj);
                    requestObj.put(Constants.CURRENT_MESSAGE_JSON_OBJECT, currentBodyJsonObj);

                    /* Send data to callback Url */
                    Future<JsonObject> result = sendDataToCallBackSubscriber(requestObj);
                    result.onComplete(resultHandler -> {
                      if (resultHandler.succeeded()) {
                        logger.info(Constants.CALLBACK_URL_RESPONSE + Constants.COLON
                            + resultHandler.result());
                        finalResponse.put(Constants.SUCCESS,
                            Constants.DATA_SEND_TO_CALLBACK_URL_SUCCESS);
                      } else {
                        logger.error(Constants.CALLBACK_URL_RESPONSE + resultHandler.cause());
                        finalResponse.put(Constants.ERROR,
                            Constants.DATA_SEND_TO_CALLBACK_URL_FAIL);
                        promise.fail(finalResponse.toString());
                      }
                    });
                  } else {
                    logger.error(
                        Constants.NO_CALLBACK_URL_FOR_ROUTING_KEY + Constants.COLON + routingKey);
                    finalResponse.put(Constants.ERROR,
                        Constants.NO_CALLBACK_URL_FOR_ROUTING_KEY + routingKey);
                    promise.fail(finalResponse.toString());
                  }
                } else {
                  logger.error(Constants.ERROR + Constants.COLON + Constants.MESSAGE_BODY_NULL);
                  finalResponse.put(Constants.ERROR, Constants.MESSAGE_BODY_NULL);
                  promise.fail(finalResponse.toString());
                }
              });
              logger.info(Constants.QUEUE_EMPTY);
              finalResponse.put(Constants.DATABASE_QUERY_RESULT,
                  Constants.CONNECT_TO_CALLBACK_DATA_QUEUE);
              promise.complete(finalResponse);
            } else {
              logger.error(Constants.ERROR + Constants.CONSUME_QUEUE_MESSAGE_FAIL + Constants.COLON
                  + queueName);
              finalResponse.put(Constants.ERROR, Constants.CONSUME_QUEUE_MESSAGE_FAIL + queueName);
              promise.fail(finalResponse.toString());
            }
          });
        } else {
          logger.error(Constants.QUEUE_CONNECTION_FAIL + Constants.COLON + queueName);
          finalResponse.put(Constants.ERROR, Constants.QUEUE_CONNECTION_FAIL + queueName);
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }


  /**
   * <p>
   * <h1>This method execute tasks</h1>
   * <li>Get callBackJsonObj and currentMessageJsonObj from request parameter</li>
   * <li>Get callBackUrl, userName and password from callBackJsonObj</li>
   * <li>Create instance of HttpRequest<Buffer> using webClient</li>
   * <li>Send message data [currentMessageJsonObj] to callbackUrl</li>
   * </p>
   *
   * @param request which is a JSON object
   * @return response which is a Future object of promise of JSON type
   * 
   */
  public Future<JsonObject> sendDataToCallBackSubscriber(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();

    if (request != null && !request.isEmpty()) {
      /* Getting entityObj */
      String callBackUrl = null;
      String userName = null;
      String password = null;
      HttpRequest<Buffer> webRequest = null;

      JsonObject callBack_JsonObj = request.getJsonObject(Constants.CALLBACK_JSON_OBJECT);
      JsonObject currentMessage_JsonObj =
          request.getJsonObject(Constants.CURRENT_MESSAGE_JSON_OBJECT);

      if (callBack_JsonObj != null && !callBack_JsonObj.isEmpty()) {
        callBackUrl = callBack_JsonObj.getString(Constants.CALLBACK_URL);
        userName = callBack_JsonObj.getString(Constants.USER_NAME);
        password = callBack_JsonObj.getString(Constants.PASSWORD);
      }

      try {
        if (callBackUrl != null && !callBackUrl.isEmpty() && !callBackUrl.isBlank()) {
          if (userName != null && password != null && !userName.isBlank() && !password.isBlank()) {
            webRequest =
                webClient.postAbs(callBackUrl.toString()).basicAuthentication(userName, password);
          } else {
            webRequest = webClient.postAbs(callBackUrl.toString());
          }

          if (webRequest != null) {
            /* Set Request Header */
            webRequest.putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
            /* Send data to callback URL */
            webRequest.sendJsonObject(currentMessage_JsonObj, handler -> {
              if (handler.succeeded()) {
                HttpResponse<Buffer> result = handler.result();
                if (result != null) {
                  int status = result.statusCode();
                  if (status == HttpStatus.SC_OK) {
                    /* Callback URL 200 OK Status */
                    String responseBody = result.bodyAsString();
                    logger.info(Constants.RESPONSE_BODY + Constants.COLON + Constants.NEW_LINE
                        + responseBody);
                    logger.info(Constants.STATUS + Constants.COLON + status);
                    finalResponse.put(Constants.TYPE, status);
                    finalResponse.put(Constants.TITLE, Constants.SUCCESS);
                    finalResponse.put(Constants.DETAIL, Constants.CALLBACK_SUCCESS);
                    logger.info(Constants.CALLBACK_URL_RESPONSE + finalResponse);
                    promise.complete(finalResponse);
                  } else if (status == HttpStatus.SC_NOT_FOUND) {
                    /* Callback URL not found */
                    finalResponse.put(Constants.TYPE, status);
                    finalResponse.put(Constants.TITLE, Constants.FAILURE);
                    finalResponse.put(Constants.DETAIL, Constants.CALLBACK_URL_NOT_FOUND);
                    logger.info(Constants.CALLBACK_URL_RESPONSE + finalResponse);
                    promise.fail(finalResponse.toString());
                  } else {
                    /* some other issue */
                    finalResponse.put(Constants.TYPE, status);
                    finalResponse.put(Constants.TITLE, Constants.FAILURE);
                    finalResponse.put(Constants.DETAIL, result.statusMessage());
                    logger.info(Constants.CALLBACK_URL_RESPONSE + finalResponse);
                    promise.fail(finalResponse.toString());
                  }
                } else {
                  logger.error(Constants.ERROR + handler.cause().getMessage());
                  finalResponse.put(Constants.ERROR, Constants.CALLBACK_URL_RESPONSE_NULL);
                  promise.fail(finalResponse.toString());
                }
              } else {
                logger.error(Constants.ERROR + handler.cause().getMessage());
                finalResponse.put(Constants.ERROR, Constants.CONNECT_TO_CALLBACK_URL_FAIL);
                promise.fail(finalResponse.toString());
              }
            });
          } else {
            logger.error(
                Constants.ERROR + Constants.COLON + Constants.CREATE_CALLBACK_REQUEST_OBJECT_FAIL);
            finalResponse.put(Constants.ERROR, Constants.CREATE_CALLBACK_REQUEST_OBJECT_FAIL);
            promise.fail(finalResponse.toString());
          }
        } else {
          logger.error(Constants.CALLBACK_URL_INVALID);
          finalResponse.put(Constants.ERROR, Constants.CALLBACK_URL_INVALID);
          promise.fail(finalResponse.toString());
        }
      } catch (Exception e) {
        logger.error(Constants.DATA_SEND_TO_CALLBACK_URL_FAIL + e.getCause());
        finalResponse.put(Constants.ERROR, Constants.DATA_SEND_TO_CALLBACK_URL_FAIL);
        promise.fail(finalResponse.toString());
      }
    }
    return promise.future();
  }

  /* Create Cache for callback */
  private void createCache() {
    pgCache = new HashMap<String, JsonObject>();
  }

  /* Update Cache for callback */
  private void updateCache(String entity, JsonObject callBackDataObj) {
    if (pgCache == null) {
      createCache();
    }

    if (pgCache != null) {
      pgCache.put(entity, callBackDataObj);
    }
  }

  /* Delete Cache for callback */
  private void clearCacheData() {
    pgCache.clear();
  }

  /**
   * <p>
   * <h1>This method execute tasks</h1>
   * <li>Create instance of pgClient and Query callback database</li>
   * <li>Update Cache for entity and callBackDataObj</li>
   * </p>
   * 
   * @param request which is a JSON object
   * @return response which is a Future object of promise of JSON type
   * 
   */
  public Future<JsonObject> queryCallBackDataBase(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();

    /* Get table name for request object */
    String tableName = request.getString(Constants.TABLE_NAME);

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(databasePoolSize);
    }

    /* Create the client pool */
    pgClient = PgPool.pool(vertx, connectOptions, poolOptions);

    if (pgClient != null) {
      try {
        /* Execute simple query */
        pgClient.preparedQuery("SELECT * FROM " + tableName).execute(action -> {
          if (action.succeeded()) {
            logger.info(Constants.EXECUTING_SQL_QUERY + Constants.COLON + tableName);
            /* Rows in Table */
            RowSet<Row> rows = action.result();
            logger.info(Constants.FETCH_DATA_FROM_DATABASE);
            logger.info(Constants.ROWS + Constants.COLON + rows.size());

            /* Clear Cache Data */
            if (pgCache != null) {
              clearCacheData();
              logger.info("Cache Data Clear.....!!!");
            }

            /* Iterating Rows */
            for (Row row : rows) {
              /* Getting entities, callBackUrl, userName and password from row */
              JsonObject callBackDataObj = new JsonObject();
              String callBackUrl = row.getString(1);
              JsonArray entities = (JsonArray) row.getValue(2);
              String userName = row.getString(6);
              String password = row.getString(7);

              /* Iterating entities JsonArray for updating Cache */
              if (entities != null) {
                entities.forEach(entity -> {
                  /* Creating entityData */
                  callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
                  callBackDataObj.put(Constants.USER_NAME, userName);
                  callBackDataObj.put(Constants.PASSWORD, password);
                  /* Update Cache for each entity */
                  if (entity != null) {
                    updateCache(entity.toString(), callBackDataObj);
                  }
                });
              }
            }
            logger.info(Constants.SUCCESS + Constants.COLON + Constants.CACHE_UPDATE_SUCCESS);
            logger.info(Constants.CACHE_DATA + Constants.COLON + pgCache);
            finalResponse.put(Constants.SUCCESS, Constants.CACHE_UPDATE_SUCCESS);
            promise.complete(finalResponse);
          } else {
            logger.info(Constants.ERROR + action.cause());
            finalResponse.put(Constants.ERROR, Constants.EXECUTE_QUERY_FAIL);
            promise.fail(finalResponse.toString());
          }
        });
      } catch (Exception e) {
        logger.info(Constants.CONNECT_DATABASE_FAIL + e.getCause());
        finalResponse.put(Constants.ERROR, Constants.CONNECT_DATABASE_FAIL);
        promise.fail(finalResponse.toString());
      } finally {
        pgClient.close();
      }
    } else {
      logger.info(Constants.ERROR + Constants.COLON + Constants.CREATE_PG_CLIENT_OBJECT_FAIL);
      finalResponse.put(Constants.ERROR, Constants.CREATE_PG_CLIENT_OBJECT_FAIL);
      promise.fail(finalResponse.toString());
    }
    return promise.future();
  }
}

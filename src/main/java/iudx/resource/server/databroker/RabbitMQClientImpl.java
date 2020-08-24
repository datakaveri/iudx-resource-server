package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.*;
import java.util.ArrayList;
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
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.databroker.util.Constants;
import iudx.resource.server.databroker.util.Util;

public class RabbitMQClientImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQClientImpl.class);

  private String username;
  private String password;
  private RabbitMQClient rabbitMQClient;
  private WebClient webClient;

  public RabbitMQClientImpl(Vertx vertx, RabbitMQOptions rabbitConfigs,
      WebClientOptions webClientOptions, JsonObject propJson) {
    this.username = propJson.getString("userName");
    this.password = propJson.getString("password");
    this.rabbitMQClient = getRabbitMQClient(vertx, rabbitConfigs);
    this.webClient = getRabbitMQWebClient(vertx, webClientOptions);
    rabbitMQClient.start(rabbitMQClientStartupHandler -> {
      if (rabbitMQClientStartupHandler.succeeded()) {
        LOGGER.info("rabbit MQ client started");
      } else if (rabbitMQClientStartupHandler.failed()) {
        LOGGER.error("rabbit MQ client startup failed.");
      }
    });
  }

  private RabbitMQClient getRabbitMQClient(Vertx vertx, RabbitMQOptions rabbitConfigs) {
    return RabbitMQClient.create(vertx, rabbitConfigs);
  }

  private WebClient getRabbitMQWebClient(Vertx vertx, WebClientOptions webClientOptions) {
    return WebClient.create(vertx, webClientOptions);
  }

  Future<JsonObject> createExchange(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQClientImpl#createExchage() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      JsonObject obj = new JsonObject();
      obj.put(TYPE, EXCHANGE_TYPE);
      obj.put(AUTO_DELETE, false);
      obj.put(DURABLE, true);
      requestAsync(REQUEST_PUT, url, obj).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          if (statusCode == HttpStatus.SC_CREATED) {
            responseJson.put(EXCHANGE, exchangeName);
          } else if (statusCode == HttpStatus.SC_NO_CONTENT) {
            responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_EXISTS);
          } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            responseJson =
                getResponseJson(statusCode, FAILURE, EXCHANGE_EXISTS_WITH_DIFFERENT_PROPERTIES);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson =
              getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_CREATE_ERROR);
          promise.fail(errorJson.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> getExchangeDetails(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQClientImpl#getExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      requestAsync(REQUEST_GET, url, null).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          if (statusCode == HttpStatus.SC_OK) {
            responseJson = new JsonObject(response.body().toString());
          } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
            responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
          } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson =
              getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_NOT_FOUND);
          promise.fail(errorJson.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> deleteExchange(JsonObject request, String vHost) {
    LOGGER.info("RabbitMQClientImpl#deleteExchange() started");
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url = "/api/exchanges/" + vHost + "/" + Util.encodedValue(exchangeName);
      requestAsync(REQUEST_DELETE, url, null).onComplete(requestHandler -> {
        if (requestHandler.succeeded()) {
          JsonObject responseJson = new JsonObject();
          HttpResponse<Buffer> response = requestHandler.result();
          int statusCode = response.statusCode();
          if (statusCode == HttpStatus.SC_NO_CONTENT) {
            responseJson = new JsonObject();
            responseJson.put(EXCHANGE, exchangeName);
          } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
            responseJson = getResponseJson(statusCode, FAILURE, EXCHANGE_NOT_FOUND);
          }
          promise.complete(responseJson);
        } else {
          JsonObject errorJson =
              getResponseJson(HttpStatus.SC_INTERNAL_SERVER_ERROR, ERROR, EXCHANGE_DELETE_ERROR);
          promise.fail(errorJson.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> listExchangeSubscribers(JsonObject request, String vhost) {
    LOGGER.info("RabbitMQClientImpl#listExchangeSubscribers() started");
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String url =
          "/api/exchanges/" + vhost + "/" + Util.encodedValue(exchangeName) + "/bindings/source";
      requestAsync(REQUEST_GET, url, null).onComplete(ar -> {
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
                  finalResponse.clear().mergeIn(
                      getResponseJson(HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND), true);
                }
              }
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(
                  getResponseJson(HttpStatus.SC_NOT_FOUND, FAILURE, EXCHANGE_NOT_FOUND), true);
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Listing of Exchange failed" + ar.cause());
          JsonObject error = getResponseJson(500, FAILURE, "Internal server error");
          promise.fail(error.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> createQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      String url = "/api/queues/" + vhost + "/" + Util.encodedValue(queueName);
      JsonObject configProp = new JsonObject();
      configProp.put(Constants.X_MESSAGE_TTL_NAME, Constants.X_MESSAGE_TTL_VALUE);
      configProp.put(Constants.X_MAXLENGTH_NAME, Constants.X_MAXLENGTH_VALUE);
      configProp.put(Constants.X_QUEUE_MODE_NAME, Constants.X_QUEUE_MODE_VALUE);
      requestAsync(REQUEST_PUT, url, configProp).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.mergeIn(getResponseJson(status, FAILURE, QUEUE_ALREADY_EXISTS), true);
            } else if (status == HttpStatus.SC_BAD_REQUEST) {
              finalResponse.mergeIn(
                  getResponseJson(status, FAILURE, QUEUE_ALREADY_EXISTS_WITH_DIFFERENT_PROPERTIES),
                  true);
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Creation of Queue failed" + ar.cause());
          finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_CREATE_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> deleteQueue(JsonObject request, String vhost) {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject finalResponse = new JsonObject();
    if (request != null && !request.isEmpty()) {
      String queueName = request.getString("queueName");
      String url = "/api/queues/" + vhost + "/" + Util.encodedValue(queueName);
      requestAsync(REQUEST_DELETE, url, null).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.QUEUE, queueName);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(getResponseJson(status, FAILURE, QUEUE_DOES_NOT_EXISTS));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Deletion of Queue failed" + ar.cause());
          finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_DELETE_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> bindQueue(JsonObject request, String vhost) {
    JsonObject finalResponse = new JsonObject();
    JsonObject requestBody = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      String url = "/api/bindings/" + vhost + "/e/" + Util.encodedValue(exchangeName) + "/q/"
          + Util.encodedValue(queueName);
      for (Object rkey : entities) {
        requestBody.put("routing_key", rkey.toString());
        requestAsync(REQUEST_POST, url, requestBody).onComplete(ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();
              LOGGER.info("Binding " + rkey.toString() + "Success. Status is " + status);
              if (status == HttpStatus.SC_CREATED) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.mergeIn(getResponseJson(status, FAILURE, QUEUE_EXCHANGE_NOT_FOUND));
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            LOGGER.error("Binding of Queue failed" + ar.cause());
            finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
            promise.fail(finalResponse.toString());
          }
        });
      }
    }
    return promise.future();
  }

  Future<JsonObject> unbindQueue(JsonObject request, String vhost) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String exchangeName = request.getString("exchangeName");
      String queueName = request.getString("queueName");
      JsonArray entities = request.getJsonArray("entities");
      int arrayPos = entities.size() - 1;
      for (Object rkey : entities) {
        String url = "/api/bindings/" + vhost + "/e/" + Util.encodedValue(exchangeName) + "/q/"
            + Util.encodedValue(queueName) + "/" + Util.encodedValue((String) rkey);
        requestAsync(REQUEST_DELETE, url, null).onComplete(ar -> {
          if (ar.succeeded()) {
            HttpResponse<Buffer> response = ar.result();
            if (response != null && !response.equals(" ")) {
              int status = response.statusCode();
              if (status == HttpStatus.SC_NO_CONTENT) {
                finalResponse.put(Constants.EXCHANGE, exchangeName);
                finalResponse.put(Constants.QUEUE, queueName);
                finalResponse.put(Constants.ENTITIES, entities);
              } else if (status == HttpStatus.SC_NOT_FOUND) {
                finalResponse.mergeIn(getResponseJson(status, FAILURE, ALL_NOT_FOUND));
              }
            }
            if (rkey == entities.getValue(arrayPos)) {
              promise.complete(finalResponse);
            }
          } else {
            LOGGER.error("Unbinding of Queue failed" + ar.cause());
            finalResponse.mergeIn(getResponseJson(500, FAILURE, QUEUE_BIND_ERROR));
            promise.fail(finalResponse.toString());
          }
        });
      }
    }
    return promise.future();
  }

  Future<JsonObject> createvHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + Util.encodedValue(vhost);
      requestAsync(REQUEST_PUT, url, null).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_CREATED) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.mergeIn(getResponseJson(status, FAILURE, VHOST_ALREADY_EXISTS));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Creation of vHost failed" + ar.cause());
          finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_CREATE_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();
  }

  Future<JsonObject> deletevHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null && !request.isEmpty()) {
      String vhost = request.getString("vHost");
      String url = "/api/vhosts/" + Util.encodedValue(vhost);
      requestAsync(REQUEST_DELETE, url, null).onComplete(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          if (response != null && !response.equals(" ")) {
            int status = response.statusCode();
            if (status == HttpStatus.SC_NO_CONTENT) {
              finalResponse.put(Constants.VHOST, vhost);
            } else if (status == HttpStatus.SC_NOT_FOUND) {
              finalResponse.mergeIn(getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Deletion of vHost failed" + ar.cause());
          finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_DELETE_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }

    return promise.future();
  }

  Future<JsonObject> listvHost(JsonObject request) {
    JsonObject finalResponse = new JsonObject();
    Promise<JsonObject> promise = Promise.promise();
    if (request != null) {
      JsonArray vhostList = new JsonArray();
      String url = "/api/vhosts";
      requestAsync(REQUEST_GET, url, null).onComplete(ar -> {
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
              finalResponse.mergeIn(getResponseJson(status, FAILURE, VHOST_NOT_FOUND));
            }
          }
          promise.complete(finalResponse);
          LOGGER.info(finalResponse);
        } else {
          LOGGER.error("Listing of vHost failed" + ar.cause());
          finalResponse.mergeIn(getResponseJson(500, FAILURE, VHOST_LIST_ERROR));
          promise.fail(finalResponse.toString());
        }
      });
    }
    return promise.future();

  }

  private Future<HttpResponse<Buffer>> requestAsync(String requestType, String url,
      JsonObject requestJson) {
    LOGGER.info("RabbitMQClientImpl#requestAsync() started");
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    HttpRequest<Buffer> webRequest = createRequest(requestType, url);
    webRequest.sendJsonObject(requestJson, ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        promise.complete(response);
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  private HttpRequest<Buffer> createRequest(String requestType, String url) {
    HttpRequest<Buffer> webRequest = null;
    switch (requestType) {
      case REQUEST_GET:
        webRequest = webClient.get(url).basicAuthentication(username, password);
        break;
      case REQUEST_POST:
        webRequest = webClient.post(url).basicAuthentication(username, password);
        break;
      case REQUEST_PUT:
        webRequest = webClient.put(url).basicAuthentication(username, password);
        break;
      case REQUEST_DELETE:
        webRequest = webClient.delete(url).basicAuthentication(username, password);
        break;
      default:
        break;
    }
    return webRequest;
  }

  private JsonObject getResponseJson(int type, String title, String detail) {
    JsonObject json = new JsonObject();
    json.put(TYPE, type);
    json.put(TITLE, title);
    json.put(DETAIL, detail);
    return json;
  }

}

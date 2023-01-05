package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.resource.server.common.ResponseUrn.MISSING_TOKEN_URN;
import static iudx.resource.server.common.Util.isValidName;
import static iudx.resource.server.databroker.util.Constants.QUEUE;
import static iudx.resource.server.metering.util.Constants.EPOCH_TIME;
import static iudx.resource.server.metering.util.Constants.ISO_TIME;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.management.ManagementApi;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.MeteringService;

/**
 * Since no one is using management API, these API endpoints will be deleted.
 */

@Deprecated
public class ManagementRestApi {


  private static final Logger LOGGER = LogManager.getLogger(ManagementRestApi.class);

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService RMQbrokerService;
  private final PostgresService pgService;
  private final MeteringService auditService;
  private final ManagementApi managementApi;
  private final Api api;

  // TODO : remove managementApi class dependency [delete Management API class and call services
  // directly]
  ManagementRestApi(Vertx vertx, DataBrokerService brokerService, PostgresService pgService,
      MeteringService auditService, ManagementApi mgmtApi, Api api) {
    this.vertx = vertx;
    this.RMQbrokerService = brokerService;
    this.pgService = pgService;
    this.auditService = auditService;
    this.managementApi = mgmtApi;
    this.router = Router.router(vertx);
    this.api = api;
  }

  public Router init() {

    /* Management Api endpoints */
    // Exchange
    router
        .post(EXCHANGE_PATH)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::createExchange);
    router
        .delete(EXCHANGE_PATH + "/:exId")
        .handler(AuthHandler.create(vertx,api))
        .handler(this::deleteExchange);
    router
        .get(EXCHANGE_PATH + "/:exId")
        .handler(AuthHandler.create(vertx,api))
        .handler(this::getExchangeDetails);
    // Queue
    router
        .post(QUEUE_PATH)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::createQueue);
    router
        .delete(QUEUE_PATH + "/:queueId")
        .handler(AuthHandler.create(vertx,api))
        .handler(this::deleteQueue);
    router
        .get(QUEUE_PATH + "/:queueId")
        .handler(AuthHandler.create(vertx,api))
        .handler(this::getQueueDetails);
    // bind
    router
        .post(BIND)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::bindQueue2Exchange);
    // unbind
    router
        .post(UNBIND)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::unbindQueue2Exchange);
    // vHost
    router
        .post(VHOST)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::createVHost);
    router
        .delete(VHOST + "/:vhostId")
        .handler(AuthHandler.create(vertx,api))
        .handler(this::deleteVHost);
    

    router
        .post(RESET_PWD)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::resetPassword);

    return router;
  }


  /**
   * Create a exchange in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void createExchange(RoutingContext routingContext) {
    LOGGER.trace("Info: createExchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.info("request ::: " + requestJson);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      Future<Boolean> isValidNameResult = isValidName(requestJson.copy().getString(JSON_EXCHANGE_NAME));
      isValidNameResult.onComplete(validNameHandler -> {
        if (validNameHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.createExchange(requestJson, RMQbrokerService);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Creating exchange");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(response, ResponseType.Created.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error("Fail: Unauthorized;" + validNameHandler.cause().getMessage());
          handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_EXCHANGE_NAME);
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }

  }

  /**
   * delete an exchange in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void deleteExchange(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteExchange method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    String exchangeId = request.getParam(EXCHANGE_ID);
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      Future<JsonObject> brokerResult = managementApi.deleteExchange(exchangeId, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Deleting exchange");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }

  }

  /**
   * get exchange details from rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void getExchangeDetails(RoutingContext routingContext) {
    LOGGER.trace("Info: getExchange method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    LOGGER.debug("Info: request :: ;" + request);
    LOGGER.debug("Info: request json :: ;" + requestJson);
    String exchangeId = request.getParam(EXCHANGE_ID);
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceID);
    HttpServerResponse response = routingContext.response();
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.getExchangeDetails(exchangeId, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Getting exchange details");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * create a queue in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void createQueue(RoutingContext routingContext) {
    LOGGER.trace("Info: createQueue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<Boolean> validNameResult =
          isValidName(requestJson.copy().getString(JSON_QUEUE_NAME));
      validNameResult.onComplete(validNameHandler -> {
        if (validNameHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.createQueue(requestJson, RMQbrokerService);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Creating Queue");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(response, ResponseType.Created.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error("Fail: Bad request");
          handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_EXCHANGE_NAME);
        }

      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * delete a queue in rabbit MQ.
   *
   * @param routingContext routingContext.
   */
  private void deleteQueue(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteQueue method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.deleteQueue(queueId, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Deleting Queue");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * get queue details from rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void getQueueDetails(RoutingContext routingContext) {
    LOGGER.debug("Info: getQueueDetails method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.getQueueDetails(queueId, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Getting Queue Details");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad Request;" + brokerResultHandler.cause());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * bind queue to exchange in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void bindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.trace("Info: bindQueue2Exchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/bind");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult =
          managementApi.bindQueue2Exchange(requestJson, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: binding queue to exchange");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Created.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * unbind a queue from an exchange in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void unbindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.trace("Info: unbindQueue2Exchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/unbind");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult =
          managementApi.unbindQueue2Exchange(requestJson, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Unbinding queue to exchange");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Created.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * create a vhost in rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void createVHost(RoutingContext routingContext) {
    LOGGER.trace("Info: createVHost method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/vhost");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<Boolean> validNameResult = isValidName(requestJson.copy().getString(JSON_VHOST));
      validNameResult.onComplete(validNameHandler -> {
        if (validNameHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.createVHost(requestJson, RMQbrokerService);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Creating vhost");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(response, ResponseType.Created.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error("Fail: Unauthorized;" + validNameHandler.cause().getMessage());
          handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_EXCHANGE_NAME);
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }

  }

  /**
   * delete vhost from rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void deleteVHost(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteVHost method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/vhost");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String vhostId = routingContext.request().getParam(JSON_VHOST_ID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.deleteVHost(vhostId, RMQbrokerService);
      brokerResult.onComplete(brokerResultHandler -> {
        if (brokerResultHandler.succeeded()) {
          LOGGER.info("Success: Deleting vhost");
          Future.future(fu -> updateAuditTable(routingContext));
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              brokerResultHandler.result().toString());
        } else if (brokerResultHandler.failed()) {
          LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
          processBackendResponse(response, brokerResultHandler.cause().getMessage());
        }
      });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  


  public void resetPassword(RoutingContext routingContext) {
    LOGGER.trace("Info: resetPassword method started");

    HttpServerResponse response = routingContext.response();
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    JsonObject request = new JsonObject();
    request.put(USER_ID, authInfo.getString(USER_ID));

    RMQbrokerService.resetPassword(request, handler -> {
      if (handler.succeeded()) {
        handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
      } else {
        handleResponse(response, UNAUTHORIZED, INVALID_TOKEN_URN);
      }
    });
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {

    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.trace("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = ResponseUrn.fromCode(urnTitle);
      } else {
        urn = ResponseUrn.fromCode(type + "");
      }
      // return urn in body
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }

  }

  private Future<Void> updateAuditTable(RoutingContext context) {
    Promise<Void> promise = Promise.promise();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");

    JsonObject request = new JsonObject();
    ZonedDateTime zst = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
    long time = zst.toInstant().toEpochMilli();
    String isoTime = zst.truncatedTo(ChronoUnit.SECONDS).toString();

    request.put(EPOCH_TIME,time);
    request.put(ISO_TIME,isoTime);
    request.put(USER_ID, authInfo.getValue(USER_ID));
    request.put(ID, authInfo.getValue(ID));
    request.put(API, authInfo.getValue(API_ENDPOINT));
    request.put(RESPONSE_SIZE,0);

    auditService.insertMeteringValuesInRMQ(request, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("message published in RMQ.");
        promise.complete();
      } else {
        LOGGER.error("failed to publish message in RMQ.");
        promise.complete();
      }
    });

    return promise.future();
  }
}

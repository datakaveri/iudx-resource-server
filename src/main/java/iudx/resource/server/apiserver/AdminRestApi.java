package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.BROKER_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.METERING_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.TOKEN_INVALID_EX;
import static iudx.resource.server.common.Constants.TOKEN_INVALID_EX_ROUTING_KEY;
import static iudx.resource.server.common.Constants.UNIQUE_ATTR_EX;
import static iudx.resource.server.common.Constants.UNIQUE_ATTR_EX_ROUTING_KEY;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.SUCCESS;
import static iudx.resource.server.common.ResponseUrn.BAD_REQUEST_URN;
import static iudx.resource.server.common.ResponseUrn.SUCCESS_URN;
import static iudx.resource.server.database.postgres.Constants.DELETE_UNIQUE_ATTR_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_REVOKE_TOKEN_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_UNIQUE_ATTR_SQL;
import static iudx.resource.server.database.postgres.Constants.UPDATE_UNIQUE_ATTR_SQL;
import static iudx.resource.server.metering.util.Constants.EPOCH_TIME;
import static iudx.resource.server.metering.util.Constants.ISO_TIME;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import iudx.resource.server.common.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.MeteringService;

public final class AdminRestApi {

  private static final Logger LOGGER = LogManager.getLogger(AdminRestApi.class);

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService RMQbrokerService;
  private final PostgresService pgService;
  private final MeteringService auditService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Api api;

  AdminRestApi(Vertx vertx, Router router, Api api) {
    this.vertx = vertx;
    this.router = router;
    this.RMQbrokerService = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    this.auditService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    this.pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    this.api = api;
  }

  public Router init() {
    router
        .post(REVOKE_TOKEN)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::handleRevokeTokenRequest);

    router
        .post(RESOURCE_ATTRIBS)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::createUniqueAttribute);

    router
        .put(RESOURCE_ATTRIBS)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::updateUniqueAttribute);

    router
        .delete(RESOURCE_ATTRIBS)
        .handler(AuthHandler.create(vertx,api))
        .handler(this::deleteUniqueAttribute);

    return router;
  }


  private void handleRevokeTokenRequest(RoutingContext context) {

    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.body().asJsonObject();

    StringBuilder query = new StringBuilder(INSERT_REVOKE_TOKEN_SQL
        .replace("$1", requestBody.getString("sub"))
        .replace("$2", LocalDateTime.now().toString()));


    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("sub", requestBody.getString("sub"));
    rmqMessage.put("expiry", LocalDateTime.now().toString());

    LOGGER.debug("query : " + query.toString());
    pgService.executeQuery(query.toString(), pgHandler -> {
      if (pgHandler.succeeded()) {
        RMQbrokerService.publishMessage(rmqMessage, TOKEN_INVALID_EX, TOKEN_INVALID_EX_ROUTING_KEY,
            rmqHandler -> {
              if (rmqHandler.succeeded()) {
                Future.future(fu -> updateAuditTable(context));
                handleResponse(response, SUCCESS, SUCCESS_URN);
              } else {
                LOGGER.error(rmqHandler.cause());
                try {
                  Response resp =
                      objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                  handleResponse(response, resp);
                } catch (JsonProcessingException e) {
                  LOGGER.error("Failure message not in format [type,title,detail]");
                  handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                }
              }
            });
      } else {
        try {
          Response resp = objectMapper.readValue(pgHandler.cause().getMessage(), Response.class);
          handleResponse(response, resp);
        } catch (JsonProcessingException e) {
          handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
        }
      }
    });
  }

  private void createUniqueAttribute(RoutingContext context) {
    LOGGER.trace("createUniqueAttribute() started");
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.CREATE);

    StringBuilder query = new StringBuilder(INSERT_UNIQUE_ATTR_SQL
        .replace("$1", id)
        .replace("$2", attribute));

    LOGGER.debug("query : " + query.toString());
    pgService.executeQuery(query.toString(), pghandler -> {
      if (pghandler.succeeded()) {
        RMQbrokerService.publishMessage(rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY,
            rmqHandler -> {
              if (rmqHandler.succeeded()) {
                Future.future(fu -> updateAuditTable(context));
                handleResponse(response, SUCCESS, SUCCESS_URN);
              } else {
                LOGGER.error(rmqHandler.cause());
                try {
                  Response resp =
                      objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                  handleResponse(response, resp);
                } catch (JsonProcessingException e) {
                  handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                }
              }
            });
      } else {
        LOGGER.error(pghandler.cause());
        try {
          Response resp = objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
          handleResponse(response, resp);
        } catch (JsonProcessingException e) {
          handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
        }
      }
    });
  }

  private void updateUniqueAttribute(RoutingContext context) {
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    JsonObject queryparams = new JsonObject().put("attribute", attribute).put("id", id);

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.UPDATE);

    pgService.executePreparedQuery(UPDATE_UNIQUE_ATTR_SQL, queryparams, pghandler -> {
      if (pghandler.succeeded()) {
        RMQbrokerService.publishMessage(rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY,
            rmqHandler -> {
              if (rmqHandler.succeeded()) {
                Future.future(fu -> updateAuditTable(context));
                handleResponse(response, SUCCESS, SUCCESS_URN);
              } else {
                LOGGER.error(rmqHandler.cause());
                try {
                  Response resp =
                      objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                  handleResponse(response, resp);
                } catch (JsonProcessingException e) {
                  handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                }
              }
            });
      } else {
        LOGGER.error(pghandler.cause());
        try {
          Response resp = objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
          handleResponse(response, resp);
        } catch (JsonProcessingException e) {
          handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
        }
      }
    });

  }

  private void deleteUniqueAttribute(RoutingContext context) {
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.body().asJsonObject();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    JsonObject queryparams = new JsonObject().put("id", id);

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.DELETE);

    pgService.executePreparedQuery(DELETE_UNIQUE_ATTR_SQL, queryparams, pghandler -> {
      if (pghandler.succeeded()) {
        RMQbrokerService.publishMessage(rmqMessage, UNIQUE_ATTR_EX, UNIQUE_ATTR_EX_ROUTING_KEY,
            rmqHandler -> {
              if (rmqHandler.succeeded()) {
                Future.future(fu -> updateAuditTable(context));
                handleResponse(response, SUCCESS, SUCCESS_URN);
              } else {
                LOGGER.error(rmqHandler.cause());
                try {
                  Response resp =
                      objectMapper.readValue(rmqHandler.cause().getMessage(), Response.class);
                  handleResponse(response, resp);
                } catch (JsonProcessingException e) {
                  handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
                }
              }
            });
      } else {
        LOGGER.error(pghandler.cause());
        try {
          Response resp = objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
          handleResponse(response, resp);
        } catch (JsonProcessingException e) {
          handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
        }
      }
    });
  }

  private void handleResponse(HttpServerResponse response, Response respObject) {
    ResponseUrn urn = ResponseUrn.fromCode(respObject.getType());
    handleResponse(response, respObject, urn.getMessage());
  }

  private void handleResponse(HttpServerResponse response, Response respObject, String message) {
    HttpStatusCode httpCode = HttpStatusCode.getByValue(respObject.getStatus());
    ResponseUrn urn = ResponseUrn.fromCode(respObject.getType());
    handleResponse(response, httpCode, urn, message);
  }


  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode statusCode,
      ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
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

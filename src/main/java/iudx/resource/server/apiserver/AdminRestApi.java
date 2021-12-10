package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.API;
import static iudx.resource.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import iudx.resource.server.common.Api;
import iudx.resource.server.common.BroadcastEventType;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
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

  AdminRestApi(Vertx vertx, DataBrokerService brokerService, PostgresService pgService,
      MeteringService auditService) {
    this.vertx = vertx;
    this.RMQbrokerService = brokerService;
    this.pgService = pgService;
    this.auditService = auditService;
    this.router = Router.router(vertx);
  }

  public Router init() {
    router
        .post(Api.REVOKE_TOKEN.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleRevokeTokenRequest);

    router
        .post(Api.RESOURCE_ATTRIBS.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::createUniqueAttribute);

    router
        .put(Api.RESOURCE_ATTRIBS.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::updateUniqueAttribute);

    router
        .delete(Api.RESOURCE_ATTRIBS.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::deleteUniqueAttribute);

    return router;
  }


  private void handleRevokeTokenRequest(RoutingContext context) {

    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.getBodyAsJson();

    JsonObject queryParams = new JsonObject()
        .put("clientId", requestBody.getString("clientId"))
        .put("rsUrl", requestBody.getString("rsUrl"))
        .put("token", requestBody.getString("token"))
        .put("expiry", authInfo.getString("expiry"));

    List<String> params = new ArrayList<>();
    params.add(requestBody.getString("clientId"));
    params.add(requestBody.getString("rsUrl"));
    params.add(requestBody.getString("token"));
    params.add(authInfo.getString("expiry"));

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("token", requestBody.getString("token"));

    pgService.executePreparedQuery(INSERT_REVOKE_TOKEN_SQL, Collections.unmodifiableList(params),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            RMQbrokerService.publishMessage(rmqMessage, TOKEN_INVALID_EX,
                TOKEN_INVALID_EX_ROUTING_KEY,
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
              Response resp =
                  objectMapper.readValue(pgHandler.cause().getMessage(), Response.class);
              handleResponse(response, resp);
            } catch (JsonProcessingException e) {
              handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
            }
          }
        });
  }

  private void createUniqueAttribute(RoutingContext context) {
    LOGGER.debug("createUniqueAttribute() started");
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.getBodyAsJson();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    JsonObject queryparams = new JsonObject().put("id", id).put("attribute", attribute);

    List<String> params = new ArrayList<>();
    params.add(id);
    params.add(attribute);
    // params.add(requestBody.getString("token"));
    // params.add(authInfo.getString("expiry"));

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.CREATE);

    pgService.executePreparedQuery(INSERT_UNIQUE_ATTR_SQL, Collections.unmodifiableList(params),
        pghandler -> {
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
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
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
    JsonObject requestBody = context.getBodyAsJson();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    if (id == null || attribute == null) {
      handleResponse(response, BAD_REQUEST, BAD_REQUEST_URN);
      return;
    }

    List<String> params = new ArrayList<>();
    params.add(attribute);
    params.add(id);


    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.UPDATE);

    pgService.executePreparedQuery(UPDATE_UNIQUE_ATTR_SQL, Collections.unmodifiableList(params),
        pghandler -> {
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
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
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
    JsonObject requestBody = context.getBodyAsJson();

    String id = requestBody.getString("id");
    String attribute = requestBody.getString("attribute");

    JsonObject queryparams = new JsonObject().put("id", id);

    List<String> params = new ArrayList<>();
    params.add(id);


    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("id", id);
    rmqMessage.put("unique-attribute", attribute);
    rmqMessage.put("eventType", BroadcastEventType.DELETE);

    pgService.executePreparedQuery(DELETE_UNIQUE_ATTR_SQL, Collections.unmodifiableList(params),
        pghandler -> {
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
              Response resp =
                  objectMapper.readValue(pghandler.cause().getMessage(), Response.class);
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
    request.put(USER_ID, authInfo.getValue(USER_ID));
    request.put(ID, authInfo.getValue(ID));
    request.put(API, authInfo.getValue(API_ENDPOINT));
    auditService.executeWriteQuery(request, handler -> {
      if (handler.succeeded()) {
        LOGGER.info("audit table updated");
        promise.complete();
      } else {
        LOGGER.error("failed to update audit table");
        promise.complete();
      }
    });

    return promise.future();
  }


}

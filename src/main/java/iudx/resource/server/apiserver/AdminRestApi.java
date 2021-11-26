package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.database.postgres.Constants.INSERT_REVOKE_TOKEN_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_UNIQUE_ATTR_SQL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Tuple;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;

public final class AdminRestApi {

  private static final Logger LOGGER = LogManager.getLogger(AdminRestApi.class);

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService RMQbrokerService;
  private final PostgresService pgService;

  AdminRestApi(Vertx vertx, DataBrokerService brokerService, PostgresService pgService) {
    this.vertx = vertx;
    this.RMQbrokerService = brokerService;
    this.pgService = pgService;
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
        .handler(this::handleRevokeTokenRequest);
    
    router
        .delete(Api.RESOURCE_ATTRIBS.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleRevokeTokenRequest);

    return router;
  }


  private void handleRevokeTokenRequest(RoutingContext context) {

    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.getBodyAsJson();

    StringBuilder sql = new StringBuilder(INSERT_REVOKE_TOKEN_SQL
        .replace("$1", requestBody.getString("clientId"))
        .replace("$2", requestBody.getString("rsUrl"))
        .replace("$3", requestBody.getString("token"))
        .replace("$4", authInfo.getString("expiry")));

    JsonObject rmqMessage = new JsonObject();
    rmqMessage.put("token", requestBody.getString("token"));

    pgService.executeQuery(sql.toString(), pgSuccessHandler -> {
      if (pgSuccessHandler.succeeded()) {
        RMQbrokerService.publishMessage(rmqMessage, "rs-token-invalidation", "rs-token-invalidation",
            rmqSuccessHandler -> {
              if (rmqSuccessHandler.succeeded()) {
                handleResponse(response, HttpStatusCode.SUCCESS, ResponseUrn.SUCCESS);
              } else {
                handleResponse(response, HttpStatusCode.BAD_REQUEST, ResponseUrn.BAD_REQUEST_URN);
              }
            });
      } else {
        handleResponse(response, HttpStatusCode.BAD_REQUEST, ResponseUrn.BAD_REQUEST_URN);
      }
    });
  }
  
  private void createUniqueAttribute(RoutingContext context) {
    HttpServerResponse response = context.response();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");
    JsonObject requestBody = context.getBodyAsJson();
    
    String id=requestBody.getString("id");
    String attribute=requestBody.getString("attribute");
    
    Tuple queryparams=Tuple.of(id,attribute);
    
    pgService.executeQuery(INSERT_UNIQUE_ATTR_SQL, queryparams, handler->{
      
    });
    

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


}

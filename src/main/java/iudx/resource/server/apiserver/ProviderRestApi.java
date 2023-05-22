package iudx.resource.server.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.apiserver.handlers.ValidationHandler;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.apiserver.util.StatusType;
import iudx.resource.server.common.*;
import iudx.resource.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.common.HttpStatusCode.getByValue;
import static iudx.resource.server.common.ResponseUrn.fromCode;

public class ProviderRestApi {
    private static final Logger LOGGER = LogManager.getLogger(ProviderRestApi.class);
    private final Vertx vertx;
    private final Router router;
    private final PostgresService pgService;
    private Api api;

    ProviderRestApi(Vertx vertx, Router router, Api api) {
        this.vertx = vertx;
        this.router = router;
        this.pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
        this.api = api;
    }

    public Router init() {
        ValidationHandler postOnboardingProviderValidation = new ValidationHandler(vertx, RequestType.PROVIDER_ONBOARDING);
        FailureHandler validationsFailureHandler = new FailureHandler();

        router.post(ONBOARDING_PROVIDER)
                .handler(postOnboardingProviderValidation)
                .handler(AuthHandler.create(vertx, api))
                .handler(this::handlPostOnbordingProvederquest)
                .failureHandler(validationsFailureHandler);


        router.get(ONBOARDING_PROVIDER_STATUS)
                //.handler(AuthHandler.create(vertx, api))
                .handler(this::handlGetStatusOnbordingProvederquest);
        return router;
    }

    private void handlPostOnbordingProvederquest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlOnbordingProvederquest method started;");
        HttpServerRequest request = routingContext.request();
        JsonObject requestJson = routingContext.body().asJsonObject();
        HttpServerResponse response = routingContext.response();
        JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
        LOGGER.debug("authInfo: "+authInfo);
        LOGGER.debug("requestJson: " + requestJson);
        String insertQuery = writeOnboardingQueryBuilder(requestJson,authInfo);
        pgService.executeQuery(
                insertQuery,
                insertPgHandler -> {
                    if (insertPgHandler.succeeded()) {
                        JsonObject resultJson = insertPgHandler.result();
                        LOGGER.debug("resultJson 59: " + resultJson);
                        handleSuccessResponse(response, ResponseType.Ok.getCode(), insertPgHandler.result().toString());
                    } else {
                        LOGGER.error(insertPgHandler.cause());
                    }

                });


    }

    private void handlGetStatusOnbordingProvederquest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlOnbordingProvederquest method started;");
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();
        String userid = request.getParam(USER_ID);
        LOGGER.debug("userid55: " + readOnboardingStatusQuery(userid));
        String getStatusQuery = readOnboardingStatusQuery(userid);
        pgService.executeQuery(
                getStatusQuery,
                selectPgHandler -> {
                    if (selectPgHandler.succeeded()) {
                        JsonObject resultJson = selectPgHandler.result();
                        LOGGER.debug("resultJson 73: " + resultJson);
                        handleSuccessResponse(response, ResponseType.Ok.getCode(), selectPgHandler.result().toString());
                    }
                });
    }

    private String writeOnboardingQueryBuilder(JsonObject requestJson, JsonObject authInfo) {
        String userid = authInfo.getString(USER_ID);
        String role = authInfo.getString(ROLE);

        StringBuilder query = new StringBuilder(
                ONBOARDING_PROVIDER_WRITE_SQL
                        .replace("$0", userid)
                        .replace("$1", role)
                        .replace("$2", StatusType.PENDING.toString())
                        .replace("$3", requestJson.toString()));

        return query.toString();
    }

    private String readOnboardingStatusQuery(String userid) {
        String primaryKey = UUID.randomUUID().toString().replace("-", "");
        StringBuilder query = new StringBuilder(SELECT_STATUS_SQL.replace("$0", userid));

        return query.toString();
    }
    private  String getStatus(String role){
        String status = StatusType.PENDING.name();
        if(role.equalsIgnoreCase("consumer")){
            status = StatusType.APPROVED.name();
        }
        return status;
    }

    public void handleResponse(HttpServerResponse response, Response respObject) {
        ResponseUrn urn = fromCode(respObject.getType());
        handleResponse(response, respObject, urn.getMessage());
    }

    private void handleResponse(HttpServerResponse response, Response respObject, String message) {
        HttpStatusCode httpCode = getByValue(respObject.getStatus());
        ResponseUrn urn = fromCode(respObject.getType());
        handleResponse(response, httpCode, urn, message);
    }

    private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
    }


    private void handleResponse(
            HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
        response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(statusCode.getValue())
                .end(generateResponse(statusCode, urn, message).toString());
    }
}

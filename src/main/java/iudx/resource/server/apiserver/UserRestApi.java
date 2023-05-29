package iudx.resource.server.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
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
import static iudx.resource.server.common.HttpStatusCode.*;
import static iudx.resource.server.common.ResponseUrn.*;

public class UserRestApi {
    private static final Logger LOGGER = LogManager.getLogger(UserRestApi.class);
    private final Vertx vertx;
    private final Router router;
    private final PostgresService pgService;
    private Api api;

    UserRestApi(Vertx vertx, Router router, Api api) {
        this.vertx = vertx;
        this.router = router;
        this.pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
        this.api = api;
    }

    public Router init() {
        ValidationHandler postOnboardingProviderValidation = new ValidationHandler(vertx, RequestType.PROVIDER_ONBOARDING);
        FailureHandler validationsFailureHandler = new FailureHandler();

        router.post(USER_REG)
                .handler(postOnboardingProviderValidation)
                // .handler(AuthHandler.create(vertx, api))
                .handler(this::handlPostUserRegRequest)
                .failureHandler(validationsFailureHandler);


        router.get(USER_REG_STATUS)
                //.handler(AuthHandler.create(vertx, api))
                .handler(this::handlGetUserStatusRquest);
        return router;
    }

    private void handlPostUserRegRequest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlOnbordingProvederquest method started;");
        HttpServerRequest request = routingContext.request();
        JsonObject requestJson = routingContext.body().asJsonObject();
        HttpServerResponse response = routingContext.response();
        //  JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
        JsonObject authInfo = new JsonObject()
                .put(USER_ID, UUID.randomUUID());

        LOGGER.debug("authInfo: " + authInfo);
        LOGGER.debug("requestJson: " + requestJson);
        String insertQuery = writeUserRegQueryBuilder(requestJson, authInfo);
        pgService.executeQuery(
                insertQuery,
                insertPgHandler -> {
                    if (insertPgHandler.succeeded()) {
                        JsonObject resultJson = insertPgHandler.result();
                        LOGGER.debug("resultJson 59: " + resultJson);
                        JsonObject respJson = new JsonObject()
                                .put(JSON_TYPE, SUCCESS.getUrn())
                                .put(JSON_TITLE, SUCCESS.getDescription())
                                .put("result", new JsonArray()
                                        .add(new JsonObject()
                                                .put(ID, authInfo.getString(USER_ID))
                                                .put("name", requestJson.getString("name"))));

                        handleSuccessResponse(response, ResponseType.Created.getCode(), respJson.toString());
                    } else {
                        processBackendResponse(response, insertPgHandler.cause().getMessage());
                    }
                });
    }

    private void handlGetUserStatusRquest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlGetStatusOnbordingProvederquest method started;");
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();
        String userid = request.getParam(USER_ID);
        LOGGER.debug("userid: " + readUserStatusQuery(userid));
        String getStatusQuery = readUserStatusQuery(userid);
        pgService.executeQuery(
                getStatusQuery,
                selectPgHandler -> {
                    if (selectPgHandler.succeeded()) {
                        JsonObject resultJson = selectPgHandler.result();
                        LOGGER.debug("resultJson: " + resultJson);
                        handleSuccessResponse(response, ResponseType.Ok.getCode(), selectPgHandler.result().toString());
                    } else {
                        processBackendResponse(response, selectPgHandler.cause().getMessage());
                    }
                });
    }

    private String writeUserRegQueryBuilder(JsonObject requestJson, JsonObject authInfo) {
        String userid = authInfo.getString(USER_ID);
        String role = IudxRole.fromRole(requestJson.getString(ROLE)).name();
        String status = getStatus(role);

        StringBuilder query = new StringBuilder(
                ONBOARDING_PROVIDER_WRITE_SQL
                        .replace("$0", userid)
                        .replace("$1", role)
                        .replace("$2", status)
                        .replace("$3", requestJson.toString()));
        LOGGER.debug("insert query: " + query);
        return query.toString();
    }

    private String readUserStatusQuery(String userid) {
        return new StringBuilder(SELECT_STATUS_SQL.replace("$0", userid)).toString();
    }

    private String getStatus(String role) {
        String status = StatusType.PENDING.name();
        if (role.equalsIgnoreCase("consumer")) {
            status = StatusType.APPROVED.name();
        }
        return status;
    }

    private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
    }

    private void processBackendResponse(HttpServerResponse response, String failureMessage) {
        LOGGER.debug("Info : " + failureMessage);
        try {
            JsonObject json = new JsonObject(failureMessage);
            int type = json.getInteger(JSON_TYPE);
            HttpStatusCode status = HttpStatusCode.getByValue(type);
            String urnTitle = json.getString(JSON_TITLE);
            ResponseUrn urn;
            if (urnTitle != null) {
                urn = fromCode(urnTitle);
            } else {
                urn = fromCode(type + "");
            }
            // return urn in body
            response
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(type)
                    .end(generateResponse(status, urn).toString());
        } catch (DecodeException ex) {
            LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
            handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
        }
    }

    private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
        handleResponse(response, code, urn, code.getDescription());
    }

    private void handleResponse(
            HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
        response
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .setStatusCode(statusCode.getValue())
                .end(generateResponse(statusCode, urn, message).toString());
    }

}

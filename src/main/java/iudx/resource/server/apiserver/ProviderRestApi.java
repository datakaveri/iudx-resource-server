package iudx.resource.server.apiserver;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.Response;
import iudx.resource.server.common.ResponseUrn;
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
        router
                .post(PROVIDER_ENDPOINT)
                //.handler(AuthHandler.create(vertx, api))
                .handler(this::handlOnbordingProvederquest);
        router
                .get(PROVIDER_ENDPOINT)
                //.handler(AuthHandler.create(vertx, api))
                .handler(this::handlGetStatusOnbordingProvederquest);
        return router;
    }

    private void handlOnbordingProvederquest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlOnbordingProvederquest method started;");
        JsonObject requestJson = routingContext.body().asJsonObject();
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();
        LOGGER.debug("requestJson: " + requestJson);

    }
    private void handlGetStatusOnbordingProvederquest(RoutingContext routingContext) {
        LOGGER.trace("Info: handlOnbordingProvederquest method started;");
        HttpServerRequest request = routingContext.request();
        HttpServerResponse response = routingContext.response();
    }

    private String writeOnboardingQueryBuilder(JsonObject payload, String userid, String role) {
        String primaryKey = UUID.randomUUID().toString().replace("-", "");
        StringBuilder query = new StringBuilder(ONBOARDING_PROVIDER_WRITE_SQL
                .replace("$1", primaryKey)
                .replace("$2", userid)
                .replace("$3", role)
                .replace("$4", "PENDING")
                .replace("$5", payload.toString())
        );

        return query.toString();
    }

    private void handleResponse(HttpServerResponse response, Response respObject) {
        ResponseUrn urn = fromCode(respObject.getType());
        handleResponse(response, respObject, urn.getMessage());
    }

    private void handleResponse(HttpServerResponse response, Response respObject, String message) {
        HttpStatusCode httpCode = getByValue(respObject.getStatus());
        ResponseUrn urn = fromCode(respObject.getType());
        handleResponse(response, httpCode, urn, message);
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

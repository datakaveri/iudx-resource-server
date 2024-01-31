package iudx.resource.server.authenticator;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

import static iudx.resource.server.authenticator.TokensForITs.*;

public class TokenSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenSetup.class);
    private static WebClient webClient;

    public static void setupTokens(String authEndpoint, String providerClientId, String providerClientSecret, String consumerClientId, String consumerClientSecret, String delegationId) {
        // Fetch tokens asynchronously and wait for all completions
        CompositeFuture.all(
                fetchToken("openResourceToken", authEndpoint, consumerClientId, consumerClientSecret),
                fetchToken("secureResourceToken", authEndpoint, consumerClientId,consumerClientSecret),
                fetchToken("adminToken", authEndpoint, consumerClientId,consumerClientSecret),
                fetchToken("adaptorToken", authEndpoint, consumerClientId,consumerClientSecret, delegationId),
                fetchToken("delegateToken", authEndpoint, consumerClientId,consumerClientSecret, delegationId),
                fetchToken("providerToken", authEndpoint, providerClientId, providerClientSecret)
        ).onComplete(result -> {
            if (result.succeeded()) {
                LOGGER.debug("Tokens setup completed successfully");
                webClient.close();
            } else {
                LOGGER.error("Error- {}", result.cause().getMessage());
                webClient.close();
            }
        });
    }

    private static Future<String> fetchToken(String userType, String authHost, String clientID, String clientSecret) {
        Promise<String> promise = Promise.promise();
        JsonObject jsonPayload = getPayload(userType);
        // Create a WebClient to make the HTTP request
        webClient = WebClient.create(Vertx.vertx(),
            new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true));

        webClient.post(443,authHost,"/auth/v1/token")
                .putHeader("Content-Type", "application/json")
                .putHeader("clientID", clientID)
                .putHeader("clientSecret", clientSecret)
                .sendJson(jsonPayload)
                .onComplete(result -> {
                    if (result.succeeded()) {
                        HttpResponse<Buffer> response = result.result();
                        if (response.statusCode() == 200) {
                        JsonObject jsonResponse = response.bodyAsJsonObject();
                        String accessToken = jsonResponse.getJsonObject("results").getString("accessToken");
                        // Store the token based on user type
                        switch (userType) {
                            case "providerToken":
                                providerToken = accessToken;
                                break;
                            case "adminToken":
                                adminToken = accessToken;
                                break;
                            case "openResourceToken":
                                openResourceToken = accessToken;
                                break;
                            case "secureResourceToken":
                                secureResourceToken = accessToken;
                        }
                            promise.complete(accessToken);
                        } else {
                            promise.fail("Failed to get token. Status code: " + response.statusCode());
                        }
                    }else {
                        LOGGER.error("Failed to fetch token", result.cause());
                        promise.fail(result.cause());
                    }
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    promise.fail(throwable);
                });

        return promise.future();
    }


    private static Future<String> fetchToken(String userType, String authHost, String clientID, String clientSecret, String delegationId) {
        Promise<String> promise = Promise.promise();
        JsonObject jsonPayload = getPayload(userType);
        // Create a WebClient to make the HTTP request for fetching delegate and adaptor tokens
        webClient = WebClient.create(Vertx.vertx(),
            new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true));

        webClient.post(443,authHost,"/auth/v1/token")
                .putHeader("Content-Type", "application/json")
                .putHeader("clientID", clientID)
                .putHeader("clientSecret", clientSecret)
                .putHeader("delegationId", delegationId)
                .sendJson(jsonPayload)
                .onComplete(result -> {
                    if (result.succeeded()) {
                        HttpResponse<Buffer> response = result.result();
                        if (response.statusCode() == 200) {
                        JsonObject jsonResponse = response.bodyAsJsonObject();
                        String accessToken = jsonResponse.getJsonObject("results").getString("accessToken");
                        // Store the token based on user type
                        switch (userType) {
                            case "adaptorToken":
                                adaptorToken = accessToken;
                                break;
                            case "delegateToken":
                                delegateToken = accessToken;
                        }
                            promise.complete(accessToken);
                        } else {
                            promise.fail("Failed to get token. Status code: " + response.statusCode());
                        }
                    }else {
                        LOGGER.error("Failed to fetch token", result.cause());
                        promise.fail(result.cause());
                    }
                })
                .onFailure(throwable -> {
                    throwable.printStackTrace();
                    promise.fail(throwable);
                });

        return promise.future();
    }

    @NotNull
    private static JsonObject getPayload(String userType) {
        JsonObject jsonPayload = new JsonObject();
        switch (userType) {
            case "openResourceToken":
                jsonPayload.put("itemId", "b58da193-23d9-43eb-b98a-a103d4b6103c");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "consumer");
                break;
            case "secureResourceToken":
                jsonPayload.put("itemId", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "consumer");
                break;
            case "adminToken":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "admin");
                break;
            case "providerToken":
                jsonPayload.put("itemId", "rs.iudx.io");
                jsonPayload.put("itemType", "resource_server");
                jsonPayload.put("role", "provider");
                break;
            case "delegateToken":
                jsonPayload.put("itemId", "83c2e5c2-3574-4e11-9530-2b1fbdfce832");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "delegate");
                break;
            case "adaptorToken":
                jsonPayload.put("itemId", "695e222b-3fae-4325-8db0-3e29d01c4fc0");
                jsonPayload.put("itemType", "resource");
                jsonPayload.put("role", "delegate");
                break;
        }
        return jsonPayload;
    }
}


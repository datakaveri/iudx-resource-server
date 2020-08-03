package iudx.resource.server.authenticator;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import org.apache.http.HttpStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The Authentication Service Implementation.
 * <h1>Authentication Service Implementation</h1>
 * <p>
 * The Authentication Service implementation in the IUDX Resource Server
 * implements the definitions of the
 * {@link iudx.resource.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final ConcurrentHashMap<String, JsonObject> tipCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> catCache = new ConcurrentHashMap<>();
    private static final Properties properties = new Properties();
    private final WebClient webClient;

    public AuthenticationServiceImpl(Vertx vertx, WebClient client) {
        webClient = client;
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }

        long oneMinute = 1000 * 60;
        vertx.setPeriodic(oneMinute * Constants.TIP_CACHE_TIMEOUT_AMOUNT, timerID -> {});
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
                                                  Handler<AsyncResult<JsonObject>> handler) {

        String token = authenticationInfo.getString("token", Constants.PUBLIC_TOKEN);
        String requestEndpoint = authenticationInfo.getString("apiEndpoint");
        if (token.equals(Constants.PUBLIC_TOKEN) && !Constants.OPEN_ENDPOINTS.contains(requestEndpoint)) {
            JsonObject result = new JsonObject();
            result.put("status", "error");
            result.put("message", "Public token cannot access requested endpoint");
            handler.handle(Future.succeededFuture(result));
            return this;
        }

        Future<JsonObject> tipResponseFut = retrieveTipResponse(token);
        Future<HashMap<String, Boolean>> catResponseFut = isOpenResource(request.getJsonArray("ids"));

        CompositeFuture.all(tipResponseFut, catResponseFut)
                .onFailure(throwable -> {
                    JsonObject result = new JsonObject();
                    result.put("status", "error");
                    result.put("message", throwable.getMessage());
                    handler.handle(Future.succeededFuture(result));
                })
                .onSuccess(compositeFuture -> {
                    JsonObject tipResponse = compositeFuture.resultAt(0);
                    HashMap<String, Boolean> catResponse = compositeFuture.resultAt(1);
                    JsonArray result = new JsonArray();

                    for (Object reqID : request.getJsonArray("ids")) {
                        String requestID = (String) reqID;
                        JsonObject tipRequest = retrieveTipRequest(requestID, tipResponse);
                        boolean isAccessible = tipRequest.isEmpty() ?
                                catResponse.getOrDefault(requestID, false) :
                                isValidEndpoint(requestEndpoint, tipRequest.getJsonArray("apis"));
                        result.add(new JsonObject()
                                .put("id", requestID)
                                .put("accessible", isAccessible));
                    }

                    JsonArray inAccessibleIDs = new JsonArray(result.stream()
                            .filter(res -> !(((JsonObject) res).getBoolean("accessible")))
                            .collect(Collectors.toList()));

                    JsonObject json = new JsonObject();
                    json.put("status", inAccessibleIDs.isEmpty() ? "success" : "error");
                    json.put("body", new JsonObject());
                    if (!inAccessibleIDs.isEmpty()) {
                        json.put("message", "Unauthorized resource IDs");
                        json.getJsonObject("body").put("rejected", inAccessibleIDs);
                    }
                    handler.handle(Future.succeededFuture(json));
                });
        return this;
    }

    private boolean isValidEndpoint(String requestEndpoint, JsonArray apis) {
        for (Object es : apis) {
            String endpoint = (String) es;
            if (endpoint.equals("/*") || endpoint.equals(requestEndpoint)) return true;
        }
        return false;
    }

    private JsonObject retrieveTipRequest(String requestID, JsonObject tipResponse) {
        for (Object r : tipResponse.getJsonArray("request")) {
            JsonObject tipRequest = (JsonObject) r;
            if (requestID.equals(tipRequest.getString("id"))) return tipRequest;
        }
        return new JsonObject();
    }

    private Future<JsonObject> retrieveTipResponse(String token) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject cacheResponse = tipCache.getOrDefault(token, new JsonObject());
        if (!cacheResponse.isEmpty()) {
            try {
                Instant tokenExpiry = Instant.parse(cacheResponse.getString("expiry"));
                Instant cacheExpiry = Instant.parse(cacheResponse.getString("cache-expiry"));
                Instant now = Instant.now(Clock.systemUTC());
                if (tokenExpiry.isBefore(now)) {
                    if (!tipCache.remove(token, cacheResponse))
                        throw new ConcurrentModificationException("TIP cache premature invalidation");
                    promise.fail(new Throwable("Token has expired"));
                }
                if (cacheExpiry.isAfter(now)) {
                    String extendedCacheExpiry = now
                            .plus(Constants.TIP_CACHE_TIMEOUT_AMOUNT, Constants.TIP_CACHE_TIMEOUT_UNIT)
                            .toString();
                    JsonObject newCacheEntry = cacheResponse.copy();
                    newCacheEntry.put("cache-expiry", extendedCacheExpiry);
                    if (!tipCache.replace(token, cacheResponse, newCacheEntry))
                        throw new ConcurrentModificationException("TIP cache premature invalidation");
                    promise.complete(newCacheEntry);
                } else {
                    if (!tipCache.remove(token, cacheResponse))
                        throw new ConcurrentModificationException("TIP cache premature invalidation");
                }
            } catch (DateTimeParseException | ConcurrentModificationException e) {
                logger.error(e.getMessage());
            }
        }

        JsonObject body = new JsonObject();
        body.put("token", token);
        webClient
                .post(443, properties.getProperty(Constants.AUTH_SERVER_HOST), Constants.AUTH_TIP_PATH)
                .expect(ResponsePredicate.JSON)
                .sendJsonObject(body, httpResponseAsyncResult -> {
                    if (httpResponseAsyncResult.failed()) {
                        promise.fail(httpResponseAsyncResult.cause());
                        return;
                    }
                    HttpResponse<Buffer> response = httpResponseAsyncResult.result();
                    if (response.statusCode() != HttpStatus.SC_OK) {
                        String errorMessage = response
                                .bodyAsJsonObject()
                                .getJsonObject("error")
                                .getString("message");
                        promise.fail(new Throwable(errorMessage));
                        return;
                    }
                    JsonObject responseBody = response.bodyAsJsonObject();
                    String cacheExpiry = Instant
                            .now(Clock.systemUTC())
                            .plus(Constants.TIP_CACHE_TIMEOUT_AMOUNT, Constants.TIP_CACHE_TIMEOUT_UNIT)
                            .toString();
                    responseBody.put("cache-expiry", cacheExpiry);
                    tipCache.put(token, responseBody);
                    promise.complete(responseBody);
                });
        return promise.future();
    }

    private Future<HashMap<String, Boolean>> isOpenResource(JsonArray requestIDs) {
        Promise<HashMap<String, Boolean>> promise = Promise.promise();
        return promise.future();
    }

}

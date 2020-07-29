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
import java.util.HashMap;
import java.util.Properties;
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
    private static final Properties properties = new Properties();
    private final WebClient webClient;

    public AuthenticationServiceImpl(WebClient client) {
        webClient = client;
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }
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

}

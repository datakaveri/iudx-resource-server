package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sync.Sync;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.authenticator.AuthenticationService;

// TODO : centralized authenticated of every request. need some work and testing
public class TokenHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenHandler.class);
  private final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private final List<String> allowedPublicEndPoints =
      List.of("/ngsi-ld/v1/temporal/entities", "/ngsi-ld/v1/entities");


  private AuthenticationService authenticator;

  @Override
  public void handle(RoutingContext event) {
    HttpServerRequest request = event.request();
    JsonObject requestJson = event.getBodyAsJson();
    LOGGER.debug("Info path(): " + request.path());
    String token = request.headers().get(HEADER_TOKEN);
    String path = request.path();

    if (token != null || isAllowedPublicTip(path)) {
      if (token == null)
        token = "public";
      JsonObject authInfo = new JsonObject().put("apiEndpoint", path).put(HEADER_TOKEN, token);
      JsonObject authResponse = auth(event.response(), requestJson, authInfo);
      LOGGER.debug("Auth Info :" + authResponse);

      // next handler
      event.next();
    } else {
      notAuthorized(event.response());
    }
  }

  private JsonObject auth(HttpServerResponse response, JsonObject request, JsonObject authInfo) {
    JsonObject resJson = new JsonObject();
    Vertx vertx = Vertx.currentContext().owner();
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);

    /*
     * vertx.<JsonObject>executeBlocking(blockingCodeHandler -> {
     * authenticator.tokenInterospect(request, authInfo, handler -> { if (handler.succeeded()) {
     * blockingCodeHandler.complete(handler.result()); } else { notAuthorized(response); } }); },
     * false, resultHandler -> { if (resultHandler.succeeded()) {
     * resJson.clear().mergeIn(resultHandler.result()); } else { notAuthorized(response); } });
     */

    // Sync.awaitResult(handler -> authenticator.tokenInterospect(request, authInfo, handler));

    return resJson;
  }

  private void notAuthorized(HttpServerResponse response) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(ResponseType.fromCode(HttpStatus.SC_UNAUTHORIZED).getCode())
        .end("unauthorized");
  }

  private boolean isAllowedPublicTip(String tip) {
    Predicate<String> pathFilter = Pattern.compile("/ngsi-ld/v1/entities/*").asPredicate();
    return allowedPublicEndPoints.stream().filter(pathFilter).findAny().isPresent();
  }

}

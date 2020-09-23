package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sync.Sync;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.authenticator.AuthenticationService;

// TODO : centralized authenticated of every request. need some work and testing
// TODO : solve Problem 1: failure handler is called always,even if auth is success.
public class TokenHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenHandler.class);

  private final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private final List<String> allowedPublicEndPoints =
      List.of("/ngsi-ld/v1/temporal/entities", "/ngsi-ld/v1/entities");


  private AuthenticationService authenticator;

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    JsonObject requestJson = context.getBodyAsJson();
    LOGGER.debug("Info path(): " + request.path());
    String token = request.headers().get(HEADER_TOKEN);
    String path = request.path();


    if (token != null || isAllowedPublicTip(path)) {
      if (token == null)
        token = "public";
      JsonObject authInfo = new JsonObject().put("apiEndpoint", path).put(HEADER_TOKEN, token);
      Vertx vertx = context.vertx();
      authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
      authenticator.tokenInterospect(requestJson, authInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.debug("Auth info : " + authHandler.result());
          // TODO : find a good way to pass on auth result to endpoints - either by rewriting body
          // or
          // using session etc.

        } else {
          LOGGER.debug("INFO :  auth failed");
          context.fail(401);
        }
        LOGGER.debug(context.currentRoute().getPath());
        LOGGER.debug(context.data());
        LOGGER.debug("INFO :  passing to next1");
        context.next();
      });
    } else {
      LOGGER.debug("INFO :  null test fail");
      context.fail(401);
    }
    return;
  }

  private boolean isAllowedPublicTip(String tip) {
    Predicate<String> pathFilter = Pattern.compile("/ngsi-ld/v1/entities/*").asPredicate();
    return allowedPublicEndPoints.stream().filter(pathFilter).findAny().isPresent();
  }
}

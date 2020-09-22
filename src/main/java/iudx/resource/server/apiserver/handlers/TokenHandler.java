package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.authenticator.AuthenticationService;

// TODO : centralized authenticated of every request. need some work
public class TokenHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(TokenHandler.class);
  private final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private final List<String> allowedPublicEndPoints =
      List.of("/ngsi-ld/v1/temporal/entities", "/ngsi-ld/v1/entities");


  private AuthenticationService authenticator;

  @Override
  public void handle(RoutingContext event) {
    HttpServerRequest request=event.request();
    JsonObject requestJson = event.getBodyAsJson();
    LOGGER.debug("Info path(): " + request.path());
    if (request.headers().contains(HEADER_TOKEN)
        || allowedPublicEndPoints.contains(request.path())) {
      auth(event.response(), requestJson,
          new JsonObject().put(HEADER_TOKEN, request.headers().get(HEADER_TOKEN)));
    } else {
      notAuthorized(event.response());
    }
    // next handler
    event.next();
  }

  private JsonObject auth(HttpServerResponse response, JsonObject request, JsonObject authInfo) {
    JsonObject resJson = new JsonObject();
    Vertx vertx = Vertx.vertx();
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    vertx.executeBlocking(blockingCodeHandler->{
      authenticator.tokenInterospect(request, authInfo, handler -> {
        blockingCodeHandler.complete();
      });
    }, false, resultHandler -> {
      if (resultHandler.succeeded()) {

      } else {
        notAuthorized(response);
      }
    });
    return new JsonObject();
  }

  private void notAuthorized(HttpServerResponse response) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(ResponseType.fromCode(HttpStatus.SC_UNAUTHORIZED).getCode())
        .end("unauthorized");
  }

}

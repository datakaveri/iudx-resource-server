package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;

public class AuthFailureHandler implements Handler<RoutingContext> {
  private static final Logger LOGGER = LogManager.getLogger(AuthFailureHandler.class);
  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("failure called" + context.statusCode());
    LOGGER.debug("frailure" + context.data());
    HttpServerResponse response = context.response();
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(ResponseType.fromCode(HttpStatus.SC_UNAUTHORIZED).getCode())
        .end(responseJson().toString());
  }

  private JsonObject responseJson() {
    return new JsonObject().put(JSON_TYPE, HttpStatus.SC_UNAUTHORIZED)
        .put(JSON_TITLE, "Not Authorized").put(JSON_DETAIL, "Invalid credentials");
  }

}

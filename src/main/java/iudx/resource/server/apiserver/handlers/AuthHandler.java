package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.authenticator.AuthenticationService;


/**
 * IUDX Authentication handler to authenticate token passed in HEADER
 * 
 *
 */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private final String AUTH_INFO = "authInfo";

  private final List<String> allowedPublicEndPoints = openEndPoints;
  private final List<String> noAuthRequired = bypassEndpoint;


  private AuthenticationService authenticator;

  @Override
  public void handle(RoutingContext context) {
    HttpServerRequest request = context.request();
    JsonObject requestJson = context.getBodyAsJson();
    LOGGER.debug("Info : path " + request.path());
    // bypassing auth for RDocs
    if (noAuthRequired.contains(request.path())) {
      context.next();
      return;
    }

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();


    if (token != null || isAllowedPublicTip(path)) {
      if (token == null)
        token = "public";

      JsonObject authInfo =
          new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

      if (!method.equalsIgnoreCase("POST")) {
        authInfo.put(ID, getId(context.request().path(), path));
      }

      LOGGER.debug("request" + requestJson);
      Vertx vertx = context.vertx();
      authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
      authenticator.tokenInterospect(requestJson, authInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.debug("Auth info : " + authHandler.result());
          context.data().put(AUTH_INFO, authHandler.result());
        } else {
          processAuthFailure(context);
          return;
        }
        context.next();
        return;
      });
    } else {
      processAuthFailure(context);
    }
  }

  private boolean isAllowedPublicTip(String tip) {
    return allowedPublicEndPoints.contains(tip);
  }

  private void processAuthFailure(RoutingContext ctx) {
    LOGGER.debug("processException");
    final String payload = responseJson().toString();
    ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(ResponseType.fromCode(HttpStatus.SC_UNAUTHORIZED).getCode()).end(payload);
  }

  private JsonObject responseJson() {
    return new JsonObject().put(JSON_TYPE, HttpStatus.SC_UNAUTHORIZED)
        .put(JSON_TITLE, "Not Authorized").put(JSON_DETAIL, "Invalid credentials");
  }

  /**
   * extract id from path param
   * 
   * @param ctx current routing context
   * @param forPath endpoint called for
   * @return id extraced fro path if present
   */
  private String getId(String path, String forPath) {
    String id = "";
    switch (forPath) {
      case NGSILD_SUBSCRIPTION_URL: {
        id = path.replaceAll(NGSILD_SUBSCRIPTION_URL + "/", "");
        break;
      }
      case IUDX_MANAGEMENT_ADAPTER_URL: {
        id = path.replaceAll(IUDX_MANAGEMENT_ADAPTER_URL + "/", "");
        break;
      }
      case IUDX_MANAGEMENT_EXCHANGE_URL: {
        id = path.replaceAll(IUDX_MANAGEMENT_EXCHANGE_URL + "/", "");
        break;
      }
      case IUDX_MANAGEMENT_QUEUE_URL: {
        id = path.replaceAll(IUDX_MANAGEMENT_QUEUE_URL + "/", "");
        break;
      }
      case IUDX_MANAGEMENT_VHOST_URL: {
        id = path.replaceAll(IUDX_MANAGEMENT_VHOST_URL + "/", "");
        break;
      }
      default: {
        id = "";
      }
    }
    return id;
  }

  /**
   * get normalized path without id as path param.
   * 
   * @param url complete path from request
   * @return path without id.
   */
  private String getNormalizedPath(String url) {
    LOGGER.debug("URL : " + url);
    String path = null;
    if (url.matches(ENTITITES_URL_REGEX)) {
      path = NGSILD_ENTITIES_URL;
    } else if (url.matches(TEMPORAL_URL_REGEX)) {
      path = NGSILD_TEMPORAL_URL;
    } else if (url.matches(POST_QUERY_URL_REGEX)) {
      path = NGSILD_POST_QUERY_PATH;
    } else if (url.matches(SUBSCRIPTION_URL_REGEX)) {
      path = NGSILD_SUBSCRIPTION_URL;
    } else if (url.matches(ADAPTER_URL_REGEX)) {
      path = IUDX_MANAGEMENT_ADAPTER_URL;
    } else if (url.matches(EXCHANGE_URL_REGEX)) {
      path = IUDX_MANAGEMENT_EXCHANGE_URL;
    } else if (url.matches(QUEUE_URL_REGEX)) {
      path = IUDX_MANAGEMENT_QUEUE_URL;
    } else if (url.matches(VHOST_URL_REGEX)) {
      path = IUDX_MANAGEMENT_VHOST_URL;
    }
    return path;
  }
}

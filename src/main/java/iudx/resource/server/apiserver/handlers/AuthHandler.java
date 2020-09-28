package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.List;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
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
  private final List<String> noAuthRequired = bypassEndpoint;
  private AuthenticationService authenticator;
  private HttpServerRequest request;

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    JsonObject requestJson = context.getBodyAsJson();
    
    if(requestJson == null) {
      requestJson = new JsonObject();
    }
    
    LOGGER.debug("Info : path " + request.path());
    // bypassing auth for RDocs
    if (noAuthRequired.contains(request.path())) {
      context.next();
      return;
    }

    String token = request.headers().get(HEADER_TOKEN);
    final String path = getNormalizedPath(request.path());
    final String method = context.request().method().toString();

    if (token == null)
      token = "public";

    JsonObject authInfo =
        new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);
    String id = getId(context.request().path(), path, context);
    authInfo.put(ID, id);
    requestJson.put(IDS, new JsonArray().add(id));
    
    LOGGER.debug("request" + requestJson);
    Vertx vertx = context.vertx();
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    authenticator.tokenInterospect(requestJson, authInfo, authHandler -> {
      if (authHandler.succeeded()) {
        LOGGER.debug("Auth info : " + authHandler.result());
        context.data().put(AUTH_INFO, authHandler.result());
      } else {
        processAuthFailure(context, authHandler.cause().getMessage());
        return;
      }
      context.next();
      return;
    });
  }

  private void processAuthFailure(RoutingContext ctx, String result) {
    if (result.contains("Not Found")) {
      LOGGER.error("Error : Item Not Found");
      final String payload = responseNotFoundJson().toString();
      ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(ResponseType.fromCode(HttpStatus.SC_NOT_FOUND).getCode()).end(payload);
    } else {
      LOGGER.error("Error : Authentication Failure");
      final String payload = responseUnauthorizedJson().toString();
      ctx.response().putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(ResponseType.fromCode(HttpStatus.SC_UNAUTHORIZED).getCode()).end(payload);
    }
  }

  private JsonObject responseUnauthorizedJson() {
    return new JsonObject().put(JSON_TYPE, HttpStatus.SC_UNAUTHORIZED)
        .put(JSON_TITLE, "Not Authorized").put(JSON_DETAIL, "Invalid credentials");
  }

  private JsonObject responseNotFoundJson() {
    return new JsonObject().put(JSON_TYPE, HttpStatus.SC_NOT_FOUND)
        .put(JSON_TITLE, "Not Found").put(JSON_DETAIL, "Resource Not Found");
  }

  /**
   * extract id from path param
   * 
   * @param ctx current routing context
   * @param forPath endpoint called for
   * @return id extraced fro path if present
   */
  private String getId(String path, String forPath, RoutingContext context) {
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
      case NGSILD_ENTITIES_URL: {
        if (path.split("/").length == 9) {
          id = path.replaceAll(NGSILD_ENTITIES_URL + "/", "");
          break;
        } else {
          id = request.getParam("id");
          break;
        }
      }
      case NGSILD_TEMPORAL_URL: {
        id = request.getParam("id");
        break;
      }
      case NGSILD_POST_QUERY_PATH: {
        JsonObject body = context.getBodyAsJson();
        id = body.getJsonArray("entities").getJsonObject(0).getString("id");
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

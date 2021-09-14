package iudx.resource.server.apiserver.handlers;

import static iudx.resource.server.apiserver.response.ResponseUrn.INVALID_TOKEN;
import static iudx.resource.server.apiserver.response.ResponseUrn.RESOURCE_NOT_FOUND;
import static iudx.resource.server.apiserver.util.Constants.ADAPTER_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.resource.server.apiserver.util.Constants.API_METHOD;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.BIND_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.DOMAIN;
import static iudx.resource.server.apiserver.util.Constants.ENTITIES_POST_QUERY_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.ENTITITES_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.EXCHANGE_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.IDS;
import static iudx.resource.server.apiserver.util.Constants.ID_REGEX;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_ADAPTER_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_BIND_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_QUEUE_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_UNBIND_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_VHOST_URL;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ENTITIES;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_ENTITIES_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_ENTITIES_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_TEMPORAL_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_SUBSCRIPTION_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_TEMPORAL_URL;
import static iudx.resource.server.apiserver.util.Constants.QUEUE_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.TEMPORAL_POST_QUERY_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.TEMPORAL_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.UNBIND_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.USERSHA;
import static iudx.resource.server.apiserver.util.Constants.VHOST_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.bypassEndpoint;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseUrn;
import iudx.resource.server.apiserver.util.HttpStatusCode;
import iudx.resource.server.authenticator.AuthenticationService;


/**
 * IUDX Authentication handler to authenticate token passed in HEADER
 * 
 *
 */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private static final Pattern regexIDPattern = ID_REGEX;
  private final String AUTH_INFO = "authInfo";
  private final List<String> noAuthRequired = bypassEndpoint;
  private static AuthenticationService authenticator;
  private HttpServerRequest request;

  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    JsonObject requestJson = context.getBodyAsJson();

    if (requestJson == null) {
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

    String pathId = getId4rmPath(context);
    LOGGER.info("id from path : " + pathId);
    String paramId = getId4rmRequest();
    LOGGER.info("id from param : " + paramId);
    String bodyId = getId4rmBody(context, path);
    LOGGER.info("id from body : " + bodyId);

    String id;
    if (pathId != null && !pathId.isBlank()) {
      id = pathId;
    } else {
      if (paramId != null && !paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    }
    LOGGER.info("id : " + id);

    authInfo.put(ID, id);
    JsonArray ids = new JsonArray();
    String[] idArray = id.split(",");
    for (String i : idArray) {
      ids.add(i);
    }

    if (path.equals(IUDX_MANAGEMENT_ADAPTER_URL) && HttpMethod.POST.name().equalsIgnoreCase(method)) {
      ids = requestJson.getJsonArray(JSON_ENTITIES);
    }
    requestJson.put(IDS, ids);

    LOGGER.debug("request" + requestJson);
    authenticator.tokenInterospect(requestJson, authInfo, authHandler -> {
      if (authHandler.succeeded()) {
        LOGGER.debug("Auth info : " + authHandler.result());
        authInfo.put("userId", authHandler.result().getValue("consumer"));
        context.data().put(AUTH_INFO, authInfo);
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
      HttpStatusCode statusCode = HttpStatusCode.getByValue(404);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(RESOURCE_NOT_FOUND, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }


  /**
   * extract id from path param
   * 
   * @param ctx current routing context
   * @param forPath endpoint called for
   * @return id extraced fro path if present
   */
  @Deprecated
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
        if (path.split("/").length >= 9) {
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
      case NGSILD_POST_TEMPORAL_QUERY_PATH: {
        JsonObject body = context.getBodyAsJson();
        id = body.getJsonArray("entities").getJsonObject(0).getString("id");
        break;
      }
      case NGSILD_POST_ENTITIES_QUERY_PATH: {
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

  private String getId(RoutingContext context, String path) {

    String pathId = getId4rmPath(context);
    LOGGER.info("id from path : " + pathId);
    String paramId = getId4rmRequest();
    LOGGER.info("id from param : " + paramId);
    String bodyId = getId4rmBody(context, path);

    String id;
    if (!pathId.isBlank()) {
      id = pathId;
    } else {
      if (!paramId.isBlank()) {
        id = paramId;
      } else {
        id = bodyId;
      }
    }
    return id;
  }

  private String getId4rmPath(RoutingContext context) {
    StringBuilder id = null;
    Map<String, String> pathParams = context.pathParams();
    LOGGER.info("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      id = new StringBuilder();
      if (pathParams.containsKey(DOMAIN)
          && pathParams.containsKey(USERSHA)
          && pathParams.containsKey(RESOURCE_SERVER)
          && pathParams.containsKey(RESOURCE_GROUP)) {

        id.append(pathParams.get(DOMAIN));
        id.append("/").append(pathParams.get(USERSHA));
        id.append("/").append(pathParams.get(RESOURCE_SERVER));
        id.append("/").append(pathParams.get(RESOURCE_GROUP));
        LOGGER.info("id :" + id.toString());
      }

      if (pathParams.containsKey(RESOURCE_NAME)) {
        id.append("/").append(pathParams.get(RESOURCE_NAME));
      }
    }
    LOGGER.info("id :" + id);
    return id != null ? id.toString() : null;
  }

  private String getId4rmRequest() {
    return request.getParam(ID);
  }


  private String getId4rmBody(RoutingContext context, String api) {
    JsonObject body = context.getBodyAsJson();
    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        if (api.matches(ADAPTER_URL_REGEX)) {
          id = array.getString(0);
        } else {
          JsonObject json = array.getJsonObject(0);
          if (json != null) {
            id = json.getString(ID);
          }
        }
      }
    }
    LOGGER.info("id : " + id);
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
    } else if (url.matches(TEMPORAL_POST_QUERY_URL_REGEX)) {
      path = NGSILD_POST_TEMPORAL_QUERY_PATH;
    } else if (url.matches(ENTITIES_POST_QUERY_URL_REGEX)) {
      path = NGSILD_POST_ENTITIES_QUERY_PATH;
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
    } else if (url.matches(BIND_URL_REGEX)) {
      path = IUDX_MANAGEMENT_BIND_URL;
    } else if (url.matches(UNBIND_URL_REGEX)) {
      path = IUDX_MANAGEMENT_UNBIND_URL;
    }
    return path;
  }
}

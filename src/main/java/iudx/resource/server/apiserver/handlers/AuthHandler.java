package iudx.resource.server.apiserver.handlers;

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
import static iudx.resource.server.apiserver.util.Constants.EXPIRY;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.IDS;
import static iudx.resource.server.apiserver.util.Constants.ID_REGEX;
import static iudx.resource.server.apiserver.util.Constants.IID;
import static iudx.resource.server.apiserver.util.Constants.IUDX_ASYNC_SEARCH;
import static iudx.resource.server.apiserver.util.Constants.IUDX_ASYNC_STATUS;
import static iudx.resource.server.apiserver.util.Constants.IUDX_CONSUMER_AUDIT_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_PROVIDER_AUDIT_URL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ALIAS;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ENTITIES;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_BASE_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_ENTITIES_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_ENTITIES_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_TEMPORAL_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_SUBSCRIPTION_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_TEMPORAL_URL;
import static iudx.resource.server.apiserver.util.Constants.QUEUE_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.RESET_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.REVOKE_TOKEN_REGEX;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.TEMPORAL_POST_QUERY_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.TEMPORAL_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.UNBIND_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.UNIQUE_ATTR_REGEX;
import static iudx.resource.server.apiserver.util.Constants.USERSHA;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.apiserver.util.Constants.VHOST_URL_REGEX;
import static iudx.resource.server.apiserver.util.Constants.bypassEndpoint;
import static iudx.resource.server.common.Api.ADMIN;
import static iudx.resource.server.common.Api.ASYNC;
import static iudx.resource.server.common.Api.BIND;
import static iudx.resource.server.common.Api.EXCHANGE;
import static iudx.resource.server.common.Api.INGESTION;
import static iudx.resource.server.common.Api.MANAGEMENT;
import static iudx.resource.server.common.Api.NGSILD_BASE;
import static iudx.resource.server.common.Api.QUEUE;
import static iudx.resource.server.common.Api.RESET_PWD;
import static iudx.resource.server.common.Api.RESOURCE_ATTRIBS;
import static iudx.resource.server.common.Api.REVOKE_TOKEN;
import static iudx.resource.server.common.Api.SEARCH;
import static iudx.resource.server.common.Api.STATUS;
import static iudx.resource.server.common.Api.SUBSCRIPTION;
import static iudx.resource.server.common.Api.UNBIND;
import static iudx.resource.server.common.Api.VHOST;
import static iudx.resource.server.common.Constants.AUTH_SERVICE_ADDRESS;
import static iudx.resource.server.common.ResponseUrn.INVALID_TOKEN_URN;
import static iudx.resource.server.common.ResponseUrn.RESOURCE_NOT_FOUND_URN;
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
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;

/** IUDX Authentication handler to authenticate token passed in HEADER */
public class AuthHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(AuthHandler.class);

  private static final Pattern regexIDPattern = ID_REGEX;
  static AuthenticationService authenticator;
  private final String AUTH_INFO = "authInfo";
  private final List<String> noAuthRequired = bypassEndpoint;
  private HttpServerRequest request;

  public static AuthHandler create(Vertx vertx) {
    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);
    return new AuthHandler();
  }

  @Override
  public void handle(RoutingContext context) {
    request = context.request();
    RequestBody requestBody = context.body();
    JsonObject requestJson=null;
    if(requestBody!=null) {
      if(requestBody.asJsonObject()!=null) {
        requestJson=requestBody.asJsonObject().copy();
      }
    }
    if(requestJson==null) {
      requestJson=new JsonObject();
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

    JsonObject authInfo = new JsonObject().put(API_ENDPOINT, path).put(HEADER_TOKEN, token).put(API_METHOD, method);

    LOGGER.debug("Info :" + context.request().path());
    LOGGER.debug("Info :" + context.request().path().split("/").length);

    String id = getId(context, path, method);
    authInfo.put(ID, id);

    JsonArray ids = new JsonArray();
    String[] idArray = (id == null ? new String[0] : id.split(","));
    for (String i : idArray) {
      ids.add(i);
    }

    if (path.equals(MANAGEMENT.path + INGESTION.path)
        && HttpMethod.POST.name().equalsIgnoreCase(method)) {
      ids = requestJson.getJsonArray(JSON_ENTITIES);
    }
    requestJson.put(IDS, ids);

    authenticator.tokenInterospect(
        requestJson,
        authInfo,
        authHandler -> {
          if (authHandler.succeeded()) {

            authInfo.put(IID, authHandler.result().getValue(IID));
            authInfo.put(USER_ID, authHandler.result().getValue(USER_ID));
            authInfo.put(EXPIRY, authHandler.result().getValue(EXPIRY));
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
          .end(generateResponse(RESOURCE_NOT_FOUND_URN, statusCode).toString());
    } else {
      LOGGER.error("Error : Authentication Failure");
      HttpStatusCode statusCode = HttpStatusCode.getByValue(401);
      ctx.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(statusCode.getValue())
          .end(generateResponse(INVALID_TOKEN_URN, statusCode).toString());
    }
  }

  private JsonObject generateResponse(ResponseUrn urn, HttpStatusCode statusCode) {
    return new JsonObject()
        .put(JSON_TYPE, urn.getUrn())
        .put(JSON_TITLE, statusCode.getDescription())
        .put(JSON_DETAIL, statusCode.getDescription());
  }

  /**
   * extract id from request (path/query or body )
   *
   *
   * @param ctx     current routing context
   * @param forPath endpoint called for
   * @return id extraced fro path if present
   */
  private String getId(RoutingContext context, String path, String method) {

    String pathId = getId4rmPath(context);
    String paramId = getId4rmRequest();
    String bodyId = getId4rmBody(context, path);
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
    if (path.matches(NGSILD_BASE.path + SUBSCRIPTION.path)
        && (!method.equalsIgnoreCase("GET") || !method.equalsIgnoreCase("DELETE"))) {
      id = bodyId;
    }
    return id;
  }

  private String getId4rmPath(RoutingContext context) {
    StringBuilder id = null;
    Map<String, String> pathParams = context.pathParams();
    LOGGER.debug("path params :" + pathParams);
    if (pathParams != null && !pathParams.isEmpty()) {
      if (pathParams.containsKey(DOMAIN)
          && pathParams.containsKey(USERSHA)
          && pathParams.containsKey(RESOURCE_SERVER)
          && pathParams.containsKey(RESOURCE_GROUP)) {
        id = new StringBuilder();
        id.append(pathParams.get(DOMAIN));
        id.append("/").append(pathParams.get(USERSHA));
        id.append("/").append(pathParams.get(RESOURCE_SERVER));
        id.append("/").append(pathParams.get(RESOURCE_GROUP));
        if (pathParams.containsKey(RESOURCE_NAME)) {
          id.append("/").append(pathParams.get(RESOURCE_NAME));
        }
        LOGGER.debug("id :" + id);
      } else if (pathParams.containsKey(USER_ID) && pathParams.containsKey(JSON_ALIAS)) {
        id = new StringBuilder();
        id.append(pathParams.get(USER_ID)).append("/").append(pathParams.get(JSON_ALIAS));
      }
    }
    LOGGER.debug("id :" + id);
    return id != null ? id.toString() : null;
  }

  private String getId4rmRequest() {
    return request.getParam(ID);
  }

  private String getId4rmBody(RoutingContext context, String api) {
    JsonObject body = context.body().asJsonObject();
    String id = null;
    if (body != null) {
      JsonArray array = body.getJsonArray(JSON_ENTITIES);
      if (array != null) {
        if (api.matches(ADAPTER_URL_REGEX) || api.matches(SUBSCRIPTION_URL_REGEX)) {
          id = array.getString(0);
        } else {
          JsonObject json = array.getJsonObject(0);
          if (json != null) {
            id = json.getString(ID);
          }
        }
      }
    }
    LOGGER.debug("id : " + id);
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
      path = NGSILD_BASE.path + INGESTION.path;
    } else if (url.matches(EXCHANGE_URL_REGEX)) {
      path = MANAGEMENT.path + EXCHANGE.path;
    } else if (url.matches(QUEUE_URL_REGEX)) {
      path = MANAGEMENT.path + QUEUE.path;
    } else if (url.matches(VHOST_URL_REGEX)) {
      path = MANAGEMENT.path + VHOST.path;
    } else if (url.matches(BIND_URL_REGEX)) {
      path = MANAGEMENT.path + BIND.path;
    } else if (url.matches(UNBIND_URL_REGEX)) {
      path = MANAGEMENT.path + UNBIND.path;
    } else if (url.matches(RESET_URL_REGEX)) {
      path = MANAGEMENT.path + RESET_PWD.path;
    } else if (url.matches(REVOKE_TOKEN_REGEX)) {
      path = ADMIN.path + REVOKE_TOKEN.path;
    } else if (url.matches(UNIQUE_ATTR_REGEX)) {
      path = ADMIN.path + RESOURCE_ATTRIBS.path;
    } else if (url.matches(IUDX_CONSUMER_AUDIT_URL)) {
      path = IUDX_CONSUMER_AUDIT_URL;
    } else if (url.matches(IUDX_PROVIDER_AUDIT_URL)) {
      path = IUDX_PROVIDER_AUDIT_URL;
    } else if (url.matches(IUDX_ASYNC_SEARCH)) {
      path = NGSILD_BASE_PATH + ASYNC.path + SEARCH.path;
    } else if (url.matches(IUDX_ASYNC_STATUS)) {
      path = NGSILD_BASE_PATH + ASYNC.path + STATUS.path;
    }
    return path;
  }
}

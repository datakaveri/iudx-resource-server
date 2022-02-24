package iudx.resource.server.apiserver;

import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.apiserver.handlers.ValidationHandler;
import iudx.resource.server.apiserver.query.NGSILDQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.service.CatalogueService;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.database.async.AsyncService;
import iudx.resource.server.common.Api;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.HEADER_HOST;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_INSTANCEID;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.apiserver.util.Constants.API;
import static iudx.resource.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;

public class AsyncRestApi {

  private static final Logger LOGGER = LogManager.getLogger(AsyncRestApi.class);

  private static final String ASYNC_SERVICE_ADDRESS = "iudx.rs.async.service";

  private final Vertx vertx;
  private final Router router;
  private final PostgresService pgService;
  private MeteringService meteringService;
  private AsyncService asyncService;
  private final ParamsValidator validator;
  private final CatalogueService catalogueService;

  AsyncRestApi(Vertx vertx, PostgresService pgService,
      CatalogueService catalogueService, ParamsValidator validator) {
    this.vertx = vertx;
    this.pgService = pgService;
    this.catalogueService = catalogueService;
    this.validator = validator;
    this.router = Router.router(vertx);
  }

  public Router init() {

    FailureHandler validationsFailureHandler = new FailureHandler();
    ValidationHandler asyncSearchValidationHandler =
        new ValidationHandler(vertx, RequestType.ASYNC);
    asyncService = AsyncService.createProxy(vertx, ASYNC_SERVICE_ADDRESS);
    router
        .get(Api.SEARCH.path)
        .handler(asyncSearchValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleAsyncSearchRequest)
        .handler(validationsFailureHandler);

    router
        .get(Api.STATUS.path)
        //				.handler(asyncSearchValidationHandler)
        //				.handler(AuthHandler.create(vertx))     // TODO: how to authenticate?
        .handler(this::handleAsyncStatusRequest)
        .handler(validationsFailureHandler);

    return router;
  }

  private void handleAsyncSearchRequest(RoutingContext routingContext) {
    LOGGER.trace("starting async search");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request params
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
            // create json
            QueryMapper queryMapper = new QueryMapper();
            JsonObject json = queryMapper.toJson(ngsildquery, true, true);
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            json.put(JSON_INSTANCEID, instanceID);
            LOGGER.debug("Info: IUDX json query;" + json);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    executeAsyncURLSearch(routingContext, json);
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Bad request;");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }

  private void executeAsyncURLSearch(RoutingContext routingContext, JsonObject json) {
    // get sub from AuthHandler result
    String sub = ((JsonObject) routingContext.data().get("authInfo")).getString(USER_ID);
    String requestURI = routingContext.request().absoluteURI();
    // generate UUID from the absolute URI of the HTTP Request
    String requestID = UUID.nameUUIDFromBytes(requestURI.getBytes()).toString();

    asyncService.asyncSearch(
        requestID,
        sub,
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Async Search Success");
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                routingContext.response(), ResponseType.Ok.getCode(), handler.result().toString());
          } else {
            LOGGER.error("Fail: Async search failed");
            processBackendResponse(routingContext.response(), handler.cause().getMessage());
          }
        });
  }

  private void handleAsyncStatusRequest(RoutingContext routingContext) {
    LOGGER.trace("starting async status");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    String searchID = request.getParam("searchID");

    asyncService.asyncStatus(
        searchID,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Async status success");
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else if (handler.failed()) {
            LOGGER.error("Fail: Async status fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void handleSuccessResponse(HttpServerResponse response, int statusCode, String result) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(statusCode).end(result);
  }

  private void processBackendResponse(HttpServerResponse response, String failureMessage) {
    LOGGER.debug("Info : " + failureMessage);
    try {
      JsonObject json = new JsonObject(failureMessage);
      int type = json.getInteger(JSON_TYPE);
      HttpStatusCode status = HttpStatusCode.getByValue(type);
      String urnTitle = json.getString(JSON_TITLE);
      String detail = json.getString(JSON_DETAIL);
      ResponseUrn urn;
      if (urnTitle != null) {
        urn = ResponseUrn.fromCode(urnTitle);
      } else {
        urn = ResponseUrn.fromCode(type + "");
      }
      // return urn in body
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(type)
          .end(generateResponse(status, urn, detail).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
  }

  private Optional<MultiMap> getQueryParams(
      RoutingContext routingContext, HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      // Internally + sign is dropped and treated as space, replacing + with %2B do the trick
      String uri = routingContext.request().uri().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET, true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        LOGGER.debug("Info: param :" + entry.getKey() + " value : " + entry.getValue());
        queryParams.add(entry.getKey(), entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(BAD_REQUEST.getValue())
          .end(generateResponse(BAD_REQUEST, INVALID_PARAM_URN).toString());
    }
    return Optional.of(queryParams);
  }

  private void handleResponse(HttpServerResponse response, HttpStatusCode code, ResponseUrn urn) {
    handleResponse(response, code, urn, code.getDescription());
  }

  private void handleResponse(
      HttpServerResponse response, HttpStatusCode statusCode, ResponseUrn urn, String message) {
    response
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(statusCode.getValue())
        .end(generateResponse(statusCode, urn, message).toString());
  }

  private Future<Void> updateAuditTable(RoutingContext context) {
    Promise<Void> promise = Promise.promise();
    JsonObject authInfo = (JsonObject) context.data().get("authInfo");

    JsonObject request = new JsonObject();
    request.put(USER_ID, authInfo.getValue(USER_ID));
    request.put(ID, authInfo.getValue(ID));
    request.put(API, authInfo.getValue(API_ENDPOINT));
    meteringService.executeWriteQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("audit table updated");
            promise.complete();
          } else {
            LOGGER.error("failed to update audit table");
            promise.complete();
          }
        });

    return promise.future();
  }
}

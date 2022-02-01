package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.response.ResponseUtil.generateResponse;
import static iudx.resource.server.apiserver.util.Constants.API;
import static iudx.resource.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.DOMAIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ACCEPT;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ALLOW_ORIGIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_CONTENT_LENGTH;
import static iudx.resource.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.HEADER_HOST;
import static iudx.resource.server.apiserver.util.Constants.HEADER_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ORIGIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_REFERER;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.resource.server.apiserver.util.Constants.ID;
import static iudx.resource.server.apiserver.util.Constants.IUDXQUERY_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.IUDX_CONSUMER_AUDIT_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_PROVIDER_AUDIT_URL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ALIAS;
import static iudx.resource.server.apiserver.util.Constants.JSON_CONSUMER;
import static iudx.resource.server.apiserver.util.Constants.JSON_COUNT;
import static iudx.resource.server.apiserver.util.Constants.JSON_ID;
import static iudx.resource.server.apiserver.util.Constants.JSON_INSTANCEID;
import static iudx.resource.server.apiserver.util.Constants.JSON_NAME;
import static iudx.resource.server.apiserver.util.Constants.JSON_SEARCH_TYPE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.MIME_TEXT_HTML;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_NAME;
import static iudx.resource.server.apiserver.util.Constants.MSG_SUB_TYPE_NOT_FOUND;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_ENTITIES_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_ENTITIES_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_TEMPORAL_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_TEMPORAL_URL;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.ROUTE_DOC;
import static iudx.resource.server.apiserver.util.Constants.ROUTE_STATIC_SPEC;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.apiserver.util.Constants.SUB_TYPE;
import static iudx.resource.server.apiserver.util.Constants.USERSHA;
import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.apiserver.util.Util.errorResponse;
import static iudx.resource.server.common.Api.ADMIN;
import static iudx.resource.server.common.Api.INGESTION;
import static iudx.resource.server.common.Api.MANAGEMENT;
import static iudx.resource.server.common.Api.NGSILD_BASE;
import static iudx.resource.server.common.Api.SUBSCRIPTION;
import static iudx.resource.server.common.Api.ASYNC;
import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;
import static iudx.resource.server.common.HttpStatusCode.BAD_REQUEST;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.BACKING_SERVICE_FORMAT_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_TEMPORAL_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.MISSING_TOKEN_URN;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.apiserver.handlers.ValidationHandler;
import iudx.resource.server.apiserver.management.ManagementApi;
import iudx.resource.server.apiserver.management.ManagementApiImpl;
import iudx.resource.server.apiserver.query.NGSILDQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.service.CatalogueService;
import iudx.resource.server.apiserver.subscription.SubsType;
import iudx.resource.server.apiserver.subscription.SubscriptionService;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.apiserver.validation.ValidatorsHandlersFactory;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.database.latest.LatestDataService;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.metering.MeteringService;

/**
 * The Resource Server API Verticle.
 *
 * <h1>Resource Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /** Service addresses */
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.database.service";

  private static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private static final String BROKER_SERVICE_ADDRESS = "iudx.rs.broker.service";
  private static final String LATEST_SEARCH_ADDRESS = "iudx.rs.latest.service";
  private static final String METERING_SERVICE_ADDRESS = "iudx.rs.metering.service";

  private HttpServer server;
  private Router router;
  private int port = 8443;
  private boolean isSSL, isProduction;
  private String keystore;
  private String keystorePassword;
  private ManagementApi managementApi;
  private SubscriptionService subsService;
  private CatalogueService catalogueService;
  private MeteringService meteringService;
  private DatabaseService database;
  private PostgresService postgresService;
  private DataBrokerService databroker;
  private AuthenticationService authenticator;
  private ParamsValidator validator;

  private LatestDataService latestDataService;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   *
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   */
  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(HEADER_ACCEPT);
    allowedHeaders.add(HEADER_TOKEN);
    allowedHeaders.add(HEADER_CONTENT_LENGTH);
    allowedHeaders.add(HEADER_CONTENT_TYPE);
    allowedHeaders.add(HEADER_HOST);
    allowedHeaders.add(HEADER_ORIGIN);
    allowedHeaders.add(HEADER_REFERER);
    allowedHeaders.add(HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    /* Create a reference to HazelcastClusterManager. */

    router = Router.router(vertx);

    /* Define the APIs, methods, endpoints and associated methods. */

    router = Router.router(vertx);
    router
        .route()
        .handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    router
        .route()
        .handler(
            requestHandler -> {
              requestHandler
                  .response()
                  .putHeader("Cache-Control", "no-cache, no-store,  must-revalidate,max-age=0")
                  .putHeader("Pragma", "no-cache")
                  .putHeader("Expires", "0")
                  .putHeader("X-Content-Type-Options", "nosniff");
              requestHandler.next();
            });

    // attach custom http error responses to router
    HttpStatusCode[] statusCodes = HttpStatusCode.values();
    Stream.of(statusCodes)
        .forEach(
            code -> {
              router.errorHandler(
                  code.getValue(),
                  errorHandler -> {
                    HttpServerResponse response = errorHandler.response();
                    if (response.headWritten()) {
                      try {
                        response.close();
                      } catch (RuntimeException e) {
                        LOGGER.error("Error : " + e);
                      }
                      return;
                    }
                    response
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .setStatusCode(code.getValue())
                        .end(errorResponse(code));
                  });
            });

    router.route().handler(BodyHandler.create());

    ValidatorsHandlersFactory validators = new ValidatorsHandlersFactory();
    FailureHandler validationsFailureHandler = new FailureHandler();

    /* NGSI-LD api endpoints */
    ValidationHandler entityValidationHandler = new ValidationHandler(vertx, RequestType.ENTITY);
    router
        .get(NGSILD_ENTITIES_URL)
        .handler(entityValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler latestValidationHandler = new ValidationHandler(vertx, RequestType.LATEST);
    router
        .get(NGSILD_ENTITIES_URL + "/:domain/:userSha/:resourceServer/:resourceGroup/:resourceName")
        .handler(latestValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleLatestEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler postTemporalValidationHandler =
        new ValidationHandler(vertx, RequestType.POST_TEMPORAL);
    router
        .post(NGSILD_POST_TEMPORAL_QUERY_PATH)
        .consumes(APPLICATION_JSON)
        .handler(postTemporalValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handlePostEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler postEntitiesValidationHandler =
        new ValidationHandler(vertx, RequestType.POST_ENTITIES);
    router
        .post(NGSILD_POST_ENTITIES_QUERY_PATH)
        .consumes(APPLICATION_JSON)
        .handler(postEntitiesValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handlePostEntitiesQuery)
        .failureHandler(validationsFailureHandler);

    ValidationHandler temporalValidationHandler =
        new ValidationHandler(vertx, RequestType.TEMPORAL);
    router
        .get(NGSILD_TEMPORAL_URL)
        .handler(temporalValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleTemporalQuery)
        .failureHandler(validationsFailureHandler);

    // create sub
    ValidationHandler subsValidationHandler =
        new ValidationHandler(vertx, RequestType.SUBSCRIPTION);
    router
        .post(NGSILD_BASE.path + SUBSCRIPTION.path)
        .handler(subsValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::handleSubscriptions)
        .failureHandler(validationsFailureHandler);
    // append sub
    router
        .patch(NGSILD_BASE.path + SUBSCRIPTION.path + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::appendSubscription)
        .failureHandler(validationsFailureHandler);
    // update sub
    router
        .put(NGSILD_BASE.path + SUBSCRIPTION.path + "/:userid/:alias")
        .handler(subsValidationHandler)
        .handler(AuthHandler.create(vertx))
        .handler(this::updateSubscription)
        .failureHandler(validationsFailureHandler);
    // get sub
    router
        .get(NGSILD_BASE.path + SUBSCRIPTION.path + "/:userid/:alias")
        .handler(AuthHandler.create(vertx))
        .handler(this::getSubscription);
    // delete sub
    router
        .delete(NGSILD_BASE.path + SUBSCRIPTION.path + "/:userid/:alias")
        .handler(AuthHandler.create(vertx))
        .handler(this::deleteSubscription);

    /* Management Api endpoints */
    // Exchange

    router
        .get(IUDX_CONSUMER_AUDIT_URL)
        .handler(AuthHandler.create(vertx))
        .handler(this::getConsumerAuditDetail);
    router
        .get(IUDX_PROVIDER_AUDIT_URL)
        .handler(AuthHandler.create(vertx))
        .handler(this::getProviderAuditDetail);

    // adapter
    router
        .post(NGSILD_BASE.path + INGESTION.path)
        .handler(AuthHandler.create(vertx))
        .handler(this::registerAdapter);
    router
        .delete(
            NGSILD_BASE.path
                + INGESTION.path
                + "/:domain/:userSha/:resourceServer/:resourceGroup/:resourceName")
        .handler(AuthHandler.create(vertx))
        .handler(this::deleteAdapter);
    router
        .delete(
            NGSILD_BASE.path + INGESTION.path + "/:domain/:userSha/:resourceServer/:resourceGroup")
        .handler(AuthHandler.create(vertx))
        .handler(this::deleteAdapter);
    router
        .get(
            NGSILD_BASE.path
                + INGESTION.path
                + "/:domain/:userSha/:resourceServer/:resourceGroup/:resourceName")
        .handler(AuthHandler.create(vertx))
        .handler(this::getAdapterDetails);

    router
        .get(NGSILD_BASE.path + INGESTION.path + "/:domain/:userSha/:resourceServer/:resourceGroup")
        .handler(AuthHandler.create(vertx))
        .handler(this::getAdapterDetails);

    router
        .post(NGSILD_BASE.path + INGESTION.path + "/heartbeat")
        .handler(AuthHandler.create(vertx))
        .handler(this::publishHeartbeat);
    router
        .post(NGSILD_BASE.path + INGESTION.path + "/downstreamissue")
        .handler(AuthHandler.create(vertx))
        .handler(this::publishDownstreamIssue);
    router
        .post(NGSILD_BASE.path + INGESTION.path + "/dataissue")
        .handler(AuthHandler.create(vertx))
        .handler(this::publishDataIssue);
    router
        .post(NGSILD_BASE.path + INGESTION.path + "/entities")
        .handler(AuthHandler.create(vertx))
        .handler(this::publishDataFromAdapter);

    /** Documentation routes */
    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router
        .get(ROUTE_STATIC_SPEC)
        .produces(MIME_APPLICATION_JSON)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/openapi.yaml");
            });
    /* Get redoc */
    router
        .get(ROUTE_DOC)
        .produces(MIME_TEXT_HTML)
        .handler(
            routingContext -> {
              HttpServerResponse response = routingContext.response();
              response.sendFile("docs/apidoc.html");
            });

    /* Read ssl configuration. */
    isSSL = config().getBoolean("ssl");

    /* Read server deployment configuration. */
    isProduction = config().getBoolean("production");

    HttpServerOptions serverOptions = new HttpServerOptions();

    if (isSSL) {
      LOGGER.info("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */

      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions
          .setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));

    } else {
      LOGGER.info("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);
      if (isProduction) {
        port = 80;
      } else {
        port = 8080;
      }
    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Get a handler for the Service Discovery interface. */

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);

    databroker = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    meteringService = MeteringService.createProxy(vertx, METERING_SERVICE_ADDRESS);
    latestDataService = LatestDataService.createProxy(vertx, LATEST_SEARCH_ADDRESS);

    managementApi = new ManagementApiImpl();
    subsService = new SubscriptionService();
    catalogueService = new CatalogueService(vertx, config());
    validator = new ParamsValidator(catalogueService);

    postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);

    router.mountSubRouter(ADMIN.path,
        new AdminRestApi(vertx, databroker, postgresService, meteringService).init());
    router.mountSubRouter(MANAGEMENT.path,
        new ManagementRestApi(vertx, databroker, postgresService, meteringService, managementApi)
            .init());
    router.mountSubRouter(ASYNC.path,
            new AsyncRestApi(vertx).init());

    router.route().last().handler(requestHandler -> {
      HttpServerResponse response = requestHandler.response();
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(404)
          .end(generateResponse(HttpStatusCode.NOT_FOUND, ResponseUrn.YET_NOT_IMPLEMENTED_URN)
              .toString());
    });

    router
        .route()
        .last()
        .handler(
            requestHandler -> {
              HttpServerResponse response = requestHandler.response();
              response
                  .putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                  .setStatusCode(404)
                  .end(
                      generateResponse(HttpStatusCode.NOT_FOUND,
                          ResponseUrn.YET_NOT_IMPLEMENTED_URN)
                              .toString());
            });

    LOGGER.info("API server deployed on :" + serverOptions.getPort());
  }

  private Future<Void> getConsumerAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getConsumerAuditDetail Started.");
    Promise<Void> promise = Promise.promise();
    JsonObject entries = new JsonObject();
    JsonObject consumer = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    entries.put("userid", consumer.getString("userid"));
    entries.put("endPoint", consumer.getString("apiEndpoint"));
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("options", request.headers().get("options"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));

    {
      LOGGER.debug(entries);
      meteringService.executeReadQuery(
          entries,
          handler -> {
            if (handler.succeeded()) {
              LOGGER.debug("Table Reading Done.");
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
              promise.complete();
            } else {
              LOGGER.error("Fail msg " + handler.cause().getMessage());
              LOGGER.error("Table reading failed.");
              processBackendResponse(response, handler.cause().getMessage());
              promise.complete();
            }
          });
      return promise.future();
    }
  }

  private Future<Void> getProviderAuditDetail(RoutingContext routingContext) {
    LOGGER.trace("Info: getProviderAuditDetail Started.");
    Promise<Void> promise = Promise.promise();
    JsonObject entries = new JsonObject();
    JsonObject provider = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    entries.put("endPoint", provider.getString("apiEndpoint"));
    entries.put("userid", provider.getString("userid"));
    entries.put("iid", provider.getString("iid"));
    entries.put("startTime", request.getParam("time"));
    entries.put("endTime", request.getParam("endTime"));
    entries.put("timeRelation", request.getParam("timerel"));
    entries.put("providerID", request.getParam("providerID"));
    entries.put("consumerID", request.getParam("consumer"));
    entries.put("resourceId", request.getParam("id"));
    entries.put("api", request.getParam("api"));
    entries.put("options", request.headers().get("options"));

    {
      LOGGER.debug(entries);
      meteringService.executeReadQuery(
          entries,
          handler -> {
            if (handler.succeeded()) {
              LOGGER.debug("Table Reading Done.");
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), handler.result().toString());
              promise.complete();
            } else {
              LOGGER.error("Fail msg " + handler.cause().getMessage());
              LOGGER.error("Table reading failed.");
              processBackendResponse(response, handler.cause().getMessage());
              promise.complete();
            }
          });
      return promise.future();
    }
  }

  private void handleLatestEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info:handleLatestEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    if (!params.isEmpty()) {
      RuntimeException ex =
          new RuntimeException("Query parameters are not allowed with latest query");
      routingContext.fail(ex);
    }
    String domain = request.getParam(DOMAIN);
    String userSha = request.getParam(USERSHA);
    String resourceServer = request.getParam(RESOURCE_SERVER);
    String resourceGroup = request.getParam(RESOURCE_GROUP);
    String resourceName = request.getParam(RESOURCE_NAME);

    String id =
        domain + "/" + userSha + "/" + resourceServer + "/" + resourceGroup + "/" + resourceName;
    JsonObject json = new JsonObject();
    Future<List<String>> filtersFuture = catalogueService.getApplicableFilters(id);
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    json.put(JSON_INSTANCEID, instanceID);
    json.put(JSON_ID, new JsonArray().add(id));
    json.put(JSON_SEARCH_TYPE, "latestSearch");
    LOGGER.debug("Info: IUDX query json;" + json);
    filtersFuture.onComplete(filtersHandler -> {
      if (filtersHandler.succeeded()) {
        json.put("applicableFilters", filtersHandler.result());
        executeLatestSearchQuery(routingContext, json, response);
      } else {
        LOGGER.error("catalogue item/group doesn't have filters.");
        handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN,
            filtersHandler.cause().getMessage());
      }
    });
  }

  /**
   * This method is used to handle all NGSI-LD queries for endpoint /ngsi-ld/v1/entities/**.
   *
   * @param routingContext RoutingContext Object
   */
  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info:handleEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    MultiMap headerParams = request.headers();
    // validate request parameters
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        // parse query params
        NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
        if (isTemporalParamsPresent(ngsildquery)) {
          DxRuntimeException ex =
              new DxRuntimeException(BAD_REQUEST.getValue(), INVALID_TEMPORAL_PARAM_URN,
                  "Temporal parameters are not allowed in entities query.");
          routingContext.fail(ex);
        }
        // create json
        QueryMapper queryMapper = new QueryMapper();
        JsonObject json = queryMapper.toJson(ngsildquery, false);
        Future<List<String>> filtersFuture =
            catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
        /* HTTP request instance/host details */
        String instanceID = request.getHeader(HEADER_HOST);
        json.put(JSON_INSTANCEID, instanceID);
        LOGGER.debug("Info: IUDX query json;" + json);
        /* HTTP request body as Json */
        JsonObject requestBody = new JsonObject();
        requestBody.put("ids", json.getJsonArray("id"));
        filtersFuture.onComplete(filtersHandler -> {
          if (filtersHandler.succeeded()) {
            json.put("applicableFilters", filtersHandler.result());
            if (json.containsKey(IUDXQUERY_OPTIONS)
                && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
              executeCountQuery(routingContext, json, response);
            } else {
              executeSearchQuery(routingContext, json, response);
            }
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Validation failed");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
      } else if (validationHandler.failed()) {
        LOGGER.error("Fail: Validation failed");
        handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN,
            validationHandler.cause().getMessage());
      }
    });
  }

  /**
   * this method is used to handle all entities queries from post endpoint.
   *
   * @param routingContext routingContext
   */
  public void handlePostEntitiesQuery(RoutingContext routingContext) {
    LOGGER.trace("Info: handlePostEntitiesQuery method started.");
    HttpServerRequest request = routingContext.request();
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.debug("Info: request Json :: ;" + requestJson);
    HttpServerResponse response = routingContext.response();
    MultiMap headerParams = request.headers();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Future<Boolean> validationResult = validator.validate(requestJson);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NGSILDQueryParams ngsildquery = new NGSILDQueryParams(requestJson);
            QueryMapper queryMapper = new QueryMapper();
            JsonObject json = queryMapper.toJson(ngsildquery, requestJson.containsKey("temporalQ"));
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            String instanceID = request.getHeader(HEADER_HOST);
            json.put(JSON_INSTANCEID, instanceID);
            requestJson.put("ids", json.getJsonArray("id"));
            LOGGER.debug("Info: IUDX query json : ;" + json);
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    // Add limit and offset value for pagination 
                    if(params.contains("limit") && params.contains("offset")) {
                    	json.put("limit", params.get("limit"));
                    	json.put("offset", params.get("offset"));
                    }
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
                  } else {
                    LOGGER.error("catalogue item/group doesn't have filters.");
                  }
                });
          } else if (validationHandler.failed()) {
            LOGGER.error("Fail: Bad request");
            handleResponse(
                response, BAD_REQUEST, INVALID_PARAM_URN, validationHandler.cause().getMessage());
          }
        });
  }


  /**
   * Execute a count query in DB
   *
   * @param json valid json query
   * @param response
   */
  private void executeCountQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    database.countQuery(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Count Success");
            Future.future(fu -> updateAuditTable(context));
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else if (handler.failed()) {
            LOGGER.error("Fail: Count Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * Execute a search query in DB
   *
   * @param json valid json query
   * @param response
   */
  private void executeSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    database.searchQuery(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Search Success");
            Future.future(fu -> updateAuditTable(context));
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else if (handler.failed()) {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  private void executeLatestSearchQuery(
      RoutingContext context, JsonObject json, HttpServerResponse response) {
    latestDataService.getLatestData(
        json,
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Latest data search succeeded");
            Future.future(fu -> updateAuditTable(context));
            handleSuccessResponse(response, ResponseType.Ok.getCode(), handler.result().toString());
          } else {
            LOGGER.error("Fail: Search Fail");
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * This method is used to handler all temporal NGSI-LD queries for endpoint
   * /ngsi-ld/v1/temporal/**.
   *
   * @param routingContext RoutingContext object
   */
  private void handleTemporalQuery(RoutingContext routingContext) {
    LOGGER.trace("Info: handleTemporalQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    MultiMap headerParams = request.headers();
    // validate request params
    Future<Boolean> validationResult = validator.validate(params);
    validationResult.onComplete(
        validationHandler -> {
          if (validationHandler.succeeded()) {
            // parse query params
            NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
            // create json
            QueryMapper queryMapper = new QueryMapper();
            JsonObject json = queryMapper.toJson(ngsildquery, true);
            Future<List<String>> filtersFuture =
                catalogueService.getApplicableFilters(json.getJsonArray("id").getString(0));
            json.put(JSON_INSTANCEID, instanceID);
            LOGGER.debug("Info: IUDX temporal json query;" + json);
            /* HTTP request body as Json */
            JsonObject requestBody = new JsonObject();
            requestBody.put("ids", json.getJsonArray("id"));
            filtersFuture.onComplete(
                filtersHandler -> {
                  if (filtersHandler.succeeded()) {
                    json.put("applicableFilters", filtersHandler.result());
                    if (json.containsKey(IUDXQUERY_OPTIONS)
                        && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
                      executeCountQuery(routingContext, json, response);
                    } else {
                      executeSearchQuery(routingContext, json, response);
                    }
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
  

  /**
   * Method used to handle all subscription requests.
   *
   * @param routingContext routingContext
   */
  private void handleSubscriptions(RoutingContext routingContext) {
    LOGGER.trace("Info: handleSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(HEADER_HOST);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType = SubsType.STREAMING.type;
    requestBody.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");

    JsonObject jsonObj = requestBody.copy();
    jsonObj.put(USER_ID, authInfo.getString(USER_ID));
    jsonObj.put(JSON_INSTANCEID, instanceID);
    LOGGER.debug("Info: json for subs :: ;" + jsonObj);
    Future<JsonObject> subsReq =
        subsService.createSubscription(jsonObj, databroker, postgresService, authInfo);
    subsReq.onComplete(
        subHandler -> {
          if (subHandler.succeeded()) {
            LOGGER.info("Success: Handle Subscription request;");
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Created.getCode(), subHandler.result().toString());
          } else {
            LOGGER.error("Fail: Handle Subscription request;");
            processBackendResponse(response, subHandler.cause().getMessage());
          }
        });
  }

  /**
   * handle append requests for subscription.
   *
   * @param routingContext routingContext
   */
  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: appendSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(USER_ID, authInfo.getString(USER_ID));
      Future<JsonObject> subsReq =
          subsService.appendSubscription(jsonObj, databroker, postgresService, authInfo);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.debug("Success: Appending subscription");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response,
                  ResponseType.Created.getCode(),
                  subsRequestHandler.result().toString());
            } else {
              LOGGER.error("Fail: Appending subscription");
              processBackendResponse(response, subsRequestHandler.cause().getMessage());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_NAME);
    }

  }

  /**
   * handle update subscription requests.
   *
   * @param routingContext routingContext
   */
  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: updateSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(HEADER_HOST);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(SUBSCRIPTION_ID, subsId);
      jsonObj.put(JSON_INSTANCEID, instanceID);
      jsonObj.put(USER_ID, authInfo.getString(USER_ID));
      Future<JsonObject> subsReq =
          subsService.updateSubscription(jsonObj, databroker, postgresService, authInfo);
      subsReq.onComplete(
          subsRequestHandler -> {
            if (subsRequestHandler.succeeded()) {
              LOGGER.info("result : " + subsRequestHandler.result());
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response,
                  ResponseType.Created.getCode(),
                  subsRequestHandler.result().toString());
            } else {
              LOGGER.error("Fail: Bad request");
              processBackendResponse(response, subsRequestHandler.cause().getMessage());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_INVALID_NAME);
    }

  }

  /**
   * get a subscription by id.
   *
   * @param routingContext routingContext
   */
  private void getSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: getSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");

    if (requestJson != null && requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
      Future<JsonObject> subsReq =
          subsService.getSubscription(jsonObj, databroker, postgresService);
      subsReq.onComplete(
          subHandler -> {
            if (subHandler.succeeded()) {
              LOGGER.info("Success: Getting subscription");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), subHandler.result().toString());
            } else {
              LOGGER.error("Fail: Bad request");
              processBackendResponse(response, subHandler.cause().getMessage());
            }
          });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * delete a subscription by id.
   *
   * @param routingContext routingContext
   */
  private void deleteSubscription(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteSubscription method started;");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String userid = request.getParam(USER_ID);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = userid + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType = SubsType.STREAMING.type;
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    if (requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(USER_ID, authInfo.getString(USER_ID));
      Future<JsonObject> subsReq =
          subsService.deleteSubscription(jsonObj, databroker, postgresService);
      subsReq.onComplete(
          subHandler -> {
            if (subHandler.succeeded()) {
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), subHandler.result().toString());
            } else {
              processBackendResponse(response, subHandler.cause().getMessage());
            }
          });
    } else {
      handleResponse(response, BAD_REQUEST, INVALID_PARAM_URN, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * register a adapter in Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  private void registerAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: registerAdapter method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceID);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    requestJson.put(USER_ID, authInfo.getString(USER_ID));

    Future<JsonObject> brokerResult = managementApi.registerAdapter(requestJson, databroker);

    brokerResult.onComplete(
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Success: Registering adapter");
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Created.getCode(), handler.result().toString());
          } else if (brokerResult.failed()) {
            LOGGER.error("Fail: Bad request" + handler.cause().getMessage());
            processBackendResponse(response, handler.cause().getMessage());
          }
        });
  }

  /**
   * delete a adapter in Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  public void deleteAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: deleteAdapter method starts;");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();

    String domain = request.getParam(DOMAIN);
    String usersha = request.getParam(USERSHA);
    String resourceGroup = request.getParam(RESOURCE_GROUP);
    String resourceServer = request.getParam(RESOURCE_SERVER);
    String resourceName = request.getParam(RESOURCE_NAME);

    StringBuilder adapterIdBuilder = new StringBuilder();
    adapterIdBuilder.append(domain);
    adapterIdBuilder.append("/").append(usersha);
    adapterIdBuilder.append("/").append(resourceServer);
    adapterIdBuilder.append("/").append(resourceGroup);
    if (resourceName != null) {
      adapterIdBuilder.append("/").append(resourceName);
    }

    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    String userId = authInfo.getString(USER_ID);
    Future<JsonObject> brokerResult =
        managementApi.deleteAdapter(adapterIdBuilder.toString(), userId, databroker);
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            LOGGER.info("Success: Deleting adapter");
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  /**
   * get Adapter details from Rabbit MQ.
   *
   * @param routingContext routingContext
   */
  public void getAdapterDetails(RoutingContext routingContext) {
    LOGGER.trace("getAdapterDetails method starts");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(DOMAIN);
    String usersha = request.getParam(USERSHA);
    String resourceGroup = request.getParam(RESOURCE_GROUP);
    String resourceServer = request.getParam(RESOURCE_SERVER);
    String resourceName = request.getParam(RESOURCE_NAME);

    StringBuilder adapterIdBuilder = new StringBuilder();
    adapterIdBuilder.append(domain);
    adapterIdBuilder.append("/").append(usersha);
    adapterIdBuilder.append("/").append(resourceServer);
    adapterIdBuilder.append("/").append(resourceGroup);
    if (resourceName != null) {
      adapterIdBuilder.append("/").append(resourceName);
    }

    Future<JsonObject> brokerResult =
        managementApi.getAdapterDetails(adapterIdBuilder.toString(), databroker);
    brokerResult.onComplete(
        brokerResultHandler -> {
          if (brokerResultHandler.succeeded()) {
            Future.future(fu -> updateAuditTable(routingContext));
            handleSuccessResponse(
                response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
          } else {
            processBackendResponse(response, brokerResultHandler.cause().getMessage());
          }
        });
  }

  /**
   * publish heartbeat details to Rabbit MQ.
   *
   * @param routingContext routingContext Note: This is too frequent an operation to have info or
   *        error level logs
   */
  public void publishHeartbeat(RoutingContext routingContext) {
    LOGGER.trace("Info: publishHeartbeat method starts;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.publishHeartbeat(requestJson, databroker);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Published heartbeat");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Unauthorized;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      LOGGER.info("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * publish downstream issues to Rabbit MQ.
   *
   * @param routingContext routingContext Note: This is too frequent an operation to have info or
   *        error level logs
   */
  public void publishDownstreamIssue(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDownStreamIssue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult =
          managementApi.publishDownstreamIssues(requestJson, databroker);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: published downstream issue");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * publish data issue to Rabbit MQ.
   *
   * @param routingContext routingContext Note: All logs are debug level only
   */
  public void publishDataIssue(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDataIssue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult = managementApi.publishDataIssue(requestJson, databroker);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: publishing a data issue");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * publish data from adapter to rabbit MQ.
   *
   * @param routingContext routingContext Note: All logs are debug level
   */
  public void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.trace("Info: publishDataFromAdapter method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));

      Future<JsonObject> brokerResult =
          managementApi.publishDataFromAdapter(requestJson, databroker);
      brokerResult.onComplete(
          brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: publishing data from adapter");
              Future.future(fu -> updateAuditTable(routingContext));
              handleSuccessResponse(
                  response, ResponseType.Ok.getCode(), brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });

    } else {
      LOGGER.debug("Fail: Unauthorized");
      handleResponse(response, UNAUTHORIZED, MISSING_TOKEN_URN);
    }
  }

  /**
   * handle HTTP response.
   *
   * @param response response object
   * @param responseType Http status for response
   * @param isBodyRequired body is required or not for response
   */
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
          .end(generateResponse(status, urn).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json from backend service [ jsonFormattingException ]");
      handleResponse(response, HttpStatusCode.BAD_REQUEST, BACKING_SERVICE_FORMAT_URN);
    }
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

  /**
   * Get the request query parameters delimited by <b>&</b>, <i><b>;</b>(semicolon) is considered as
   * part of the parameter</i>.
   *
   * @param routingContext RoutingContext Object
   * @param response HttpServerResponse
   * @return Optional Optional of Map
   */
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
          .setStatusCode(HttpStatusCode.BAD_REQUEST.getValue())
          .end(generateResponse(HttpStatusCode.BAD_REQUEST, INVALID_PARAM_URN).toString());

    }
    return Optional.of(queryParams);
  }

  @Override
  public void stop() {
    LOGGER.info("Stopping the API server");
  }

  private boolean isTemporalParamsPresent(NGSILDQueryParams ngsildquery) {
    return ngsildquery.getTemporalRelation().getTemprel() != null
        || ngsildquery.getTemporalRelation().getTime() != null
        || ngsildquery.getTemporalRelation().getEndTime() != null;
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

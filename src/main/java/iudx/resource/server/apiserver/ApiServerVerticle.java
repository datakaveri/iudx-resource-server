package iudx.resource.server.apiserver;


import static iudx.resource.server.apiserver.util.Constants.API_ENDPOINT;
import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.APP_NAME_REGEX;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.EXCHANGE_ID;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ACCEPT;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ALLOW_ORIGIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_CONTENT_LENGTH;
import static iudx.resource.server.apiserver.util.Constants.HEADER_CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.HEADER_HOST;
import static iudx.resource.server.apiserver.util.Constants.HEADER_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.HEADER_ORIGIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_REFERER;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.resource.server.apiserver.util.Constants.IUDXQUERY_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_ADAPTER_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_BIND_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_QUEUE_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_UNBIND_URL;
import static iudx.resource.server.apiserver.util.Constants.IUDX_MANAGEMENT_VHOST_URL;
import static iudx.resource.server.apiserver.util.Constants.JSON_ALIAS;
import static iudx.resource.server.apiserver.util.Constants.JSON_CONSUMER;
import static iudx.resource.server.apiserver.util.Constants.JSON_COUNT;
import static iudx.resource.server.apiserver.util.Constants.JSON_DOMAIN;
import static iudx.resource.server.apiserver.util.Constants.JSON_EXCHANGE_NAME;
import static iudx.resource.server.apiserver.util.Constants.JSON_INSTANCEID;
import static iudx.resource.server.apiserver.util.Constants.JSON_NAME;
import static iudx.resource.server.apiserver.util.Constants.JSON_PROVIDER;
import static iudx.resource.server.apiserver.util.Constants.JSON_QUEUE_NAME;
import static iudx.resource.server.apiserver.util.Constants.JSON_RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.JSON_RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.JSON_RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.apiserver.util.Constants.JSON_USERSHA;
import static iudx.resource.server.apiserver.util.Constants.JSON_VHOST;
import static iudx.resource.server.apiserver.util.Constants.JSON_VHOST_ID;
import static iudx.resource.server.apiserver.util.Constants.MIME_APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.MIME_TEXT_HTML;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_EXCHANGE_NAME;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_NAME;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_PARAM;
import static iudx.resource.server.apiserver.util.Constants.MSG_PARAM_DECODE_ERROR;
import static iudx.resource.server.apiserver.util.Constants.MSG_SUB_TYPE_NOT_FOUND;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_ENTITIES_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_POST_QUERY_PATH;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_SUBSCRIPTION_URL;
import static iudx.resource.server.apiserver.util.Constants.NGSILD_TEMPORAL_URL;
import static iudx.resource.server.apiserver.util.Constants.ROUTE_DOC;
import static iudx.resource.server.apiserver.util.Constants.ROUTE_STATIC_SPEC;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION_ID;
import static iudx.resource.server.apiserver.util.Constants.SUB_TYPE;
import static iudx.resource.server.apiserver.util.Util.toUriFunction;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
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
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.management.ManagementApi;
import iudx.resource.server.apiserver.management.ManagementApiImpl;
import iudx.resource.server.apiserver.query.NGSILDQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.response.RestResponse;
import iudx.resource.server.apiserver.subscription.SubsType;
import iudx.resource.server.apiserver.subscription.SubscriptionService;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;


/**
 * The Resource Server API Verticle.
 * <h1>Resource Server API Verticle</h1>
 * <p>
 * The API Server verticle implements the IUDX Resource Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 * </p>
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

  private HttpServer server;
  private Router router;
  private final int port = 80;
  private boolean isSSL;
  private String keystore;
  private String keystorePassword;
  private ManagementApi managementApi;
  private SubscriptionService subsService;

  private DatabaseService database;
  private DataBrokerService databroker;
  private AuthenticationService authenticator;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443 or an HTTP server at port 8080.
   * 
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   * 
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
    router.route().handler(
        CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
    // router.route().handler(new TokenHandler());
    router.route().handler(BodyHandler.create());
    router.route().handler(AuthHandler.create(vertx));

    /* NGSI-LD api endpoints */
    router.get(NGSILD_ENTITIES_URL).handler(this::handleEntitiesQuery);
    router
        .get(NGSILD_ENTITIES_URL + "/:domain/:userSha/:resourceServer/:resourceGroup/:resourceName")
        .handler(this::handleEntitiesQuery);
    router.post(NGSILD_POST_QUERY_PATH).handler(this::handlePostEntitiesQuery);
    router.get(NGSILD_TEMPORAL_URL).handler(this::handleTemporalQuery);
    router.post(NGSILD_SUBSCRIPTION_URL).handler(this::handleSubscriptions);
    // append sub
    router.patch(NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
        .handler(this::appendSubscription);
    // update sub
    router.put(NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
        .handler(this::updateSubscription);
    // get sub
    router.get(NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias").handler(this::getSubscription);
    // delete sub
    router.delete(NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
        .handler(this::deleteSubscription);

    /* Management Api endpoints */
    // Exchange
    router.post(IUDX_MANAGEMENT_EXCHANGE_URL).handler(this::createExchange);
    router.delete(IUDX_MANAGEMENT_EXCHANGE_URL + "/:exId").handler(this::deleteExchange);
    router.get(IUDX_MANAGEMENT_EXCHANGE_URL + "/:exId").handler(this::getExchangeDetails);
    // Queue
    router.post(IUDX_MANAGEMENT_QUEUE_URL).handler(this::createQueue);
    router.delete(IUDX_MANAGEMENT_QUEUE_URL + "/:queueId").handler(this::deleteQueue);
    router.get(IUDX_MANAGEMENT_QUEUE_URL + "/:queueId").handler(this::getQueueDetails);
    // bind
    router.post(IUDX_MANAGEMENT_BIND_URL).handler(this::bindQueue2Exchange);
    // unbind
    router.post(IUDX_MANAGEMENT_UNBIND_URL).handler(this::unbindQueue2Exchange);
    // vHost
    router.post(IUDX_MANAGEMENT_VHOST_URL).handler(this::createVHost);
    router.delete(IUDX_MANAGEMENT_VHOST_URL + "/:vhostId").handler(this::deleteVHost);
    // adapter
    router.post(IUDX_MANAGEMENT_ADAPTER_URL + "/register").handler(this::registerAdapter);
    router.delete(IUDX_MANAGEMENT_ADAPTER_URL + "/:domain/:userSHA/:resourceServer/:resourceGroup")
        .handler(this::deleteAdapter);
    router.get(IUDX_MANAGEMENT_ADAPTER_URL + "/:domain/:userSHA/:resourceServer/:resourceGroup")
        .handler(this::getAdapterDetails);
    router.post(IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat").handler(this::publishHeartbeat);
    router.post(IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue")
        .handler(this::publishDownstreamIssue);
    router.post(IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue").handler(this::publishDataIssue);
    router.post(IUDX_MANAGEMENT_ADAPTER_URL + "/entities").handler(this::publishDataFromAdapter);

    /**
     * Documentation routes
     */
    /* Static Resource Handler */
    /* Get openapiv3 spec */
    router.get(ROUTE_STATIC_SPEC).produces(MIME_APPLICATION_JSON).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.sendFile("docs/openapi.yaml");
    });
    /* Get redoc */
    router.get(ROUTE_DOC).produces(MIME_TEXT_HTML).handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response.sendFile("docs/apidoc.html");
    });
    
    /* Read ssl configuration. */
    isSSL = config().getBoolean("ssl");
    HttpServerOptions serverOptions = new HttpServerOptions();
    
    if (isSSL) {
      LOGGER.debug("Info: Starting HTTPs server");

      /* Read the configuration and set the HTTPs server properties. */
      
      keystore = config().getString("keystore");
      keystorePassword = config().getString("keystorePassword");

      /* Setup the HTTPs server properties, APIs and port. */

      serverOptions.setSsl(true)
          .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
      
    } else {
      LOGGER.debug("Info: Starting HTTP server");

      /* Setup the HTTP server properties, APIs and port. */

      serverOptions.setSsl(false);

    }

    serverOptions.setCompressionSupported(true).setCompressionLevel(5);
    server = vertx.createHttpServer(serverOptions);
    server.requestHandler(router).listen(port);

    /* Get a handler for the Service Discovery interface. */

    database = DatabaseService.createProxy(vertx, DATABASE_SERVICE_ADDRESS);

    authenticator = AuthenticationService.createProxy(vertx, AUTH_SERVICE_ADDRESS);

    databroker = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);


    managementApi = new ManagementApiImpl();
    subsService = new SubscriptionService();
  }

  /**
   * This method is used to handle all NGSI-LD queries for endpoint /ngsi-ld/v1/entities/**.
   * 
   * @param routingContext RoutingContext Object
   */
  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.debug("Info:handleEntitiesQuery method started.;");
    /* Handles HTTP request from client */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.debug("authInfo : " + authInfo);
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Future<Boolean> validationResult = Validator.validate(params);
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        String domain = request.getParam(JSON_DOMAIN);
        String userSha = request.getParam(JSON_USERSHA);
        String resourceServer = request.getParam(JSON_RESOURCE_SERVER);
        String resourceGroup = request.getParam(JSON_RESOURCE_GROUP);
        String resourceName = request.getParam(JSON_RESOURCE_NAME);
        String pathId = domain + "/" + userSha + "/" + resourceServer + "/" + resourceGroup + "/"
            + resourceName;
        // parse query params
        NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
        LOGGER.debug("Info : PathId " + pathId);
        if (!pathId.contains("null")) {
          List<URI> ids = new ArrayList<>();
          ids.add(toUriFunction.apply(pathId));
          ngsildquery.setId(ids);
        }
        // create json
        QueryMapper queryMapper = new QueryMapper();
        JsonObject json = queryMapper.toJson(ngsildquery, false);
        /* HTTP request instance/host details */
        String instanceID = request.getHeader(HEADER_HOST);
        json.put(JSON_INSTANCEID, instanceID);
        LOGGER.debug("Info: IUDX query json;" + json);
        /* HTTP request body as Json */
        JsonObject requestBody = new JsonObject();
        requestBody.put("ids", json.getJsonArray("id"));
        if (json.containsKey(IUDXQUERY_OPTIONS)
            && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
          database.countQuery(json, handler -> {
            if (handler.succeeded()) {
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        } else {
          // call database vertical for seaarch
          database.searchQuery(json, handler -> {
            if (handler.succeeded()) {
              LOGGER.info("Success: Search Query success");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Search Query failed");
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        }
      } else if (validationHandler.failed()) {
        LOGGER.error("Fail: Validation failed");
        handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_PARAM);
      }
    });
  }

  /**
   * this method is used to handle all entities queries from post endpoint.
   * 
   * @param routingContext routingContext
   *
   */
  public void handlePostEntitiesQuery(RoutingContext routingContext) {
    LOGGER.debug("Info: handlePostEntitiesQuery method started.");
    HttpServerRequest request = routingContext.request();
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.debug("Info: request Json :: ;" + requestJson);
    HttpServerResponse response = routingContext.response();
    // validate request parameters
    Future<Boolean> validationResult = Validator.validate(requestJson);
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        // parse query params
        NGSILDQueryParams ngsildquery = new NGSILDQueryParams(requestJson);
        QueryMapper queryMapper = new QueryMapper();
        JsonObject json = queryMapper.toJson(ngsildquery, requestJson.containsKey("temporalQ"));
        String instanceID = request.getHeader(HEADER_HOST);
        json.put(JSON_INSTANCEID, instanceID);
        requestJson.put("ids", json.getJsonArray("id"));
        LOGGER.debug("Info: IUDX query json : ;" + json);
        if (json.containsKey(IUDXQUERY_OPTIONS)
            && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
          database.countQuery(json, handler -> {
            if (handler.succeeded()) {
              LOGGER.info("Success: Count Success");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Count Fail");
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        } else {
          // call database vertical for search
          database.searchQuery(json, handler -> {
            if (handler.succeeded()) {
              LOGGER.info("Success: Search Success");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Search Fail");
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        }
      } else if (validationHandler.failed()) {
        LOGGER.error("Fail: Bad request");
        handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_PARAM);
      }
    });
  }

  /**
   * This method is used to handler all temporal NGSI-LD queries for endpoint
   * /ngsi-ld/v1/temporal/**.
   * 
   * @param routingContext RoutingContext object
   * 
   */
  private void handleTemporalQuery(RoutingContext routingContext) {
    LOGGER.debug("Info: handleTemporalQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request params
    Future<Boolean> validationResult = Validator.validate(params);
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        // parse query params
        NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
        // create json
        QueryMapper queryMapper = new QueryMapper();
        JsonObject json = queryMapper.toJson(ngsildquery, true);
        json.put(JSON_INSTANCEID, instanceID);
        LOGGER.debug("Info: IUDX temporal json query;" + json);
        /* HTTP request body as Json */
        JsonObject requestBody = new JsonObject();
        requestBody.put("ids", json.getJsonArray("id"));
        if (json.containsKey(IUDXQUERY_OPTIONS)
            && JSON_COUNT.equalsIgnoreCase(json.getString(IUDXQUERY_OPTIONS))) {
          database.countQuery(json, handler -> {
            if (handler.succeeded()) {
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        } else {
          // call database vertical for normal seaarch
          database.searchQuery(json, handler -> {
            if (handler.succeeded()) {
              LOGGER.info("Success: Temporal query");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  handler.result().toString());
            } else if (handler.failed()) {
              LOGGER.error("Fail: Temporal query");
              processBackendResponse(response, handler.cause().getMessage());
            }
          });
        }
      } else if (validationHandler.failed()) {
        LOGGER.error("Fail: Bad request;");
        handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_PARAM);
      }
    });

  }

  /**
   * Method used to handle all subscription requests.
   * 
   * @param routingContext routingContext
   */
  private void handleSubscriptions(RoutingContext routingContext) {
    LOGGER.debug("Info: handleSubscription method started");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestBody.put(SUB_TYPE, subscrtiptionType);
    /* checking authentication info in requests */
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.debug("authInfo : " + authInfo);

    if (requestBody.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestBody.copy();
      jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
      jsonObj.put(JSON_INSTANCEID, instanceID);
      LOGGER.debug("Info: json for subs :: ;" + jsonObj);
      Future<JsonObject> subsReq = subsService.createSubscription(jsonObj, databroker, database);
      subsReq.onComplete(subHandler -> {
        if (subHandler.succeeded()) {
          LOGGER.info("Success: Handle Subscription request;");
          handleSuccessResponse(response, ResponseType.Created.getCode(),
              subHandler.result().toString());
        } else {
          LOGGER.error("Fail: Handle Subscription request;");
          processBackendResponse(response, subHandler.cause().getMessage());
        }
      });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, ResponseType.BadRequestData, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * handle append requests for subscription.
   * 
   * @param routingContext routingContext
   */
  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.debug("Info: appendSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    LOGGER.debug("authInfo : " + authInfo);
    if (requestJson != null && requestJson.containsKey(SUB_TYPE)) {
      if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
        JsonObject jsonObj = requestJson.copy();
        jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
        Future<JsonObject> subsReq = subsService.appendSubscription(jsonObj, databroker, database);
        subsReq.onComplete(subsRequestHandler -> {
          if (subsRequestHandler.succeeded()) {
            LOGGER.info("Success: Appending subscription");
            handleSuccessResponse(response, ResponseType.Created.getCode(),
                subsRequestHandler.result().toString());
          } else {
            LOGGER.error("Fail: Appending subscription");
            processBackendResponse(response, subsRequestHandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error("Fail: Bad request");
        handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_NAME);
      }
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, ResponseType.BadRequestData, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * handle update subscription requests.
   * 
   * @param routingContext routingContext
   */
  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.debug("Info: updateSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(HEADER_HOST);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    if (requestJson != null && requestJson.containsKey(SUB_TYPE)) {
      if (requestJson.getString(JSON_NAME).equalsIgnoreCase(alias)) {
        JsonObject jsonObj = requestJson.copy();
        jsonObj.put(SUBSCRIPTION_ID, subsId);
        jsonObj.put(JSON_INSTANCEID, instanceID);
        jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
        Future<JsonObject> subsReq = subsService.updateSubscription(jsonObj, databroker, database);
        subsReq.onComplete(subsRequestHandler -> {
          if (subsRequestHandler.succeeded()) {
            handleSuccessResponse(response, ResponseType.Created.getCode(),
                subsRequestHandler.result().toString());
          } else {
            LOGGER.error("Fail: Bad request");
            processBackendResponse(response, subsRequestHandler.cause().getMessage());
          }
        });
      } else {
        LOGGER.error("Fail: Bad request");
        handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_NAME);
      }
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, ResponseType.BadRequestData, MSG_SUB_TYPE_NOT_FOUND);
    }


  }

  /**
   * get a subscription by id.
   * 
   * @param routingContext routingContext
   */
  private void getSubscription(RoutingContext routingContext) {
    LOGGER.debug("Info: getSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");

    if (requestJson != null && requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
      Future<JsonObject> subsReq = subsService.getSubscription(jsonObj, databroker, database);
      subsReq.onComplete(subHandler -> {
        if (subHandler.succeeded()) {
          LOGGER.info("Success: Getting subscription");
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              subHandler.result().toString());
        } else {
          LOGGER.error("Fail: Bad request");
          processBackendResponse(response, subHandler.cause().getMessage());
        }
      });
    } else {
      LOGGER.error("Fail: Bad request");
      handleResponse(response, ResponseType.BadRequestData, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * delete a subscription by id.
   * 
   * @param routingContext routingContext
   */
  private void deleteSubscription(RoutingContext routingContext) {
    LOGGER.debug("Info: deleteSubscription method started;");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String alias = request.getParam(JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(SUBSCRIPTION_ID, subsId);
    requestJson.put(JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(SUB_TYPE, subscrtiptionType);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    if (requestJson.containsKey(SUB_TYPE)) {
      JsonObject jsonObj = requestJson.copy();
      jsonObj.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
      Future<JsonObject> subsReq = subsService.deleteSubscription(jsonObj, databroker, database);
      subsReq.onComplete(subHandler -> {
        if (subHandler.succeeded()) {
          handleSuccessResponse(response, ResponseType.Ok.getCode(),
              subHandler.result().toString());
        } else {
          processBackendResponse(response, subHandler.cause().getMessage());
        }
      });
    } else {
      handleResponse(response, ResponseType.BadRequestData, MSG_SUB_TYPE_NOT_FOUND);
    }
  }

  /**
   * Create a exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createExchange(RoutingContext routingContext) {
    LOGGER.debug("Info: createExchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.info("request ::: " + requestJson);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.debug("Info: Authenticating response ;".concat(authHandler.result().toString()));
          LOGGER.debug("Info: databroker :: ;" + databroker);
          Future<Boolean> isValidNameResult =
              isValidName(requestJson.copy().getString(JSON_EXCHANGE_NAME));
          isValidNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult =
                  managementApi.createExchange(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  LOGGER.info("Success: Creating exchange");
                  handleSuccessResponse(response, ResponseType.Created.getCode(),
                      brokerResultHandler.result().toString());
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause());
                  processBackendResponse(response, brokerResultHandler.cause().getMessage());
                }
              });
            } else {
              LOGGER.error("Fail: Unauthorized;" + validNameHandler.cause().getMessage());
              handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_EXCHANGE_NAME);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }

  }

  /**
   * delete an exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void deleteExchange(RoutingContext routingContext) {
    LOGGER.debug("Info: deleteExchange method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    String exchangeId = request.getParam(EXCHANGE_ID);
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteExchange(exchangeId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Deleting exchange");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }

  }

  /**
   * get exchange details from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void getExchangeDetails(RoutingContext routingContext) {
    LOGGER.debug("Info: getExchange method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/exchange");
    LOGGER.debug("Info: request :: ;" + request);
    LOGGER.debug("Info: request json :: ;" + requestJson);
    String exchangeId = request.getParam(EXCHANGE_ID);
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceID);
    HttpServerResponse response = routingContext.response();
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.getExchangeDetails(exchangeId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Getting exchange details");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * create a queue in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createQueue(RoutingContext routingContext) {
    LOGGER.debug("Info: createQueue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        LOGGER.debug("Info: Authenticating response ;".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<Boolean> validNameResult =
              isValidName(requestJson.copy().getString(JSON_QUEUE_NAME));
          validNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult = managementApi.createQueue(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  LOGGER.info("Success: Creating Queue");
                  handleSuccessResponse(response, ResponseType.Created.getCode(),
                      brokerResultHandler.result().toString());
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
                  processBackendResponse(response, brokerResultHandler.cause().getMessage());
                }
              });
            } else {
              LOGGER.error("Fail: Bad request");
              handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_EXCHANGE_NAME);
            }

          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * delete a queue in rabbit MQ.
   * 
   * @param routingContext routingContext.
   */
  private void deleteQueue(RoutingContext routingContext) {
    LOGGER.debug("Info: deleteQueue method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        LOGGER.debug("Info: Authenticating response ;".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteQueue(queueId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Deleting Queue");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * get queue details from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void getQueueDetails(RoutingContext routingContext) {
    LOGGER.debug("Info: getQueueDetails method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/queue");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        LOGGER.debug("Info: Authenticating response;".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.getQueueDetails(queueId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Getting Queue Details");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad Request;" + brokerResultHandler.cause());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * bind queue to exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void bindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.debug("Info: bindQueue2Exchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/bind");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.bindQueue2Exchange(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: binding queue to exchange");
              handleSuccessResponse(response, ResponseType.Created.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * unbind a queue from an exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void unbindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.debug("Info: unbindQueue2Exchange method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/unbind");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.unbindQueue2Exchange(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Unbinding queue to exchange");
              handleSuccessResponse(response, ResponseType.Created.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * create a vhost in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createVHost(RoutingContext routingContext) {
    LOGGER.debug("Info: createVHost method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/vhost");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<Boolean> validNameResult = isValidName(requestJson.copy().getString(JSON_VHOST));
          validNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult = managementApi.createVHost(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  LOGGER.info("Success: Creating vhost");
                  handleSuccessResponse(response, ResponseType.Created.getCode(),
                      brokerResultHandler.result().toString());
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
                  processBackendResponse(response, brokerResultHandler.cause().getMessage());
                }
              });
            } else {
              LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
              handleResponse(response, ResponseType.BadRequestData, MSG_INVALID_EXCHANGE_NAME);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized");
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }

  }

  /**
   * delete vhost from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void deleteVHost(RoutingContext routingContext) {
    LOGGER.debug("Info: deleteVHost method started;");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/management/vhost");
    requestJson.put(JSON_INSTANCEID, instanceID);
    String vhostId = routingContext.request().getParam(JSON_VHOST_ID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteVHost(vhostId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.info("Success: Deleting vhost");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else if (brokerResultHandler.failed()) {
              LOGGER.error("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.error("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * register a adapter in Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void registerAdapter(RoutingContext routingContext) {
    LOGGER.debug("Info: registerAdapter method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    requestJson.put(JSON_INSTANCEID, instanceID);
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    requestJson.put(JSON_CONSUMER, authInfo.getString(JSON_CONSUMER));
    requestJson.put(JSON_PROVIDER, authInfo.getString(JSON_PROVIDER));
    Future<JsonObject> brokerResult = managementApi.registerAdapter(requestJson, databroker);
    brokerResult.onComplete(brokerResultHandler -> {
      if (brokerResultHandler.succeeded()) {
        LOGGER.info("Success: Registering adapter");
        handleSuccessResponse(response, ResponseType.Created.getCode(),
            brokerResultHandler.result().toString());
      } else if (brokerResult.failed()) {
        LOGGER.error("Fail: Bad request" + brokerResultHandler.cause().getMessage());
        processBackendResponse(response, brokerResultHandler.cause().getMessage());
      }
    });
  }

  /**
   * delete a adapter in Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void deleteAdapter(RoutingContext routingContext) {
    LOGGER.debug("Info: deleteAdapter method starts;");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String resourceGroup = request.getParam(JSON_RESOURCE_GROUP);
    String resourceServer = request.getParam(JSON_RESOURCE_SERVER);
    String adapterId = domain + "/" + usersha + "/" + resourceServer + "/" + resourceGroup;
    Future<JsonObject> brokerResult = managementApi.deleteAdapter(adapterId, databroker);
    brokerResult.onComplete(brokerResultHandler -> {
      if (brokerResultHandler.succeeded()) {
        LOGGER.info("Success: Deleting adapter");
        handleSuccessResponse(response, ResponseType.Ok.getCode(),
            brokerResultHandler.result().toString());
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
    LOGGER.info("getAdapterDetails method starts");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(JSON_DOMAIN);
    String usersha = request.getParam(JSON_USERSHA);
    String resourceGroup = request.getParam(JSON_RESOURCE_GROUP);
    String resourceServer = request.getParam(JSON_RESOURCE_SERVER);
    String adapterId = domain + "/" + usersha + "/" + resourceServer + "/" + resourceGroup;
    Future<JsonObject> brokerResult = managementApi.getAdapterDetails(adapterId, databroker);
    brokerResult.onComplete(brokerResultHandler -> {
      if (brokerResultHandler.succeeded()) {
        handleSuccessResponse(response, ResponseType.Ok.getCode(),
            brokerResultHandler.result().toString());
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
    LOGGER.debug("Info: publishHeartbeat method starts;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.publishHeartbeat(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: Published heartbeat");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Unauthorized;" + authHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.debug("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.info("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * publish downstream issues to Rabbit MQ.
   * 
   * @param routingContext routingContext Note: This is too frequent an operation to have info or
   *        error level logs
   */
  public void publishDownstreamIssue(RoutingContext routingContext) {
    LOGGER.debug("Info: publishDownStreamIssue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.publishDownstreamIssues(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: published downstream issue");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.debug("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.debug("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * publish data issue to Rabbit MQ.
   * 
   * @param routingContext routingContext Note: All logs are debug level only
   */
  public void publishDataIssue(RoutingContext routingContext) {
    LOGGER.debug("Info: publishDataIssue method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.publishDataIssue(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: publishing a data issue");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure);
    }
  }

  /**
   * publish data from adapter to rabbit MQ.
   * 
   * @param routingContext routingContext Note: All logs are debug level
   */
  public void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.debug("Info: publishDataFromAdapter method started;");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    authenticationInfo.put(API_ENDPOINT, "/iudx/v1/adapter");
    requestJson.put(JSON_INSTANCEID, instanceID);
    if (request.headers().contains(HEADER_TOKEN)) {
      authenticationInfo.put(HEADER_TOKEN, request.getHeader(HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.publishDataFromAdapter(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              LOGGER.debug("Success: publishing data from adapter");
              handleSuccessResponse(response, ResponseType.Ok.getCode(),
                  brokerResultHandler.result().toString());
            } else {
              LOGGER.debug("Fail: Bad request;" + brokerResultHandler.cause().getMessage());
              processBackendResponse(response, brokerResultHandler.cause().getMessage());
            }
          });
        } else {
          LOGGER.debug("Fail: Unauthorized;" + authHandler.cause().getMessage());
          handleResponse(response, ResponseType.AuthenticationFailure);
        }
      });
    } else {
      LOGGER.debug("Fail: Unauthorized");
      handleResponse(response, ResponseType.AuthenticationFailure);
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
      ResponseType responseType = ResponseType.fromCode(type);
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(type)
          .end(generateResponse(responseType).toString());
    } catch (DecodeException ex) {
      LOGGER.error("ERROR : Expecting Json received else from backend service");
      handleResponse(response, ResponseType.BadRequestData);
    }

  }

  private void handleResponse(HttpServerResponse response, ResponseType responseType) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(responseType.getCode())
        .end(generateResponse(responseType).toString());
  }

  private void handleResponse(HttpServerResponse response, ResponseType responseType,
      String message) {
    response.putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(responseType.getCode())
        .end(generateResponse(responseType, message).toString());
  }

  private JsonObject generateResponse(ResponseType responseType) {
    int type = responseType.getCode();
    return new RestResponse.Builder().withType(type)
        .withTitle(ResponseType.fromCode(type).getMessage())
        .withMessage(ResponseType.fromCode(type).getMessage()).build().toJson();
  }

  private JsonObject generateResponse(ResponseType responseType, String message) {
    int type = responseType.getCode();
    return new RestResponse.Builder().withType(type)
        .withTitle(ResponseType.fromCode(type).getMessage()).withMessage(message).build().toJson();

  }
  /**
   * validate if name passes the regex test for IUDX queue,exchage name.
   * 
   * @param name name(queue,exchange)
   * @return Future true if name matches the regex else false
   */
  public Future<Boolean> isValidName(String name) {
    Promise<Boolean> promise = Promise.promise();
    if (Pattern.compile(APP_NAME_REGEX).matcher(name).matches()) {
      promise.complete(true);
    } else {
      LOGGER.error(MSG_INVALID_NAME + name);
      promise.fail(MSG_INVALID_NAME);
    }
    return promise.future();
  }

  /**
   * Get the request query parameters delimited by <b>&</b>, <i><b>;</b>(semicolon) is considered as
   * part of the parameter</i>.
   * 
   * @param routingContext RoutingContext Object
   * @param response HttpServerResponse
   * @return Optional Optional of Map
   */
  private Optional<MultiMap> getQueryParams(RoutingContext routingContext,
      HttpServerResponse response) {
    MultiMap queryParams = null;
    try {
      queryParams = MultiMap.caseInsensitiveMultiMap();
      //Internally + sign is dropped and treated as space, replacing + with %2B do the trick 
      String uri=routingContext.request().uri().toString().replaceAll("\\+", "%2B");
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(uri, HttpConstants.DEFAULT_CHARSET,
              true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        queryParams.add(entry.getKey(), entry.getValue());
      }
      LOGGER.debug("Info: Decoded multimap");
    } catch (IllegalArgumentException ex) {
      response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(ResponseType.BadRequestData.getCode())
          .end(generateResponse(ResponseType.BadRequestData, MSG_PARAM_DECODE_ERROR).toString());


    }
    return Optional.of(queryParams);
  }

}

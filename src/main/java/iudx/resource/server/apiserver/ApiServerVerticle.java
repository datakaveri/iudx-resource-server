package iudx.resource.server.apiserver;


import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.apiserver.management.ManagementApi;
import iudx.resource.server.apiserver.management.ManagementApiImpl;
import iudx.resource.server.apiserver.query.GeoRelation;
import iudx.resource.server.apiserver.query.NGSILDQueryParams;
import iudx.resource.server.apiserver.query.QueryMapper;
import iudx.resource.server.apiserver.query.TemporalRelation;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.response.RestResponse;
import iudx.resource.server.apiserver.subscription.SubsType;
import iudx.resource.server.apiserver.subscription.SubscriptionService;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.authenticator.AuthenticationService;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;
import iudx.resource.server.filedownload.FileDownloadService;
import iudx.resource.server.media.MediaService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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

  private static final Logger LOGGER = LoggerFactory.getLogger(ApiServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private DatabaseService database;
  private DataBrokerService databroker;
  private AuthenticationService authenticator;
  private FileDownloadService filedownload;
  private MediaService media;
  private HttpServer server;
  private Router router;
  private Properties properties;
  private InputStream inputstream;
  private final int port = 8443;
  private String keystore;
  private String keystorePassword;
  private ManagementApi managementApi;
  private SubscriptionService subsService;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443.
   * 
   * @throws Exception which is a startup exception TODO Need to add documentation for all the
   * 
   */

  @Override
  public void start() throws Exception {

    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add(Constants.HEADER_ACCEPT);
    allowedHeaders.add(Constants.HEADER_TOKEN);
    allowedHeaders.add(Constants.HEADER_CONTENT_LENGTH);
    allowedHeaders.add(Constants.HEADER_CONTENT_TYPE);
    allowedHeaders.add(Constants.HEADER_HOST);
    allowedHeaders.add(Constants.HEADER_ORIGIN);
    allowedHeaders.add(Constants.HEADER_REFERER);
    allowedHeaders.add(Constants.HEADER_ALLOW_ORIGIN);

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;

        /* Define the APIs, methods, endpoints and associated methods. */

        router = Router.router(vertx);
        router.route().handler(
            CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
        router.route("/apis/*").handler(StaticHandler.create());
        router.route().handler(BodyHandler.create());

        /* NGSI-LD api endpoints */
        router.get(Constants.NGSILD_ENTITIES_URL).handler(this::handleEntitiesQuery);
        router.post(Constants.NGSILD_POST_QUERY_PATH).handler(this::handlePostEntitiesQuery);
        router.get(Constants.NGSILD_TEMPORAL_URL).handler(this::handleTemporalQuery);
        router.post(Constants.NGSILD_SUBSCRIPTION_URL).handler(this::handleSubscriptions);
        // append sub
        router.patch(Constants.NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
            .handler(this::appendSubscription);
        // update sub
        router.put(Constants.NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
            .handler(this::updateSubscription);
        // get sub
        router.get(Constants.NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
            .handler(this::getSubscription);
        // delete sub
        router.delete(Constants.NGSILD_SUBSCRIPTION_URL + "/:domain/:userSHA/:alias")
            .handler(this::deleteSubscription);

        /* Management Api endpoints */
        // Exchange
        router.post(Constants.IUDX_MANAGEMENT_EXCHANGE_URL).handler(this::createExchange);
        router.delete(Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/:exId")
            .handler(this::deleteExchange);
        router.get(Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/:exId")
            .handler(this::getExchangeDetails);
        // Queue
        router.post(Constants.IUDX_MANAGEMENT_QUEUE_URL).handler(this::createQueue);
        router.delete(Constants.IUDX_MANAGEMENT_QUEUE_URL + "/:queueId").handler(this::deleteQueue);
        router.get(Constants.IUDX_MANAGEMENT_QUEUE_URL + "/:queueId")
            .handler(this::getQueueDetails);
        // bind
        router.post(Constants.IUDX_MANAGEMENT_BIND_URL).handler(this::bindQueue2Exchange);
        // unbind
        router.post(Constants.IUDX_MANAGEMENT_UNBIND_URL).handler(this::unbindQueue2Exchange);
        // vHost
        router.post(Constants.IUDX_MANAGEMENT_VHOST_URL).handler(this::createVHost);
        router.delete(Constants.IUDX_MANAGEMENT_VHOST_URL + "/:vhostId").handler(this::deleteVHost);
        // adapter
        router.post(Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register")
            .handler(this::registerAdapter);
        router.delete(Constants.IUDX_MANAGEMENT_ADAPTER_URL
            + "/:domain/:userSHA/:resourceServer/:resourceGroup").handler(this::deleteAdapter);
        router
            .get(Constants.IUDX_MANAGEMENT_ADAPTER_URL
                + "/:domain/:userSHA/:resourceServer/:resourceGroup")
            .handler(this::getAdapterDetails);
        router.post(Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat")
            .handler(this::publishHeartbeat);
        router.post(Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue")
            .handler(this::publishDownstreamIssue);
        router.post(Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue")
            .handler(this::publishDataIssue);
        router.post(Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/entities")
            .handler(this::publishDataFromAdapter);
        /* Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          keystore = properties.getProperty("keystore");
          keystorePassword = properties.getProperty("keystorePassword");

        } catch (Exception ex) {

          LOGGER.info(ex.toString());

        }

        /* Setup the HTTPs server properties, APIs and port. */

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(port);

        /* Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /* Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class,
            databaseServiceDiscoveryHandler -> {
              if (databaseServiceDiscoveryHandler.succeeded()) {
                database = databaseServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + database.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the DataBrokerService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DataBrokerService.class,
            databrokerServiceDiscoveryHandler -> {
              if (databrokerServiceDiscoveryHandler.succeeded()) {
                databroker = databrokerServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + databroker.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /*
         * Get a handler for the AuthenticationService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, AuthenticationService.class,
            authenticatorServiceDiscoveryHandler -> {
              if (authenticatorServiceDiscoveryHandler.succeeded()) {
                authenticator = authenticatorServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + authenticator.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /*
         * Get a handler for the FileDownloadService from Service Discovery interface.
         */

        EventBusService.getProxy(discovery, FileDownloadService.class,
            filedownloadServiceDiscoveryHandler -> {
              if (filedownloadServiceDiscoveryHandler.succeeded()) {
                filedownload = filedownloadServiceDiscoveryHandler.result();
                LOGGER.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + filedownload.getClass().getName() + " +++++++ ");
              } else {
                LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the MediaService from Service Discovery interface. */

        EventBusService.getProxy(discovery, MediaService.class, mediaServiceDiscoveryHandler -> {
          if (mediaServiceDiscoveryHandler.succeeded()) {
            media = mediaServiceDiscoveryHandler.result();
            LOGGER
                .info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                    + media.getClass().getName() + " +++++++ ");
          } else {
            LOGGER.info("\n +++++++ Service Discovery Failed. +++++++ ");
          }
        });
        managementApi = new ManagementApiImpl();
        subsService = new SubscriptionService();
      }
    });
  }

  /**
   * This method is used to handle all NGSI-LD queries for endpoint /ngsi-ld/v1/entities/**.
   * 
   * @param routingContext RoutingContext Object
   */
  private void handleEntitiesQuery(RoutingContext routingContext) {
    LOGGER.info("handleEntitiesQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();
    /* checking authentication info in requests */
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
    } else {
      authenticationInfo.put(Constants.HEADER_TOKEN, Constants.PUBLIC_TOKEN);
    }
    // get query paramaters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request parameters
    Future<Boolean> validationResult = Validator.validate(params);
    // parse query params
    NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
    QueryMapper queryMapper = new QueryMapper();
    // create json
    JsonObject json = queryMapper.toJson(ngsildquery, false);
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    json.put(Constants.JSON_INSTANCEID, instanceID);
    LOGGER.info("IUDX query json : " + json);
    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();
    /* Authenticating the request */
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        authenticator.tokenInterospect(requestBody, authenticationInfo, authHandler -> {
          if (authHandler.succeeded()) {
            LOGGER.info(
                "Authenticating entity search request ".concat(authHandler.result().toString()));
            if (json.containsKey(Constants.IUDXQUERY_OPTIONS) && Constants.JSON_COUNT
                .equalsIgnoreCase(json.getString(Constants.IUDXQUERY_OPTIONS))) {
              database.countQuery(json, handler -> {
                if (handler.succeeded()) {
                  handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
                } else if (handler.failed()) {
                  handleResponse(response, ResponseType.BadRequestData,
                      handler.cause().getMessage(), true);
                }
              });
            } else {
              // call database vertical for seaarch
              database.searchQuery(json, handler -> {
                if (handler.succeeded()) {
                  handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
                } else if (handler.failed()) {
                  handleResponse(response, ResponseType.BadRequestData,
                      handler.cause().getMessage(), true);
                }
              });
            }
          } else if (authHandler.failed()) {
            LOGGER.error("Unathorized request".concat(authHandler.cause().toString()));
            handleResponse(response, ResponseType.AuthenticationFailure, true);
          }
        });
      } else if (validationHandler.failed()) {
        handleResponse(response, ResponseType.BadRequestData, Constants.MSG_INVALID_PARAM, true);
      }
    });
  }

  // TODO: complete method
  /**
   * this method is used to handle all entities queries from post endpoint.
   * 
   * @param routingContext routingContext
   *
   */
  public void handlePostEntitiesQuery(RoutingContext routingContext) {
    LOGGER.info("handlePostEntitiesQuery method started.");
    HttpServerRequest request = routingContext.request();
    JsonObject authenticationInfo = new JsonObject();
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
    } else {
      authenticationInfo.put(Constants.HEADER_TOKEN, Constants.PUBLIC_TOKEN);
    }
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.info("request Json :: " + requestJson);
    NGSILDQueryParams ngsildquery = new NGSILDQueryParams(requestJson);
    QueryMapper queryMapper = new QueryMapper();
    JsonObject json = queryMapper.toJson(ngsildquery, requestJson.containsKey("temporalQ"));
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    json.put(Constants.JSON_INSTANCEID, instanceID);
    LOGGER.info("IUDX query json : " + json);
    HttpServerResponse response = routingContext.response();
    authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
      if (authHandler.succeeded()) {
        LOGGER
            .info("Authenticating entity search request ".concat(authHandler.result().toString()));
        if (json.containsKey(Constants.IUDXQUERY_OPTIONS)
            && Constants.JSON_COUNT.equalsIgnoreCase(json.getString(Constants.IUDXQUERY_OPTIONS))) {
          database.countQuery(json, handler -> {
            if (handler.succeeded()) {
              handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
            } else if (handler.failed()) {
              handleResponse(response, ResponseType.BadRequestData, handler.cause().getMessage(),
                  true);
            }
          });
        } else {
          // call database vertical for search
          database.searchQuery(json, handler -> {
            if (handler.succeeded()) {
              handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
            } else if (handler.failed()) {
              handleResponse(response, ResponseType.BadRequestData, handler.cause().getMessage(),
                  true);
            }
          });
        }
      } else if (authHandler.failed()) {
        LOGGER.error("Unathorized request".concat(authHandler.cause().toString()));
        handleResponse(response, ResponseType.AuthenticationFailure, true);
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
    LOGGER.info("handleTemporalQuery method started.");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();
    /* checking authentication info in requests */
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
    } else {
      authenticationInfo.put(Constants.HEADER_TOKEN, Constants.PUBLIC_TOKEN);
    }
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    // get query parameters
    MultiMap params = getQueryParams(routingContext, response).get();
    // validate request params
    Future<Boolean> validationResult = Validator.validate(params);
    // parse query params
    NGSILDQueryParams ngsildquery = new NGSILDQueryParams(params);
    QueryMapper queryMapper = new QueryMapper();
    // create json
    JsonObject json = queryMapper.toJson(ngsildquery, true);
    json.put(Constants.JSON_INSTANCEID, instanceID);
    LOGGER.info("IUDX temporal json query : " + json);
    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();
    /* Authenticating the request */
    validationResult.onComplete(validationHandler -> {
      if (validationHandler.succeeded()) {
        authenticator.tokenInterospect(requestBody, authenticationInfo, authHandler -> {
          if (authHandler.succeeded()) {
            LOGGER.info("Authenticating entity temporal search request "
                .concat(authHandler.result().toString()));
            if (json.containsKey(Constants.IUDXQUERY_OPTIONS) && Constants.JSON_COUNT
                .equalsIgnoreCase(json.getString(Constants.IUDXQUERY_OPTIONS))) {
              database.countQuery(json, handler -> {
                if (handler.succeeded()) {
                  handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
                } else if (handler.failed()) {
                  handleResponse(response, ResponseType.BadRequestData,
                      handler.cause().getMessage(), true);
                }
              });
            } else {
              // call database vertical for normal seaarch
              database.searchQuery(json, handler -> {
                if (handler.succeeded()) {
                  handleResponse(response, ResponseType.Ok, handler.result().toString(), false);
                } else if (handler.failed()) {
                  handleResponse(response, ResponseType.BadRequestData,
                      handler.cause().getMessage(), true);
                }
              });
            }
          } else if (authHandler.failed()) {
            LOGGER.error("Unathorized request".concat(authHandler.cause().toString()));
            handleResponse(response, ResponseType.AuthenticationFailure, true);
          }
        });
      } else if (validationHandler.failed()) {
        handleResponse(response, ResponseType.BadRequestData, Constants.MSG_INVALID_PARAM, true);
      }
    });

  }

  /**
   * Method used to handle all subscription requests.
   * 
   * @param routingContext routingContext
   */
  private void handleSubscriptions(RoutingContext routingContext) {
    LOGGER.info("handleSubscription method started");
    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();
    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    /* JsonObject of authentication related information */
    JsonObject authenticationInfo = new JsonObject();
    /* HTTP request body as Json */
    JsonObject requestBody = routingContext.getBodyAsJson();
    /* HTTP request instance/host details */
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    String subHeader = request.getHeader(Constants.HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestBody.put(Constants.SUB_TYPE, subscrtiptionType);
    /* checking authentication info in requests */
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestBody.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          if (requestBody.containsKey(Constants.SUB_TYPE)) {
            JsonObject authJson = authHandler.result();
            JsonObject jsonObj = requestBody.copy();
            jsonObj.put(Constants.JSON_CONSUMER, authJson.getString(Constants.JSON_CONSUMER));
            jsonObj.put(Constants.JSON_INSTANCEID, instanceID);
            LOGGER.info("json for subs :: " + jsonObj);
            Future<JsonObject> subsReq =
                subsService.createSubscription(jsonObj, databroker, database);
            subsReq.onComplete(subHandler -> {
              if (subHandler.succeeded()) {
                handleResponse(response, ResponseType.Created, subHandler.result().toString(),
                    false);
              } else {
                handleResponse(response, ResponseType.BadRequestData,
                    subHandler.cause().getMessage(), false);
              }
            });
          } else {
            handleResponse(response, ResponseType.BadRequestData, Constants.MSG_SUB_TYPE_NOT_FOUND,
                true);
          }
        } else if (authHandler.failed()) {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * handle append requests for subscription.
   * 
   * @param routingContext routingContext
   */
  private void appendSubscription(RoutingContext routingContext) {
    LOGGER.info("appendSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String alias = request.getParam(Constants.JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    requestJson.put(Constants.SUBSCRIPTION_ID, subsId);
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(Constants.HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(Constants.SUB_TYPE, subscrtiptionType);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
          if (requestJson != null && requestJson.containsKey(Constants.SUB_TYPE)) {
            if (requestJson.getString(Constants.JSON_NAME).equalsIgnoreCase(alias)) {
              JsonObject authResult = authHandler.result();
              JsonObject jsonObj = requestJson.copy();
              jsonObj.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
              Future<JsonObject> subsReq =
                  subsService.appendSubscription(jsonObj, databroker, database);
              subsReq.onComplete(subsRequestHandler -> {
                if (subsRequestHandler.succeeded()) {
                  handleResponse(response, ResponseType.Created,
                      subsRequestHandler.result().toString(), false);
                } else {
                  handleResponse(response, ResponseType.BadRequestData,
                      subsRequestHandler.result().toString(), false);
                }
              });
            } else {
              handleResponse(response, ResponseType.BadRequestData, Constants.MSG_INVALID_NAME,
                  true);
            }
          } else {
            handleResponse(response, ResponseType.BadRequestData, Constants.MSG_SUB_TYPE_NOT_FOUND,
                true);
          }
        } else {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, Constants.MSG_SUB_INVALID_TOKEN,
          true);
    }
  }

  /**
   * handle update subscription requests.
   * 
   * @param routingContext routingContext
   */
  private void updateSubscription(RoutingContext routingContext) {
    LOGGER.info("updateSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String alias = request.getParam(Constants.JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestJson = routingContext.getBodyAsJson();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    String subHeader = request.getHeader(Constants.HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(Constants.SUB_TYPE, subscrtiptionType);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
          if (requestJson != null && requestJson.containsKey(Constants.SUB_TYPE)) {
            if (requestJson.getString(Constants.JSON_NAME).equalsIgnoreCase(alias)) {
              JsonObject authResult = authHandler.result();
              JsonObject jsonObj = requestJson.copy();
              jsonObj.put(Constants.SUBSCRIPTION_ID, subsId);
              jsonObj.put(Constants.JSON_INSTANCEID, instanceID);
              jsonObj.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
              Future<JsonObject> subsReq =
                  subsService.updateSubscription(jsonObj, databroker, database);
              subsReq.onComplete(subsRequestHandler -> {
                if (subsRequestHandler.succeeded()) {
                  handleResponse(response, ResponseType.Created,
                      subsRequestHandler.result().toString(), false);
                } else {
                  handleResponse(response, ResponseType.BadRequestData,
                      subsRequestHandler.result().toString(), false);
                }
              });
            } else {
              handleResponse(response, ResponseType.BadRequestData, Constants.MSG_INVALID_NAME,
                  true);
            }
          } else {
            handleResponse(response, ResponseType.BadRequestData, Constants.MSG_SUB_TYPE_NOT_FOUND,
                true);
          }
        } else {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, Constants.MSG_SUB_INVALID_TOKEN,
          true);
    }
  }

  /**
   * get a subscription by id.
   * 
   * @param routingContext routingContext
   */
  private void getSubscription(RoutingContext routingContext) {
    LOGGER.info("getSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String alias = request.getParam(Constants.JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    requestJson.put(Constants.SUBSCRIPTION_ID, subsId);
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(Constants.HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(Constants.SUB_TYPE, subscrtiptionType);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
          if (requestJson != null && requestJson.containsKey(Constants.SUB_TYPE)) {
            JsonObject authResult = authHandler.result();
            JsonObject jsonObj = requestJson.copy();
            jsonObj.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
            Future<JsonObject> subsReq = subsService.getSubscription(jsonObj, databroker, database);
            subsReq.onComplete(subHandler -> {
              if (subHandler.succeeded()) {
                handleResponse(response, ResponseType.Ok, subHandler.result().toString(), false);
              } else {
                handleResponse(response, ResponseType.BadRequestData,
                    subHandler.result().toString(), false);
              }
            });
          } else {
            handleResponse(response, ResponseType.BadRequestData, Constants.MSG_SUB_TYPE_NOT_FOUND,
                true);
          }
        } else if (authHandler.failed()) {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * delete a subscription by id.
   * 
   * @param routingContext routingContext
   */
  private void deleteSubscription(RoutingContext routingContext) {
    LOGGER.info("deleteSubscription method started");
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String alias = request.getParam(Constants.JSON_ALIAS);
    String subsId = domain + "/" + usersha + "/" + alias;
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestJson = new JsonObject();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    requestJson.put(Constants.SUBSCRIPTION_ID, subsId);
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String subHeader = request.getHeader(Constants.HEADER_OPTIONS);
    String subscrtiptionType =
        subHeader != null && subHeader.contains(SubsType.STREAMING.getMessage())
            ? SubsType.STREAMING.getMessage()
            : SubsType.CALLBACK.getMessage();
    requestJson.put(Constants.SUB_TYPE, subscrtiptionType);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          if (requestJson.containsKey(Constants.SUB_TYPE)) {
            JsonObject authResult = authHandler.result();
            JsonObject jsonObj = requestJson.copy();
            jsonObj.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
            LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
            Future<JsonObject> subsReq =
                subsService.deleteSubscription(jsonObj, databroker, database);
            subsReq.onComplete(subHandler -> {
              if (subHandler.succeeded()) {
                handleResponse(response, ResponseType.Ok, subHandler.result().toString(), false);
              } else {
                handleResponse(response, ResponseType.BadRequestData,
                    subHandler.result().toString(), false);
              }
            });
          } else {
            handleResponse(response, ResponseType.BadRequestData, Constants.MSG_SUB_TYPE_NOT_FOUND,
                true);
          }
        } else if (authHandler.failed()) {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * Create a exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createExchange(RoutingContext routingContext) {
    LOGGER.info("createExchange method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    LOGGER.info("request ::: " + requestJson);
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
          LOGGER.info("databroker :: " + databroker);
          Future<Boolean> isValidNameResult =
              isValidName(requestJson.copy().getString(Constants.JSON_EXCHANGE_NAME));
          isValidNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult =
                  managementApi.createExchange(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  handleResponse(response, ResponseType.Created,
                      brokerResultHandler.result().toString(), false);
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error(brokerResultHandler.cause());
                  handleResponse(response, ResponseType.BadRequestData,
                      brokerResultHandler.cause().getMessage(), false);
                }
              });
            } else {
              LOGGER.error(authHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  Constants.MSG_INVALID_EXCHANGE_NAME, true);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }

  }

  /**
   * delete an exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void deleteExchange(RoutingContext routingContext) {
    LOGGER.info("deleteExchange method started");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    String exchangeId = request.getParam(Constants.EXCHANGE_ID);
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteExchange(exchangeId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }

  }

  /**
   * get exchange details from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void getExchangeDetails(RoutingContext routingContext) {
    LOGGER.info("getExchange method started");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    JsonObject authenticationInfo = new JsonObject();
    LOGGER.info("request :: " + request);
    LOGGER.info("request json :: " + requestJson);
    String exchangeId = request.getParam(Constants.EXCHANGE_ID);
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    HttpServerResponse response = routingContext.response();
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.getExchangeDetails(exchangeId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * create a queue in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createQueue(RoutingContext routingContext) {
    LOGGER.info("createQueue method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<Boolean> validNameResult =
              isValidName(requestJson.copy().getString(Constants.JSON_QUEUE_NAME));
          validNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult = managementApi.createQueue(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  handleResponse(response, ResponseType.Created,
                      brokerResultHandler.result().toString(), false);
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error(brokerResultHandler.cause());
                  handleResponse(response, ResponseType.BadRequestData,
                      brokerResultHandler.cause().getMessage(), false);
                }
              });
            } else {
              LOGGER.error(authHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  Constants.MSG_INVALID_EXCHANGE_NAME, true);
            }

          });
        } else if (authHandler.failed()) {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * delete a queue in rabbit MQ.
   * 
   * @param routingContext routingContext.
   */
  private void deleteQueue(RoutingContext routingContext) {
    LOGGER.info("deleteQueue method started");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteQueue(queueId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * get queue details from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void getQueueDetails(RoutingContext routingContext) {
    LOGGER.info("getQueueDetails method started");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String queueId = routingContext.request().getParam("queueId");
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, requestJson, authHandler -> {
        LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.getQueueDetails(queueId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * bind queue to exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void bindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.info("bindQueue2Exchange method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.bindQueue2Exchange(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Created,
                  brokerResultHandler.result().toString(), false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * unbind a queue from an exchange in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void unbindQueue2Exchange(RoutingContext routingContext) {
    LOGGER.info("unbindQueue2Exchange method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.unbindQueue2Exchange(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Created,
                  brokerResultHandler.result().toString(), false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * create a vhost in rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void createVHost(RoutingContext routingContext) {
    LOGGER.info("createVHost method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<Boolean> validNameResult =
              isValidName(requestJson.copy().getString(Constants.JSON_VHOST));
          validNameResult.onComplete(validNameHandler -> {
            if (validNameHandler.succeeded()) {
              Future<JsonObject> brokerResult = managementApi.createVHost(requestJson, databroker);
              brokerResult.onComplete(brokerResultHandler -> {
                if (brokerResultHandler.succeeded()) {
                  handleResponse(response, ResponseType.Created,
                      brokerResultHandler.result().toString(), false);
                } else if (brokerResultHandler.failed()) {
                  LOGGER.error(brokerResultHandler.cause());
                  handleResponse(response, ResponseType.BadRequestData,
                      brokerResultHandler.cause().getMessage(), false);
                }
              });
            } else {
              LOGGER.error(authHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  Constants.MSG_INVALID_EXCHANGE_NAME, true);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }

  }

  /**
   * delete vhost from rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void deleteVHost(RoutingContext routingContext) {
    LOGGER.info("deleteVHost method started");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String vhostId = routingContext.request().getParam(Constants.JSON_VHOST_ID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson, authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.deleteVHost(vhostId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else if (brokerResultHandler.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * register a adapter in Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  private void registerAdapter(RoutingContext routingContext) {
    LOGGER.info("registerAdapter method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        LOGGER.info("Authenticating response ".concat(authHandler.result().toString()));
        if (authHandler.succeeded()) {
          JsonObject authResult = authHandler.result();
          requestJson.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
          Future<JsonObject> brokerResult = managementApi.registerAdapter(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Created,
                  brokerResultHandler.result().toString(), false);
            } else if (brokerResult.failed()) {
              LOGGER.error(brokerResultHandler.cause());
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else if (authHandler.failed()) {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }

  }

  /**
   * delete a adapter in Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void deleteAdapter(RoutingContext routingContext) {
    LOGGER.info("deleteAdapter method starts");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String resourceGroup = request.getParam(Constants.JSON_RESOURCE_GROUP);
    String resourceServer = request.getParam(Constants.JSON_RESOURCE_SERVER);
    String adapterId = domain + "/" + usersha + "/" +  resourceServer + "/" + resourceGroup;
    requestJson.put(Constants.JSON_ID, adapterId);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          JsonObject authResult = authHandler.result();
          requestJson.put(Constants.JSON_CONSUMER, authResult.getString(Constants.JSON_CONSUMER));
          Future<JsonObject> brokerResult = managementApi.deleteAdapter(adapterId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * get Adapter details from Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void getAdapterDetails(RoutingContext routingContext) {
    LOGGER.info("getAdapterDetails method starts");
    JsonObject requestJson = new JsonObject();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    String domain = request.getParam(Constants.JSON_DOMAIN);
    String usersha = request.getParam(Constants.JSON_USERSHA);
    String resourceGroup = request.getParam(Constants.JSON_RESOURCE_GROUP);
    String resourceServer = request.getParam(Constants.JSON_RESOURCE_SERVER);
    String adapterId = domain + "/" + usersha + "/" +  resourceServer + "/" + resourceGroup;
    requestJson.put(Constants.JSON_ID, adapterId);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          JsonObject authResult = authHandler.result();
          requestJson.put(Constants.JSON_ID, authResult.getString(Constants.JSON_CONSUMER));
          Future<JsonObject> brokerResult = managementApi.getAdapterDetails(adapterId, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * publish heartbeat details to Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void publishHeartbeat(RoutingContext routingContext) {
    LOGGER.info("publishHeartbeat method starts");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.publishHeartbeat(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * publish downstream issues to Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void publishDownstreamIssue(RoutingContext routingContext) {
    LOGGER.info("publishDownStreamIssue method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.publishDownstreamIssues(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * publish data issue to Rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void publishDataIssue(RoutingContext routingContext) {
    LOGGER.info("publishDataIssue method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult = managementApi.publishDataIssue(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * publish data from adapter to rabbit MQ.
   * 
   * @param routingContext routingContext
   */
  public void publishDataFromAdapter(RoutingContext routingContext) {
    LOGGER.info("publishDataFromAdapter method started");
    JsonObject requestJson = routingContext.getBodyAsJson();
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    String instanceID = request.getHeader(Constants.HEADER_HOST);
    JsonObject authenticationInfo = new JsonObject();
    requestJson.put(Constants.JSON_INSTANCEID, instanceID);
    if (request.headers().contains(Constants.HEADER_TOKEN)) {
      authenticationInfo.put(Constants.HEADER_TOKEN, request.getHeader(Constants.HEADER_TOKEN));
      authenticator.tokenInterospect(requestJson.copy(), authenticationInfo, authHandler -> {
        if (authHandler.succeeded()) {
          Future<JsonObject> brokerResult =
              managementApi.publishDataFromAdapter(requestJson, databroker);
          brokerResult.onComplete(brokerResultHandler -> {
            if (brokerResultHandler.succeeded()) {
              handleResponse(response, ResponseType.Ok, brokerResultHandler.result().toString(),
                  false);
            } else {
              handleResponse(response, ResponseType.BadRequestData,
                  brokerResultHandler.cause().getMessage(), false);
            }
          });
        } else {
          LOGGER.error(authHandler.cause());
          handleResponse(response, ResponseType.AuthenticationFailure, true);
        }
      });
    } else {
      handleResponse(response, ResponseType.AuthenticationFailure, true);
    }
  }

  /**
   * handle HTTP response.
   * 
   * @param response response object
   * @param responseType Http status for response
   * @param isBodyRequired body is required or not for response
   */
  private void handleResponse(HttpServerResponse response, ResponseType responseType,
      boolean isBodyRequired) {
    handleResponse(response, responseType, responseType.getMessage(), isBodyRequired);
  }

  /**
   * handle HTTP response.
   * 
   * @param response response object
   * @param responseType Http status for response
   * @param reply json response body
   * @param isBodyRequired body is required or not for response
   */
  private void handleResponse(HttpServerResponse response, ResponseType responseType, String reply,
      boolean isBodyRequired) {
    if (isBodyRequired) {
      response.putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
          .setStatusCode(responseType.getCode()).end(new RestResponse.Builder()
              .withError(responseType).withMessage(reply).build().toJsonString());
    } else {
      response.putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
          .setStatusCode(responseType.getCode()).end(reply);

    }
  }

  /**
   * validate if name passes the regex test for IUDX queue,exchage name.
   * 
   * @param name name(queue,exchange)
   * @return Future true if name matches the regex else false
   */
  public Future<Boolean> isValidName(String name) {
    Promise<Boolean> promise = Promise.promise();
    if (Pattern.compile(Constants.APP_NAME_REGEX).matcher(name).matches()) {
      promise.complete(true);
    } else {
      LOGGER.error(Constants.MSG_INVALID_NAME + name);
      promise.fail(Constants.MSG_INVALID_NAME);
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
      Map<String, List<String>> decodedParams =
          new QueryStringDecoder(routingContext.request().uri(), HttpConstants.DEFAULT_CHARSET,
              true, 1024, true).parameters();
      for (Map.Entry<String, List<String>> entry : decodedParams.entrySet()) {
        queryParams.add(entry.getKey(), entry.getValue());
        LOGGER.info(entry.getKey() + " : " + entry.getValue());
      }
    } catch (IllegalArgumentException ex) {
      response.putHeader(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON)
          .setStatusCode(ResponseType.BadRequestData.getCode())
          .end(new RestResponse.Builder().withError(ResponseType.BadRequestData)
              .withMessage(Constants.MSG_PARAM_DECODE_ERROR).build().toJsonString());
    }
    return Optional.of(queryParams);
  }

}

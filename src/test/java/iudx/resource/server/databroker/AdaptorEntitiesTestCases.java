package iudx.resource.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.databroker.util.Constants;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdaptorEntitiesTestCases {

  static DataBrokerService databroker;
  static private Properties properties;
  static private InputStream inputstream;
  static private String dataBrokerIP;
  static private int dataBrokerPort;
  static private int dataBrokerManagementPort;
  static private String dataBrokerVhost;
  static private String dataBrokerUserName;
  static private String dataBrokerPassword;
  static private int connectionTimeout;
  static private int requestedHeartbeat;
  static private int handshakeTimeout;
  static private int requestedChannelMax;
  static private int networkRecoveryInterval;
  static private WebClient webClient;
  static private WebClientOptions webConfig;
  private static RabbitMQOptions config;
  private static RabbitMQClient client;
  private static String exchangeName;
  static JsonObject propObj;
  private static String resourceGroup;
  private static String resourceServer;
  private static String id, anotherid;
  /* Database Properties */
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int poolSize;
  private static PgConnectOptions connectOptions;
  private static PoolOptions poolOptions;
  private static PgPool pgclient;
  private static RabbitClient rabbitMQStreamingClient;
  private static RabbitWebClient rabbitMQWebClient;
  private static PostgresClient pgClient;

  private static final Logger logger = LoggerFactory.getLogger(AdaptorEntitiesTestCases.class);

  @BeforeAll
  @DisplayName("Initialize the Databroker class with web client and rabbitmq client")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      dataBrokerIP = properties.getProperty("dataBrokerIP");
      dataBrokerPort = Integer.parseInt(properties.getProperty("dataBrokerPort"));
      dataBrokerManagementPort =
          Integer.parseInt(properties.getProperty("dataBrokerManagementPort"));
      dataBrokerVhost = properties.getProperty("dataBrokerVhost");
      dataBrokerUserName = properties.getProperty("dataBrokerUserName");
      dataBrokerPassword = properties.getProperty("dataBrokerPassword");
      connectionTimeout = Integer.parseInt(properties.getProperty("connectionTimeout"));
      requestedHeartbeat = Integer.parseInt(properties.getProperty("requestedHeartbeat"));
      handshakeTimeout = Integer.parseInt(properties.getProperty("handshakeTimeout"));
      requestedChannelMax = Integer.parseInt(properties.getProperty("requestedChannelMax"));
      networkRecoveryInterval = Integer.parseInt(properties.getProperty("networkRecoveryInterval"));
      databaseIP = properties.getProperty("callbackDatabaseIP");
      databasePort = Integer.parseInt(properties.getProperty("callbackDatabasePort"));
      databaseName = properties.getProperty("callbackDatabaseName");
      databaseUserName = properties.getProperty("callbackDatabaseUserName");
      databasePassword = properties.getProperty("callbackDatabasePassword");
      poolSize = Integer.parseInt(properties.getProperty("callbackpoolSize"));

    } catch (Exception ex) {
      logger.info(ex.toString());
    }

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setVirtualHost(dataBrokerVhost);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);


    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);


    /* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. */
    client = RabbitMQClient.create(vertx, config);
    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */
    
    webClient = WebClient.create(vertx, webConfig);

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
          .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    /* Create a Json Object for properties */
    propObj = new JsonObject();
    propObj.put("userName", dataBrokerUserName);
    propObj.put(Constants.PASSWORD, dataBrokerPassword);
    propObj.put(Constants.VHOST, dataBrokerVhost);
    propObj.put("databaseIP", databaseIP);
    propObj.put("databasePort", databasePort);
    propObj.put("databaseName", databaseName);
    propObj.put("databaseUserName", databaseUserName);
    propObj.put("databasePassword", databasePassword);
    propObj.put("databasePoolSize", poolSize);
    
    /* Call the databroker constructor with the RabbitMQ client Vertx web client. */

    rabbitMQWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    rabbitMQStreamingClient = new RabbitClient(vertx, config, rabbitMQWebClient);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);

    databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, dataBrokerVhost);
    
    resourceGroup = UUID.randomUUID().toString();
    resourceServer = UUID.randomUUID().toString();
    id = Constants.USER_NAME_TEST_EXAMPLE + "/" + resourceServer + "/" + resourceGroup;

    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor method with a new adaptor (adaptor-1) registration (with new user)")
  @Order(1)
  void successRegisterAdaptor(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.CONSUMER, Constants.CONSUMER_TEST_EXAMPLE);
    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);

    JsonObject expected = new JsonObject();
    expected.put(Constants.USER_NAME, Constants.USER_NAME_TEST_EXAMPLE);
    expected.put(Constants.APIKEY, Constants.APIKEY_TEST_EXAMPLE);
    expected.put(Constants.ID, id);
    expected.put(Constants.VHOST, Constants.VHOST_IUDX);

    databroker.registerAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside  successRegisterAdaptor - RegisteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing getExchange method for exchange created after new adaptor (adaptor-1) registration")
  @Order(2)
  void successGetExchange(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.ID, id);
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_FOUND);

    databroker.getExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside  successGetExchange - getExchange response : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing listAdaptor method for bindings done after new adaptor (adaptor-1) registration")
  @Order(3)
  void successListAdaptor(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.ID, id);
    request.put("exchangeName", id);// TODO : discuss conflict between impl and test code

    JsonObject expected = new JsonObject();
    JsonArray adaptorLogs_entities = new JsonArray();
    JsonArray database_entities = new JsonArray();
    adaptorLogs_entities.add(id + Constants.DATA_ISSUE);
    adaptorLogs_entities.add(id + Constants.DOWNSTREAM_ISSUE);
    adaptorLogs_entities.add(id + Constants.HEARTBEAT);
    database_entities.add(id + Constants.ALLOW_ROUTING_KEY);
    expected.put(Constants.QUEUE_ADAPTOR_LOGS, adaptorLogs_entities);
    expected.put(Constants.QUEUE_DATA, database_entities);

    databroker.listAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - listAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing publishHeartbeat method for publication of heartbeat data")
  @Order(4)
  void successPublishHeartbeat(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.ID, id);
    request.put(Constants.STATUS, "heartbeat");

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, Constants.SUCCESS);
    expected.put(Constants.QUEUE_NAME, Constants.QUEUE_ADAPTOR_LOGS);
    expected.put(Constants.ROUTING_KEY, id + ".heartbeat");
    expected.put(Constants.DETAIL, "routingKey matched");


    databroker.publishHeartbeat(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - publishHeartbeat response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing registerAdaptor method for another adaptor (adaptor-2) registration (with an existing user)")
  @Order(5)
  void successRegisterAdaptorwithExistingUser(VertxTestContext testContext)
      throws InterruptedException {
    Thread.sleep(1000);
    resourceGroup = UUID.randomUUID().toString();
    resourceServer = UUID.randomUUID().toString();
    anotherid = Constants.USER_NAME_TEST_EXAMPLE + "/" + resourceServer + "/" + resourceGroup;

    JsonObject request = new JsonObject();
    request.put(Constants.CONSUMER, Constants.CONSUMER_TEST_EXAMPLE);
    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);

    JsonObject expected = new JsonObject();
    expected.put(Constants.USER_NAME, Constants.USER_NAME_TEST_EXAMPLE);
    expected.put(Constants.APIKEY, Constants.APIKEY_TEST_EXAMPLE);
    expected.put(Constants.ID, anotherid);
    expected.put(Constants.VHOST, Constants.VHOST_IUDX);

    databroker.registerAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info(
            "inside  successRegisterAdaptor with existing user - RegisteAdaptor response is : "
                + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing getExchange method for exchanges created after another adaptor (adaptor-2) registration")
  @Order(6)
  void successGetExchangeExistingUser(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.ID, anotherid);
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_FOUND);

    databroker.getExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside  successGetExchange - getExchange response : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing listAdaptor method for bindings done after another adaptor (adaptor-2) registration")
  @Order(7)
  void successListAdaptorExistingUser(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.ID, anotherid);
    request.put("exchangeName", anotherid);// TODO : discuss conflict between impl and test code

    JsonObject expected = new JsonObject();
    JsonArray adaptorLogs_entities = new JsonArray();
    JsonArray database_entities = new JsonArray();
    adaptorLogs_entities.add(anotherid + Constants.DATA_ISSUE);
    adaptorLogs_entities.add(anotherid + Constants.DOWNSTREAM_ISSUE);
    adaptorLogs_entities.add(anotherid + Constants.HEARTBEAT);
    database_entities.add(anotherid + Constants.ALLOW_ROUTING_KEY);
    expected.put(Constants.QUEUE_ADAPTOR_LOGS, adaptorLogs_entities);
    expected.put(Constants.QUEUE_DATA, database_entities);

    databroker.listAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - listAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing registerAdaptor method for registering an adaptor (adaptor-2) which was already registered")
  @Order(8)
  void failureRegisterAdaptorwithExistingUser(VertxTestContext testContext)
      throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    request.put(Constants.CONSUMER, Constants.CONSUMER_TEST_EXAMPLE);

    JsonObject expected = new JsonObject();
    expected.put(Constants.DETAILS, Constants.EXCHANGE_EXISTS);

    databroker.registerAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside  failureRegisterAdaptorwithExistingUser - RegisteAdaptor response is : "
            + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing deleteAdaptor method for deleting new (adaptor-1) adaptor")
  @Order(9)
  void successDeleteAdaptor(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(5000);
    JsonObject expected = new JsonObject();
    expected.put(Constants.ID, id);
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, "adaptor deleted");

    JsonObject request = new JsonObject();
    request.put(Constants.ID, id);

    databroker.deleteAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - DeleteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing deleteAdaptor method for deleting another (adaptor-2) adaptor")
  @Order(10)
  void successDeleteAdaptorExistingUser(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(5000);
    JsonObject expected = new JsonObject();
    expected.put(Constants.ID, anotherid);
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, "adaptor deleted");

    JsonObject request = new JsonObject();
    request.put(Constants.ID, anotherid);

    databroker.deleteAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - DeleteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing registerAdaptor with invalid ID")
  @Order(11)
  void failureRegisterInvalidAdaptorID(VertxTestContext testContext) throws InterruptedException {
    Thread.sleep(1000);
    JsonObject request = new JsonObject();
    request.put(Constants.JSON_RESOURCE_GROUP, resourceGroup + "+()*&");
    request.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    request.put(Constants.CONSUMER, Constants.CONSUMER_TEST_EXAMPLE);

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "invalid id");

    databroker.registerAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside invalid ID test - RegisteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }


}



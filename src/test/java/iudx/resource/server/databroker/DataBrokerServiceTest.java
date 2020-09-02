package iudx.resource.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
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
public class DataBrokerServiceTest {

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
  private static String queueName;
  private static JsonArray entities;
  private static JsonObject expected;
  static JsonObject propObj;
  private static String vHost;
  private static int statusOk;
  private static int statusNotFound;
  private static int statusNoContent;
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


  private static RabbitMQStreamingClient rabbitMQStreamingClient;
  private static RabbitMQWebClient rabbitMQWebClient;
  private static PostgresQLClient pgClient;

  private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceTest.class);

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    /* Choose a Character random from this String */
    String AlphaNumericString =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";

    /* Create StringBuffer size of AlphaNumericString */
    StringBuilder sb = new StringBuilder(20);

    for (int i = 0; i < 20; i++) {

      /*
       * Generate a random number between 0 to AlphaNumericString variable length
       */
      int index = (int) (AlphaNumericString.length() * Math.random());

      /* Add Character one by one in end of sb */
      sb.append(AlphaNumericString.charAt(index));
    }

    exchangeName = sb.toString();
    queueName = sb.toString();
    entities = new JsonArray("[\"id1\", \"id2\"]");
    vHost = "IUDX-Test";
    statusOk = 200;
    statusNotFound = 404;
    statusNoContent = 204;


    logger.info("Exchange Name is " + exchangeName);
    logger.info("Queue Name is " + queueName);

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

    // client = RabbitMQClient.create(vertx, config);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */

    // webClient = WebClient.create(vertx, webConfig);

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

    /* Call the databroker constructor with the RabbitMQ client. */

    rabbitMQWebClient = new RabbitMQWebClient(vertx, webConfig, propObj);
    rabbitMQStreamingClient = new RabbitMQStreamingClient(vertx, config, rabbitMQWebClient);
    pgClient = new PostgresQLClient(vertx, connectOptions, poolOptions);
    databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, dataBrokerVhost);

    testContext.completeNow();
  }


  @Test
  @DisplayName("Testing Create Exchange")
  @Order(1)
  void successCreateExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.EXCHANGE, exchangeName);

    JsonObject request = new JsonObject();
    request.put(Constants.EXCHANGE_NAME, exchangeName);

    databroker.createExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Creating already existing exchange")
  @Order(2)
  void failCreateExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, statusNoContent);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_EXISTS);

    JsonObject request = new JsonObject();
    request.put(Constants.EXCHANGE_NAME, exchangeName);

    databroker.createExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing Create Queue")
  @Order(3)
  void successCreateQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.QUEUE, queueName);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);

    databroker.createQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Create Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Creating already existing queue")
  @Order(4)
  void failCreateQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, statusNoContent);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.QUEUE_ALREADY_EXISTS);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);

    databroker.createQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Create Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Binding Exchange and Queue")
  @Order(5)
  void successBindQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.QUEUE, queueName);
    expected.put(Constants.EXCHANGE, exchangeName);
    expected.put(Constants.ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);
    request.put(Constants.EXCHANGE_NAME, exchangeName);
    request.put(Constants.ENTITIES, entities);

    databroker.bindQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Bind Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Listing all bindings of exchange")
  @Order(6)
  void successListExchangeBindings(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(queueName, entities);

    JsonObject request = new JsonObject();
    request.put(Constants.EXCHANGE_NAME, exchangeName);

    databroker.listExchangeSubscribers(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List exchnage bindings response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }



  @Test
  @DisplayName("Listing all bindings of queue")
  @Order(7)
  void successListQueueBindings(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);

    databroker.listQueueSubscribers(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List queue bindings response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Unbinding Exchange and Queue")
  @Order(8)
  void successUnbindQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.QUEUE, queueName);
    expected.put(Constants.EXCHANGE, exchangeName);
    expected.put(Constants.ENTITIES, entities);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);
    request.put(Constants.EXCHANGE_NAME, exchangeName);
    request.put(Constants.ENTITIES, entities);

    databroker.unbindQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Unbind Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting a Queue")
  @Order(9)
  void successDeleteQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.QUEUE, queueName);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);

    databroker.deleteQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an already deleted Queue")
  @Order(10)
  void failDeleteQueue(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, statusNotFound);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, queueName);

    databroker.deleteQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete Queue response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an Exchange")
  @Order(11)
  void successDeleteExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.EXCHANGE, exchangeName);

    JsonObject request = new JsonObject();
    request.put(Constants.EXCHANGE_NAME, exchangeName);

    databroker.deleteExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting an already deleted Exchange")
  @Order(12)
  void failDeleteExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, statusNotFound);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.EXCHANGE_NOT_FOUND);

    JsonObject request = new JsonObject();
    request.put(Constants.EXCHANGE_NAME, exchangeName);

    databroker.deleteExchange(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete Exchange response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Creating a Virtual Host")
  @Order(13)
  void successCreateVhost(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.VHOST, vHost);

    JsonObject request = new JsonObject();
    request.put(Constants.VHOST, vHost);

    databroker.createvHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Create vHost response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Deleting a Virtual Host")
  @Order(14)
  void successDeleteVhost(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.VHOST, vHost);

    JsonObject request = new JsonObject();
    request.put(Constants.VHOST, vHost);

    databroker.deletevHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete vHost response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("List all Virtual Hosts")
  @Order(15)
  void successListVhosts(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put(Constants.VHOST, new JsonArray("[\"/\",\"IUDX\",\"test\"]"));

    JsonObject request = new JsonObject();

    databroker.listvHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List vHost response is : " + response);
        assertTrue(response.getJsonArray(Constants.VHOST) instanceof JsonArray);
        // assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Publish message to exchange")
  @Order(16)
  void successPublishMessage(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put("status", statusOk);

    JsonObject request = new JsonObject();
    request.put("PM2_5", 4.14);
    request.put("AQI", 64);
    request.put("CO2", 529.23);
    request.put("CO", 0.75);
    request.put("SoundMax", 100.82);
    request.put("PM10", 4.83);
    request.put("Rainfall", 0);
    request.put("id", "ankita_exchange/EM_01_0103_01");
    request.put("SoundMin", 73.54);
    request.put("Avg_Humidity", 100);
    request.put("SO2", 17.95);
    request.put("AmbientLight", 32040.91);
    request.put("Pressure", 994.02);
    request.put("ULTRA_VIOLET", 1.67);
    request.put("Avg_Temp", 24.9);
    request.put("Date", "2020-06-05T09:00:01+05:30");
    request.put("O3", 34.59);
    request.put("O2", 19.66);
    request.put("NO2", 50.62);


    databroker.publishFromAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Message from adaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });

  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with empty request")
  @Order(17)
  void failedregisterStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Error in payload");
    
    JsonObject request = new JsonObject();
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with non-existing (but valid) routingKey")
  @Order(18)
  void failedregisterStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Binding Failed");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with routingKey = null")
  @Order(19)
  void failedregisterStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with routingKey = invalid key")
  @Order(20)
  void failedregisterStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("Testing success case : Register streaming subscription with valid queue and exchange names")
  @Order(21)
  void successregisterStreamingSubscription(VertxTestContext testContext) {

    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonObject expected = new JsonObject();
    expected.put(Constants.SUBSCRIPTION_ID, queueName);

    expected.put("streamingURL",
        "amqp://pawan@google.org:1234@68.183.80.248:5672/IUDX/google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");
 
    request.put(Constants.ENTITIES, array);

    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Register streaming subscription with already existing alias-name")
  @Order(22)
  void failedregisterStreamingSubscriptionAlreadyExistingQueue(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Queue Creation Failed");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response for already existing alias-name request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : get streaming subscription")
  @Order(23)
  void successlistStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    expected.put(Constants.ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : list streaming subscription (ID not available)")
  @Order(24)
  void failedlistStreamingSubscriptionIdNotFound(VertxTestContext testContext) {
    String id = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias";
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 404);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, id);

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : Update streaming subscription with valid queue and exchange names")
  @Order(25)
  void successupdateStreamingSubscription(VertxTestContext testContext) {

    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonObject expected = new JsonObject();
    expected.put(Constants.SUBSCRIPTION_ID, queueName);

    expected.put("streamingURL",
        "amqp://pawan@google.org:1234@68.183.80.248:5672/IUDX/google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/.*");
 
    request.put(Constants.ENTITIES, array);

    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
  

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with empty request")
  @Order(26)
  void failedupdateStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Error in payload");
    
    JsonObject request = new JsonObject();
    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with non-existing (but valid) routingKey")
  @Order(27)
  void failedupdateStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Binding Failed");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    request.put(Constants.ENTITIES, array);
    
    databroker.updateStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with routingKey = null")
  @Order(28)
  void failedupdateStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update streaming subscription with routingKey = invalid key")
  @Order(29)
  void failedupdateStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.NAME, "alias-pawan");
    request.put(Constants.CONSUMER, "pawan@google.org");
    request.put(Constants.TYPE, "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");
    request.put(Constants.ENTITIES, array);
    
    databroker.registerStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
  
  @Test
  @DisplayName("Testing success case : get streaming subscription of updated subscription")
  @Order(30)
  void successlistupdatedStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/.*");
    expected.put(Constants.ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Get subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : Update (Append) streaming subscription with valid queue and exchange names")
  @Order(31)
  void successappendStreamingSubscription(VertxTestContext testContext) {

    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    JsonObject expected = new JsonObject();
    expected.put(Constants.SUBSCRIPTION_ID, queueName);
    expected.put(Constants.ENTITIES, array);
    
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, queueName);
    request.put(Constants.ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (Append) subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update(append) streaming subscription with empty request")
  @Order(32)
  void failedappendStreamingSubscriptionEmptyRequest(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Error in payload");
    
    JsonObject request = new JsonObject();
    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (Append) subscription response for empty request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update (append) streaming subscription with non-existing (but valid) routingKey")
  @Order(33)
  void failedappendStreamingSubscriptionInvalidExchange(VertxTestContext testContext) {

    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonArray array = new JsonArray();
    array.add(
        "iudx.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Binding Failed");
    
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, queueName);
    request.put(Constants.ENTITIES, array);
    
    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (Append) subscription response for invalid exchange request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : Update (Append) streaming subscription with routingKey = null")
  @Order(34)
  void failedappendStreamingSubscriptionNullRoutingKey(VertxTestContext testContext) {
    
    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonArray array = new JsonArray();
    array.add(
        "");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, queueName);
    request.put(Constants.ENTITIES, array);
    
    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (Append) subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }


  @Test
  @DisplayName("Testing failure case : Update (append)streaming subscription with routingKey = invalid key")
  @Order(35)
  void failedappendStreamingSubscriptionInvalidRoutingKey(VertxTestContext testContext) {

    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, queueName);
    request.put(Constants.ENTITIES, array);
        
    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (append)subscription response for invalid routingKey request is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : get streaming subscription of updated (append) subscription")
  @Order(36)
  void successlistappenddStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    JsonArray routingKeys = new JsonArray();
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/.*");
    routingKeys.add("rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");
    expected.put(Constants.ENTITIES, routingKeys);

    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Get subscription (after append) response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
 
  @Test
  @DisplayName("Testing success case : Update (Append) streaming subscription with invalid valid queue (subscriptionID)")
  @Order(37)
  void failureappendStreamingSubscriptionQueueNotPresent(VertxTestContext testContext) {

    String queueName = "non-existing-queue";
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/.*");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Invalid routingKey");
    
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, queueName);
    request.put(Constants.ENTITIES, array);

    databroker.appendStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Update (Append) with invalid subscriptionID response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing success case : delete streaming subscription")
  @Order(38)
  void successdeleteStreamingSubscription(VertxTestContext testContext) {
    String queueName = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    JsonObject expected = new JsonObject();
    expected.put(Constants.SUBSCRIPTION_ID, queueName);
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing failure case : delete streaming subscription (ID not available)")
  @Order(39)
  void faileddeleteStreamingSubscriptionIdNotFound(VertxTestContext testContext) {
    String id = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias";
    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 404);
    expected.put(Constants.TITLE, Constants.FAILURE);
    expected.put(Constants.DETAIL, Constants.QUEUE_DOES_NOT_EXISTS);
    JsonObject request = new JsonObject();
    request.put(Constants.SUBSCRIPTION_ID, id);

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Delete subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

}



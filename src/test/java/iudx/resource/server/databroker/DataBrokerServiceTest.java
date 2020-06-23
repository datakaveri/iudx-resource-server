package iudx.resource.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataBrokerServiceTest {

  static DataBrokerService databroker;
  private static Properties properties;
  private static InputStream inputstream;
  private static String dataBrokerIP;
  private static int dataBrokerPort;
  private static int dataBrokerManagementPort;
  private static String dataBrokerVhost;
  private static String dataBrokerUserName;
  private static String dataBrokerPassword;
  private static int connectionTimeout;
  private static int requestedHeartbeat;
  private static int handshakeTimeout;
  private static int requestedChannelMax;
  private static int networkRecoveryInterval;
  private static WebClient webClient;
  private static WebClientOptions webConfig;
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
    vHost = "IUDX";
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

    /*
     * Create a RabbitMQ Clinet with the configuration and vertx cluster instance.
     */

    client = RabbitMQClient.create(vertx, config);

    /*
     * Create a Vertx Web Client with the configuration and vertx cluster instance.
     */

    webClient = WebClient.create(vertx, webConfig);

    /* Create a Json Object for properties */

    propObj = new JsonObject();

    propObj.put("userName", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);
    propObj.put("vHost", dataBrokerVhost);
    propObj.put("IP", dataBrokerIP);
    propObj.put("port", dataBrokerPort);

    /* Call the databroker constructor with the RabbitMQ client. */

    databroker = new DataBrokerServiceImpl(client, webClient, propObj);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Testing Create Exchange")
  @Order(1)
  void successCreateExchange(VertxTestContext testContext) {

    JsonObject expected = new JsonObject();
    expected.put("exchange", exchangeName);

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);

    databroker.createExchange(
        request,
        handler -> {
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
    expected.put("type", statusNoContent);
    expected.put("title", "Failure");
    expected.put("detail", "Exchange already exists");

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);

    databroker.createExchange(
        request,
        handler -> {
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
    expected.put("queue", queueName);

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);

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
    expected.put("type", statusNoContent);
    expected.put("title", "Failure");
    expected.put("detail", "Queue already exists");

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);

    databroker.createQueue(
        request,
        handler -> {
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
    expected.put("queue", queueName);
    expected.put("exchange", exchangeName);
    expected.put("entities", entities);

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    request.put("exchangeName", exchangeName);
    request.put("entities", entities);

    databroker.bindQueue(
        request,
        handler -> {
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
    request.put("exchangeName", exchangeName);

    databroker.listExchangeSubscribers(
        request,
        handler -> {
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
    expected.put("entities", entities);

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);

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
    expected.put("queue", queueName);
    expected.put("exchange", exchangeName);
    expected.put("entities", entities);

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    request.put("exchangeName", exchangeName);
    request.put("entities", entities);

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
    expected.put("queue", queueName);

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);

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
    expected.put("type", statusNotFound);
    expected.put("title", "Failure");
    expected.put("detail", "Queue does not exist");

    JsonObject request = new JsonObject();
    request.put("queueName", queueName);

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
    expected.put("exchange", exchangeName);

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);

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
    expected.put("type", statusNotFound);
    expected.put("title", "Failure");
    expected.put("detail", "Exchange does not exist");

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);

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
    expected.put("vHost", vHost);

    JsonObject request = new JsonObject();
    request.put("vHost", vHost);

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
    expected.put("vHost", vHost);

    JsonObject request = new JsonObject();
    request.put("vHost", vHost);

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
    expected.put("vHost", new JsonArray("[\"/\",\"test\"]"));

    JsonObject request = new JsonObject();

    databroker.listvHost(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("List vHost response is : " + response);
        assertEquals(expected, response);
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
  @DisplayName("Testing register streaming subscription")
  @Order(17)
  void successregisterStreamingSubscription(VertxTestContext testContext) {

    Base64.Encoder encoder = Base64.getEncoder();
    String queueNameencode = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    String queueName = encoder.encodeToString(queueNameencode.getBytes());
    JsonObject expected = new JsonObject();
    expected.put("subscriptionID", queueName);

    expected.put("streamingURL",
        "amqp://pawan@google.org:1234@68.183.80.248:5672/iudx/google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    JsonObject request = new JsonObject();
    request.put("name", "alias-pawan");
    request.put("consumer", "pawan@google.org");
    request.put("type", "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/EM_01_0103_01");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    request.put("entities", array);

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
  @DisplayName("Testing get streaming subscription")
  @Order(18)
  void successlistStreamingSubscription(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    entities = new JsonArray("[\r\n"
        + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02\\\",\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03\\\"]\",\r\n"
        + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/EM_01_0103_01\\\"]\",\r\n"
        + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04\\\"]\"\r\n"
        + " ]");

    expected.put("entities", entities);
    JsonObject request = new JsonObject();
    request.put("subscriptionID", "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
    databroker.listStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing delete streaming subscription")
  @Order(19)
  void successdeleteStreamingSubscription(VertxTestContext testContext) {
    Base64.Encoder encoder = Base64.getEncoder();
    String queueNameencode = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    String queueName = encoder.encodeToString(queueNameencode.getBytes());
    JsonObject expected = new JsonObject();
    expected.put("subscriptionID", queueName);
    JsonObject request = new JsonObject();
    request.put("subscriptionID", "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    databroker.deleteStreamingSubscription(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("Register subscription response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
}

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
public class DataBrokerServiceRagisterStreamingTest {

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

  private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceRagisterStreamingTest.class);

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
   
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
    propObj.put("ip", dataBrokerIP);
    propObj.put("port", dataBrokerPort);

    /* Call the databroker constructor with the RabbitMQ client. */

    databroker = new DataBrokerServiceImpl(client, webClient, propObj);

    vertx.deployVerticle(
        new DataBrokerVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }  

  @Test
  @DisplayName("Testing register streaming subscription")
  @Order(1)
  void successregisterStreamingSubscription(VertxTestContext testContext) {

    Base64.Encoder encoder = Base64.getEncoder();
    String queueNameencode = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
    String queueName = encoder.encodeToString(queueNameencode.getBytes());
    JsonObject expected = new JsonObject();
    expected.put("subscriptionID", queueName);

    expected.put(
        "streamingURL",
        "amqp://pawan@google.org:1234@68.183.80.248:5672/iudx/google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");

    JsonObject requestpost = new JsonObject();
    requestpost.put("name", "alias-pawan");
    requestpost.put("consumer", "pawan@google.org");
    requestpost.put("type", "streaming");
    JsonArray array = new JsonArray();
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/EM_01_0103_01");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03");
    array.add(
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04");
    requestpost.put("entities", array);

    databroker.registerStreamingSubscription(
        requestpost,
        handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result();
            logger.info("Register subscription response is : " + response);
            assertEquals(expected, response);
          }
          testContext.completeNow();
        });
  }

//  @Test
//  @DisplayName("Testing get streaming subscription")
//  @Order(2)
//  void successlistStreamingSubscription(VertxTestContext testContext) {
//    JsonObject expected1 = new JsonObject();
//    JsonArray entities1 =
//        new JsonArray(
//            "[\r\n"
//                + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_02\\\",\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_03\\\"]\",\r\n"
//                + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm1/EM_01_0103_01\\\"]\",\r\n"
//                + "     \"[\\\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.varanasi.iudx.org.in/varanasi-aqm2/EM_01_0103_04\\\"]\"\r\n"
//                + " ]");
//
//    expected1.put("entities", entities1);
//    JsonObject requestget = new JsonObject();
//    requestget.put("subscriptionID", "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
//    databroker.listStreamingSubscription(
//        requestget,
//        handler -> {
//          if (handler.succeeded()) {
//            JsonObject response = handler.result();
//            logger.info("Register subscription response is : " + response);
//            assertEquals(expected1, response);
//          }
//          testContext.completeNow();
//        });
//  }

//  @Test
//  @DisplayName("Testing delete streaming subscription")
//  @Order(19)
//  void successdeleteStreamingSubscription(VertxTestContext testContext) {
//    Base64.Encoder encoder = Base64.getEncoder();
//    String queueNameencode = "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan";
//    String queueName = encoder.encodeToString(queueNameencode.getBytes());
//    JsonObject expected = new JsonObject();
//    expected.put("subscriptionID", queueName);
//    JsonObject request = new JsonObject();
//    request.put("subscriptionID", "google.org/63ac4f5d7fd26840f955408b0e4d30f2/alias-pawan");
//    databroker.deleteStreamingSubscription(
//        request,
//        handler -> {
//          if (handler.succeeded()) {
//            JsonObject response = handler.result();
//            logger.info("Register subscription response is : " + response);
//            assertEquals(expected, response);
//          }
//          testContext.completeNow();
//        });
//  }
}

package iudx.resource.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileInputStream;
import java.io.InputStream;
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

  private static final Logger logger = LoggerFactory.getLogger(AdaptorEntitiesTestCases.class);

  @BeforeAll
  @DisplayName("Deploy a verticle")
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
    /* Create a Json Object for properties */
    propObj = new JsonObject();
    propObj.put("userName", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);
    propObj.put("vHost", dataBrokerVhost);

    /* Call the databroker constructor with the RabbitMQ client Vertx web client. */

    databroker = new DataBrokerServiceImpl(client, webClient, propObj);
    testContext.completeNow();

  }

  @Test
  @DisplayName("Testing registerAdaptor")
  @Order(1)
  void successRegisterAdaptor(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("id", "umesh_adaptor");
    request.put("consumer", "umesh.pacholi@trigyn.com");

    JsonObject expected = new JsonObject();
    expected.put("username", "trigyn.com%2F3e6c80469f94feb3bba9ea8d46219d5c");
    expected.put("apiKey", "123456");
    expected.put("id", "umesh_adaptor");
    expected.put("vHost", "%2F");

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
  @DisplayName("Testing getExchange")
  @Order(2)
  void successGetExchange(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("id", "umesh_adaptor");
    JsonObject expected = new JsonObject();
    expected.put("type", 200);
    expected.put("title", "success");
    expected.put("detail", "Exchange found");

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
  @DisplayName("Testing listAdaptor")
  @Order(3)
  void successListAdaptor(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("exchangeName", "umesh_adaptor");

    JsonObject expected = new JsonObject();
    JsonArray adaptorLogs_entities = new JsonArray();
    JsonArray database_entities = new JsonArray();
    adaptorLogs_entities.add("umesh_adaptor.dataIssue");
    adaptorLogs_entities.add("umesh_adaptor.downstreamIssue");
    adaptorLogs_entities.add("umesh_adaptor.heartbeat");
    database_entities.add("umesh_adaptor");
    expected.put("adaptorLogs", adaptorLogs_entities);
    expected.put("database", database_entities);

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
  @DisplayName("Testing deleteAdaptor")
  @Order(4)
  void successDeleteAdaptor(VertxTestContext testContext) {
    JsonObject expected = new JsonObject();
    expected.put("id", "umesh_adaptor");
    expected.put("type", "adaptor deletion");
    expected.put("title", "success");
    expected.put("detail", "adaptor deleted");

    JsonObject request = new JsonObject();
    request.put("id", "umesh_adaptor");

    databroker.deleteAdaptor(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        logger.info("inside test - DeleteAdaptor response is : " + response);
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }


}



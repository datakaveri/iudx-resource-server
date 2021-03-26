package iudx.resource.server.callback;

import static org.junit.jupiter.api.Assertions.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 *@Disabled test cases disabled for current release.
 */
@Disabled 
public class CallbackServiceTest {

  static CallbackService callback;
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
  static JsonObject propObj;
  private static Vertx vertxObj;

  /* Database Properties */
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int poolSize;
  static private String callBackUrl;

  private static Configuration appConfig;

  private static final Logger logger = LoggerFactory.getLogger(CallbackServiceTest.class);

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void startVertx(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {

    vertxObj = vertx;

    /* Read the configuration and set the rabbitMQ server properties. */
    appConfig = new Configuration();
    JsonObject callbackConfig = appConfig.configLoader(3, vertx2);

    try {

      dataBrokerIP = callbackConfig.getString("dataBrokerIP");
      dataBrokerPort = Integer.parseInt(callbackConfig.getString("dataBrokerPort"));
      dataBrokerManagementPort =
          Integer.parseInt(callbackConfig.getString("dataBrokerManagementPort"));
      dataBrokerVhost = callbackConfig.getString("dataBrokerVhost");
      dataBrokerUserName = callbackConfig.getString("dataBrokerUserName");
      dataBrokerPassword = callbackConfig.getString("dataBrokerPassword");
      connectionTimeout = Integer.parseInt(callbackConfig.getString("connectionTimeout"));
      requestedHeartbeat = Integer.parseInt(callbackConfig.getString("requestedHeartbeat"));
      handshakeTimeout = Integer.parseInt(callbackConfig.getString("handshakeTimeout"));
      requestedChannelMax = Integer.parseInt(callbackConfig.getString("requestedChannelMax"));
      networkRecoveryInterval =
          Integer.parseInt(callbackConfig.getString("networkRecoveryInterval"));

      databaseIP = callbackConfig.getString("callbackDatabaseIP");
      databasePort = Integer.parseInt(callbackConfig.getString("callbackDatabasePort"));
      databaseName = callbackConfig.getString("callbackDatabaseName");
      databaseUserName = callbackConfig.getString("callbackDatabaseUserName");
      databasePassword = callbackConfig.getString("callbackDatabasePassword");
      poolSize = Integer.parseInt(callbackConfig.getString("callbackpoolSize"));

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

    /* Dummy callbackUrl */
    callBackUrl = callbackConfig.getString("callbackUrl");

    /*
     * Create a RabbitMQ Client with the configuration and vertx cluster instance.
     */
    client = RabbitMQClient.create(vertxObj, config);

    /*
     * Create a Vertx Web Client with the configuration and vertx cluster instance.
     */
    webClient = WebClient.create(vertxObj, webConfig);

    /* Create a Json Object for properties */
    propObj = new JsonObject();

    propObj.put("userName", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);
    propObj.put("vHost", dataBrokerVhost);
    propObj.put("dataBrokerIP", dataBrokerIP);
    propObj.put("dataBrokerPort", dataBrokerPort);
    propObj.put("callbackDatabaseIP", databaseIP);
    propObj.put("callbackDatabasePort", databasePort);
    propObj.put("callbackDatabaseName", databaseName);
    propObj.put("callbackDatabaseUserName", databaseUserName);
    propObj.put("callbackDatabasePassword", databasePassword);
    propObj.put("callbackpoolSize", poolSize);

    /* Call the callback constructor with the RabbitMQ client. */
    callback = new CallbackServiceImpl(client, webClient, propObj, vertxObj);
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Testing create Connection with queue callback.notification")
  void successGetMessageFromCallbackNotificationQueue(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, "callback.notification");

    JsonObject expected = new JsonObject();
    expected.put(Constants.DATABASE_QUERY_RESULT, Constants.CONNECT_TO_CALLBACK_NOTIFICATION_QUEUE);

    callback.connectToCallbackNotificationQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(2)
  @DisplayName("Testing create Connection with incorrect Queue name")
  void failGetMessageFromCallbackNotificationQueue(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, "callback.notification_incorrectName");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, Constants.CONSUME_QUEUE_MESSAGE_FAIL + Constants.COLON
        + "callback.notification_incorrectName");
    callback.connectToCallbackNotificationQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(3)
  @DisplayName("Testing create Connection with queue callback.data")
  void successGetMessageFromCallbackDataQueue(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, "callback.data");

    JsonObject expected = new JsonObject();
    expected.put(Constants.DATABASE_QUERY_RESULT, Constants.CONNECT_TO_CALLBACK_DATA_QUEUE);

    callback.connectToCallbackDataQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(4)
  @DisplayName("Testing create Connection with incorrect Queue name")
  void failGetMessageFromCallbackDataQueue(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.QUEUE_NAME, "callback.data_incorrectName");

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR,
        Constants.CONSUME_QUEUE_MESSAGE_FAIL + Constants.COLON + "callback.data_incorrectName");

    callback.connectToCallbackNotificationQueue(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }


  @Test
  @Order(7)
  @DisplayName("Testing query send data to callbackurl")
  void successSendDataToCallBackSubscriber(VertxTestContext testContext) {

    JsonObject callBackDataObj = new JsonObject();
    JsonObject _currentMessageJsonObj =
        new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature", 44);


    String userName = "iudx";
    String password = "iudx@123";

    callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
    callBackDataObj.put(Constants.USER_NAME, userName);
    callBackDataObj.put(Constants.PASSWORD, password);

    JsonObject request = new JsonObject();
    request.put("callBackJsonObj", callBackDataObj);
    request.put("currentMessageJsonObj", _currentMessageJsonObj);

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.CALLBACK_SUCCESS);

    callback.sendDataToCallBackSubscriber(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(8)
  @DisplayName("Testing send data to callbackurl when callBackUrl is Empty")
  void failSendDataToCallBackSubscriber(VertxTestContext testContext) {

    JsonObject callBackDataObj = new JsonObject();
    JsonObject _currentMessageJsonObj =
        new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature", 44);

    String callBackUrl = "";

    String userName = "iudx";
    String password = "iudx@123";

    callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
    callBackDataObj.put(Constants.USER_NAME, userName);
    callBackDataObj.put(Constants.PASSWORD, password);

    JsonObject request = new JsonObject();
    request.put("callBackJsonObj", callBackDataObj);
    request.put("currentMessageJsonObj", _currentMessageJsonObj);

    JsonObject expected = new JsonObject().put(Constants.ERROR, Constants.CALLBACK_URL_INVALID);

    callback.sendDataToCallBackSubscriber(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(9)
  @DisplayName("Testing send data to callbackurl for NULL Username and Password")
  void successSendDataToCallBackSubscriberNullUserNameAndPassword(VertxTestContext testContext) {

    JsonObject callBackDataObj = new JsonObject();
    JsonObject _currentMessageJsonObj =
        new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature", 44);


    String userName = null;
    String password = null;

    callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
    callBackDataObj.put(Constants.USER_NAME, userName);
    callBackDataObj.put(Constants.PASSWORD, password);

    JsonObject request = new JsonObject();
    request.put("callBackJsonObj", callBackDataObj);
    request.put("currentMessageJsonObj", _currentMessageJsonObj);

    JsonObject expected = new JsonObject();
    expected.put(Constants.TYPE, 200);
    expected.put(Constants.TITLE, Constants.SUCCESS);
    expected.put(Constants.DETAIL, Constants.CALLBACK_SUCCESS);

    callback.sendDataToCallBackSubscriber(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }

  @Test
  @Order(10)
  @DisplayName("Testing send data to incorrect CallbackUrl")
  void failSendDataToCallBackSubscriberForCallbackurlIncorrect(VertxTestContext testContext) {

    JsonObject callBackDataObj = new JsonObject();
    JsonObject _currentMessageJsonObj =
        new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature", 44);

    String callBackUrl = "http://localhost:9089/incorrectUrl";

    String userName = "iudx";
    String password = "iudx@123";

    callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
    callBackDataObj.put(Constants.USER_NAME, userName);
    callBackDataObj.put(Constants.PASSWORD, password);

    JsonObject request = new JsonObject();
    request.put("callBackJsonObj", callBackDataObj);
    request.put("currentMessageJsonObj", _currentMessageJsonObj);

    JsonObject expected = new JsonObject();
    expected.put(Constants.ERROR, "Failed to connect callbackUrl");

    callback.sendDataToCallBackSubscriber(request, handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result();
        assertEquals(expected, response);
      }
      testContext.completeNow();
    });
  }
}

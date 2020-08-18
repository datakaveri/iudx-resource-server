package iudx.resource.server.callback;

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

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
	static private String callBackUrl_Dummy;

	private static final Logger logger = LoggerFactory.getLogger(CallbackServiceTest.class);

	@BeforeAll
	@DisplayName("Deploy a verticle")
	static void startVertx(Vertx vertx, VertxTestContext testContext) {

		ClusterManager mgr = new HazelcastClusterManager();
		VertxOptions options = new VertxOptions().setClusterManager(mgr);

		Vertx.clusteredVertx(options, res -> {
			if (res.succeeded()) {
				vertxObj = res.result();

				/* Read the configuration and set the rabbitMQ server properties. */
				properties = new Properties();
				inputstream = null;

				try {
					inputstream = new FileInputStream("config.properties");
					properties.load(inputstream);
					inputstream = new FileInputStream("config.properties");
					properties.load(inputstream);

					dataBrokerIP = properties.getProperty("dataBrokerIP");
					dataBrokerPort = Integer.parseInt(properties.getProperty("dataBrokerPort"));
					dataBrokerManagementPort = Integer.parseInt(properties.getProperty("dataBrokerManagementPort"));
					dataBrokerVhost = properties.getProperty("dataBrokerVhost");
					dataBrokerUserName = properties.getProperty("dataBrokerUserName");
					dataBrokerPassword = properties.getProperty("dataBrokerPassword");
					connectionTimeout = Integer.parseInt(properties.getProperty("connectionTimeout"));
					requestedHeartbeat = Integer.parseInt(properties.getProperty("requestedHeartbeat"));
					handshakeTimeout = Integer.parseInt(properties.getProperty("handshakeTimeout"));
					requestedChannelMax = Integer.parseInt(properties.getProperty("requestedChannelMax"));
					networkRecoveryInterval = Integer.parseInt(properties.getProperty("networkRecoveryInterval"));

		            databaseIP = propObj.getString("callbackDatabaseIP");
		            databasePort = propObj.getInteger("callbackDatabasePort");
		            databaseName = propObj.getString("callbackDatabaseName");
		            databaseUserName = propObj.getString("callbackDatabaseUserName");
		            databasePassword = propObj.getString("callbackDatabasePassword");
		            poolSize = propObj.getInteger("callbackpoolSize");

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
				callBackUrl_Dummy = "http://localhost:9088/getMessage";

				/*
				 * Create a RabbitMQ Client with the configuration and vertx cluster instance.
				 */
				client = RabbitMQClient.create(vertxObj, config);

				/*
				 * Create a Vertx Web Client with the configuration and vertx cluster instance.
				 */
				webClient = WebClient.create(vertxObj, webConfig);

				/* Create a Json Object for properties */
				JsonObject propObj = new JsonObject();

				propObj.put("userName", dataBrokerUserName);
				propObj.put("password", dataBrokerPassword);
				propObj.put("vHost", dataBrokerVhost);
				propObj.put("dataBrokerIP", dataBrokerIP);
				propObj.put("dataBrokerPort", dataBrokerPort);
				propObj.put("dataBrokerPort", dataBrokerPort);
				propObj.put("databaseIP", databaseIP);
				propObj.put("databasePort", databasePort);
				propObj.put("databaseName", databaseName);
				propObj.put("databaseUserName", databaseUserName);
				propObj.put("databasePassword", databasePassword);
				propObj.put("databasePoolSize", poolSize);

				/* Call the callback constructor with the RabbitMQ client. */
				callback = new CallbackServiceImpl(client, webClient, propObj, vertxObj);
				testContext.completeNow();
			}
		});
	}

	@Test
	@Order(1)
	@DisplayName("Testing create Connection with queue callback.notification")
	void successGetMessageFromCallbackNotificationQueue(VertxTestContext testContext) {

		JsonObject request = new JsonObject();
		request.put("queueName", "callback.notification");
        
		JsonObject expected = new JsonObject();
		expected.put("success", "Connected to callback.notification queue");

		callback.getMessageFromCallbackNotificationQueue(request, handler -> {
			if (handler.succeeded()) {
				JsonObject response = handler.result();
				logger.info(response);
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
		request.put("queueName", "callback.notification_incorrectName");
       
		JsonObject expected = new JsonObject();
		expected.put("failure", "Failed to connect with callback.notification queue");

		
		callback.getMessageFromCallbackNotificationQueue(request, handler -> {
			if (handler.failed()) {
				String response = handler.cause().getMessage();
				System.out.println(response);
				logger.info(response);
				assertEquals(expected.toString(), response);
			}
			testContext.completeNow();
		});
	}
	
	@Test
	@Order(3)
	@DisplayName("Testing query callback database")
	void successQueryCallBackDataBase(VertxTestContext testContext) {

		JsonObject requestObj = new JsonObject();
		requestObj.put(Constants.TABLE_NAME, "registercallback");

		JsonObject expected = new JsonObject();
		expected.put("success", "Cache Updated Successfully");

		callback.queryCallBackDataBase(requestObj, handler -> {
			if (handler.succeeded()) {
				JsonObject response = handler.result();
				logger.info(response);
				assertEquals(expected, response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(4)
	@DisplayName("Testing query callback database when table name is incorrect")
	void failQueryCallBackDataBase(VertxTestContext testContext) {

		JsonObject requestObj = new JsonObject();
		requestObj.put(Constants.TABLE_NAME, "registercallback__test");

		JsonObject expected = new JsonObject();
		expected.put("failure", "Database Query Failed");

		callback.queryCallBackDataBase(requestObj, handler -> {
			if (handler.failed()) {
				String response = handler.cause().getMessage();
				logger.info(response);
				assertEquals(expected.toString(), response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(5)
	@DisplayName("Testing query callback database when database port and IP is incorrect")
	void failQueryCallBackDataBaseIncorrectPortAndIp(VertxTestContext testContext) {

		JsonObject requestObj = new JsonObject();
		requestObj.put(Constants.TABLE_NAME, "registercallback");

		JsonObject expected = new JsonObject();
		expected.put("failure", "Database Query Failed");

		callback.queryCallBackDataBase(requestObj, handler -> {
			if (handler.failed()) {
				String response = handler.cause().getMessage();
				logger.info(response);
				assertEquals(expected.toString(), response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(6)
	@DisplayName("Testing query callback database when pgClient is null")
	void failQueryCallBackDataBasePgclientNull(VertxTestContext testContext) {

		/* CReate Mock propObj Obj */
		JsonObject propObj = new JsonObject();
		propObj.put("userName", dataBrokerUserName);
		propObj.put("password", dataBrokerPassword);
		propObj.put("vHost", dataBrokerVhost);
		propObj.put("dataBrokerIP", dataBrokerIP);
		propObj.put("dataBrokerPort", dataBrokerPort);
		propObj.put("dataBrokerPort", dataBrokerPort);
		propObj.put("databaseIP", databaseIP);
		propObj.put("databasePort", 8088);
		propObj.put("databaseName", "mockDataBaseName");
		propObj.put("databaseUserName", "mockUserName");
		propObj.put("databasePassword", "mockPassword");
		propObj.put("databasePoolSize", 25);

		callback = new CallbackServiceImpl(client, webClient, propObj, vertxObj);
		JsonObject requestObj = new JsonObject();
		requestObj.put(Constants.TABLE_NAME, "registercallback");

		JsonObject expected = new JsonObject();
		expected.put("failure", "Database Query Failed");

		callback.queryCallBackDataBase(requestObj, handler -> {
			if (handler.failed()) {
				String response = handler.cause().getMessage();
				logger.info(response);
				assertEquals(expected.toString(), response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(7)
	@DisplayName("Testing query send data to callbackurl")
	void successSendDataToCallBackSubscriber(VertxTestContext testContext) {

		JsonObject callBackDataObj = new JsonObject();
		JsonObject _currentMessageJsonObj = new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature",
				44);

		String callBackUrl = callBackUrl_Dummy;

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
				logger.info(response);
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
		JsonObject _currentMessageJsonObj = new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature",
				44);

		String callBackUrl = "";

		String userName = "iudx";
		String password = "iudx@123";

		callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
		callBackDataObj.put(Constants.USER_NAME, userName);
		callBackDataObj.put(Constants.PASSWORD, password);

		JsonObject request = new JsonObject();
		request.put("callBackJsonObj", callBackDataObj);
		request.put("currentMessageJsonObj", _currentMessageJsonObj);

		JsonObject expected = new JsonObject().put(Constants.FAILURE, "Failed to send data to callBackUrl");

		callback.sendDataToCallBackSubscriber(request, handler -> {
			if (handler.failed()) {
				String response = handler.cause().getMessage();
				logger.info(response);
				assertEquals(expected.toString(), response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(9)
	@DisplayName("Testing send data to callbackurl for NULL Username and Password")
	void failSendDataToCallBackSubscriberNullUserNameAndPassword(VertxTestContext testContext) {

		JsonObject callBackDataObj = new JsonObject();
		JsonObject _currentMessageJsonObj = new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature",
				44);

		String callBackUrl = callBackUrl_Dummy;

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
				logger.info(response);
				assertEquals(expected, response);
			}
			testContext.completeNow();
		});
	}

	@Test
	@Order(10)
	@DisplayName("Testing send data to callbackurl for Incorrect CallbackUrl")
	void failSendDataToCallBackSubscriberForCallbackurlIncorrect(VertxTestContext testContext) {

		JsonObject callBackDataObj = new JsonObject();
		JsonObject _currentMessageJsonObj = new JsonObject().put("id", "key_1").put("pressure", 34).put("temprature",
				44);

		String callBackUrl = "http://localhost:9088/incorrectUrl";

		String userName = "iudx";
		String password = "iudx@123";

		callBackDataObj.put(Constants.CALLBACK_URL, callBackUrl);
		callBackDataObj.put(Constants.USER_NAME, userName);
		callBackDataObj.put(Constants.PASSWORD, password);

		JsonObject request = new JsonObject();
		request.put("callBackJsonObj", callBackDataObj);
		request.put("currentMessageJsonObj", _currentMessageJsonObj);

		JsonObject expected = new JsonObject();
		expected.put(Constants.TYPE, 404);
		expected.put(Constants.TITLE, Constants.FAILURE);
		expected.put(Constants.DETAIL, Constants.CALLBACK_URL_NOT_FOUND);

		callback.sendDataToCallBackSubscriber(request, handler -> {
			if (handler.succeeded()) {
				JsonObject response = handler.result();
				logger.info(response);
				assertEquals(expected, response);
			}
			testContext.completeNow();
		});
	}
}

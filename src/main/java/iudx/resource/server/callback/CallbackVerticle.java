package iudx.resource.server.callback;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class CallbackVerticle extends AbstractVerticle {

  private static final String CALLBACK_SERVICE_ADDRESS = "iudx.rs.callback.service";
  private static final Logger LOGGER = LogManager.getLogger(CallbackVerticle.class);
  private RabbitMQOptions config;
  private RabbitMQClient client;
  private Properties properties;
  private InputStream inputstream;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private CallbackService callback;
  private WebClient webClient;
  private WebClientOptions webConfig;
  /* Database Properties */
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster.
   */
  @Override
  public void start() throws Exception {

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
      networkRecoveryInterval =
        Integer.parseInt(properties.getProperty("networkRecoveryInterval"));

      databaseIP = properties.getProperty("callbackDatabaseIP");
      databasePort = Integer.parseInt(properties.getProperty("callbackDatabasePort"));
      databaseName = properties.getProperty("callbackDatabaseName");
      databaseUserName = properties.getProperty("callbackDatabaseUserName");
      databasePassword = properties.getProperty("callbackDatabasePassword");
      poolSize = Integer.parseInt(properties.getProperty("callbackpoolSize"));

    } catch (Exception ex) {

      LOGGER.info(ex.toString());

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

    /* Create a RabbitMQ Client with the configuration and vertx cluster instance. */

    client = RabbitMQClient.create(vertx, config);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */

    webClient = WebClient.create(vertx, webConfig);

    /* Create a Json Object for properties */

    JsonObject propObj = new JsonObject();

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
    callback = new CallbackServiceImpl(client, webClient, propObj, vertx);

    /* Publish the Callback service with the Event Bus against an address. */

    new ServiceBinder(vertx).setAddress(CALLBACK_SERVICE_ADDRESS)
      .register(CallbackService.class, callback);

    LOGGER.info("Callback Verticle started");
  }
}

package iudx.resource.server.callback;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CallbackVerticle extends AbstractVerticle {

  private static final String CALLBACK_SERVICE_ADDRESS = "iudx.rs.callback.service";
  private static final Logger LOGGER = LogManager.getLogger(CallbackVerticle.class);
  private RabbitMQOptions config;
  private RabbitMQClient client;
  private String dataBrokerIp;
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
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  /* Database Properties */
  private String databaseIp;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;

  /** This method is used to start the Verticle. It deploys a verticle in a cluster. */
  @Override
  public void start() throws Exception {

    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
    dataBrokerVhost = config().getString("dataBrokerVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");

    databaseIp = config().getString("callbackDatabaseIP");
    databasePort = config().getInteger("callbackDatabasePort");
    databaseName = config().getString("callbackDatabaseName");
    databaseUserName = config().getString("callbackDatabaseUserName");
    databasePassword = config().getString("callbackDatabasePassword");
    poolSize = config().getInteger("callbackpoolSize");

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIp);
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
    webConfig.setDefaultHost(dataBrokerIp);
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
    propObj.put("dataBrokerIP", dataBrokerIp);
    propObj.put("dataBrokerPort", dataBrokerPort);
    propObj.put("callbackDatabaseIP", databaseIp);
    propObj.put("callbackDatabasePort", databasePort);
    propObj.put("callbackDatabaseName", databaseName);
    propObj.put("callbackDatabaseUserName", databaseUserName);
    propObj.put("callbackDatabasePassword", databasePassword);
    propObj.put("callbackpoolSize", poolSize);

    /* Call the callback constructor with the RabbitMQ client. */
    binder = new ServiceBinder(vertx);
    callback = new CallbackServiceImpl(client, webClient, propObj, vertx);

    /* Publish the Callback service with the Event Bus against an address. */

    consumer =
        binder.setAddress(CALLBACK_SERVICE_ADDRESS).register(CallbackService.class, callback);

    LOGGER.info("Callback Verticle started");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

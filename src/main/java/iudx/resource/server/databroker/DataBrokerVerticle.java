package iudx.resource.server.databroker;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Data Broker Verticle.
 * <h1>Data Broker Verticle</h1>
 * <p>
 * The Data Broker Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.databroker.DataBrokerService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DataBrokerVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(DataBrokerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private Record record;
  private DataBrokerService databroker;
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
  private WebClient webClient;
  private WebClientOptions webConfig;

  private RabbitMQStreamingClient rabbitMQStreamingClient;
  private RabbitMQWebClient rabbitMQWebClient;
  private PostgresQLClient pgClient;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */

  @Override
  public void start() throws Exception {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

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

        /* Create a Json Object for properties */

        JsonObject propObj = new JsonObject();

        propObj.put("userName", dataBrokerUserName);
        propObj.put("password", dataBrokerPassword);
        propObj.put("vHost", dataBrokerVhost);

        /* Call the databroker constructor with the RabbitMQ client. */

        // databroker = new DataBrokerServiceImpl(client, webClient, propObj);

        // rabbitMQClientImpl = new RabbitMQClientImpl(vertx, config, webConfig, propObj);
        // databroker = new DataBrokerServiceImpl(rabbitMQClientImpl, propObj);

        rabbitMQWebClient = new RabbitMQWebClient(vertx, webConfig, propObj);
        rabbitMQStreamingClient =
            new RabbitMQStreamingClient(vertx, config, rabbitMQWebClient);
        pgClient = new PostgresQLClient();
        databroker = new DataBrokerServiceImpl(rabbitMQStreamingClient, pgClient, dataBrokerVhost);


        /* Publish the Data Broker service with the Event Bus against an address. */

        new ServiceBinder(vertx).setAddress("iudx.rs.databroker.service")
            .register(DataBrokerService.class, databroker);

        /* Get a handler for the Service Discovery interface and publish a service record. */

        discovery = ServiceDiscovery.create(vertx);
        record = EventBusService.createRecord("iudx.rs.databroker.service", // The service name
            "iudx.rs.databroker.service", // the service address,
            DataBrokerService.class // the service interface
        );

        discovery.publish(record, publishRecordHandler -> {
          if (publishRecordHandler.succeeded()) {
            Record publishedRecord = publishRecordHandler.result();
            logger.info("Publication succeeded " + publishedRecord.toJson());
          } else {
            logger.info("Publication failed " + publishRecordHandler.result());
          }
        });

      }

    });

    System.out.println("DataBrokerVerticle started");

  }

}

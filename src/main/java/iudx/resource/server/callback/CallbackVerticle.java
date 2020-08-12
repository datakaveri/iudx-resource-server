package iudx.resource.server.callback;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class CallbackVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(CallbackVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private RabbitMQOptions config;
  private RabbitMQClient client;
  private Properties properties;
  private InputStream inputstream;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;


  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster.
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

        /* Create a RabbitMQ Client with the configuration and vertx cluster instance. */

        client = RabbitMQClient.create(vertx, config);

        /*
         * TODO : Connect with the RabbitMQ callback notification Queue (callback.notification) to
         * get new notifications about subscribers
         */
        /*
         * TODO : Create/Update/Delete cache of subscribers and their end-points from the
         * notification message
         */
        /*
         * TODO : Create/Update/Delete cache of subscribers and their end-points by querying the
         * database
         */
        /*
         * TODO : Connect with the RabbitMQ callback data Queue (callback.data) to get new data to
         * be sent to subscribers
         */
        /* TODO : Get the routing key of the data, find the subscribers interested on the data */
        /* TODO : Setup a web client using the subscription information */
        /* TODO : Send data to interested subscribes */

      }

    });

    System.out.println("Callback Verticle started");
  }
}

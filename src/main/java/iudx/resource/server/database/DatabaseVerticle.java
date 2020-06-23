package iudx.resource.server.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

/**
 * The Database Verticle.
 * <h1>Database Verticle</h1>
 * <p>
 * The Database Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.database.DatabaseService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private Record record;
  private DatabaseService database;
  private RestClient client;
  private Properties properties;
  private InputStream inputstream;
  private String databaseIP;
  private int databasePort;
  private static final String DATABASE_SERVICE_ADDRESS = "iudx.rs.database.service";

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start(Promise<Void> startPromise) throws Exception {


    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      databaseIP = properties.getProperty("databaseIP");
      databasePort = Integer.parseInt(properties.getProperty("databasePort"));

    } catch (Exception ex) {

      logger.info(ex.toString());

    }

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);
    client = RestClient.builder(new HttpHost(databaseIP, databasePort, "http")).build();

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();
        database = new DatabaseServiceImpl(client);

        /* Publish the Database service with the Event Bus against an address. */

        new ServiceBinder(vertx).setAddress(DATABASE_SERVICE_ADDRESS)
            .register(DatabaseService.class, database);

        /* Get a handler for the Service Discovery interface and publish a service record. */

        discovery = ServiceDiscovery.create(vertx);
        record = EventBusService.createRecord(DATABASE_SERVICE_ADDRESS, // The service name
            DATABASE_SERVICE_ADDRESS, // the service address,
            DatabaseService.class // the service interface
        );

        discovery.publish(record, publishRecordHandler -> {
          if (publishRecordHandler.succeeded()) {
            Record publishedRecord = publishRecordHandler.result();
            startPromise.complete();
            logger.info("Publication succeeded " + publishedRecord.toJson());
          } else {
            logger.info("Publication failed " + publishRecordHandler.result());
          }
        });

      }

    });

  }
}

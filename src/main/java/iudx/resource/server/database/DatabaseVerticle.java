package iudx.resource.server.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
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

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   * 
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);
    client = RestClient.builder(new HttpHost("localhost",9201,"http")).build();

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        Router router = Router.router(vertx);
        router.post("/search").handler(this::sendDataToService);
        vertx.createHttpServer()
                .requestHandler(router::accept).listen(54000);

        database = new DatabaseServiceImpl(client);

        /* Publish the Database service with the Event Bus against an address. */

        new ServiceBinder(vertx).setAddress("iudx.rs.database.service")
            .register(DatabaseService.class, database);

        /* Get a handler for the Service Discovery interface and publish a service record. */

        discovery = ServiceDiscovery.create(vertx);
        record = EventBusService.createRecord("iudx.rs.database.service", // The service name
            "iudx.rs.database.service", // the service address,
            DatabaseService.class // the service interface
        );

        discovery.publish(record, publishRecordHandler -> {
          if (publishRecordHandler.succeeded()) {
            Record publishedRecord = publishRecordHandler.result();
            logger.info("Publication succeeded " + publishedRecord.toJson());
            getDatabaseService();
          } else {
            logger.info("Publication failed " + publishRecordHandler.result());
          }
        });

      }

    });

  }

  private void sendDataToService(RoutingContext context) {
    HttpServerResponse httpResponse = context.response();
    database.searchQuery(context.getBodyAsJson(),res->{
      if(res.succeeded()){
        JsonArray response = res.result();
        logger.info("Successful: "+response.toString());
        httpResponse.setStatusCode(200).end(response.toString());
      }else{
        logger.info("Error: "+ res.cause().getMessage());
        httpResponse.setStatusCode(400).end(res.cause().getMessage());
      }
    });
  }

  private void getDatabaseService() {
    EventBusService.getProxy(discovery,DatabaseService.class, ar->{
      if(ar.succeeded()){
        database = ar.result();
        logger.info("Success with Proxy Class: " + database.getClass().toString());
      }else{
        logger.info("Database Proxy returned failed");
      }
    });
  }

}

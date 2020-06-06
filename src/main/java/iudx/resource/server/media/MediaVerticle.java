package iudx.resource.server.media;

import io.vertx.core.AbstractVerticle;
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

/**
 * The Media Verticle.
 * <h1>Media Verticle</h1>
 * <p>
 * The Media Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.media.MediaService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class MediaVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(MediaVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private Record record;
  private MediaService mediaDownload;

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

        mediaDownload = new MediaServiceImpl();

        /* Publish the Media service with the Event Bus against an address. */

        new ServiceBinder(vertx).setAddress("iudx.rs.media.download.service")
            .register(MediaService.class, mediaDownload);

        /* Get a handler for the Service Discovery interface and publish a service record. */

        discovery = ServiceDiscovery.create(vertx);
        record = EventBusService.createRecord("iudx.rs.media.download.service", // The service name
            "iudx.rs.media.download.service", // the service address,
            MediaService.class // the service interface
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

  }

}

package iudx.resource.server.filedownload;

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
 * The File Download Verticle.
 * <h1>File Download Verticle</h1>
 * <p>
 * The File Download Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.filedownload.FileDownloadService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class FileDownloadVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(FileDownloadVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private Record record;
  private FileDownloadService fileDownload;

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

        fileDownload = new FileDownloadServiceImpl();

        /* Publish the File Download service with the Event Bus against an address. */

        new ServiceBinder(vertx).setAddress("iudx.rs.file.download.service")
            .register(FileDownloadService.class, fileDownload);

        /* Get a handler for the Service Discovery interface and publish a service record. */

        discovery = ServiceDiscovery.create(vertx);
        record = EventBusService.createRecord("iudx.rs.file.download.service", // The service name
            "iudx.rs.file.download.service", // the service address,
            FileDownloadService.class // the service interface
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

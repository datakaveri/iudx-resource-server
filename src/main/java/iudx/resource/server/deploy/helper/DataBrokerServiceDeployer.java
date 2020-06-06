package iudx.resource.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.databroker.DataBrokerVerticle;

/**
 * The Resource Server Data Broker Service Deployer.
 * <h1>Resource Server Data Broker Service Deployer</h1>
 * <p>
 * The Data Broker Service Deployer deploys the API Server Verticle.
 * </p>
 * 
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */

public class DataBrokerServiceDeployer {

  private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the Data Broker verticle in
   * the resource server.
   * 
   * @param args which is a String array
   */

  public static void main(String[] args) {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /* Deploy the Data Broker Verticle. */

        vertx.deployVerticle(new DataBrokerVerticle(), ar -> {
          if (ar.succeeded()) {
            logger.info("The Data Broker Service is ready !");
          } else {
            logger.info("The Data Broker Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}

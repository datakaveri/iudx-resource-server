package iudx.resource.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.database.DatabaseVerticle;

/**
 * The Resource Server Database Service Deployer.
 * <h1>Resource Server Database Service Deployer</h1>
 * <p>
 * The Database Service Deployer deploys the API Server Verticle.
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

public class DatabaseServiceDeployer {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the Database verticle in the
   * resource server.
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

        /* Deploy the Database Verticle. */

        vertx.deployVerticle(new DatabaseVerticle(), ar -> {
          if (ar.succeeded()) {
            logger.info("The Database Service is ready !");
          } else {
            logger.info("The Database Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}

package iudx.resource.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.callback.CallbackVerticle;

public class CallbackServerDeployer {

  private static final Logger logger = LoggerFactory.getLogger(CallbackServerDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the Callback verticle in the
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

        vertx.deployVerticle(new CallbackVerticle(), ar -> {
          if (ar.succeeded()) {
            logger.info("The Callback Server is ready !");
          } else {
            logger.info("The Callback Server failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}


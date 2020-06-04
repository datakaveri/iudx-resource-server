package iudx.resource.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.apiserver.ApiServerVerticle;
import iudx.resource.server.authenticator.AuthenticationVerticle;

/**
 * The Resource Server Authentication Service Deployer.
 * <h1>Resource Server Authentication Service Deployer</h1>
 * <p>
 * The API Server Deployer deploys the Authentication Verticle.
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

public class AuthenticationServiceDeployer {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the Authentication verticle
   * in the resource server.
   * 
   * @param args which is a String array
   */

  public static void main(String[] args) {

    /** Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /** Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /** Deploy the Authentication Server Verticle. */

        vertx.deployVerticle(new AuthenticationVerticle(), ar -> {
          if (ar.succeeded()) {
            logger.info("The Authentication Service is ready !");
          } else {
            logger.info("The Authentication Service failed !");
          }
        });
      } else {
        logger.info("The Vertx Cluster failed !");
      }
    });

  }

}

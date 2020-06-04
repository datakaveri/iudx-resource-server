package iudx.resource.server.deploy.helper;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.resource.server.apiserver.ApiServerVerticle;
import iudx.resource.server.authenticator.AuthenticationVerticle;
import iudx.resource.server.database.DatabaseVerticle;
import iudx.resource.server.databroker.DataBrokerVerticle;
import iudx.resource.server.filedownload.FileDownloadVerticle;
import iudx.resource.server.media.MediaVerticle;

/**
 * The Resource Server Deployer.
 * <h1>Resource Server Deployer</h1>
 * <p>
 * The Resource Server Deployer deploys the API Server, Database, Databroker, File download, Media
 * Verticle.
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

public class ResourceServerDeployer {

  private static final Logger logger = LoggerFactory.getLogger(ResourceServerDeployer.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the resource server.
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

        /** Deploy the Database Verticle. */

        vertx.deployVerticle(new DatabaseVerticle(), DatabaseVerticle -> {
          if (DatabaseVerticle.succeeded()) {
            logger.info("The Database Service is ready !");

            /** Deploy the Data Broker Verticle. */

            vertx.deployVerticle(new DataBrokerVerticle(), DataBrokerVerticle -> {
              if (DataBrokerVerticle.succeeded()) {
                logger.info("The Data Broker Service is ready !");

                /** Deploy the Authentication Server Verticle. */

                vertx.deployVerticle(new AuthenticationVerticle(), AuthenticationVerticle -> {
                  if (AuthenticationVerticle.succeeded()) {
                    logger.info("The Authentication Service is ready !");

                    /** Deploy the File Download Verticle. */

                    vertx.deployVerticle(new FileDownloadVerticle(), FileDownloadVerticle -> {
                      if (FileDownloadVerticle.succeeded()) {
                        logger.info("The File Download Service is ready !");

                        /** Deploy the Media Verticle. */

                        vertx.deployVerticle(new MediaVerticle(), MediaVerticle -> {
                          if (MediaVerticle.succeeded()) {
                            logger.info("The Media Service is ready !");

                            /** Deploy the Api Server Verticle. */

                            vertx.deployVerticle(new ApiServerVerticle(), ar -> {
                              if (ar.succeeded()) {
                                logger.info("The Resource API Server is ready at 8443");
                                logger.info("Check /apis/ for supported APIs");
                              } else {
                                logger.info("The Resource API Server startup failed !");
                              }
                            });

                          } else {
                            logger.info("The Media Service failed !");
                          }
                        });


                      } else {
                        logger.info("The File Download Service failed !");
                      }
                    });


                  } else {
                    logger.info("The Authentication Service failed !");
                  }
                });

              } else {
                logger.info("The Data Broker Service failed !");
              }
            });

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

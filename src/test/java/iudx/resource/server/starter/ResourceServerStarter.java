package iudx.resource.server.starter;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
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

public class ResourceServerStarter {

  private static final Logger logger = LoggerFactory.getLogger(ResourceServerStarter.class);
  private static Vertx vertx;
  private static ClusterManager mgr;
  private static VertxOptions options;

  /**
   * The main method implements the deploy helper script for deploying the resource server.
   * 
   * @param args which is a String array
   */

  public Future<JsonObject> startServer() {

    Promise<JsonObject> promise = Promise.promise();

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        vertx = res.result();

        /* Deploy the Database Verticle. */

        vertx.deployVerticle(new DatabaseVerticle(), databaseVerticle -> {
          if (databaseVerticle.succeeded()) {
            logger.info("The Database Service is ready !");

            /* Deploy the Data Broker Verticle. */

            vertx.deployVerticle(new DataBrokerVerticle(), dataBrokerVerticle -> {
              if (dataBrokerVerticle.succeeded()) {
                logger.info("The Data Broker Service is ready !");

                /* Deploy the Authentication Server Verticle. */

                vertx.deployVerticle(new AuthenticationVerticle(), authenticationVerticle -> {
                  if (authenticationVerticle.succeeded()) {
                    logger.info("The Authentication Service is ready !");

                    /* Deploy the File Download Verticle. */

                    vertx.deployVerticle(new FileDownloadVerticle(), fileDownloadVerticle -> {
                      if (fileDownloadVerticle.succeeded()) {
                        logger.info("The File Download Service is ready !");

                        /* Deploy the Media Verticle. */

                        vertx.deployVerticle(new MediaVerticle(), mediaVerticle -> {
                          if (mediaVerticle.succeeded()) {
                            logger.info("The Media Service is ready !");

                            /* Deploy the Api Server Verticle. */

                            vertx.deployVerticle(new ApiServerVerticle(), apiServerVerticle -> {
                              if (apiServerVerticle.succeeded()) {
                                logger.info("The Resource API Server is ready at 8443");
                                logger.info("Check /apis/ for supported APIs");
                                promise.complete(new JsonObject());
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

    return promise.future();

  }


}

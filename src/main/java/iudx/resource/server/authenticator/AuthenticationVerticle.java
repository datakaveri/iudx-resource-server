package iudx.resource.server.authenticator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The Authentication Verticle.
 * <h1>Authentication Verticle</h1>
 * <p>
 * The Authentication Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.authenticator.AuthenticationService} over the Vert.x Event Bus.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationVerticle.class);
    private final Properties properties = new Properties();
    private Vertx vertx;
    private ClusterManager mgr;
    private VertxOptions options;
    private ServiceDiscovery discovery;
    private Record record;
    private AuthenticationService authentication;

    static WebClient createWebClient(Vertx vertx, Properties properties) {
        return createWebClient(vertx, properties, false);
    }

    static WebClient createWebClient(Vertx vertxObj, Properties properties, boolean testing) {
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }
        WebClientOptions webClientOptions = new WebClientOptions();
        if (testing) webClientOptions.setTrustAll(true).setVerifyHost(false);
        webClientOptions
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions()
                        .setPath(properties.getProperty(Constants.KEYSTORE_PATH))
                        .setPassword(properties.getProperty(Constants.KEYSTORE_PASSWORD)));
        return WebClient.create(vertxObj, webClientOptions);
    }

    /**
     * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
     * service with the Event bus against an address, publishes the service with the service discovery
     * interface.
     *
     * @throws Exception which is a startup exception
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

                authentication = new AuthenticationServiceImpl(createWebClient(vertx, properties));

                /* Publish the Authentication service with the Event Bus against an address. */

                new ServiceBinder(vertx).setAddress("iudx.rs.authentication.service")
                        .register(AuthenticationService.class, authentication);

                /* Get a handler for the Service Discovery interface and publish a service record. */

                discovery = ServiceDiscovery.create(vertx);
                record = EventBusService.createRecord("iudx.rs.authentication.service", // The service name
                        "iudx.rs.authentication.service", // the service address,
                        AuthenticationService.class // the service interface
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

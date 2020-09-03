package iudx.resource.server.authenticator;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
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

  private static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private final Properties properties = new Properties();
  private AuthenticationService authentication;

  static WebClient createWebClient(Vertx vertx, Properties properties) {
    return createWebClient(vertx, properties, false);
  }

  static WebClient createWebClient(Vertx vertxObj, Properties properties, boolean testing) {
    try {
      FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
      if (properties.isEmpty()) {
        properties.load(configFile);
      }
    } catch (IOException e) {
      LOGGER.error("Could not load properties from config file", e);
    }
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true).setKeyStoreOptions(
        new JksOptions().setPath(properties.getProperty(Constants.KEYSTORE_PATH))
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

    authentication = new AuthenticationServiceImpl(vertx, createWebClient(vertx, properties));

    /* Publish the Authentication service with the Event Bus against an address. */

    new ServiceBinder(vertx).setAddress(AUTH_SERVICE_ADDRESS)
      .register(AuthenticationService.class, authentication);
  }
}

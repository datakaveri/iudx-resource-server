package iudx.resource.server.authenticator;

import static iudx.resource.server.common.Constants.PG_SERVICE_ADD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.database.postgres.PostgresService;

/**
 * The Authentication Verticle.
 *
 * <h1>Authentication Verticle</h1>
 *
 * <p>
 * The Authentication Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.authenticator.AuthenticationService} over the Vert.x Event Bus.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class AuthenticationVerticle extends AbstractVerticle {

  private static final String AUTH_SERVICE_ADDRESS = "iudx.rs.authentication.service";
  private static final Logger LOGGER = LogManager.getLogger(AuthenticationVerticle.class);
  private AuthenticationService authentication;
  private AuthenticationService jwtAuthenticationService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;
  private WebClient webClient;

  private PostgresService postgresService;

  static WebClient createWebClient(Vertx vertx, JsonObject config) {
    return createWebClient(vertx, config, false);
  }

  static WebClient createWebClient(Vertx vertxObj, JsonObject config, boolean testing) {
    WebClientOptions webClientOptions = new WebClientOptions();
    if (testing) {
      webClientOptions.setTrustAll(true).setVerifyHost(false);
    }
    webClientOptions.setSsl(true);
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

    getJwtPublicKey(vertx, config())
        .onSuccess(
            handler -> {
              String cert = handler;
              LOGGER.info("cert : " + cert);
              binder = new ServiceBinder(vertx);

              JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
              jwtAuthOptions.addPubSecKey(
                  new PubSecKeyOptions().setAlgorithm("ES256").setBuffer(cert));
              /*
               * Default jwtIgnoreExpiry is false. If set through config, then that value is taken
               */
              boolean jwtIgnoreExpiry =
                  config().getBoolean("jwtIgnoreExpiry") != null &&
                      config().getBoolean("jwtIgnoreExpiry");
              if (jwtIgnoreExpiry) {
                jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
                LOGGER.warn(
                    "JWT ignore expiration set to true, do not set IgnoreExpiration in production!!");
              }
              JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

              postgresService = PostgresService.createProxy(vertx, PG_SERVICE_ADD);

              jwtAuthenticationService =
                  new JwtAuthenticationServiceImpl(
                      vertx, jwtAuth, createWebClient(vertx, config()), config(), postgresService);

              /* Publish the Authentication service with the Event Bus against an address. */
              consumer =
                  binder
                      .setAddress(AUTH_SERVICE_ADDRESS)
                      .register(AuthenticationService.class, jwtAuthenticationService);

              LOGGER.info("Authentication verticle deployed");
            })
        .onFailure(
            handler -> {
              LOGGER.error("failed to get JWT public key from auth server");
              LOGGER.error("Authentication verticle deployment failed.");
            });
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }

  private Future<String> getJwtPublicKey(Vertx vertx, JsonObject config) {
    Promise<String> promise = Promise.promise();
    webClient = createWebClient(vertx, config);
    webClient
        .get(443, config.getString("authServerHost"), "/auth/v1/cert")
        .send(
            handler -> {
              if (handler.succeeded()) {
                JsonObject json = handler.result().bodyAsJsonObject();
                promise.complete(json.getString("cert"));
              } else {
                promise.fail("fail to get JWT public key");
              }
            });
    return promise.future();
  }
}

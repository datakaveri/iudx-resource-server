package iudx.resource.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * The Authentication Service Implementation.
 * <h1>Authentication Service Implementation</h1>
 * <p>
 * The Authentication Service implementation in the IUDX Resource Server
 * implements the definitions of the
 * {@link iudx.resource.server.authenticator.AuthenticationService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */

public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    private static final Properties properties = new Properties();
    private final WebClient webClient;

    public AuthenticationServiceImpl(WebClient client) {
        webClient = client;
        try {
            FileInputStream configFile = new FileInputStream(Constants.CONFIG_FILE);
            if (properties.isEmpty()) properties.load(configFile);
        } catch (IOException e) {
            logger.error("Could not load properties from config file", e);
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
                                                  Handler<AsyncResult<JsonObject>> handler) {
        // added for testing.
        JsonObject json = new JsonObject();
        handler.handle(Future.succeededFuture(json));
        return this;
    }

}

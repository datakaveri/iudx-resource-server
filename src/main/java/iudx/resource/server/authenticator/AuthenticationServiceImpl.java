package iudx.resource.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.apiserver.util.Constants;

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

  /**
   * {@inheritDoc}
   */

  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {
    // added for testing.
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, Constants.APP_TEST_NAME);
    json.put(Constants.JSON_CONSUMER, Constants.APP_TEST_CONSUMER);
    handler.handle(Future.succeededFuture(json));
    return this;
  }

}

package iudx.resource.server.authenticator.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  static {
    // delegate allowed to access all endpoints
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData,
      JsonObject quotaConsumed) {
    return isAuthorized(authRequest, jwtData);
  }
}

package iudx.resource.server.authenticator.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  private final Api api;
  private ProviderAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }
  public static ProviderAuthStrategy getInstance(Api api)
  {
    return new ProviderAuthStrategy(api);
  }
  private void buildPermissions(Api api) {
    // provider allowed to access all endpoints
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData,
      JsonObject quotaConsumed) {
    // TODO Auto-generated method stub
    return isAuthorized(authRequest, jwtData);
  }
}

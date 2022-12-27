package iudx.resource.server.authenticator.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  private final Api api;

  private static volatile DelegateAuthStrategy instance;


  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  private DelegateAuthStrategy(Api api)
  {
    this.api = api;
    buildPermissions(api);
  }
  public static DelegateAuthStrategy getInstance(Api api)
  {

    if(instance == null)
    {
      synchronized (DelegateAuthStrategy.class)
      {
        if(instance == null)
        {
          instance = new DelegateAuthStrategy(api);
        }
      }
    }
    return instance;

  }
  private void buildPermissions(Api api) {
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

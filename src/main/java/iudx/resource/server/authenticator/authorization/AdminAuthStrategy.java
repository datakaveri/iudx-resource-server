package iudx.resource.server.authenticator.authorization;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public class AdminAuthStrategy implements AuthorizationStrategy{

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

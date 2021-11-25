package iudx.resource.server.authenticator.authorization;

import iudx.resource.server.authenticator.model.JwtData;

public class AdminAuthStrategy implements AuthorizationStrategy{

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }

}

package iudx.resource.server.authenticator.authorization;

import iudx.resource.server.authenticator.model.JwtData;

public interface AuthorizationStrategy {

  boolean isAuthorized(AuthorizationRequest authRequest,JwtData jwtData);

}

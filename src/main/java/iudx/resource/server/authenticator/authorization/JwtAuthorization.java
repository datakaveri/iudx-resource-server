package iudx.resource.server.authenticator.authorization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.resource.server.authenticator.model.JwtData;

public final class JwtAuthorization {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthorization.class);

  private final AuthorizationStrategy authStrategy;

  public JwtAuthorization(final AuthorizationStrategy authStrategy) {
    this.authStrategy = authStrategy;
  }

  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return authStrategy.isAuthorized(authRequest, jwtData);
  }
  
}

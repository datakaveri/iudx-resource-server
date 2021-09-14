package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.authenticator.authorization.Api.INGESTION;
import static iudx.resource.server.authenticator.authorization.Method.DELETE;
import static iudx.resource.server.authenticator.authorization.Method.GET;
import static iudx.resource.server.authenticator.authorization.Method.POST;
import static iudx.resource.server.authenticator.authorization.Method.PUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.resource.server.authenticator.model.JwtData;

public class DelegateAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(DelegateAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> delegateAuthorizationRules = new HashMap<>();
  static {

    // ingestion access list/rules
    List<AuthorizationRequest> ingestAccessList = new ArrayList<>();
    ingestAccessList.add(new AuthorizationRequest(GET, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(POST, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(DELETE, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(PUT, INGESTION));
    delegateAuthorizationRules.put(IudxAccess.INGESTION.getAccess(), ingestAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    return true;
  }
}

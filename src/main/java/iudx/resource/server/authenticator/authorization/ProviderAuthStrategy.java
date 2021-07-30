package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.authenticator.authorization.Api.ENTITIES;
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

import io.vertx.core.json.JsonArray;
import iudx.resource.server.authenticator.model.JwtData;

public class ProviderAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ProviderAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> providerAuthorizationRules = new HashMap<>();
  static {

    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(POST, ENTITIES));
    providerAuthorizationRules.put("api", apiAccessList);

    // ingestion access list/rules
    List<AuthorizationRequest> ingestAccessList = new ArrayList<>();
    ingestAccessList.add(new AuthorizationRequest(GET, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(POST, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(DELETE, INGESTION));
    ingestAccessList.add(new AuthorizationRequest(PUT, INGESTION));
    providerAuthorizationRules.put("ingest", ingestAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi().getApiEndpoint();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);

    if (!result && access.contains("api")) {
      result = providerAuthorizationRules.get("api").contains(authRequest);
    }
    if (!result && access.contains("ingest")) {  
      result = providerAuthorizationRules.get("ingest").contains(authRequest);
    }

    return result;
  }

}

package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.authenticator.authorization.Api.ENTITIES;
import static iudx.resource.server.authenticator.authorization.Api.ENTITY_OPERATION;
import static iudx.resource.server.authenticator.authorization.Api.ENTITY_OPERATION_TEMPORAL;
import static iudx.resource.server.authenticator.authorization.Api.SUBSCRIPTION;
import static iudx.resource.server.authenticator.authorization.Api.TEMPORAL;
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

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);

  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  static {

    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, ENTITIES));
    apiAccessList.add(new AuthorizationRequest(GET, TEMPORAL));
    apiAccessList.add(new AuthorizationRequest(POST, ENTITY_OPERATION));
    apiAccessList.add(new AuthorizationRequest(POST, ENTITY_OPERATION_TEMPORAL));
    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);

    // subscriptions access list/rules
    List<AuthorizationRequest> subsAccessList = new ArrayList<>();
    subsAccessList.add(new AuthorizationRequest(GET, SUBSCRIPTION));
    subsAccessList.add(new AuthorizationRequest(POST, SUBSCRIPTION));
    subsAccessList.add(new AuthorizationRequest(DELETE, SUBSCRIPTION));
    subsAccessList.add(new AuthorizationRequest(PUT, SUBSCRIPTION));
    consumerAuthorizationRules.put(IudxAccess.SUBSCRIPTION.getAccess(), subsAccessList);
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

    if (!result && access.contains(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }
    if (!result && access.contains(IudxAccess.SUBSCRIPTION.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.SUBSCRIPTION.getAccess()).contains(authRequest);
    }

    return result;
  }

}

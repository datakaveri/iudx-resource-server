package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.apiserver.util.Constants.RESET_PWD;
import static iudx.resource.server.apiserver.util.Constants.SUBSCRIPTION;
import static iudx.resource.server.authenticator.authorization.Method.DELETE;
import static iudx.resource.server.authenticator.authorization.Method.GET;
import static iudx.resource.server.authenticator.authorization.Method.PATCH;
import static iudx.resource.server.authenticator.authorization.Method.POST;
import static iudx.resource.server.authenticator.authorization.Method.PUT;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);
  
  private final boolean isLimitsEnabled;
  private final Api api;
  
  private ConsumerAuthStrategy(boolean isLimitsAllowed,Api api) {
    this.isLimitsEnabled=isLimitsAllowed;
    this.api = api;
    buildPermissions(api);
  }
  public static ConsumerAuthStrategy getInstance(boolean isLimitsEnabled , Api api)
  {
    return new ConsumerAuthStrategy(isLimitsEnabled, api);
  }
  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  private void buildPermissions(Api api) {

    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, api.getEntitiesUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getTemporalUrl()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostEntitiesQueryPath()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostTemporalQueryPath()));
    apiAccessList.add(new AuthorizationRequest(GET,api.getIudxConsumerAuditUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getIudxAsyncSearchApi()));
    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);

    // subscriptions access list/rules
    List<AuthorizationRequest> subsAccessList = new ArrayList<>();
    subsAccessList.add(new AuthorizationRequest(GET, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(POST, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(DELETE, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PUT, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PATCH, api.getSubscriptionUrl()));
    consumerAuthorizationRules.put(IudxAccess.SUBSCRIPTION.getAccess(), subsAccessList);
    
    //management
    List<AuthorizationRequest> mgmtAccessList=new ArrayList<>();
    mgmtAccessList.add(new AuthorizationRequest(POST, RESET_PWD));
    consumerAuthorizationRules.put(IudxAccess.MANAGEMENT.getAccess(), mgmtAccessList);
    
    //async access list
    List<AuthorizationRequest> asyncAccessList=new ArrayList<>();
    asyncAccessList.add(new AuthorizationRequest(POST, api.getIudxAsyncSearchApi()));
    consumerAuthorizationRules.put(IudxAccess.ASYNC.getAccess(), asyncAccessList);
    
  }


  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    LOGGER.debug(access);
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);

    if (!result && access.contains(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }
    if (!result && access.contains(IudxAccess.SUBSCRIPTION.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.SUBSCRIPTION.getAccess()).contains(authRequest);
    }
    if(!result) {
      result=consumerAuthorizationRules.get(IudxAccess.MANAGEMENT.getAccess()).contains(authRequest);
    }
    if(!result) {
      result=consumerAuthorizationRules.get(IudxAccess.ASYNC.getAccess()).contains(authRequest);
    }
    return result;
  }


  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData,
      JsonObject quotaConsumed) {
    JsonArray limitsArray=jwtData.getCons() != null ? jwtData.getCons().getJsonArray("limits"):null;
    boolean isUsageWithinLimits=true;
    if(isLimitsEnabled) {
      isUsageWithinLimits=false;
      //TODO: evaluate allowed vs what consumed
      for(Object jsonObject:limitsArray) {
        JsonObject json=(JsonObject)jsonObject;
        if(json.containsKey("api")) {
          int consumed=quotaConsumed.getInteger("api");
          if(consumed<json.getInteger("api")) {
            isUsageWithinLimits=true;
            
          }
        }
      }
    }
    String withinAllowedLimits=isUsageWithinLimits?"within":"exceeds";
    LOGGER.info("usage limits {} defined limits",withinAllowedLimits);
    return isAuthorized(authRequest, jwtData) && isUsageWithinLimits;
  }

}

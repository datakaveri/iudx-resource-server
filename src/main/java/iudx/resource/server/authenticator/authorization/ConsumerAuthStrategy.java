package iudx.resource.server.authenticator.authorization;

import static iudx.resource.server.apiserver.util.Constants.RESET_PWD;
import static iudx.resource.server.authenticator.authorization.Method.DELETE;
import static iudx.resource.server.authenticator.authorization.Method.GET;
import static iudx.resource.server.authenticator.authorization.Method.PATCH;
import static iudx.resource.server.authenticator.authorization.Method.POST;
import static iudx.resource.server.authenticator.authorization.Method.PUT;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.common.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsumerAuthStrategy implements AuthorizationStrategy {

  private static final Logger LOGGER = LogManager.getLogger(ConsumerAuthStrategy.class);
  static Map<String, List<AuthorizationRequest>> consumerAuthorizationRules = new HashMap<>();
  private static volatile ConsumerAuthStrategy instance;
  private final boolean isLimitsEnabled;

  private ConsumerAuthStrategy(boolean isLimitsAllowed, Api api) {
    this.isLimitsEnabled = isLimitsAllowed;
    buildPermissions(api);
  }

  public static ConsumerAuthStrategy getInstance(boolean isLimitsAllowed, Api api) {
    if (instance == null) {
      synchronized (ConsumerAuthStrategy.class) {
        if (instance == null) {
          instance = new ConsumerAuthStrategy(isLimitsAllowed, api);
        }
      }
    }
    return instance;
  }

  private void buildPermissions(Api api) {

    // api access list/rules
    List<AuthorizationRequest> apiAccessList = new ArrayList<>();
    apiAccessList.add(new AuthorizationRequest(GET, api.getEntitiesUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getTemporalUrl()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostEntitiesQueryPath()));
    apiAccessList.add(new AuthorizationRequest(POST, api.getPostTemporalQueryPath()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getIudxConsumerAuditUrl()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getIudxAsyncSearchApi()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getMonthlyOverview()));
    apiAccessList.add(new AuthorizationRequest(GET, api.getSummaryPath()));
    consumerAuthorizationRules.put(IudxAccess.API.getAccess(), apiAccessList);

    // subscriptions access list/rules
    List<AuthorizationRequest> subsAccessList = new ArrayList<>();
    subsAccessList.add(new AuthorizationRequest(GET, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(POST, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(DELETE, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PUT, api.getSubscriptionUrl()));
    subsAccessList.add(new AuthorizationRequest(PATCH, api.getSubscriptionUrl()));
    consumerAuthorizationRules.put(IudxAccess.SUBSCRIPTION.getAccess(), subsAccessList);

    // management
    List<AuthorizationRequest> mgmtAccessList = new ArrayList<>();
    mgmtAccessList.add(new AuthorizationRequest(POST, RESET_PWD));
    consumerAuthorizationRules.put(IudxAccess.MANAGEMENT.getAccess(), mgmtAccessList);

    // async access list
    List<AuthorizationRequest> asyncAccessList = new ArrayList<>();
    asyncAccessList.add(new AuthorizationRequest(POST, api.getIudxAsyncSearchApi()));
    asyncAccessList.add(new AuthorizationRequest(GET, api.getIudxAsyncStatusApi()));
    consumerAuthorizationRules.put(IudxAccess.ASYNC.getAccess(), asyncAccessList);
  }

  @Override
  public boolean isAuthorized(AuthorizationRequest authRequest, JwtData jwtData) {
    JsonArray access = jwtData.getCons() != null ? jwtData.getCons().getJsonArray("access") : null;
    boolean result = false;
    LOGGER.info(access + "result: {}", result);
    if (access == null) {
      return result;
    }
    String endpoint = authRequest.getApi();
    Method method = authRequest.getMethod();
    LOGGER.info("authorization request for : " + endpoint + " with method : " + method.name());
    LOGGER.info("allowed access : " + access);

    if (!result && access.getJsonObject(0).containsKey(IudxAccess.API.getAccess())) {
      result = consumerAuthorizationRules.get(IudxAccess.API.getAccess()).contains(authRequest);
    }
    if (!result && access.getJsonObject(0).containsKey(IudxAccess.SUBSCRIPTION.getAccess())) {
      result =
          consumerAuthorizationRules.get(IudxAccess.SUBSCRIPTION.getAccess()).contains(authRequest);
    }
    if (!result) {
      result =
          consumerAuthorizationRules.get(IudxAccess.MANAGEMENT.getAccess()).contains(authRequest);
    }
    if (!result) {
      result = consumerAuthorizationRules.get(IudxAccess.ASYNC.getAccess()).contains(authRequest);
    }
    return result;
  }

  @Override
  public boolean isAuthorized(
      AuthorizationRequest authRequest, JwtData jwtData, JsonObject quotaConsumed) {
    JsonObject accessJson =
        jwtData.getCons() != null
            ? jwtData.getCons().getJsonArray("access").getJsonObject(0)
            : null;
    boolean isUsageWithinLimits = true;
    if (isLimitsEnabled) {
      isUsageWithinLimits = false;

      if (accessJson.containsKey("api")) {
        int dataConsumed = quotaConsumed.getInteger("consumed_data");
        int apiCount = quotaConsumed.getInteger("api_count");
        LOGGER.debug("apiCount {} dataConsumed {}", apiCount, dataConsumed);
        if (apiCount < Integer.parseInt(accessJson.getString("api"))
            && dataConsumed < Integer.parseInt(accessJson.getString("sub"))) {
          isUsageWithinLimits = true;
        }
      }
    }
    String withinAllowedLimits = isUsageWithinLimits ? "within" : "exceeds";
    LOGGER.info("usage limits {} defined limits", withinAllowedLimits);
    return isAuthorized(authRequest, jwtData) && isUsageWithinLimits;
  }
}

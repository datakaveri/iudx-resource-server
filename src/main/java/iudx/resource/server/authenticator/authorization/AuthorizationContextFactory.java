package iudx.resource.server.authenticator.authorization;

import iudx.resource.server.common.Api;

public class AuthorizationContextFactory {

  private final AuthorizationStrategy consumerAuth;
  private final AuthorizationStrategy providerAuth;
  private final AuthorizationStrategy delegateAuth;
  private final AuthorizationStrategy adminStrategy;

  public AuthorizationContextFactory(boolean isLimitsEnabled, Api api) {
    consumerAuth = ConsumerAuthStrategy.getInstance(isLimitsEnabled, api);
    providerAuth = ProviderAuthStrategy.getInstance(api);
    delegateAuth = DelegateAuthStrategy.getInstance(api);
    adminStrategy = AdminAuthStrategy.getInstance(api);
  }

  public AuthorizationStrategy create(IudxRole role) {
    switch (role) {
      case CONSUMER:
        {
          return consumerAuth;
        }
      case PROVIDER:
        {
          return providerAuth;
        }
      case DELEGATE:
        {
          return delegateAuth;
        }
      case ADMIN:
        {
          return adminStrategy;
        }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }
}

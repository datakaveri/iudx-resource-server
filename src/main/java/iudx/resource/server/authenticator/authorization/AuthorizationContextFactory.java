package iudx.resource.server.authenticator.authorization;

public class AuthorizationContextFactory {


  private final AuthorizationStrategy consumerAuth;
  private final AuthorizationStrategy providerAuth;
  private final AuthorizationStrategy delegateAuth;
  private final AuthorizationStrategy adminStrategy;

  
  public AuthorizationContextFactory(boolean isLimitsEnabled ) {
    consumerAuth=new ConsumerAuthStrategy(isLimitsEnabled);
    providerAuth=new ProviderAuthStrategy();
    delegateAuth=new DelegateAuthStrategy();
    adminStrategy=new AdminAuthStrategy();
  }
  
  public AuthorizationStrategy create(IudxRole role) {
    switch (role) {
      case CONSUMER: {
        return consumerAuth;
      }
      case PROVIDER: {
        return providerAuth;
      }
      case DELEGATE: {
        return delegateAuth;
      }
      case ADMIN:{
        return adminStrategy;
      }
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}

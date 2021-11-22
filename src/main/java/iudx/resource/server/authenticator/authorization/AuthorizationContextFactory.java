package iudx.resource.server.authenticator.authorization;

public class AuthorizationContextFactory {


  private final static AuthorizationStrategy consumerAuth = new ConsumerAuthStrategy();
  private final static AuthorizationStrategy providerAuth = new ProviderAuthStrategy();
  private final static AuthorizationStrategy delegateAuth = new DelegateAuthStrategy();

  public static AuthorizationStrategy create(IudxRole role) {
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
      default:
        throw new IllegalArgumentException(role + "role is not defined in IUDX");
    }
  }

}

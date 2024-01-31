package iudx.resource.server.apiserver.integrationTests;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.configuration.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import iudx.resource.server.authenticator.TokenSetup;
import static iudx.resource.server.authenticator.TokensForITs.*;
import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.proxy;

/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link org.junit.jupiter.api.extension.ExtendWith}.
 */
public class RestAssuredConfiguration implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        try {
            Vertx vertx = Vertx.vertx();
            Configuration resourceServerConfig = new Configuration();
            JsonObject config = resourceServerConfig.configLoader(3, vertx);
            String testHost = config.getString("host");

            JsonObject config2 = resourceServerConfig.configLoader(1, vertx);
            String authServerHost = config2.getString("authServerHost");

            if (testHost != null) {
                baseURI = "https://" + testHost;
            } else {
                baseURI = "https://localhost";
            }

            /*String testPort = config.getString("httpPort");

            if (testPort != null) {
                port = Integer.parseInt(testPort);
            } else {
                port = 8443;
            }*/
            basePath = "ngsi-ld/v1";
            String dxAuthBasePath = "auth/v1";
            String authEndpoint = "https://"+ authServerHost + "/" + dxAuthBasePath +  "/token";
            String proxyHost = System.getProperty("intTestProxyHost");
            String proxyPort = System.getProperty("intTestProxyPort");

            JsonObject providerCredentials = config2.getJsonObject("clientCredentials").getJsonObject("provider");
            String providerClientId = providerCredentials.getString("clientID");
            String providerClientSecret = providerCredentials.getString("clientSecret");

            JsonObject consumerCredentials = config2.getJsonObject("clientCredentials").getJsonObject("consumer");
            String consumerClientId = consumerCredentials.getString("clientID");
            String consumerClientSecret = consumerCredentials.getString("clientSecret");
            String delegationId = consumerCredentials.getString("delegationId");
            if (proxyHost != null && proxyPort != null) {
                proxy(proxyHost, Integer.parseInt(proxyPort));
            }

            System.out.println("setting up the tokens");
           TokenSetup.setupTokens(authEndpoint, providerClientId, providerClientSecret, consumerClientId, consumerClientSecret, delegationId);

//    String proxyHost = System.getProperty("intTestProxyHost");
//    String proxyPort = System.getProperty("intTestProxyPort");
//
//    if (proxyHost != null && proxyPort != null) {
//      proxy(proxyHost, Integer.parseInt(proxyPort));
//    }
            // Wait for tokens to be available before proceeding
            waitForTokens();

            // System.out.println("done with setting up the tokens");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        enableLoggingOfRequestAndResponseIfValidationFails();
    }
    private void waitForTokens() {
        int maxAttempts = 5;
        int attempt = 0;

        // Keep trying to get tokens until they are available or max attempts are reached
        while ((secureResourceToken == null || adminToken == null || providerToken == null || openResourceToken==null || delegateToken==null || adaptorToken==null) && attempt < maxAttempts) {
            System.out.println("Waiting for tokens to be available. Attempt: " + (attempt + 1));
            // Introduce a delay between attempts
            try {
                Thread.sleep(1000); // Adjust the delay as needed
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            attempt++;
        }

        if (secureResourceToken == null || adminToken == null || providerToken == null || openResourceToken==null || delegateToken==null || adaptorToken==null) {
            // Log an error or throw an exception if tokens are still not available
            throw new RuntimeException("Failed to retrieve tokens after multiple attempts.");
        } else {
            System.out.println("Tokens are now available. Proceeding with RestAssured configuration.");
        }
    }
}

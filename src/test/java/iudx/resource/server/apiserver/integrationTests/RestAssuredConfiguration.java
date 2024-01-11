package iudx.resource.server.apiserver.integrationTests;

import io.vertx.core.Vertx;
import iudx.resource.server.configuration.Configuration;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.authenticator.TokenSetup;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.restassured.RestAssured.*;
import static iudx.resource.server.authenticator.TokensForITs.*;


/**
 * JUnit5 extension to allow {@link RestAssured} configuration to be injected into all integration
 * tests using {@link ExtendWith}. Java properties can be passed in arguments when running the
 * integration tests to configure a host (<code>intTestHost</code>), port (<code>intTestPort</code>
 * ), proxy host (<code>intTestProxyHost</code>) and proxy port (<code>intTestProxyPort</code>).
 */
public class RestAssuredConfiguration implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        Vertx vertx = Vertx.vertx();
        JsonObject config = Configuration.configLoader(3, vertx);
        String testHost = config.getString("host");

        JsonObject config2 = Configuration.configLoader(1, vertx);
        String authServerHost = config2.getString("authServerHost");

        if (testHost != null) {
            baseURI = "https://"+testHost;
        } else {
            baseURI = "http://localhost";
        }

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

        // Wait for tokens to be available before proceeding
        waitForTokens();

        // System.out.println("done with setting up the tokens");

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

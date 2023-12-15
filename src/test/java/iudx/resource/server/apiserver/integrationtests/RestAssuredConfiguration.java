package iudx.resource.server.apiserver.integrationtests;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.configuration.Configuration;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.port;
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        enableLoggingOfRequestAndResponseIfValidationFails();
    }
}

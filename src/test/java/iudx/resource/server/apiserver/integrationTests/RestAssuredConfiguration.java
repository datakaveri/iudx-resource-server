package iudx.resource.server.apiserver.integrationTests;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.enableLoggingOfRequestAndResponseIfValidationFails;
import static io.restassured.RestAssured.proxy;

import io.vertx.core.Vertx;
import iudx.resource.server.configuration.Configuration;
import io.restassured.RestAssured;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;



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

        if (testHost != null) {
            baseURI = "https://"+testHost;
        } else {
            baseURI = "http://localhost";
        }

        /*String testPort = config.getString("httpPort");

        if (testPort != null) {
            port = Integer.parseInt(testPort);
            System.out.println("Inside testPOrtttttt..");
        } else {
            System.out.println("Inside else.....");
            port = 8443;
        }*/
        //baseURI = "http://rs.iudx.io";
        basePath = "ngsi-ld/v1";

//    String proxyHost = System.getProperty("intTestProxyHost");
//    String proxyPort = System.getProperty("intTestProxyPort");
//
//    if (proxyHost != null && proxyPort != null) {
//      proxy(proxyHost, Integer.parseInt(proxyPort));
//    }

        enableLoggingOfRequestAndResponseIfValidationFails();
    }
}

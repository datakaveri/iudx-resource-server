package iudx.resource.server.apiserver.integrationtests.SubscriptionAPIs;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.delegateToken;
import static iudx.resource.server.authenticator.JwtTokenHelper.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class getStreamingSubscriptionIT {
    String subscriptionID="fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions Test 15";
    @Test
    @DisplayName("200 (success) - Get streaming subscription")
    public void getStreamingSubscriptionTest(){
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .when()
                .get("/subscription/"+subscriptionID)
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"));
    }
    @Test
    @DisplayName("401 (not authorized) - Get streaming subscription")
    public void getStreamingSubscriptionWithInvalidTokenTest(){
        String invalidToken="pubilc_1";
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", invalidToken)
                .when()
                .get("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @DisplayName("404 (not found) - Get streaming subscription")
    public void getNonExistingStreamingSubscriptionTest() {
        String nonExistingId = "fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions RS Test-1234invalidId";
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("token", delegateToken)
                .when()
                .get("/subscription/"+nonExistingId)
                .then()
                .log().body()
                .statusCode(404)

                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }



}


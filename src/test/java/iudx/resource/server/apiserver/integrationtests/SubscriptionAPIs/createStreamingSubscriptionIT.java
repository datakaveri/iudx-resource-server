package iudx.resource.server.apiserver.integrationtests.SubscriptionAPIs;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.*;
import static iudx.resource.server.authenticator.JwtTokenHelper.secureResourceToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class createStreamingSubscriptionIT {
    String appName = "Subscriptions Test 15";
    @Test
    @DisplayName("201 (created) - create a streaming subscription RL")
    public void createSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add("83c2e5c2-3574-4e11-9530-2b1fbdfce832"));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"))
              .body("results[0].id", containsString('/'+ appName));
    }
    @Test
    @DisplayName("401 (not authorized) create a streaming subscription")
    public void createSubscriptionWithInvalidTokenTest(){
        String invalidToken="public_1";
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add("83c2e5c2-3574-4e11-9530-2b1fbdfce832"));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
    @Test
    @DisplayName("409 (Already exist) create a streaming subscription")
    public void createAlreadyExistingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add("83c2e5c2-3574-4e11-9530-2b1fbdfce832"));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/subscription")
                .then()
                .statusCode(409)
                .log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Conflict"));
    }
}

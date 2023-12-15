package iudx.resource.server.apiserver.integrationtests.SubscriptionAPIs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.secureResourceToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class updateStreamingSubscriptionIT {
    String appName = "Subscriptions Test 15";
    String inValidAppName="RS-integration-test-alias-RL";
    String inValidToken="public_1";
    String EntityId="83c2e5c2-3574-4e11-9530-2b1fbdfce832";
    String subscriptionID="fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions Test 15";
    @Test
    @DisplayName("201 (created) - Update streaming subscription")
    public void updateStreamingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(EntityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/subscription/"+subscriptionID)
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("success"));
    }
    @Test
    @DisplayName("400 (invalid name) - Update streaming subscription")
    public void updateStreamingSubscriptionWithInvalidNameTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", inValidAppName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(EntityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .put("/subscription/"+subscriptionID)
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidParamameter"))
                .body("title", equalTo("Bad Request"));
    }
    @Test
    @DisplayName("401 (not authorized) Update streaming subscription")
    public void updateStreamingSubscriptionWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(EntityId));
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", inValidToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }
}

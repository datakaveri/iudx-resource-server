package iudx.resource.server.apiserver.integrationtests.SubscriptionAPIs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.*;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class appendStreamingSubscriptionIT {
    String appName = "Subscriptions Test 15";
    String inValidAppName="RS-integration-test-alias-RL";
    String inValidToken="public_1";
    String subscriptionID="fd47486b-3497-4248-ac1e-082e4d37a66c/Subscriptions Test 15";
    JsonArray entitiesArray = new JsonArray()
            .add("83c2e5c2-3574-4e11-9530-2b1fbdfce832")
            .add("b58da193-23d9-43eb-b98a-a103d4b6103c");
    @Test
    @DisplayName("201 (created) - Append streaming subscription")
    public void appendStreamingSubscriptionTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", entitiesArray);
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(201)
                .log().body();
    }
    @Test
    @DisplayName("400 (invalid name) - Append streaming subscription")
    public void appendStreamingSubscriptionWithInvalidNameTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", inValidAppName)
                .put("type", "subscription")
                .put("entities",entitiesArray);
        given()
                .header("Content-Type", "application/json")
                .header("options","streaming")
                .header("token", secureResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidParamameter"))
                .body("title", equalTo("Bad Request"));
    }
    @Test
    @DisplayName("401 (not authorized) append streaming subscription")
    public void appendStreamingSubscriptionWithInvalidTokenTest(){
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", entitiesArray);
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
    @Test
    @DisplayName("404 (not found) - Append streaming subscription")
    public void appendNonExistingStreamingSubscriptionTest() {
        final var invalidEntity = "8b95ab80-2aaf-4636-a65e-7f2563d0d370";
        JsonObject requestBody = new JsonObject()
                .put("name", appName)
                .put("type", "subscription")
                .put("entities", new JsonArray().add(invalidEntity));
        given()
                .header("Content-Type", "application/json")
                .header("options", "streaming")
                .header("token", openResourceToken)
                .body(requestBody.encodePrettily())
                .when()
                .patch("/subscription/"+subscriptionID)
                .then()
                .log().body()
                .statusCode(404)
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"));
    }


}

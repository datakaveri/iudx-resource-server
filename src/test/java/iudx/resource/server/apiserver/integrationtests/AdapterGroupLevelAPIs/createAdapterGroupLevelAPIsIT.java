package iudx.resource.server.apiserver.integrationtests.AdapterGroupLevelAPIs;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.providerToken;
import static org.hamcrest.Matchers.*;

@ExtendWith(RestAssuredConfiguration.class)
public class createAdapterGroupLevelAPIsIT {
    @Test
    @DisplayName("201 (created successfully) - Register adaptor (GL)")
    public void createRegisterAdaptersTest(){
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add("935f2045-f5c6-4c76-b14a-c29a88589bf3"));
        given()
                .header("Content-Type", "application/json")
                .header("token", providerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .body("results", notNullValue())
                .body("results[0].id", notNullValue())
                .body("results[0].username", notNullValue())
                .body("results[0].apiKey", notNullValue());
    }
    @Test
    @DisplayName("409 (already exist) - Register adaptor")
    public void createAlreadyExistingRegisterAdaptersTest(){
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add("935f2045-f5c6-4c76-b14a-c29a88589bf3"));
        given()
                .header("Content-Type", "application/json")
                .header("token", providerToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(409)
                .log().body()
                .body("type", equalTo("urn:dx:rs:resourceAlreadyExist"))
                .body("title", equalTo("Conflict"));
    }
    @Test
    @DisplayName("401(not authorized) register adaptor")
    public void createRegisterAdaptersWithInvalidTokenTest(){
        String invalidToken="public_1";
        JsonObject requestBody = new JsonObject()
                .put("entities", new JsonArray().add("935f2045-f5c6-4c76-b14a-c29a88589bf3"));
        given()
                .header("Content-Type", "application/json")
                .header("token", invalidToken)
                .body(requestBody.encodePrettily())
                .when()
                .post("/ingestion")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"));
    }

}

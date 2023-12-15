package iudx.resource.server.apiserver.integrationtests.AdapterGroupLevelAPIs;
import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.providerToken;
import static org.hamcrest.Matchers.equalTo;


@ExtendWith(RestAssuredConfiguration.class)
public class getAdapterGroupLevelAPIsIT {
    @Test
    @DisplayName("200 (success) - Get adaptor details (GL)")
    public void getRegisterAdaptersTest(){
      String adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3";
        given()
                .header("Content-Type", "application/json")
                .header("token", providerToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .get("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }
    @Test
    @DisplayName("404 (not found) - Get adaptor details")
    public void getNonExistingRegisterAdaptersTest(){
        String nonExisting_adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3-123";
        given()
                .header("Content-Type", "application/json")
                .header("token", providerToken)
                .pathParam("nonExisting_adapter_id_GL", nonExisting_adapter_id_GL)
                .when()
                .get("/ingestion/{nonExisting_adapter_id_GL}")
                .then()
                .statusCode(404)
                .log().body()
                .body("type", equalTo("urn:dx:rs:general"))
                .body("title", equalTo("Not Found"))
                .extract().response();
    }
    @Test
    @DisplayName("401 (not authorized) - Get adaptor details")
    public void getRegisterAdaptersWithInvalidTokenTest(){
        String adapter_id_GL="935f2045-f5c6-4c76-b14a-c29a88589bf3";
        String invalidToken="public_1";
        given()
                .header("Content-Type", "application/json")
                .header("token", invalidToken)
                .pathParam("adapter_id_GL", adapter_id_GL)
                .when()
                .get("/ingestion/{adapter_id_GL}")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }


}

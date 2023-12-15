package iudx.resource.server.apiserver.integrationtests.ComplexCountAPIs;

import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.secureResourceToken;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class complexCountAPIsIT {
    String complexId="83c2e5c2-3574-4e11-9530-2b1fbdfce832";
    String geoproperty = "location";
    String georel = "near;maxDistance=10";
    String geometry = "Point";
    String coordinates = "[21.178,72.834]";
    String timerel = "before";
    String time = "2020-10-19T14:00:00Z";
    String options="count";
    String inValidOperator="speed<<500";

    @Test
    @DisplayName("200 (success) - Search - circle geom + temporal before + response filter")
    public void getComplexCountTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(200)
                .log().body()
                .body("title", equalTo("Success"))
                .body("type", equalTo("urn:dx:rs:success"))
                .body("results[0]", notNullValue())
                .body("results[0].totalHits", notNullValue());
    }
    @Test
    @DisplayName("200 (success) - Search - circle geom + temporal before + response filter with optional encryption")
    public void getComplexCountWithOptionalEncryptionTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(200)
                .log().body()
                .body("title", equalTo("Success"))
                .body("type", equalTo("urn:dx:rs:success"))
                .body("results[0]", notNullValue())
                .body("results[0].totalHits", notNullValue());
    }
    @Test
    @DisplayName("204 (Empty Response) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithEmptyResponseTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", "[31.178,72.834]")
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(204)
                .log().body();
    }
    @Test
    @DisplayName("400 (invalid params) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithInvalidParamsTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geo", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParamameter"));
    }
    @Test
    @DisplayName("400 (invalid geometry type) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithInvalidGeometryTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel",georel)
                .queryParam("geometry", "PointB")
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidGeoValue"));
    }
    @Test
    @DisplayName("404 (not found) - Search - circle geom + temporal before + response filter")
    public void complexCountNotFoundTest(){
        String nonExistingComplexId="83c2e5c2-3574-4e11-9530-2b1fbdfce812";
        given()
                .queryParam("id",nonExistingComplexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(404)
                .log().body()
                .body("title", equalTo("Not Found"))
                .body("type", equalTo("urn:dx:rs:resourceNotFound"));
    }
    @Test
    @DisplayName("401 (not authorized) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithInvalidTokenTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .header("Content-Type", "application/json")
                .header("token", "abc")
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(401)
                .log().body()
                .body("title", equalTo("Not Authorized"))
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"));
    }
    @Test
    @DisplayName("400 (Invalid Operator) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithInvalidOperatorTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("options", options)
                .queryParam("q",inValidOperator)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParamameterValue"));
    }
    @Test
    @DisplayName("400 (Invalid Data format) - Search - circle geom + temporal before + response filter")
    public void getComplexCountWithInvalidDataFormatTest(){
        given()
                .queryParam("id",complexId)
                .queryParam("geoproperty", geoproperty)
                .queryParam("georel", georel)
                .queryParam("geometry", geometry)
                .queryParam("coordinates", coordinates)
                .queryParam("timerel", timerel)
                .queryParam("time", "2020-09-19X14:00:00X")
                .queryParam("options", options)
                .queryParam("q",inValidOperator)
                .header("Content-Type", "application/json")
                .header("token", secureResourceToken)
                .when()
                .get("/temporal/entities")
                .then()
                .statusCode(400)
                .log().body()
                .body("title", equalTo("Bad Request"))
                .body("type", equalTo("urn:dx:rs:invalidParamameterValue"));
    }
}

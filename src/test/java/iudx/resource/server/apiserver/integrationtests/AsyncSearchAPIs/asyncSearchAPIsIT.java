package iudx.resource.server.apiserver.integrationtests.AsyncSearchAPIs;

import iudx.resource.server.apiserver.integrationtests.RestAssuredConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static io.restassured.RestAssured.given;
import static iudx.resource.server.authenticator.JwtTokenHelper.openResourceToken;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(RestAssuredConfiguration.class)
public class asyncSearchAPIsIT {
    String asyncSearchId="b58da193-23d9-43eb-b98a-a103d4b6103c";
    String searchId="59fef571-274e-4b74-acec-9008cc4caa8e";
    String timerel="between";
    String time="2020-10-10T14:20:00Z";
    String endTime="2020-10-13T14:20:00Z";
    String format="csv";
    @Test
    @DisplayName("201 (Success) Async Search")
    public void getAsyncSearchSuccessTest(){
        given()
                .queryParam("id", asyncSearchId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("query submitted successfully"))
                .extract().response();
    }
    @Test
    @DisplayName("201 (Success) Async Search with csv format")
    public void getAsyncSearchWithCSVFormatSuccessTest(){
        given()
                .queryParam("id", asyncSearchId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("format",format)
                .header("token", openResourceToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("query submitted successfully"))
                .extract().response();
    }
    @Test
    @DisplayName("201 (Success) Async Search With Optional Encryption")
    public void getAsyncSearchWithOptionalEncryptionSuccessTest(){
        given()
                .queryParam("id", asyncSearchId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(201)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("query submitted successfully"))
                .extract().response();
    }
    @Test
    @DisplayName("400 (Bad request) Async Search")
    public void getAsyncSearchWithBadRequestTest(){
        given()
                .queryParam("id", asyncSearchId)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(400)
                .log().body()
                .body("type", equalTo("urn:dx:rs:badRequest"))
                .body("title", equalTo("Bad Request"))
                .extract().response();
    }
    @Test
    @DisplayName("401 (not authorized) Async Search")
    public void getAsyncSearchWithInvalidTokenTest(){
        String invalidToken="abcd";
        given()
                .queryParam("id", asyncSearchId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("token",invalidToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(401)
                .log().body()
                .body("type", equalTo("urn:dx:rs:invalidAuthorizationToken"))
                .body("title", equalTo("Not Authorized"))
                .extract().response();
    }
    @Test
    @DisplayName("404 (not found) Async Search")
    public void getNonExistingAsyncSearchTest(){
        String nonExistingAsyncSearchId="b58da193-23d9-43eb-b98a-a103d4b6103e";
        given()
                .queryParam("id", nonExistingAsyncSearchId)
                .queryParam("timerel", timerel)
                .queryParam("time", time)
                .queryParam("endtime", endTime)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/async/search")
                .then()
                .statusCode(404)
                .log().body()
                .body("type", equalTo("urn:dx:rs:resourceNotFound"))
                .body("title", equalTo("Not Found"))
                .extract().response();
    }
    @Test
    @DisplayName("200 (Success) Async Status")
    public void getAsyncSearchStatusSuccessTest(){
        given()
                .queryParam("searchID", searchId)
                .header("Content-Type", "application/json")
                .header("token", openResourceToken)
                .when()
                .get("/async/status")
                .then()
                .statusCode(200)
                .log().body()
                .body("type", equalTo("urn:dx:rs:success"))
                .body("title", equalTo("Success"))
                .extract().response();
    }

}

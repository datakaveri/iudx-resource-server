package iudx.resource.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);
  private static JsonObject authConfig;
  private static Vertx vertxObj;
  private static AuthenticationService authenticationService;
  private static Configuration config;

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void initialize(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {
    vertxObj = vertx;
    config = new Configuration();
    authConfig = config.configLoader(1, vertx2);

    WebClient client = AuthenticationVerticle.createWebClient(vertxObj, authConfig, true);
    authenticationService = new AuthenticationServiceImpl(vertxObj, client, authConfig);
    logger.info("Auth tests setup complete");
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test if webClient has been initialized properly")
  public void testWebClientSetup(VertxTestContext testContext) {
    WebClient client = AuthenticationVerticle.createWebClient(vertxObj, authConfig, true);
    String host = authConfig.getString(Constants.AUTH_SERVER_HOST);
    client.post(443, host, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
      if (httpResponseAsyncResult.failed()) {
        logger.error("Cert info call failed");
        testContext.failNow(httpResponseAsyncResult.cause());
        return;
      }
      logger.info("Cert info call to auth server succeeded");
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    logger.info("Default test is passing");
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test the happy path without any caching")
  public void testHappyPath(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("ids", new JsonArray().add(authConfig.getString("testResourceID")));
    JsonObject authInfo = new JsonObject().put("token", authConfig.getString("testAuthToken"))
        .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
    authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
      if (asyncResult.failed()) {
        logger.error("Unexpected failure");
        testContext.failNow(asyncResult.cause());
        return;
      }
      JsonObject result = asyncResult.result();
      if (result.containsKey("status")) {
        testContext.failNow(new Throwable("Unexpected result"));
        return;
      }
      logger.info("Happy path test without caching success");
      testContext.completeNow();
    });
  }

  @Test
  @DisplayName("Test the happy path with TIP caching")
  public void testHappyPathTipCache(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("ids", new JsonArray().add(authConfig.getString("testResourceID")));
    JsonObject authInfo = new JsonObject().put("token", authConfig.getString("testAuthToken"))
        .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
    authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
      if (asyncResult.failed()) {
        logger.error("Unexpected failure");
        testContext.failNow(asyncResult.cause());
        return;
      }
      JsonObject result = asyncResult.result();
      if (result.containsKey("status")) {
        testContext.failNow(new Throwable("Unexpected result"));
        return;
      }

      JsonObject authInfo2 = new JsonObject().put("token", authConfig.getString("testAuthToken"))
          .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
      authenticationService.tokenInterospect(request, authInfo2, asyncResult2 -> {
        if (asyncResult2.failed()) {
          logger.error("Unexpected failure");
          testContext.failNow(asyncResult2.cause());
          return;
        }
        JsonObject result2 = asyncResult2.result();
        if (result.containsKey("status")) {
          testContext.failNow(new Throwable("Unexpected result"));
          return;
        }
        logger.info("Happy path test with caching success");
        testContext.completeNow();
      });
    });
  }
  
  @Test
  @DisplayName("Test valid token(token for entities) with invalid path(/subscription)")
  public void testInvalidPathAccess(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/alias"));;
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testAuthToken"))
            .put("apiEndpoint", Constants.CLOSED_ENDPOINTS.get(1))
            .put("method","GET")
            .put("id","iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/alias");
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @DisplayName("Test secure item with invalid token")
  public void testSecureItemAccessInvalidToken(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));;
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testExpiredAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        String response=handler.cause().getMessage();
        assertTrue(response.contains("Invalid 'token'"));
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @DisplayName("Test secure item with valid token")
  public void testSecureItemAccessValidToken(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));;
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        testContext.failNow(handler.cause());
      }else {
        testContext.completeNow();
      }
    });
  }
  
  @Test
  @DisplayName("Test secure+open item with invalid token")
  public void testSecureAndValidItemAccessInValidToken(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta")
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"));
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testExpiredAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        String result=handler.cause().getMessage();
        assertTrue(result.contains("Invalid 'token'"));
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @DisplayName("Test secure+open item with valid token")
  public void testSecureAndValidItemAccessValidToken(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta")
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"));
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.succeeded()) {
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  
  @Test
  @DisplayName("Test invalid(not found in cat server) secure item")
  public void testInvalidSecureItem(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta_invalid"));;
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        String response=handler.cause().getMessage();
        assertTrue(response.contains("Not Found"));
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }
  
  @Test
  @DisplayName("Test invalid(not found in cat server) open item")
  public void testInvalidOpenItem(VertxTestContext testContext) {
    JsonObject request=new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055_invalid"));;
    JsonObject authInfo =
        new JsonObject()
            .put("token", authConfig.getString("testAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(1));
    authenticationService.tokenInterospect(request, authInfo, handler->{
      if(handler.failed()) {
        String response=handler.cause().getMessage();
        assertTrue(response.contains("Not Found"));
        testContext.completeNow();
      }else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("Test expired token for failure")
  public void testExpiredToken(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("ids", new JsonArray()
        .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"));
    JsonObject authInfo =
        new JsonObject().put("token", authConfig.getString("testExpiredAuthToken"))
            .put("apiEndpoint", Constants.OPEN_ENDPOINTS.get(0));
    authenticationService.tokenInterospect(request, authInfo, asyncResult -> {
      if (asyncResult.failed()) {
        logger.info("Expired token TIP failed properly");
        testContext.completeNow();
        testContext.failNow(asyncResult.cause());
        return;
      } else {
        testContext.failNow(asyncResult.cause());
        return;
      }
    });
  }

  @Test
  @DisplayName("Test CAT resource group ID API")
  public void testCatAPI(Vertx vertx, VertxTestContext testContext) {
    int catPort = Integer.parseInt(authConfig.getString("catServerPort"));
    String catHost = authConfig.getString("catServerHost");
    String catPath = Constants.CAT_RSG_PATH;
    String groupID =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood";

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    WebClient client = WebClient.create(vertx, options);
    client.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + groupID + "]]")
        .addQueryParam("filter", "[accessPolicy]").expect(ResponsePredicate.JSON)
        .send(httpResponseAsyncResult -> {
          if (httpResponseAsyncResult.failed()) {
            testContext.failNow(httpResponseAsyncResult.cause());
            return;
          }
          HttpResponse<Buffer> response = httpResponseAsyncResult.result();
          if (response.statusCode() != HttpStatus.SC_OK) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }
          JsonObject responseBody = response.bodyAsJsonObject();
          if (!responseBody.getString("status").equals("success")) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }
          String resourceACL = responseBody.getJsonArray("results").getJsonObject(0)
              .getString("accessPolicy");
          if (!resourceACL.equals("OPEN")) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }
          testContext.completeNow();
        });
  }


  @Test
  @DisplayName("Test CAT resource group invalid ID API")
  public void testCatAPIWithInvalidID(Vertx vertx, VertxTestContext testContext) {
    int catPort = Integer.parseInt(authConfig.getString("catServerPort"));
    String catHost = authConfig.getString("catServerHost");
    String catPath = Constants.CAT_RSG_PATH;
    String groupID =
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/invalid";

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true).setVerifyHost(false).setSsl(true);
    WebClient client = WebClient.create(vertx, options);
    client.get(catPort, catHost, catPath).addQueryParam("property", "[id]")
        .addQueryParam("value", "[[" + groupID + "]]")
        .addQueryParam("filter", "[resourceAuthControlLevel]").expect(ResponsePredicate.JSON)
        .send(httpResponseAsyncResult -> {
          if (httpResponseAsyncResult.failed()) {
            testContext.failNow(httpResponseAsyncResult.cause());
            return;
          }
          HttpResponse<Buffer> response = httpResponseAsyncResult.result();
          if (response.statusCode() != HttpStatus.SC_OK) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }
          JsonObject responseBody = response.bodyAsJsonObject();
          if (!responseBody.getString("status").equals("success")) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }

          if (responseBody.containsKey("resourceAuthControlLevel")) {
            testContext.failNow(new Throwable(response.bodyAsString()));
            return;
          }
          testContext.completeNow();
        });
  }

}

package iudx.resource.server.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.configuration.Configuration;

@ExtendWith(VertxExtension.class)
public class JwtAuthServiceImplTest {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static Configuration config;

  private static String jwt =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiIzNDliNGI1NS0wMjUxLTQ5MGUtYmVlOS0wMGYzYTVkM2U2NDMiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc0MDg4NjUsImlhdCI6MTYyNzM2NTY2NSwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJjb25zdW1lciIsImNvbnMiOnsiYWNjZXNzIjpbImFwaSIsInN1YnMiLCJpbmdlc3QiLCJmaWxlIl19fQ.C27Rtn5cFQRwGmphs5lmmZVHSNgOVqcG7YvV7vQ-BnxgDydVHPnT9D7iXf0bceXZqcUijtbEvt7QDNeIYMyeyw";

  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    config = new Configuration();
    authConfig = config.configLoader(1, vertx);



    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
        new PubSecKeyOptions()
            .setAlgorithm("ES256")
            .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                "-----END PUBLIC KEY-----\n" +
                ""));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    WebClient webClient = AuthenticationVerticle.createWebClient(vertx, authConfig, true);
    jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx, jwtAuth, webClient, authConfig);


    // authenticationService = new AuthenticationServiceImpl(vertxObj, client, authConfig);
    LOGGER.info("Auth tests setup complete");
    testContext.completeNow();
  }


  @Test
  @DisplayName("Testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("Default test is passing");
    testContext.completeNow();
  }


  @Disabled
  @Test
  @DisplayName("success - allow access to all open endpoints")
  public void allow4OpenEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("ri:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject());



    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }


  @Disabled
  @Test
  @DisplayName("success - allow consumer access to /entities endpoint")
  public void success4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "POST");

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }


  @Disabled
  @Test
  @DisplayName("success - allow consumer access to /subscription endpoint")
  public void success4ConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Disabled
  @Test
  @DisplayName("success - allow consumer access to /entities endpoint")
  public void allow4ConsumerTokenIngestAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }



  @Disabled("test once have a provider token")
  @Test
  @DisplayName("decode valid jwt")
  public void decodeJwtProviderSuccess(VertxTestContext testContext) {
    String jwt =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
    jwtAuthenticationService.decodeJwt(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals("provider",handler.result().getRole());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("decode valid jwt")
  public void decodeJwtDelegateSuccess(VertxTestContext testContext) {
    String jwt =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
    jwtAuthenticationService.decodeJwt(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        assertEquals( "delegate",handler.result().getRole());
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

 
  @Test
  @DisplayName("decode invalid jwt")
  public void decodeJwtFailure(VertxTestContext testContext) {
    String jwt =
        "eyJ0eXAiOiJKV1QiLCJbGciOiJFUzI1NiJ9.eyJzdWIiOiJhM2U3ZTM0Yy00NGJmLTQxZmYtYWQ4Ni0yZWUwNGE5NTQ0MTgiLCJpc3MiOiJhdXRoLnRlc3QuY29tIiwiYXVkIjoiZm9vYmFyLml1ZHguaW8iLCJleHAiOjE2Mjc2ODk5NDAsImlhdCI6MTYyNzY0Njc0MCwiaWlkIjoicmc6ZXhhbXBsZS5jb20vNzllN2JmYTYyZmFkNmM3NjViYWM2OTE1NGMyZjI0Yzk0Yzk1MjIwYS9yZXNvdXJjZS1ncm91cCIsInJvbGUiOiJkZWxlZ2F0ZSIsImNvbnMiOnt9fQ.eJjCUvWuGD3L3Dn2fKj8Ydl1byGoyRS59VfL6ZJcdKR3_eIhm6SOY-CW3p5XDSYVhRTlWvlPLjfXYo9t_PxgnA";
    jwtAuthenticationService.decodeJwt(jwt).onComplete(handler -> {
      if (handler.succeeded()) {
        // assertEquals(handler.result().getRole(), "delegate");;
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();

      }
    });
  }

  @Test
  @DisplayName("success - allow consumer access to /entities endpoint for access [api,subs]")
  public void access4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("subs")));


    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @DisplayName("failure - consumer access to /entities endpoint for access [api]")
  public void access4ConsumerTokenEntitiesPostAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - consumer access to /subscription endpoint for access [api,subs]")
  public void access4ConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("subs")));


    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @DisplayName("failure - consumer access to /subscription endpoint for access [api]")
  public void access4ConsumerTokenSubsAPIFailure(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("failure - consumer access to /ingestion endpoint for access [api]")
  public void access4ConsumerTokenIngestAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        LOGGER.debug("failed access ");
        testContext.completeNow();
      }
    });
  }


  @Test
  @DisplayName("failure - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access provided");
      } else {
        LOGGER.debug("failed access ");
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenIngestionPostAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        LOGGER.debug("failed access ");
        testContext.failNow("failed for provider");

      }
    });
  }


  @Test
  @DisplayName("success - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenIngestionGetAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", jwt);
    authInfo.put("id", "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        LOGGER.debug("failed access ");
        testContext.failNow("failed for provider");
      }
    });
  }


  @Test
  @DisplayName("success - validId check")
  public void validIdCheck4JwtToken(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
        .isValidId(jwtData, "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053")
        .onComplete(handler -> {
          if (handler.succeeded()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }


  @Test
  @DisplayName("failure - invalid validId check")
  public void invalidIdCheck4JwtToken(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
        .isValidId(jwtData, "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR055")
        .onComplete(handler -> {
          if (handler.succeeded()) {
            testContext.failNow("fail");
          } else {
            testContext.completeNow();
          }
        });
  }
}

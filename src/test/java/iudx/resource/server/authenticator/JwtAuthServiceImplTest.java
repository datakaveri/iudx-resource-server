package iudx.resource.server.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import io.micrometer.core.ipc.http.HttpSender.Method;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.metering.MeteringService;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceImplTest {
  @Mock
  HttpRequest<Buffer> httpRequest;
  @Mock
  HttpResponse<Buffer> httpResponse;

  @Mock
  AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock
  HttpRequest<Buffer> httpRequestMock;
  @Mock
  HttpResponse<Buffer> httpResponseMock;



  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static Configuration config;
  private static String openId;
  private static String closeId;
  private static String invalidId;
  private static PostgresService pgService;
  private static CacheService cacheService;
  private static MeteringService meteringService;
  private static Api apis;
  private static String dxApiBasePath;


  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    config = new Configuration();
    authConfig = config.configLoader(1, vertx);
    authConfig.put("dxApiBasePath","/ngsi-ld/v1");

    dxApiBasePath = "/ngsi-ld/v1";
    apis = Api.getInstance(dxApiBasePath);
    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
            new PubSecKeyOptions()
                    .setAlgorithm("ES256")
                    .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                            "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                            "-----END PUBLIC KEY-----\n" +
                            ""));
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only
    // for
    // test
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    cacheService = Mockito.mock(CacheService.class);
    meteringService=Mockito.mock(MeteringService.class);
    WebClient webClient = AuthenticationVerticle.createWebClient(vertx, authConfig, true);
    jwtAuthenticationService =
            new JwtAuthenticationServiceImpl(vertx, jwtAuth, webClient, authConfig, cacheService,meteringService,apis);

    // since test token doesn't contains valid id's, so forcibly put some dummy id in cache
    // for
    // test.
    openId =
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood";
    closeId =
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information";
    invalidId = "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group1";

    jwtAuthenticationService.resourceIdCache.put(openId, "OPEN");
    jwtAuthenticationService.resourceIdCache.put(closeId, "CLOSED");
    jwtAuthenticationService.resourceIdCache.put(invalidId, "CLOSED");
    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(cacheService).get(any(), any());


    LOGGER.info("Auth tests setup complete");

    testContext.completeNow();


  }


  @Test
  @DisplayName("Testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("Default test is passing");
    testContext.completeNow();
  }


  @Test
  @DisplayName("success - allow access to all open endpoints")
  public void allow4OpenEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("ri:foobar.iudx.io");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));



    jwtAuthenticationService.validateAccess(jwtData, true, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }

  @Test
  @DisplayName("success - allow access to closed endpoint")
  public void allow4ClosedEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();


    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("invalid access");
      }
    });
  }


  @Test
  @DisplayName("success - disallow access to closed endpoint for different id")
  public void disallow4ClosedEndpoint(VertxTestContext testContext) {
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", invalidId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    JsonObject request = new JsonObject();


    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow("invalid access");
      } else {
        testContext.completeNow();
      }
    });
  }


  @Test
  @DisplayName("success - allow consumer access to /entities endpoint")
  public void success4ConsumerTokenEntitiesAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }


  @Test
  @DisplayName("success - allow consumer access to /subscription endpoint")
  public void success4ConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", openId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("success - allow delegate access to /ingestion endpoint")
  public void allow4DelegateTokenIngestAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedDelegateIngestToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getIngestionPath());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("failure - provider role -> subscription access")
  public void providerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("success - consumer role -> subscription access")
  public void closedConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedConsumerApiSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint",  apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("success - consumer role -> subscription access")
  public void openConsumerTokenSubsAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", openId);
    authInfo.put("apiEndpoint", apis.getSubscriptionUrl());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("failure - provider role -> api access")
  public void closeProviderTokenApiAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getEntitiesUrl());
    authInfo.put("method", Method.GET);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("success - provider role -> ingest access")
  public void closeProviderTokenIngestAPI(VertxTestContext testContext) {

    JsonObject request = new JsonObject();
    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderIngestToken);
    authInfo.put("id", closeId);
    authInfo.put("apiEndpoint", apis.getIngestionPath());
    authInfo.put("method", Method.POST);

    jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }



  @Test
  @DisplayName("decode valid jwt")
  public void decodeJwtProviderSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedProviderApiToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("provider", handler.result().getRole());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("decode valid jwt - delegate")
  public void decodeJwtDelegateSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedDelegateApiToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("delegate", handler.result().getRole());
                testContext.completeNow();
              } else {
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("decode valid jwt - consumer")
  public void decodeJwtConsumerSuccess(VertxTestContext testContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.closedConsumerApiSubsToken)
            .onComplete(handler -> {
              if (handler.succeeded()) {
                assertEquals("consumer", handler.result().getRole());
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

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));


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

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
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

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("consumer");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api").add("sub")));


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

    authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/subscription");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
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

    authInfo.put("token", JwtTokenHelper.openConsumerApiToken);
    authInfo.put("id",
            "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
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

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/entities");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("api")));


    jwtAuthenticationService.validateAccess(jwtData, false, authInfo).onComplete(handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      } else {
        testContext.failNow("provider not provided access to API");
      }
    });
  }

  @Test
  @DisplayName("success - provider access to /entities endpoint for access [api]")
  public void access4ProviderTokenIngestionPostAPI(VertxTestContext testContext) {

    JsonObject authInfo = new JsonObject();

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "POST");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingestion")));


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

    authInfo.put("token", JwtTokenHelper.closedProviderApiToken);
    authInfo.put("id", "example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    authInfo.put("apiEndpoint", "/ngsi-ld/v1/ingestion");
    authInfo.put("method", "GET");

    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("rs.iudx.io");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid("rg:example.com/79e7bfa62fad6c765bac69154c2f24c94c95220a/resource-group");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingestion")));


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
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
            .isValidId(jwtData,
                    "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053")
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
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));

    jwtAuthenticationService
            .isValidId(jwtData,
                    "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR055")
            .onComplete(handler -> {
              if (handler.succeeded()) {
                testContext.failNow("fail");
              } else {
                testContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("failure - invalid audience")
  public void invalidAudienceCheck(VertxTestContext testContext) {
    JwtData jwtData = new JwtData();
    jwtData.setIss("auth.test.com");
    jwtData.setAud("abc.iudx.io1");
    jwtData.setExp(1627408865);
    jwtData.setIat(1627408865);
    jwtData.setIid(
            "rg:datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject().put("access", new JsonArray().add("ingest")));
    jwtAuthenticationService.isValidAudienceValue(jwtData).onComplete(handler -> {
      if (handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("fail");

      }
    });
  }
  @Test
  @DisplayName("Testing Success for isItemExist method with List of String IDs")
  public void testIsItemExistSuccess(VertxTestContext vertxTestContext)
  {
    JwtAuthenticationServiceImpl.catWebClient=mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(httpResponse);
    String id="abcd/*";
    JsonObject responseJSonObject = new JsonObject();
    responseJSonObject.put("status","success");
    responseJSonObject.put("totalHits", 10);
    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.isItemExist(id).onComplete(handler -> {
      if (handler.succeeded())
      {
        assertTrue(handler.result());
        vertxTestContext.completeNow();
      }
      else
      {
        vertxTestContext.failNow(handler.cause());
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isItemExist method with List of String IDs")
  public void testIsItemExistFailure(VertxTestContext vertxTestContext)
  {
    JwtAuthenticationServiceImpl.catWebClient= mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    String id="abcd/*";
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.isItemExist(id).onComplete(handler -> {
      if (handler.succeeded())
      {
        vertxTestContext.failNow(handler.cause());
      }
      else
      {
        vertxTestContext.completeNow();
      }
    });

  }
  @Test
  @DisplayName("Test isOpenResource method for Cache miss for Valid Group ID")
  public void testIsOpenResource(VertxTestContext vertxTestContext)
  {

    AsyncResult<HttpResponse<Buffer>> asyncResultMock = mock(AsyncResult.class);

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("accessPolicy", "Dummy Access Policy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0,jsonObject2);

    JsonObject jsonObject = new JsonObject();
    jsonObject.put("type", "urn:dx:cat:Success");
    jsonObject.put("totalHits", 3);
    jsonObject.put("results",jsonArray);
    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";


    jwtAuthenticationService.catWebClient = mock(WebClient.class);

    when(jwtAuthenticationService.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.addQueryParam(anyString(),anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.expect(any())).thenReturn(httpRequestMock);
    when(asyncResultMock.result()).thenReturn(httpResponseMock);
    when(httpResponseMock.statusCode()).thenReturn(200);

    when(httpResponseMock.bodyAsJsonObject()).thenReturn(jsonObject);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>(){
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable{
        (  (Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResultMock);
        return null;
      }
    }).when(httpRequestMock).send(any());
    jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
      if(openResourceHandler.succeeded()) {
        assertEquals("Dummy Access Policy",openResourceHandler.result());
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow("open resource validation failed : " + openResourceHandler.cause());

      }
    });
  }
  @Test
  @DisplayName("Test isOpenResource method for Cache miss with 0 total hits")
  public void testIsOpenResourceWith0TotalHits(VertxTestContext vertxTestContext)
  {

    AsyncResult<HttpResponse<Buffer>> asyncResultMock = mock(AsyncResult.class);

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("accessPolicy", "Dummy Access Policy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0,jsonObject2);
    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("type", "urn:dx:cat:Success");
    jsonObject.put("totalHits", 0);
    jsonObject.put("results",jsonArray);
    jwtAuthenticationService.catWebClient = mock(WebClient.class);
    when(jwtAuthenticationService.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.addQueryParam(anyString(),anyString())).thenReturn(httpRequestMock);
    when(httpRequestMock.expect(any())).thenReturn(httpRequestMock);
    when(asyncResultMock.result()).thenReturn(httpResponseMock);
    when(httpResponseMock.statusCode()).thenReturn(200);

    when(httpResponseMock.bodyAsJsonObject()).thenReturn(jsonObject);

    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>(){
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable{
        (  (Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResultMock);
        return null;
      }
    }).when(httpRequestMock).send(any());

    jwtAuthenticationService.isOpenResource(id).onComplete(openResourceHandler -> {
      if(openResourceHandler.succeeded()) {
        vertxTestContext.failNow("open resource validation failed : " + openResourceHandler.cause());
      } else {
        assertNull(openResourceHandler.result());
        vertxTestContext.completeNow();
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testIsResourceExistFailure(VertxTestContext vertxTestContext)
  {
    String id="Dummy id";
    String groupACL="Dummy id";
    JsonObject responseJSonObject=new JsonObject();
    responseJSonObject.put("type","urn:dx:cat:Success");
    responseJSonObject.put("totalHits", 10);
    JwtAuthenticationServiceImpl.catWebClient=mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    when(httpResponse.statusCode()).thenReturn(400);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.isResourceExist(id,groupACL).onComplete(handler -> {
      if (handler.succeeded())
      {
        vertxTestContext.failNow("false");
      }
      else
      {
        vertxTestContext.completeNow();
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testIsResourceExistFailure2(VertxTestContext vertxTestContext)
  {
    String id="Dummy id";
    String groupACL="Dummy id";
    JsonObject responseJSonObject=new JsonObject();
    responseJSonObject.put("type","dummy");
    responseJSonObject.put("totalHits", 10);
    JwtAuthenticationServiceImpl.catWebClient=mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    when(httpResponse.statusCode()).thenReturn(200);
    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.isResourceExist(id,groupACL).onComplete(handler -> {
      if (handler.succeeded())
      {
        vertxTestContext.failNow("Not Found");
      }
      else
      {
        vertxTestContext.completeNow();
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testIsResourceExistFailure3(VertxTestContext vertxTestContext) {
    String id = "Dummy id";
    String groupACL = "Dummy id";
    String resourceACL="SECURE";
    JsonObject responseJSonObject = new JsonObject();
    JsonArray jsonarray=new JsonArray();
    responseJSonObject.put("type", "wrong type");
    responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
    JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    when(httpResponse.statusCode()).thenReturn(200);


    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.isResourceExist(id, groupACL).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow("Not Found");
      } else {
        vertxTestContext.completeNow();
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testGroupAccessPolicyFailure2(VertxTestContext vertxTestContext) {
    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";    String groupACL = "Dummy id";
    String resourceACL="SECURE";
    JsonObject responseJSonObject = new JsonObject();
    JsonArray jsonarray=new JsonArray();
    responseJSonObject.put("type", "urn:dx:cat:Success");
    responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
    JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.result()).thenReturn(httpResponse);
//    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    when(httpResponse.statusCode()).thenReturn(400);


    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.getGroupAccessPolicy(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow("Not Found");
      } else {
        vertxTestContext.completeNow();
      }
    });
  }
  @Test
  @DisplayName("Testing Failure for isResourceExist method with List of String IDs")
  public void testGroupAccessPolicyFailure(VertxTestContext vertxTestContext) {
    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR053";    String groupACL = "Dummy id";
    String resourceACL="SECURE";
    JsonObject responseJSonObject = new JsonObject();
    JsonArray jsonarray=new JsonArray();
    responseJSonObject.put("type", "wrong type");
    responseJSonObject.put("totalHits", 10);
//   resourceACL=responseJSonObject.getJsonArray("results").getJsonObject(0).getString("accessPolicy");
    JwtAuthenticationServiceImpl.catWebClient = mock(WebClient.class);
    when(JwtAuthenticationServiceImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.expect(any())).thenReturn(httpRequest);
    when(asyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    when(httpResponse.statusCode()).thenReturn(200);


    doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
      @Override
      public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

        ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
        return null;
      }
    }).when(httpRequest).send(any());
    jwtAuthenticationService.getGroupAccessPolicy(id).onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow("Not Found");
      } else {
        vertxTestContext.completeNow();
      }
    });
  }


}

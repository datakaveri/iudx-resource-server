package iudx.resource.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.authenticator.model.JwtData;
import iudx.resource.server.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtAuthServiceTest {
    @Mock
    JwtData jwtData;
    @Mock
    JWTAuth jwtAuth1;
    @Mock
    WebClient webClient;
    @Mock
    JsonObject config;
    @Mock
    CacheService cacheService;
    @Mock
    Throwable throwable;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    JwtAuthenticationServiceImpl jwtAuthenticationService;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
//        jwtAuthenticationService.jwtDecodeFuture = mock(Future.class);

        jwtAuthOptions.addPubSecKey(
                new PubSecKeyOptions()
                        .setAlgorithm("ES256")
                        .setBuffer("-----BEGIN PUBLIC KEY-----\n" +
                                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE8BKf2HZ3wt6wNf30SIsbyjYPkkTS\n" +
                                "GGyyM2/MGF/zYTZV9Z28hHwvZgSfnbsrF36BBKnWszlOYW0AieyAUKaKdg==\n" +
                                "-----END PUBLIC KEY-----\n" +
                                ""));
        jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);// ignore token expiration only

        JWTAuth jwtAuth = JWTAuth.create(Vertx.vertx(), jwtAuthOptions);
        when(config.getString(anyString())).thenReturn("Dummy String");
        when(config.getInteger(anyString())).thenReturn(8443);
        jwtAuthenticationService = new JwtAuthenticationServiceImpl(Vertx.vertx(),jwtAuth,webClient,config,cacheService);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test isRevokedClientToken method : Failure")
    public void testRevokedClientToken(VertxTestContext vertxTestContext)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("value", "2022-06-13T17:20:00.330");
        when(jwtData.getSub()).thenReturn("Dummy ID");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(jsonObject);
        when(jwtData.getIat()).thenReturn(3000);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(cacheService).get(any(),any());

        jwtAuthenticationService.isRevokedClientToken(jwtData).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"401\":\"revoked token passes\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test isRevokedClientToken method : Success")
    public void testRevokedClientTokenSuccess(VertxTestContext vertxTestContext)
    {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("value", "1022-06-13T17:20:00.330");
        when(jwtData.getSub()).thenReturn("Dummy ID");
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(jsonObject);
        when(jwtData.getIat()).thenReturn(952097820);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(jwtAuthenticationService.cache).get(any(),any());

        jwtAuthenticationService.isRevokedClientToken(jwtData).onComplete(handler -> {
            if(handler.succeeded())
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

//    @ParameterizedTest
//    @ValueSource(strings = {"/ngsi-ld/v1/subscription",
//            "GET", "DELETE","/management/user/resetPassword",
//            "/ngsi-ld/v1/consumer/audit","/admin/revokeToken",
//            "/admin/resourceattribute","/ngsi-ld/v1/provider/audit",
//            "/ngsi-ld/v1/async/status"
//    })
//    @DisplayName("Test tokenIntrospect method : ")
//    public void testTokenIntrospect(String str,VertxTestContext vertxTestContext)
//    {
//        String openId = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood";
////        Future<User> userFuture = mock(Future.class);
////        when(config.getString(anyString())).thenReturn(str);
////        when(jwtAuth.authenticate(any(TokenCredentials.class))).thenReturn(userFuture);
////        when(userFuture.onSuccess(any())).thenReturn(userFuture);
////        jwtAuthenticationService.tokenInterospect(config,config, handler -> {
////            if(handler.succeeded())
////            {
////                System.out.println("Success");
////                System.out.println(handler.result());
////            }
////            else
////            {
////                System.out.println("Failure");
////            }
////        });
//        JwtData event = mock(JwtData.class);
//        when(event.getAud()).thenReturn("Dummy String");
////        doAnswer(new Answer<JwtData>() {
////            @Override
////            public JwtData answer(InvocationOnMock arg0) throws Throwable {
////                ((Handler<JwtData>) arg0.getArgument(0)).handle(event);
////                return null;
////            }
////        }).when(jwtAuthenticationService.jwtDecodeFuture).compose(any());
//
//
//        JsonObject request = new JsonObject();
//        JsonObject authInfo = new JsonObject();
//
//        authInfo.put("token", JwtTokenHelper.openConsumerSubsToken);
//        authInfo.put("id", openId);
//        authInfo.put("apiEndpoint", Api.SUBSCRIPTION.getApiEndpoint());
//        authInfo.put("method", HttpSender.Method.POST);
//
//        jwtAuthenticationService.tokenInterospect(request, authInfo, handler -> {
//            if (handler.failed()) {
//                System.out.println(handler.cause().getMessage());
//                vertxTestContext.completeNow();
//            } else {
//                vertxTestContext.failNow(handler.cause());
//            }
//        });
//        vertxTestContext.completeNow();
//    }
//
//    @Test
//    @DisplayName("Test tokenIntrospect method : ")
//    public void testTokenIntrospect2(VertxTestContext vertxTestContext)
//    {
//        ArgumentCaptor<Handler<User>> captorUser = ArgumentCaptor.forClass(Handler.class);
//        User event = mock(User.class);
//        Future<User> userFuture = mock(Future.class);
//        when(config.getString(anyString())).thenReturn("/admin/revokeToken");
//
//        when(config.getInteger(anyString())).thenReturn(8443);
//        when(jwtAuth1.authenticate(any(TokenCredentials.class))).thenReturn(userFuture);
////        verify(jwtAuth1).authenticate(any(TokenCredentials.class));
////        userFuture.onSuccess(captorUser.capture());
////        Handler<User> userHandler = captorUser.getValue();
////        userHandler.handle(event);
////        when(event.principal()).thenReturn(config);
////        when(event.get(anyString())).thenReturn("Dummy string");
//        jwtAuthenticationService = new JwtAuthenticationServiceImpl(Vertx.vertx(),jwtAuth1,webClient,config,cacheService);
//
//        jwtAuthenticationService.tokenInterospect(config, config, handler -> {
//            if (handler.failed()) {
//                System.out.println(handler.cause().getMessage());
//                vertxTestContext.completeNow();
//            } else {
//                vertxTestContext.failNow(handler.cause());
//            }
//        });
//        vertxTestContext.completeNow();
//    }

    //>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
//    @Test
//    @DisplayName("Test tokenInterospect method : ")
//    public void testtokenInterospect(VertxTestContext vertxTestContext)
//    {
//        JsonObject jwtJson = new JsonObject("{ \"access_token\" : \"citizens of Asgard\" }");
//        JsonObject request = new JsonObject();
//        request.put("apiEndpoint","/ngsi-ld/v1/subscription");
//        request.put("id","Dummy id");
//        request.put("token",JwtTokenHelper.closedConsumerApiToken);
//        request.put("method","GET");
//
//        Future<User> userFuture = mock(Future.class);
//        User userEvent = mock(User.class);
//
////        when(jwtAuth1.authenticate(any(TokenCredentials.class))).thenReturn(userFuture);
////        when(userEvent.principal()).thenReturn(jwtJson);
////        when(userEvent.get(anyString())).thenReturn(20);
////        when(throwable.getMessage()).thenReturn("DUMMYYYYYYYYYY");
//
////        System.out.println(jwtAuth1.authenticate(new TokenCredentials("/ngsi-ld/v1/async/status")));
////        JwtData jwtData3333 = new JwtData(jwtJson);
////        jwtData3333.setIat(20);
////        jwtData3333.setExp(20);
//
////        System.out.println(jwtData3333);
//        // "token" :
//        // authenticate : Future<User>
//        // AsyncResult X User : io.vertx.ext.auth.User
////     "access_token" : "avdfsfgsfgsfgsd"
////       jwtData.getAud() == audience "Dummy string"
////        System.out.println("********** " + new TokenCredentials("/ngsi-ld/v1/async/status"));
//
////        doAnswer(new Answer<Throwable>() {
////            @Override
////            public Throwable answer(InvocationOnMock arg0) {
////                ((Handler<Throwable>) arg0.getArgument(0)).handle(throwable);
////                return null;
////            }
////        }).when(userFuture).onFailure(any());
////
////
////        doAnswer(new Answer<User>() {
////            @Override
////            public User answer(InvocationOnMock arg0) throws Throwable {
////                ((Handler<User>) arg0.getArgument(0)).handle(userEvent);
////                return null;
////            }
////        }).when(userFuture).onSuccess(any());
//
//        jwtAuthenticationService.tokenInterospect(request,request,handler -> {
//            if(handler.succeeded())
//            {
//                System.out.println("Success");
//            }
//            else
//            {
//                System.out.println("failure");
//            }
//        });
//        vertxTestContext.completeNow();
//    }

}

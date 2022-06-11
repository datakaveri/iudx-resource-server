package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.databroker.util.PermissionOpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RabbitClientTest {

    RabbitClient rabbitClient;
    String userID;
    String password;
    @Mock
    RabbitMQOptions rabbitConfigs;
    Vertx vertxObj;
    @Mock
    RabbitWebClient webClient;
    @Mock
    PostgresClient pgSQLClient;
    @Mock
    JsonObject configs;
    @Mock
    Future<HttpResponse<Buffer>> httpResponseFuture;
    @Mock
    Future<RowSet<Row>> rowSetFuture;
    @Mock
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;
    @Mock
    HttpResponse<Buffer> bufferHttpResponse;
    @Mock
    Buffer buffer;
    @Mock
    AsyncResult<RowSet<Row>> rowSetAsyncResult;
    @Mock
    Throwable throwable;
    PermissionOpType type;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        userID = "Dummy UserID";
        password = "Dummy password";
        vertxObj = Vertx.vertx();
        when(configs.getString(anyString())).thenReturn("Dummy string");
        when(configs.getInteger(anyString())).thenReturn(400);
        when(rabbitConfigs.setVirtualHost(anyString())).thenReturn(rabbitConfigs);
        rabbitClient = new RabbitClient(vertxObj, rabbitConfigs, webClient, pgSQLClient, configs);
        vertxTestContext.completeNow();
    }

    static Stream<Arguments> statusCodeValues() {
        return Stream.of(
                Arguments.of(204, "{\"Dummy UserID\":\"Dummy UserID\",\"password\":\"Dummy password\"}"),
                Arguments.of(400, "{\"failure\":\"Network Issue\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("statusCodeValues")
    @DisplayName("Test resetPasswordInRMQ method with different statusCode")
    public void testResetPasswordInRMQ(int statusCode, String expected, VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(statusCode);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.resetPasswordInRMQ(userID, password).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
            } else {
                assertEquals(expected, handler.cause().getMessage());
            }
        });
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test resetPasswordInRMQ method : Failure")
    public void testResetPasswordInRMQFailure(VertxTestContext vertxTestContext) {
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        String expectedValue = "{\"failure\":\"Something went wrong while creating user using mgmt API. Check credentials\"}";
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.resetPasswordInRMQ(userID, password).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(expectedValue, handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

    }


    public static Stream<Arguments> statusCode() {
        return Stream.of(
                Arguments.of(200, "{\"response\":\"Dummy response\"}"),
                Arguments.of(404, "{\"type\":\"urn:dx:rs:badRequest\",\"status\":404,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"user not exist.\"}"),
                Arguments.of(402, "{\"type\":\"urn:dx:rs:badRequest\",\"status\":402,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"problem while getting user permissions\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("statusCode")
    @DisplayName("Test getUserPermissions method with different status code")
    public void testGetUserPermissions(int statusCode, String str, VertxTestContext vertxTestContext) {

        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        lenient().when(bufferHttpResponse.body()).thenReturn(buffer);
        lenient().when(buffer.toString()).thenReturn("[\n" +
                "  {\n" +
                "    \"response\" : \"Dummy response\"\n" +
                "   }\n" +
                "]");
        when(bufferHttpResponse.statusCode()).thenReturn(statusCode);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getUserPermissions(userID).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(str, handler.result().toString());
            } else {
                assertEquals(str, handler.cause().getMessage());
            }
        });
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test getUserPermissions method : Failure")
    public void testGetUserPermissionsFailure(VertxTestContext vertxTestContext) {

        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        when(throwable.getLocalizedMessage()).thenReturn("Dummy failure message");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.getUserPermissions(userID).onComplete(handler -> {

            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("{\"type\":\"urn:dx:rs:badRequest\",\"status\":400,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"Dummy failure message\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> booleanValues() {
        return Stream.of(
                Arguments.of(true, "{\"type\":\"urn:dx:rs:badRequest\",\"status\":500,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"Dummy status message\"}"),
                Arguments.of(false, "{\"type\":\"urn:dx:rs:badRequest\",\"status\":500,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"Dummy failure message\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("booleanValues")
    @DisplayName("Test updateUserPermissions method : Failure")
    public void testUpdateUserPermissionsFailure(boolean booleanValue, String expected, VertxTestContext vertxTestContext) {
        Future<HttpResponse<Buffer>> httpResponseFuture1 = mock(Future.class);
        AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult1 = mock(AsyncResult.class);
        HttpResponse<Buffer> bufferHttpResponse1 = mock(HttpResponse.class);

        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture1);
        when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
        lenient().when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        lenient().when(bufferHttpResponse.statusCode()).thenReturn(500);
        lenient().when(bufferHttpResponse.statusMessage()).thenReturn("Dummy status message");
        lenient().when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        lenient().when(throwable.getMessage()).thenReturn("Dummy failure message");

        when(httpResponseAsyncResult1.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult1.result()).thenReturn(bufferHttpResponse1);
        when(bufferHttpResponse1.statusCode()).thenReturn(200);
        when(bufferHttpResponse1.body()).thenReturn(buffer);
        when(buffer.toString()).thenReturn("[\n" +
                "  {\n" +
                "    \"write\" : \"Dummy response\"\n" +
                "   }\n" +
                "]");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult1);
                return null;
            }
        }).when(httpResponseFuture1).onComplete(any());
        when(httpResponseAsyncResult.succeeded()).thenReturn(booleanValue);
        type = PermissionOpType.ADD_WRITE;

        rabbitClient.updateUserPermissions(userID, userID, type, userID).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(200, "{\"response\":\"Dummy response\"}"),
                Arguments.of(400, "{\"type\":400,\"title\":\"failure\",\"detail\":\"Exchange not found\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("values")
    @DisplayName("Test getExchangeDetails method with different status code")
    public void testGetExchangeDetails(int statusCode, String expected, VertxTestContext vertxTestContext) {
        String vHost = "Dummy Virtual Host";
        JsonObject request = new JsonObject();
        request.put("exchangeName", "Dummy exchangeName");
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(statusCode);
        lenient().when(bufferHttpResponse.body()).thenReturn(buffer);
        lenient().when(buffer.toString()).thenReturn("{ \"response\" : \"Dummy response\" }");
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.getExchangeDetails(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
            } else {
                assertEquals(expected, handler.cause().getMessage());
            }
        });
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test getExchangeDetails method : Failure")
    public void testGetExchangeDetailsFailure(VertxTestContext vertxTestContext) {
        JsonObject request = new JsonObject();
        request.put("exchangeName", "Dummy exchangeName");
        String vHost = "Dummy Virtual Host";
        when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchangeDetails(request, vHost).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Exchange not found\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });

    }
}

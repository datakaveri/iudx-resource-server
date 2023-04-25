package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.*;
import iudx.resource.server.databroker.util.PermissionOpType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static iudx.resource.server.databroker.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RabbitClientTest {

  RabbitClient rabbitClient;
  String userID;
  String password;
  @Mock RabbitMQOptions rabbitConfigs;
  Vertx vertxObj;
  @Mock RabbitWebClient webClient;
  @Mock PostgresClient pgSQLClient;
  @Mock JsonObject configs;
  @Mock Future<HttpResponse<Buffer>> httpResponseFuture;
  @Mock Future<RowSet<Row>> rowSetFuture;
  @Mock AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;
  @Mock HttpResponse<Buffer> bufferHttpResponse;
  @Mock HttpRequest<Buffer> bufferHttpRequest;
  @Mock Buffer buffer;
  @Mock AsyncResult<RowSet<Row>> rowSetAsyncResult;
  @Mock Throwable throwable;
  JsonObject request;
  JsonArray jsonArray;
  String vHost;
  PermissionOpType type;
  JsonObject expected;
  RabbitWebClient rabbitWebClient;
  @Mock WebClientOptions webClientOptions;
  @Mock PoolOptions poolOptions;
  @Mock PgConnectOptions pgConnectOptions;
  PostgresClient postgresClient;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    userID = "Dummy UserID";
    password = "Dummy password";
    vertxObj = Vertx.vertx();
    vHost = "Dummy vHost";
    expected = new JsonObject();
    when(configs.getString(anyString())).thenReturn("Dummy string");
    when(configs.getInteger(anyString())).thenReturn(400);
    when(rabbitConfigs.setVirtualHost(anyString())).thenReturn(rabbitConfigs);
    request = new JsonObject();
    jsonArray = new JsonArray();
    RabbitWebClient.webClient = mock(WebClient.class);
    PostgresClient.pgPool = mock(PgPool.class);
    jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD");
    jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH");
    request.put("exchangeName", "Dummy exchangeName");
    request.put("queueName", "Dummy Queue name");
    request.put("resourceGroup", "Dummy Resource Group");
    request.put("id", "Dummy ID");
    request.put(USER_ID, "Dummy userID");
    request.put("vHost", "Dummy vHost");
    request.put("userName", "Dummy userName");
    request.put("password", "Dummy password");
    request.put("entities", jsonArray);
    rabbitClient = new RabbitClient(vertxObj, rabbitConfigs, webClient, pgSQLClient, configs);
    rabbitWebClient = new RabbitWebClient(vertxObj, webClientOptions, request);
    postgresClient = new PostgresClient(vertxObj, pgConnectOptions, poolOptions);
    vertxTestContext.completeNow();
  }

  static Stream<Arguments> statusCodeValues() {
    return Stream.of(
        Arguments.of(204, "{\"Dummy UserID\":\"Dummy UserID\",\"password\":\"Dummy password\"}"),
        Arguments.of(400, "{\"failure\":\"Network Issue\"}"));
  }

  @ParameterizedTest
  @MethodSource("statusCodeValues")
  @DisplayName("Test resetPasswordInRMQ method with different statusCode")
  public void testResetPasswordInRMQ(
      int statusCode, String expected, VertxTestContext vertxTestContext) {
    when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(statusCode);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .resetPasswordInRMQ(userID, password)
        .onComplete(
            handler -> {
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
    String expectedValue =
        "{\"failure\":\"Something went wrong while creating user using mgmt API. Check"
            + " credentials\"}";
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .resetPasswordInRMQ(userID, password)
        .onComplete(
            handler -> {
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
        Arguments.of(
            404,
            "{\"type\":\"urn:dx:rs:badRequest\",\"status\":404,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"user"
                + " not exist.\"}"),
        Arguments.of(
            402,
            "{\"type\":\"urn:dx:rs:badRequest\",\"status\":402,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"problem"
                + " while getting user permissions\"}"));
  }

  @ParameterizedTest
  @MethodSource("statusCode")
  @DisplayName("Test getUserPermissions method with different status code")
  public void testGetUserPermissions(
      int statusCode, String str, VertxTestContext vertxTestContext) {

    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    lenient().when(bufferHttpResponse.body()).thenReturn(buffer);
    lenient()
        .when(buffer.toString())
        .thenReturn("[\n" + "  {\n" + "    \"response\" : \"Dummy response\"\n" + "   }\n" + "]");
    when(bufferHttpResponse.statusCode()).thenReturn(statusCode);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .getUserPermissions(userID)
        .onComplete(
            handler -> {
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
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());

    rabbitClient
        .getUserPermissions(userID)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
              } else {
                assertEquals(
                    "{\"type\":\"urn:dx:rs:badRequest\",\"status\":400,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"Dummy"
                        + " failure message\"}",
                    handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  static Stream<Arguments> booleanValues() {
    return Stream.of(
        Arguments.of(
            true,
            "{\"type\":\"urn:dx:rs:badRequest\",\"status\":400,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"problem"
                + " while getting user permissions\"}"),
        Arguments.of(
            false,
            "{\"type\":\"urn:dx:rs:badRequest\",\"status\":400,\"title\":\"urn:dx:rs:badRequest\",\"detail\":\"problem"
                + " while getting user permissions\"}"));
  }

  @ParameterizedTest
  @MethodSource("booleanValues")
  @DisplayName("Test updateUserPermissions method : Failure")
  public void testUpdateUserPermissionsFailure(
      boolean booleanValue, String expected, VertxTestContext vertxTestContext) {
    Future<HttpResponse<Buffer>> httpResponseFuture1 = mock(Future.class);
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult1 = mock(AsyncResult.class);
    HttpResponse<Buffer> bufferHttpResponse1 = mock(HttpResponse.class);

    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture1);
    when(httpResponseAsyncResult1.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult1.result()).thenReturn(bufferHttpResponse1);
    when(bufferHttpResponse1.statusCode()).thenReturn(400);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult1);
                return null;
              }
            })
        .when(httpResponseFuture1)
        .onComplete(any());
    type = PermissionOpType.ADD_WRITE;

    rabbitClient
        .updateUserPermissions(userID, userID, type, userID)
        .onComplete(
            handler -> {
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
        Arguments.of(
            400, "{\"type\":400,\"title\":\"failure\",\"detail\":\"Exchange not found\"}"));
  }

  @ParameterizedTest
  @MethodSource("values")
  @DisplayName("Test getExchangeDetails method with different status code")
  public void testGetExchangeDetails(
      int statusCode, String expected, VertxTestContext vertxTestContext) {
    String vHost = "Dummy Virtual Host";
    JsonObject request = new JsonObject();
    request.put("exchangeName", "Dummy exchangeName");
    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(statusCode);
    lenient().when(bufferHttpResponse.body()).thenReturn(buffer);
    lenient().when(buffer.toString()).thenReturn("{ \"response\" : \"Dummy response\" }");
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());

    rabbitClient
        .getExchangeDetails(request, vHost)
        .onComplete(
            handler -> {
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

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .getExchangeDetails(request, vHost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
              } else {
                assertEquals(
                    "{\"type\":500,\"title\":\"error\",\"detail\":\"Exchange not found\"}",
                    handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test deleteExchange method : when status code is 204")
  public void testDeleteExchangeWhenSC_NO_CONTENT(VertxTestContext vertxTestContext) {
    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(204);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .deleteExchange(request, vHost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals("{\"exchange\":\"Dummy exchangeName\"}", handler.result().toString());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  static Stream<Arguments> statusCodeInput() {
    return Stream.of(
        Arguments.of(204, "{\"queue\":\"Dummy Queue name\"}"),
        Arguments.of(
            404, "{\"type\":404,\"title\":\"failure\",\"detail\":\"Queue does not exist\"}"));
  }

  @ParameterizedTest
  @MethodSource("statusCodeInput")
  @DisplayName("Test deleteQueue method : with different status code")
  public void testDeleteQueue(int code, String expected, VertxTestContext vertxTestContext) {
    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(code);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .deleteQueue(request, vHost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  static Stream<Arguments> statusCodes() {
    return Stream.of(
        Arguments.of(
            201,
            "{\"exchange\":\"Dummy exchangeName\",\"queue\":\"Dummy Queue"
                + " name\",\"entities\":[\"ABCD/ABCD/ABCD/ABCD/ABCD\",\"EFGH/EFGH/EFGH/EFGH/EFGH\"]}"),
        Arguments.of(
            404,
            "{\"type\":404,\"title\":\"failure\",\"detail\":\"Queue/Exchange does not exist\"}"));
  }

  @ParameterizedTest
  @MethodSource("statusCodes")
  @DisplayName("Test bindQueue method : with different status code")
  public void testBindQueue(int code, String expected, VertxTestContext vertxTestContext) {
    when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(code);

    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .bindQueue(request, vHost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test registerAdapter method : Success")
  public void testRegisterAdapterSuccess(VertxTestContext vertxTestContext) {
    Future<HttpResponse<Buffer>> httpResponseFuture1 = mock(Future.class);
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult1 = mock(AsyncResult.class);
    HttpResponse<Buffer> bufferHttpResponse1 = mock(HttpResponse.class);

    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.body()).thenReturn(buffer);
    when(buffer.toString())
        .thenReturn("[{\"write\" : \"Dummy ADD_WRITE\"},{ \"key\" :  \"value\"}]");
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(bufferHttpResponse.statusCode()).thenReturn(200);

    when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture1);
    when(httpResponseAsyncResult1.result()).thenReturn(bufferHttpResponse1);
    when(httpResponseAsyncResult1.succeeded()).thenReturn(true);
    when(bufferHttpResponse1.statusCode()).thenReturn(201);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult1);
                return null;
              }
            })
        .when(httpResponseFuture1)
        .onComplete(any());
    rabbitClient
        .registerAdapter(request, vHost)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    "{\"username\":\"Dummy userID\",\"apiKey\":\"Use the apiKey returned on"
                        + " registration, if lost please use /resetPassword"
                        + " API\",\"id\":\"ABCD/ABCD/ABCD/ABCD/ABCD\",\"URL\":\"Dummy"
                        + " string\",\"port\":400,\"vHost\":\"Dummy vHost\"}",
                    handler.result().toString());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test deleteAdapter method : Success")
  public void testDeleteAdapterSuccess(VertxTestContext vertxTestContext) {
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(200);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    rabbitClient
        .deleteAdapter(request, vHost)
        .onComplete(
            handler -> {
              expected.put("type", 200);
              expected.put("title", "success");
              expected.put("detail", "adaptor deleted");
              if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test createUser method : Success")
  public void testCreateUserSuccess(VertxTestContext vertxTestContext) {
    when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.statusCode()).thenReturn(201);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture);
    when(rowSetAsyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(rowSetFuture)
        .onComplete(any());
    rabbitClient
        .createUser(userID, password, vHost, "Dummy/ABCD/ABCD/ABCD")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    "{\"userid\":\"Dummy UserID\",\"password\":\"Dummy"
                        + " password\",\"type\":200,\"title\":\"vhostPermissions\",\"detail\":\"write"
                        + " permission set\"}",
                    handler.result().toString());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test updateUserPermissions method : Success")
  public void testUpdateUserPermissionsSuccess(VertxTestContext vertxTestContext) {
    Future<HttpResponse<Buffer>> httpResponseFuture1 = mock(Future.class);
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult1 = mock(AsyncResult.class);
    HttpResponse<Buffer> bufferHttpResponse1 = mock(HttpResponse.class);

    when(webClient.requestAsync(anyString(), anyString())).thenReturn(httpResponseFuture);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    when(bufferHttpResponse.body()).thenReturn(buffer);
    when(buffer.toString())
        .thenReturn("[{\"write\" : \"Dummy DELETE_WRITE\"},{ \"key\" :  \"value\"}]");
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(bufferHttpResponse.statusCode()).thenReturn(200);

    when(webClient.requestAsync(anyString(), anyString(), any())).thenReturn(httpResponseFuture1);
    when(httpResponseAsyncResult1.result()).thenReturn(bufferHttpResponse1);
    when(httpResponseAsyncResult1.succeeded()).thenReturn(true);
    when(bufferHttpResponse1.statusCode()).thenReturn(204);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(httpResponseFuture)
        .onComplete(any());
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult1);
                return null;
              }
            })
        .when(httpResponseFuture1)
        .onComplete(any());
    rabbitClient
        .updateUserPermissions(vHost, userID, PermissionOpType.DELETE_WRITE, "Dummy/ABCD/ABCD/ABCD")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    "{\"type\":\"urn:dx:rs:success\",\"status\":204,\"title\":\"urn:dx:rs:success\",\"detail\":\"Permission"
                        + " updated successfully.\"}",
                    handler.result().toString());
                vertxTestContext.completeNow();

              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {REQUEST_POST, REQUEST_PUT, REQUEST_DELETE})
  @DisplayName("Test requestAsync method Success : with different requestTypes")
  public void testRequestAsyncSuccess(String requestType, VertxTestContext vertxTestContext) {
    lenient().when(RabbitWebClient.webClient.post(anyString())).thenReturn(bufferHttpRequest);
    lenient().when(RabbitWebClient.webClient.put(anyString())).thenReturn(bufferHttpRequest);
    lenient().when(RabbitWebClient.webClient.delete(anyString())).thenReturn(bufferHttpRequest);
    when(bufferHttpRequest.basicAuthentication(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(bufferHttpRequest)
        .sendJsonObject(any(), any(Handler.class));

    rabbitWebClient
        .requestAsync(requestType, "Dummy/ABCD/ABCD/ABCD", request)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(bufferHttpResponse, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test requestAsync method  : Failure")
  public void testRequestAsyncFailure(VertxTestContext vertxTestContext) {
    when(RabbitWebClient.webClient.post(anyString())).thenReturn(bufferHttpRequest);
    when(bufferHttpRequest.basicAuthentication(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(false);
    when(httpResponseAsyncResult.cause()).thenReturn(throwable);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(1))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(bufferHttpRequest)
        .sendJsonObject(any(), any(Handler.class));

    rabbitWebClient
        .requestAsync(REQUEST_POST, "Dummy/ABCD/ABCD/ABCD", request)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(throwable, handler.cause());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test requestAsync method  : Success")
  public void test_requestAsync_success(VertxTestContext vertxTestContext) {
    when(RabbitWebClient.webClient.post(anyString())).thenReturn(bufferHttpRequest);
    when(bufferHttpRequest.basicAuthentication(anyString(), anyString()))
        .thenReturn(bufferHttpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
    doAnswer(
            new Answer<AsyncResult<HttpResponse<Buffer>>>() {
              @Override
              public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0))
                    .handle(httpResponseAsyncResult);
                return null;
              }
            })
        .when(bufferHttpRequest)
        .send(any());

    rabbitWebClient
        .requestAsync(REQUEST_POST, "Dummy/ABCD/ABCD/ABCD")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(bufferHttpResponse, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test executeAsync method  : Success")
  public void test_executeAsync_success(VertxTestContext vertxTestContext) {
    AsyncResult<SqlConnection> connectionAsyncResult = mock(AsyncResult.class);
    SqlConnection sqlConnection = mock(SqlConnection.class);
    Query<RowSet<Row>> query = mock(Query.class);
    RowSet<Row> value = mock(RowSet.class);
    when(connectionAsyncResult.succeeded()).thenReturn(true);
    when(connectionAsyncResult.result()).thenReturn(sqlConnection);
    when(sqlConnection.query(anyString())).thenReturn(query);
    when(rowSetAsyncResult.succeeded()).thenReturn(true);
    when(rowSetAsyncResult.result()).thenReturn(value);
    doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(connectionAsyncResult);
                return null;
              }
            })
        .when(PostgresClient.pgPool)
        .getConnection(any());
    doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(query)
        .execute(any());
    postgresClient
        .executeAsync("Dummy query")
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(value, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test executeAsync method  : Failure")
  public void test_executeAsync_failure(VertxTestContext vertxTestContext) {
    AsyncResult<SqlConnection> connectionAsyncResult = mock(AsyncResult.class);
    SqlConnection sqlConnection = mock(SqlConnection.class);
    Query<RowSet<Row>> query = mock(Query.class);
    when(connectionAsyncResult.succeeded()).thenReturn(true);
    when(connectionAsyncResult.result()).thenReturn(sqlConnection);
    when(sqlConnection.query(anyString())).thenReturn(query);
    when(rowSetAsyncResult.succeeded()).thenReturn(false);
    when(rowSetAsyncResult.cause()).thenReturn(throwable);
    doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(connectionAsyncResult);
                return null;
              }
            })
        .when(PostgresClient.pgPool)
        .getConnection(any());
    doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(query)
        .execute(any());
    postgresClient
        .executeAsync("Dummy query")
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(throwable, handler.cause());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }
}

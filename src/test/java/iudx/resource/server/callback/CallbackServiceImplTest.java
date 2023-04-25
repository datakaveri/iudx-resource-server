package iudx.resource.server.callback;

import com.rabbitmq.client.Envelope;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQMessage;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
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

import java.util.HashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class CallbackServiceImplTest {
  @Mock RabbitMQClient rabbitMQClient;
  @Mock WebClient webClient;
  @Mock AsyncResult<SqlConnection> sqlConnectionAsyncResult;
  @Mock AsyncResult<RowSet<Row>> rowSetAsyncResult;
  @Mock AsyncResult<Void> voidAsyncResult;
  @Mock Throwable throwable;
  @Mock AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;
  @Mock AsyncResult<RabbitMQConsumer> rabbitMQConsumerAsyncResult;
  JsonObject jsonObject;
  Vertx vertxObj;
  CallbackServiceImpl callbackService;
  @Mock RabbitMQConsumer rabbitMQConsumer;
  @Mock HttpRequest<Buffer> httpRequest;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock JsonObject request;
  @Mock RabbitMQMessage rabbitMQMessage;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    vertxObj = Vertx.vertx();
    jsonObject = new JsonObject();
    jsonObject.put("callbackDatabaseIP", "Dummy callbackDatabaseIP value");
    jsonObject.put("callbackDatabasePort", 8888);
    jsonObject.put("callbackDatabaseName", "localhost");
    jsonObject.put("callbackDatabaseUserName", "guest");
    jsonObject.put("callbackDatabasePassword", "guest");
    jsonObject.put("callbackpoolSize", 10);
    callbackService = new CallbackServiceImpl(rabbitMQClient, webClient, jsonObject, vertxObj);
    vertxTestContext.completeNow();
  }

  static Stream<Arguments> statusCode() {
    return Stream.of(
        Arguments.of(
            404, "{\"type\":404,\"title\":\"failure\",\"detail\":\"Callback Url not found\"}"),
        Arguments.of(400, "{\"type\":400,\"title\":\"failure\",\"detail\":null}"),
        Arguments.of(
            200,
            "{\"type\":200,\"title\":\"success\",\"detail\":\"Data Send to CallBackUrl"
                + " Successfully\"}"));
  }

  @ParameterizedTest
  @DisplayName("Test sendDataToCallBackSubscriber method : with Different HTTP status")
  @MethodSource("statusCode")
  public void testSendDataToCallBackSubscriber(
      int code, String expected, VertxTestContext vertxTestContext) {
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(code);
    lenient().when(httpResponse.bodyAsString()).thenReturn("Dummy string");

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
        .when(httpRequest)
        .sendJsonObject(any(), any());
    callbackService.sendDataToCallBackSubscriber(
        request,
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
  @DisplayName("Test sendDataToCallBackSubscriber method : with null response")
  public void testSendDataToCallBackSubscriberForNullResponse(VertxTestContext vertxTestContext) {
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);
    when(httpResponseAsyncResult.result()).thenReturn(null);
    when(httpResponseAsyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy failure message");
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
        .when(httpRequest)
        .sendJsonObject(any(), any());
    callbackService.sendDataToCallBackSubscriber(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"CallbackUrl response is null\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test sendDataToCallBackSubscriber method : Failure to connect callback url")
  public void testSendDataToCallBackSubscriberFailure(VertxTestContext vertxTestContext) {
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);
    when(httpResponseAsyncResult.succeeded()).thenReturn(false);
    when(httpResponseAsyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy failure message");
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
        .when(httpRequest)
        .sendJsonObject(any(), any());
    callbackService.sendDataToCallBackSubscriber(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"Failed to connect callbackUrl\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test sendDataToCallBackSubscriber method : with invalid web request")
  public void testSendDataToCallBackSubscriberForInvalidWebRequest(
      VertxTestContext vertxTestContext) {
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(null);

    callbackService.sendDataToCallBackSubscriber(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"Failed to send data to callBackUrl\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test sendDataToCallBackSubscriber method : with null callbBackUrl")
  public void testSendDataToCallBackSubscriberForNullCallBackURL(
      VertxTestContext vertxTestContext) {
    lenient().when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(null);

    callbackService.sendDataToCallBackSubscriber(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals("{\"error\":\"CallbackUrl is Invalid\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test sendDataToCallBackSubscriber method : with null http request")
  public void testSendDataToCallBackSubscriberForNullHttpRequest(
      VertxTestContext vertxTestContext) {
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(null);

    callbackService.sendDataToCallBackSubscriber(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"Failed to create request object for sending callback request\"}",
                handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test queryCallBackDataBase method : Failure in connecting database")
  public void testQueryCallBackDataBaseFailure(VertxTestContext vertxTestContext) {
    SqlConnection sqlConnection = mock(SqlConnection.class);
    PreparedQuery<RowSet<Row>> preparedquery = mock(PreparedQuery.class);
    RowSet<Row> rowSet = mock(RowSet.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);

    when(sqlConnectionAsyncResult.result()).thenReturn(sqlConnection);
    when(sqlConnection.preparedQuery(anyString())).thenReturn(preparedquery);
    when(rowSetAsyncResult.succeeded()).thenReturn(true);
    when(rowSetAsyncResult.result()).thenReturn(rowSet);

    when(sqlConnectionAsyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(sqlConnectionAsyncResult);
                return null;
              }
            })
        .when(CallbackServiceImpl.pgClient)
        .getConnection(any());

    doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(preparedquery)
        .execute(any(Handler.class));
    callbackService.queryCallBackDataBase(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"Error in Connecting Database\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test queryCallBackDataBase method : with Failure in Query Execution")
  public void testQueryCallBackDataBaseQueryExecutionFailure(VertxTestContext vertxTestContext) {
    SqlConnection sqlConnection = mock(SqlConnection.class);
    PreparedQuery<RowSet<Row>> preparedquery = mock(PreparedQuery.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);

    when(sqlConnectionAsyncResult.result()).thenReturn(sqlConnection);
    when(sqlConnection.preparedQuery(anyString())).thenReturn(preparedquery);
    when(rowSetAsyncResult.succeeded()).thenReturn(false);

    when(sqlConnectionAsyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(sqlConnectionAsyncResult);
                return null;
              }
            })
        .when(CallbackServiceImpl.pgClient)
        .getConnection(any());

    doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(preparedquery)
        .execute(any(Handler.class));
    callbackService.queryCallBackDataBase(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals("{\"error\":\"Failed to execute Query\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test queryCallBackDataBase method : Failure in connecting Database")
  public void testQueryCallBackDataBaseWithFailureInDBConnection(
      VertxTestContext vertxTestContext) {
    SqlConnection sqlConnection = mock(SqlConnection.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);

    when(sqlConnectionAsyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy failure message");
    when(sqlConnectionAsyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(sqlConnectionAsyncResult);
                return null;
              }
            })
        .when(CallbackServiceImpl.pgClient)
        .getConnection(any());

    callbackService.queryCallBackDataBase(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());
          } else {
            assertEquals(
                "{\"error\":\"Error in Connecting Database\"}", handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : Success")
  public void testConnectToCallbackDataQueueSuccess(VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    Envelope envelopeValue = mock(Envelope.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);

    when(request.getString(anyString())).thenReturn("Dummy string");
    lenient().when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);

    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn("{\"Buffer\":\"Dummy buffer value\"}");

    when(CallbackServiceImpl.pgCache.get(anyString())).thenReturn(request);

    when(rabbitMQMessage.envelope()).thenReturn(envelopeValue);
    when(envelopeValue.getRoutingKey()).thenReturn("Dummy routing key");

    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(200);
    when(httpResponse.bodyAsString()).thenReturn("Dummy http response string");

    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);

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
        .when(httpRequest)
        .sendJsonObject(any(), any());

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());

    String expected =
        "{\"success\":\"Data Send to CallBackUrl Successfully\",\"Database Query"
            + " Result\":\"Connected to callback.data queue\"}";

    callbackService.connectToCallbackDataQueue(
        request,
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
  @DisplayName("Test connectToCallbackDataQueue method : with failure start method")
  public void testConnectToCallbackDataQueueForFailedConnection(VertxTestContext vertxTestContext) {

    String expected =
        "{\"error\":\"rabbitmq client failed to create connection with QueueDummy string\"}";
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(voidAsyncResult.succeeded()).thenReturn(false);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    callbackService.connectToCallbackDataQueue(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : with failure in consuming message")
  public void testConnectToCallbackDataQueueForFailure(VertxTestContext vertxTestContext) {

    String expected = "{\"error\":\"Failed to consume message from QueueDummy string\"}";
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(false);

    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    callbackService.connectToCallbackDataQueue(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
    vertxTestContext.completeNow();
  }

  static Stream<Arguments> operations() {
    return Stream.of(
        Arguments.of(
            "{\"abcd\":\"dummy database operation\"}",
            "{\"error\":\"Database operation not found in message body\"}"),
        Arguments.of(
            "{\"operation\":\"dummy operation\"}",
            "{\"error\":\"Invalid database operation. Operation should be one of [create or update"
                + " or delete]\"}"),
        Arguments.of(
            "{\"operation\":\"delete\"}", "{\"Database Query Result\":\"Database Query Failed\"}"));
  }

  @ParameterizedTest
  @MethodSource("operations")
  @DisplayName("Test connectToCallbackNotificationQueue method : Failure to query database ")
  public void testConnectToCallbackNotificationQueueQueryDBFailure(
      String operation, String expected, VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);

    when(request.getString(anyString())).thenReturn("Dummy string");
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn(operation);

    SqlConnection sqlConnection = mock(SqlConnection.class);
    PreparedQuery<RowSet<Row>> preparedquery = mock(PreparedQuery.class);
    RowSet<Row> rowSet = mock(RowSet.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);

    lenient().when(sqlConnectionAsyncResult.result()).thenReturn(sqlConnection);
    lenient().when(sqlConnection.preparedQuery(anyString())).thenReturn(preparedquery);
    lenient().when(rowSetAsyncResult.succeeded()).thenReturn(true);
    lenient().when(rowSetAsyncResult.result()).thenReturn(rowSet);

    lenient().when(sqlConnectionAsyncResult.succeeded()).thenReturn(true);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<SqlConnection>>() {
              @Override
              public AsyncResult<SqlConnection> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<SqlConnection>>) arg0.getArgument(0))
                    .handle(sqlConnectionAsyncResult);
                return null;
              }
            })
        .when(CallbackServiceImpl.pgClient)
        .getConnection(any());

    lenient()
        .doAnswer(
            new Answer<AsyncResult<RowSet<Row>>>() {
              @Override
              public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
              }
            })
        .when(preparedquery)
        .execute(any(Handler.class));
    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());
    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());

    callbackService.connectToCallbackNotificationQueue(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());

          } else {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  static Stream<Arguments> booleanValues() {
    return Stream.of(
        Arguments.of(true, "{\"error\":\"Failed to consume message from Queue :: Dummy string\"}"),
        Arguments.of(
            false,
            "{\"error\":\"rabbitmq client failed to create connection with Queue :: Dummy"
                + " string\"}"));
  }

  @ParameterizedTest
  @MethodSource("booleanValues")
  @DisplayName("Test connectToCallbackNotificationQueue method : Failure ")
  public void testConnectToCallbackNotificationQueueFailure(
      boolean booleanValue, String expected, VertxTestContext vertxTestContext) {

    CallbackServiceImpl.pgCache = mock(HashMap.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(voidAsyncResult.succeeded()).thenReturn(booleanValue);
    lenient().when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(false);

    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);
    lenient()
        .doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());
    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());

    callbackService.connectToCallbackNotificationQueue(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow(handler.cause());

          } else {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Test connectToCallbackNotificationQueue method : Failure to query database ")
  public void testConnectToCallbackNotificationQueueQueryDBFailure(
      VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);

    when(request.getString(anyString())).thenReturn("Dummy string");
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn("{\"operation\"}");

    when(request.getString(anyString())).thenReturn("Dummy string");
    CallbackServiceImpl.pgClient = mock(PgPool.class);

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());
    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());

    assertThrows(
        NullPointerException.class,
        () -> {
          callbackService.connectToCallbackNotificationQueue(
              request,
              handler -> {
                if (handler.succeeded()) {
                  vertxTestContext.failNow(handler.cause());
                } else {
                  assertEquals("", handler.cause().getMessage());
                  vertxTestContext.completeNow();
                }
              });
        });
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : Callback URL failure")
  public void testConnectToCallbackDataQueueForInvalidURL(VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    Envelope envelopeValue = mock(Envelope.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);

    when(request.getString(anyString())).thenReturn("Dummy string");
    lenient().when(request.getJsonObject(anyString())).thenReturn(request);
    when(webClient.postAbs(anyString())).thenReturn(httpRequest);
    when(httpRequest.basicAuthentication(anyString(), anyString())).thenReturn(httpRequest);
    when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);

    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn("{\"Buffer\":\"Dummy buffer value\"}");

    when(CallbackServiceImpl.pgCache.get(anyString())).thenReturn(request);

    when(rabbitMQMessage.envelope()).thenReturn(envelopeValue);
    when(envelopeValue.getRoutingKey()).thenReturn("Dummy routing key");

    when(httpResponseAsyncResult.result()).thenReturn(httpResponse);
    when(httpResponse.statusCode()).thenReturn(404);
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(httpResponseAsyncResult.succeeded()).thenReturn(true);

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
        .when(httpRequest)
        .sendJsonObject(any(), any());

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());

    String expected = "{\"error\":\"Failed to send data to callBackUrl\"}";

    callbackService.connectToCallbackDataQueue(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : when Callback URL does not exist")
  public void testConnectToCallbackDataQueueWithAbsentCallBackURL(
      VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    Envelope envelopeValue = mock(Envelope.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn("{\"Buffer\":\"Dummy buffer value\"}");
    when(CallbackServiceImpl.pgCache.get(anyString())).thenReturn(null);
    when(rabbitMQMessage.envelope()).thenReturn(envelopeValue);
    when(envelopeValue.getRoutingKey()).thenReturn("Dummy routing key");
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());

    String expected = "{\"error\":\"No callBackUrl exist for routing keyDummy routing key\"}";

    callbackService.connectToCallbackDataQueue(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : when message body is NULL")
  public void testConnectToCallbackDataQueueWithNullMessageBody(VertxTestContext vertxTestContext) {

    when(request.getString(anyString())).thenReturn("Dummy string");
    when(rabbitMQMessage.body()).thenReturn(null);
    when(voidAsyncResult.succeeded()).thenReturn(true);
    CallbackServiceImpl.pgCache = mock(HashMap.class);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());
    String expected = "{\"error\":\"Message body is NULL\"}";
    callbackService.connectToCallbackDataQueue(
        request,
        handler -> {
          if (handler.failed()) {
            assertEquals(expected, handler.cause().getMessage());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Test connectToCallbackDataQueue method : for invalid message body")
  public void testConnectToCallbackDataQueueWithInvalidMessageBody(
      VertxTestContext vertxTestContext) {

    Buffer bufferValue = mock(Buffer.class);
    Envelope envelopeValue = mock(Envelope.class);
    CallbackServiceImpl.pgCache = mock(HashMap.class);
    when(request.getString(anyString())).thenReturn("Dummy string");
    when(rabbitMQMessage.body()).thenReturn(bufferValue);
    when(bufferValue.toString()).thenReturn("{ \"buffer\" }");
    when(rabbitMQMessage.envelope()).thenReturn(envelopeValue);
    when(envelopeValue.getRoutingKey()).thenReturn("Dummy routing key");
    when(voidAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.succeeded()).thenReturn(true);
    when(rabbitMQConsumerAsyncResult.result()).thenReturn(rabbitMQConsumer);

    doAnswer(
            new Answer<AsyncResult<Void>>() {
              @Override
              public AsyncResult<Void> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(voidAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .start(any());
    doAnswer(
            new Answer<AsyncResult<RabbitMQConsumer>>() {
              @Override
              public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2))
                    .handle(rabbitMQConsumerAsyncResult);
                return null;
              }
            })
        .when(rabbitMQClient)
        .basicConsumer(anyString(), any(), any());

    doAnswer(
            new Answer<RabbitMQMessage>() {
              @Override
              public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(rabbitMQMessage);
                return null;
              }
            })
        .when(rabbitMQConsumer)
        .handler(any());

    assertThrows(
        DecodeException.class,
        () -> {
          callbackService.connectToCallbackDataQueue(
              request,
              handler -> {
                if (handler.succeeded()) {
                  vertxTestContext.completeNow();
                } else {
                  vertxTestContext.failNow(handler.cause());
                }
              });
        });
    vertxTestContext.completeNow();
  }
}

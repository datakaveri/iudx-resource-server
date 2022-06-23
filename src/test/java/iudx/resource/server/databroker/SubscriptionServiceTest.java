package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.databroker.util.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
public class SubscriptionServiceTest {
    @Mock
    RabbitClient rabbitClient;
    @Mock
    RabbitMQClient rabbitMQClient;
    @Mock
    PostgresClient pgSQLClient;
    @Mock
    JsonObject config;
    @Mock
    Future<RowSet<Row>> rowSetFuture;
    SubscriptionService service;
    @Mock
    AsyncResult<RowSet<Row>> asyncResult;
    @Mock
    AsyncResult<JsonObject> asyncResult1;
    @Mock
    AsyncResult<Void> voidAsyncResul;
    static JsonObject request;
    JsonArray jsonArray;
    @Mock
    Future<JsonObject> jsonObjectFuture;
    @Mock
    Throwable throwable;
    String str;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        str = "Dummy string";
        jsonArray = new JsonArray();
        request = new JsonObject();
        jsonArray.add("ABCD/ABCD/ABCD/ABCD/ABCD");
        jsonArray.add("EFGH/EFGH/EFGH/EFGH/EFGH");

        request.put(Constants.CONSUMER, "Dummy@consumer");
        request.put(Constants.NAME, "Dummy name");
        request.put(Constants.CALLBACKURL, "Dummy callbackurl");
        request.put(Constants.QUEUE, "Dummy Queue");
        request.put(Constants.ENTITIES, jsonArray);

        when(config.getString(anyString())).thenReturn(str);
        when(config.getInteger(anyString())).thenReturn(200);
        service = new SubscriptionService(rabbitClient, pgSQLClient, config);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test UpdateCallbackSubscription method : Success")
    public void testUpdateCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        String str = "{\"subscriptionID\":\"consumer/cc92a0dbd4da9a5a024a5395062f6f905c7e5797/Dummy name\"}";
        JsonObject expected = new JsonObject(str);

        when(asyncResult1.succeeded()).thenReturn(true);
        when(asyncResult1.result()).thenReturn(request);
        when(asyncResult.succeeded()).thenReturn(true);
        when(voidAsyncResul.succeeded()).thenReturn(true);

        when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rabbitClient.getRabbitMQClient()).thenReturn(rabbitMQClient);

        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        doAnswer(new Answer<AsyncResult<Void>>() {
            @Override
            public AsyncResult<Void> answer(InvocationOnMock arg3) throws Throwable {
                ((Handler<AsyncResult<Void>>) arg3.getArgument(3)).handle(voidAsyncResul);
                return null;
            }
        }).when(rabbitMQClient).basicPublish(anyString(), anyString(), any(Buffer.class), any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    public static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of("ABCD/ABCD/ABCD/ABCD/ABCD", "{\"type\":400,\"title\":\"error\",\"detail\":\"Binding failed\"}"),
                Arguments.of("ABCD/ABCD/ABCD", "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid or null routing key\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("values")
    @DisplayName("Test UpdateCallbackSubscription method : Failure")
    public void testUpdateCallbackSubscriptionFailure(String inputString, String str, VertxTestContext vertxTestContext) {
        JsonArray jsonArray = new JsonArray();
        JsonObject expected = new JsonObject(str);
        jsonArray.add(inputString);

        request.put(Constants.ENTITIES, jsonArray);

        lenient().when(asyncResult1.failed()).thenReturn(true);
        lenient().when(asyncResult1.cause()).thenReturn(throwable);
        lenient().when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);

        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.updateCallbackSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals(expected.toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test updateCallbackSubscription for null request")
    public void testUpdateCallbackSubscriptionForNullRequest(VertxTestContext vertxTestContext) {
        service.updateCallbackSubscription(null).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test appendStreamingSubscription method : Success")
    public void testAppendStreamingSubscriptionSuccess(VertxTestContext vertxTestContext) {

        lenient().when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(rabbitClient.bindQueue(any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(rabbitClient.updateUserPermissions(anyString(), anyString(), any(), anyString())).thenReturn(jsonObjectFuture);
        lenient().when(asyncResult1.succeeded()).thenReturn(true);
        lenient().when(asyncResult1.result()).thenReturn(request);
        lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.succeeded()) {
                assertEquals("{\"type\":\"urn:dx:rs:success\",\"title\":\"success\",\"results\":[{\"entities\":[\"ABCD/ABCD/ABCD/ABCD/ABCD\",\"EFGH/EFGH/EFGH/EFGH/EFGH\"]}]}",
                        handler.result().toString());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test appendStreamingSubscription method : for empty request")
    public void testAppendStreamingSubscriptionWithInvalidInput(VertxTestContext vertxTestContext) {
        service.appendStreamingSubscription(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test appendStreamingSubscription method : listQueueSubscribers Failure")
    public void testAppendStreamingSubscriptionWithInvalidPayload(VertxTestContext vertxTestContext) {

        when(rabbitClient.listQueueSubscribers(any(), anyString())).thenReturn(jsonObjectFuture);

        when(asyncResult1.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult1);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        service.appendStreamingSubscription(request).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test registerCallbackSubscription method: Null request")
    public void testRegisterCallbackSubscriptionWithInvalidInput(VertxTestContext vertxTestContext)
    {
        service.registerCallbackSubscription(null).onComplete( handler ->{
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test deleteCallbackSubscription method: Null request")
    public void testDeleteCallbackSubscriptionWithInvalidInput(VertxTestContext vertxTestContext)
    {
        service.deleteCallbackSubscription(null).onComplete( handler ->{
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}",
                        handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> listCallbackInputValues()
    {
        return Stream.of(
                Arguments.of(new JsonObject(), "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}"),
                Arguments.of(SubscriptionServiceTest.request, "{\"type\":400,\"title\":\"error\",\"detail\":\"Invalid request payload\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("listCallbackInputValues")
    @DisplayName("Test listCallbackSubscription method: with different input requests")
    public void testListCallbackSubscription(JsonObject input, String expected, VertxTestContext vertxTestContext)
    {
        lenient().when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        lenient().when(asyncResult.succeeded()).thenReturn(false);
        lenient().when(asyncResult.cause()).thenReturn(throwable);
        lenient().when(throwable.getMessage()).thenReturn("Dummy failure message");
        lenient().doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        service.listCallbackSubscription(input).onComplete( handler ->{
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }


}

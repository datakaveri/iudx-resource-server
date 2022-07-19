package iudx.resource.server.databroker.listeners;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQMessage;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.database.async.AsyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestAsyncQueryListener {
    AsyncQueryListener asyncQueryListener;
    @Mock
    Vertx vertx;
    @Mock
    RabbitMQOptions config;
    @Mock
    RabbitMQClient client;
    @Mock
    Future<Void> voidFuture;
    String vHost;
    @Mock
    AsyncResult<RabbitMQConsumer> consumerAsyncResult;
    @Mock
    RabbitMQConsumer rabbitMQConsumer;
    @Mock
    AsyncService asyncService;
    @Mock
    RabbitMQMessage message;
    @Mock
    Throwable throwable;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "Dummy vHost";
        asyncQueryListener = new AsyncQueryListener(vertx, config, vHost,asyncService);
        asyncQueryListener.client = mock(client.getClass());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test start method : Success")
    public void test_start_success(VertxTestContext vertxTestContext) {
        when(consumerAsyncResult.succeeded()).thenReturn(true);
        when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
        when(asyncQueryListener.client.start()).thenReturn(voidFuture);
        JsonObject object = new JsonObject();
        object.put("requestId", "dummy_key");
        object.put("searchId", "Dummy_unique-attribute");
        object.put("sub", "Dummy_value");
        object.put("query",new JsonObject());
        Buffer buffer = Buffer.buffer(object.toString());
        when(voidFuture.succeeded()).thenReturn(true);
        when(message.body()).thenReturn(buffer);
        doAnswer(new Answer<RabbitMQMessage>() {
            @Override
            public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
                return null;
            }
        }).when(rabbitMQConsumer).handler(any());

        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
                return null;
            }
        }).when(asyncQueryListener.client).basicConsumer(anyString(), any(), any());

        asyncQueryListener.start();
        verify(voidFuture, times(1)).succeeded();
        verify(asyncService).asyncSearch(anyString(),anyString(),any(),any());
        verify(message).body();
        assertEquals(buffer, message.body());
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test start method : with null message body")
    public void test_start_with_null_body(VertxTestContext vertxTestContext) {
        when(consumerAsyncResult.succeeded()).thenReturn(true);
        when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
        when(asyncQueryListener.client.start()).thenReturn(voidFuture);
        when(voidFuture.succeeded()).thenReturn(true);
        when(message.body()).thenReturn(null);
        doAnswer(new Answer<RabbitMQMessage>() {
            @Override
            public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
                return null;
            }
        }).when(rabbitMQConsumer).handler(any());

        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
                return null;
            }
        }).when(asyncQueryListener.client).basicConsumer(anyString(), any(), any());

        asyncQueryListener.start();
        verify(voidFuture).succeeded();
        verify(message).body();
        assertEquals(null, message.body());
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test start method : Failure")
    public void test_start_failure(VertxTestContext vertxTestContext) {
        when(asyncQueryListener.client.start()).thenReturn(voidFuture);
        when(voidFuture.succeeded()).thenReturn(false);
        when(voidFuture.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn("Dummy failure message");
        asyncQueryListener.start();
        verify(voidFuture).succeeded();
        assertFalse(voidFuture.succeeded());
        vertxTestContext.completeNow();
    }
}

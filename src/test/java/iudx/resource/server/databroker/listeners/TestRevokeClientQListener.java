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
import iudx.resource.server.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestRevokeClientQListener {
    RevokeClientQListener revokeClientQListener;
    @Mock
    Vertx vertx;
    @Mock
    CacheService cache;
    @Mock
    RabbitMQOptions config;
    @Mock
    RabbitMQClient client;
    @Mock
    Future<Void> voidFuture;
    String vHost;
    @Mock
    AsyncResult<JsonObject> jsonObjectAsyncResult;
    @Mock
    AsyncResult<RabbitMQConsumer> consumerAsyncResult;
    @Mock
    RabbitMQConsumer rabbitMQConsumer;
    @Mock
    RabbitMQMessage message;
    @Mock
    Throwable throwable;
    @Mock
    Void event;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "Dummy vHost";
        revokeClientQListener = new RevokeClientQListener(vertx, cache, config, vHost);
        revokeClientQListener.client = mock(client.getClass());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test start method : Success")
    public void test_start_success(VertxTestContext vertxTestContext) {
        when(consumerAsyncResult.succeeded()).thenReturn(true);
        when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
        when(revokeClientQListener.client.start()).thenReturn(voidFuture);
        JsonObject object = new JsonObject();
        object.put("id", "dummy_key");
        object.put("unique-attribute", "Dummy_unique-attribute");
        Buffer buffer = Buffer.buffer(object.toString());
        when(message.body()).thenReturn(buffer);
        when(jsonObjectAsyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<RabbitMQMessage>() {
            @Override
            public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
                return null;
            }
        }).when(rabbitMQConsumer).handler(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(jsonObjectAsyncResult);
                return null;
            }
        }).when(cache).refresh(any(), any());
        lenient().doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Void>) arg0.getArgument(0)).handle(event);
                return null;
            }
        }).when(voidFuture).onSuccess(any());
        lenient().doAnswer(new Answer<Throwable>() {
            @Override
            public Throwable answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Throwable>) arg0.getArgument(0)).handle(throwable);
                return null;
            }
        }).when(voidFuture).onFailure(any());
        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
                return null;
            }
        }).when(revokeClientQListener.client).basicConsumer(anyString(), any(), any());

        assertThrows(NullPointerException.class,()-> revokeClientQListener.start());
        verify(voidFuture, times(1)).onSuccess(any());
        verify(message).body();
        verify(jsonObjectAsyncResult).succeeded();
        assertEquals(buffer, message.body());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test start method : Failure in cacheHandler")
    public void test_start_cache_handler_failure(VertxTestContext vertxTestContext) {
        when(consumerAsyncResult.succeeded()).thenReturn(true);
        when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
        when(revokeClientQListener.client.start()).thenReturn(voidFuture);
        JsonObject object = new JsonObject();
        object.put("id", "dummy_key");
        object.put("unique-attribute", "Dummy_unique-attribute");
        Buffer buffer = Buffer.buffer(object.toString());
        when(message.body()).thenReturn(buffer);
        when(jsonObjectAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<RabbitMQMessage>() {
            @Override
            public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
                return null;
            }
        }).when(rabbitMQConsumer).handler(any());
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(jsonObjectAsyncResult);
                return null;
            }
        }).when(cache).refresh(any(), any());
        lenient().doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Void>) arg0.getArgument(0)).handle(event);
                return null;
            }
        }).when(voidFuture).onSuccess(any());
        lenient().doAnswer(new Answer<Throwable>() {
            @Override
            public Throwable answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Throwable>) arg0.getArgument(0)).handle(throwable);
                return null;
            }
        }).when(voidFuture).onFailure(any());
        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
                return null;
            }
        }).when(revokeClientQListener.client).basicConsumer(anyString(), any(), any());

        assertThrows(NullPointerException.class,()-> revokeClientQListener.start());
        verify(voidFuture, times(1)).onSuccess(any());
        verify(message).body();
        verify(jsonObjectAsyncResult).succeeded();
        assertEquals(buffer, message.body());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test start method : with null message body")
    public void test_start_with_null_body(VertxTestContext vertxTestContext) {
        when(consumerAsyncResult.succeeded()).thenReturn(true);
        when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
        when(revokeClientQListener.client.start()).thenReturn(voidFuture);
        JsonObject object = new JsonObject();
        object.put("id", "dummy_key");
        object.put("unique-attribute", "Dummy_unique-attribute");
        when(message.body()).thenReturn(null);
        doAnswer(new Answer<RabbitMQMessage>() {
            @Override
            public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
                return null;
            }
        }).when(rabbitMQConsumer).handler(any());

        lenient().doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Void>) arg0.getArgument(0)).handle(event);
                return null;
            }
        }).when(voidFuture).onSuccess(any());
        lenient().doAnswer(new Answer<Throwable>() {
            @Override
            public Throwable answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<Throwable>) arg0.getArgument(0)).handle(throwable);
                return null;
            }
        }).when(voidFuture).onFailure(any());
        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
                return null;
            }
        }).when(revokeClientQListener.client).basicConsumer(anyString(), any(), any());

        assertThrows(NullPointerException.class,()-> revokeClientQListener.start());
        verify(voidFuture).onSuccess(any());
        verify(message).body();
        assertEquals(null, message.body());
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test start method : Failure")
    public void test_start_failure(VertxTestContext vertxTestContext) {
        when(revokeClientQListener.client.start()).thenReturn(voidFuture);
        when(voidFuture.succeeded()).thenReturn(false);
        assertThrows(NullPointerException.class,()-> revokeClientQListener.start());
        assertFalse(voidFuture.succeeded());
        vertxTestContext.completeNow();
    }
}

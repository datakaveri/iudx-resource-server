package iudx.resource.server.databroker.listeners;

import static iudx.resource.server.common.BroadcastEventType.CREATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
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


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestUniqueAttribQlistener {
  UniqueAttribQlistener uniqueAttribQListener;
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
    uniqueAttribQListener = new UniqueAttribQlistener(vertx, cache, config, vHost);
    uniqueAttribQListener.client = mock(client.getClass());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : Success")
  public void test_start_success(VertxTestContext vertxTestContext) {

    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);

    when(consumerAsyncResult.succeeded()).thenReturn(true);
    when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(uniqueAttribQListener.client.start()).thenReturn(voidFuture);
    JsonObject object = new JsonObject();
    object.put("id", "dummy_key");
    object.put("unique-attribute", "Dummy_unique-attribute");
    object.put("eventType", CREATE);
    Buffer buffer = Buffer.buffer(object.toString());
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(message.body()).thenReturn(buffer);


    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

    doAnswer(new Answer<RabbitMQMessage>() {
      @Override
      public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
        return null;
      }
    }).when(rabbitMQConsumer).handler(any());
    when(cache.refresh(any())).thenReturn(Future.succeededFuture(new JsonObject()));
    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
        return null;
      }
    }).when(uniqueAttribQListener.client).basicConsumer(anyString(), any(), any());

    uniqueAttribQListener.start();
    verify(voidFuture, times(1)).onComplete(any());
    verify(message).body();
    verify(cache,times(1)).refresh(any());
    assertEquals(buffer, message.body());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : Failure in cacheHandler")
  public void test_start_cache_handler_failure(VertxTestContext vertxTestContext) {

    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);

    when(consumerAsyncResult.succeeded()).thenReturn(true);
    when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(uniqueAttribQListener.client.start()).thenReturn(voidFuture);
    JsonObject object = new JsonObject();
    object.put("id", "dummy_key");
    object.put("unique-attribute", "Dummy_unique-attribute");
    object.put("eventType", CREATE);
    Buffer buffer = Buffer.buffer(object.toString());
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(message.body()).thenReturn(buffer);

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

    doAnswer(new Answer<RabbitMQMessage>() {
      @Override
      public RabbitMQMessage answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<RabbitMQMessage>) arg0.getArgument(0)).handle(message);
        return null;
      }
    }).when(rabbitMQConsumer).handler(any());
    when(cache.refresh(any())).thenReturn(Future.failedFuture(""));
    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(consumerAsyncResult);
        return null;
      }
    }).when(uniqueAttribQListener.client).basicConsumer(anyString(), any(), any());

    uniqueAttribQListener.start();
    verify(voidFuture, times(1)).onComplete(any());
    verify(message).body();
    verify(cache,times(1)).refresh(any());
    assertEquals(buffer, message.body());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : with null message body")
  public void test_start_with_null_body(VertxTestContext vertxTestContext) {
    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);
    when(consumerAsyncResult.succeeded()).thenReturn(true);
    when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(uniqueAttribQListener.client.start()).thenReturn(voidFuture);
    JsonObject object = new JsonObject();
    object.put("id", "dummy_key");
    object.put("unique-attribute", "Dummy_unique-attribute");
    object.put("eventType", CREATE);
    Buffer buffer = Buffer.buffer(object.toString());
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(message.body()).thenReturn(null);

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());
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
    }).when(uniqueAttribQListener.client).basicConsumer(anyString(), any(), any());

    uniqueAttribQListener.start();
    verify(voidFuture).onComplete(any());
    verify(message).body();
    assertEquals(null, message.body());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : with invalid BroadcastEventType")
  public void test_start_with_null_event(VertxTestContext vertxTestContext) {
    AsyncResult<Void> clientStartAsyncResult = mock(AsyncResult.class);
    Void event = mock(Void.class);
    when(consumerAsyncResult.succeeded()).thenReturn(true);
    when(consumerAsyncResult.result()).thenReturn(rabbitMQConsumer);
    when(uniqueAttribQListener.client.start()).thenReturn(voidFuture);
    JsonObject object = new JsonObject();
    object.put("id", "dummy_key");
    object.put("unique-attribute", "Dummy_unique-attribute");
    object.put("eventType", "Dummy_eventType");
    Buffer buffer = Buffer.buffer(object.toString());
    when(clientStartAsyncResult.succeeded()).thenReturn(true);
    when(message.body()).thenReturn(buffer);

    doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
      @Override
      public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<Void>>) arg0.getArgument(0)).handle(clientStartAsyncResult);
        return null;
      }
    }).when(voidFuture).onComplete(any());

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
    }).when(uniqueAttribQListener.client).basicConsumer(anyString(), any(), any());

    uniqueAttribQListener.start();
    verify(voidFuture).onComplete(any());
    verify(message).body();
    assertEquals(buffer, message.body());
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test start method : Failure")
  public void test_start_failure(VertxTestContext vertxTestContext) {
    when(uniqueAttribQListener.client.start()).thenReturn(voidFuture);
    when(voidFuture.succeeded()).thenReturn(false);
    uniqueAttribQListener.start();
    verify(voidFuture).onComplete(any());
    assertFalse(voidFuture.succeeded());
    vertxTestContext.completeNow();
  }
}

package iudx.resource.server.databroker.listeners;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
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
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @Mock
    Void event;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "Dummy vHost";
        asyncQueryListener = new AsyncQueryListener(vertx, config, vHost,asyncService);
        asyncQueryListener.client = mock(client.getClass());
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test start method : Failure")
    public void test_start_failure(VertxTestContext vertxTestContext) {
        when(asyncQueryListener.client.start()).thenReturn(voidFuture);
        assertThrows(NullPointerException.class, () -> asyncQueryListener.start());
        verify(voidFuture).onSuccess(any());
        vertxTestContext.completeNow();
    }
}

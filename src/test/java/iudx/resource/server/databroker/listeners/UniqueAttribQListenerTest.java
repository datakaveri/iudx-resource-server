package iudx.resource.server.databroker.listeners;

import io.netty.util.Recycler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.resource.server.cache.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class UniqueAttribQListenerTest {
    UniqueAttribQListener uniqueAttribQListener;
    @Mock
    Vertx vertx;
    @Mock
    CacheService cache;
    @Mock
    RabbitMQOptions config;
    @Mock
    AsyncResult<RabbitMQConsumer>rmqConsumer;
    @Mock
    JsonObject json;

    @Mock
    RabbitMQClient client;
    @Mock
    RabbitMQConsumer rabbitMQConsumer;
    @Mock
    Future<Void> startObj;
    @Mock
    Handler onSuccessObj;

    @Test
    @DisplayName("Test start method success")
    public void testStart(VertxTestContext vertxTestContext)
    {
        String vhost="internalVhost";

        uniqueAttribQListener=new UniqueAttribQListener(vertx,cache,config,vhost);
        when(client.start()).thenReturn(startObj);
        when(rmqConsumer.succeeded()).thenReturn(true);
        when(rmqConsumer.result()).thenReturn(rabbitMQConsumer);
        when(json.getString(anyString())).thenReturn("dummy attribute");
        doAnswer(new Answer<AsyncResult<RabbitMQConsumer>>() {
            @Override
            public AsyncResult<RabbitMQConsumer> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQConsumer>>) arg0.getArgument(2)).handle(rmqConsumer);
                return null;
            }
        }).when(client).basicConsumer(anyString(),any(),any());

     /*   doAnswer(new Answer<AsyncResult<RabbitMQOptions>>() {
            @Override
            public AsyncResult<RabbitMQOptions> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RabbitMQOptions>>) arg0.getArgument(0)).handle(rmqConsumer);
                return null;
            }
        }).when(client).basicConsumer(anyString(),any(),any());
        uniqueAttribQListener.start();*/
        uniqueAttribQListener.start();




    }
}

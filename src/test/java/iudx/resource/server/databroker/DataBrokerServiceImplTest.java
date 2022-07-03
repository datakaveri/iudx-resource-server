package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.resource.server.databroker.util.Constants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class DataBrokerServiceImplTest {

    JsonObject request;
    String throwableMessage;
    DataBrokerServiceImpl databroker;
    String vHost;
    @Mock
    Future<JsonObject> jsonObjectFuture;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    Throwable throwable;
    @Mock
    RabbitClient webClient;
    @Mock
    PostgresClient pgClient;


    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        vHost = "IUDX_INTERNAL";
        JsonObject config = mock(JsonObject.class);
        request = new JsonObject();
        request.put("Dummy key", "Dummy value");
        request.put(ID,"Dummy ID");
        request.put("status","Dummy status");
        request.put("routingKey","routingKeyValue");
        request.put("type", HttpStatus.SC_OK);
        throwableMessage = "Dummy failure message";
        when(config.getString(anyString())).thenReturn("internalVhost");
        databroker = new DataBrokerServiceImpl(webClient, pgClient, config);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test registerCallbackSubscription method : Success")
    public void testRegisterCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.registerCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());
        databroker.registerCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test registerCallbackSubscription : Failure")
    public void testRegisterCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.registerCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());


        databroker.registerCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test updateCallbackSubscription method : Success")
    public void testUpdateCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.updateCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test updateCallbackSubscription : Failure")
    public void testUpdateCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.updateCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.updateCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteCallbackSubscription method : Success")
    public void testDeleteCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.deleteCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteCallbackSubscription : Failure")
    public void testDeleteCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.deleteCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.deleteCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listCallbackSubscription method : Success")
    public void testListCallbackSubscriptionSuccess(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(request);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.listCallbackSubscription(request, handler -> {
            if (handler.succeeded()) {
                assertEquals(request, handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listCallbackSubscription : Failure")
    public void testListCallbackSubscriptionFailure(VertxTestContext vertxTestContext) {

        DataBrokerServiceImpl.subscriptionService = mock(SubscriptionService.class);
        when(DataBrokerServiceImpl.subscriptionService.listCallbackSubscription(any())).thenReturn(jsonObjectFuture);
        when(asyncResult.failed()).thenReturn(true);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(jsonObjectFuture).onComplete(any());

        databroker.listCallbackSubscription(request, handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


}

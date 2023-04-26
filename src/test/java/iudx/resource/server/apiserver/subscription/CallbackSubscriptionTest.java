package iudx.resource.server.apiserver.subscription;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class CallbackSubscriptionTest {
    CallbackSubscription subscription;
    @Mock
    DataBrokerService databroker;
    @Mock
    PostgresService pgService;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    Throwable throwable;
    String throwableMessage;
    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        subscription = new CallbackSubscription(databroker);
        throwableMessage = "Dummy failure message";
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test create method : Success")
    public void test_create_success(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).registerCallbackSubscription(any(), any());
        subscription.create(new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNull(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test create method : Failure")
    public void test_create_failure(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).registerCallbackSubscription(any(), any());
        subscription.create(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test update method : Success")
    public void test_update_success(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateCallbackSubscription(any(), any());
        subscription.update(new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNull(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test update method : Failure")
    public void test_update_failure(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateCallbackSubscription(any(), any());
        subscription.update(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test append method : Success")
    public void test_append_success(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateCallbackSubscription(any(), any());
        subscription.append(new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNull(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test append method : Failure")
    public void test_append_failure(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).updateCallbackSubscription(any(), any());
        subscription.append(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test delete method : Success")
    public void test_delete_success(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).deleteStreamingSubscription(any(), any());
        subscription.delete(new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNull(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test delete method : Failure")
    public void test_delete_failure(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).deleteStreamingSubscription(any(), any());
        subscription.delete(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test get method : Success")
    public void test_get_success(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).listCallbackSubscription(any(), any());
        subscription.get(new JsonObject()).onComplete(handler -> {
            if (handler.succeeded()) {
                assertNull(handler.result());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test get method : Failure")
    public void test_get_failure(VertxTestContext vertxTestContext) {
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        when(throwable.getMessage()).thenReturn(throwableMessage);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(databroker).listCallbackSubscription(any(), any());
        subscription.get(new JsonObject()).onComplete(handler -> {
            if (handler.failed()) {
                assertEquals("Dummy failure message",handler.cause().getMessage());
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
}

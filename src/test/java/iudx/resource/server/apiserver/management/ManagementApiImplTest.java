package iudx.resource.server.apiserver.management;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.common.ResponseUrn;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import static iudx.resource.server.databroker.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class ManagementApiImplTest {
  ManagementApiImpl managementApi;
  @Mock DataBrokerService dataBrokerService;
  @Mock PostgresService postgresService;
  @Mock CacheService cacheService;
  @Mock AsyncResult<JsonObject> asyncResult, asyncResultForBroker;
  @Mock Throwable throwable;
  JsonObject json;
  JsonArray jsonArray = new JsonArray();
  @Mock JsonObject mockJsonObject;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    json = new JsonObject();

    json.put("Dummy key", "Dummy value");
    json.put(
        "entities",
        jsonArray.add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));
    json.put(USER_ID, "dummy user");
    managementApi = new ManagementApiImpl();
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test createExchange method for succeeded Aync result")
  public void testCreateExchangeSuccess(VertxTestContext vertxTestContext) {

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createExchange(any(), any(), any());
    managementApi
        .createExchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).createExchange(any(), any(), any());
  }

  @Test
  @DisplayName("Test createExchange method for failure in Aync result")
  public void testCreateExchangeFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy failure message");
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createExchange(any(), any(), any());
    managementApi
        .createExchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy failure message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Succeeded for " + handler.cause());
              }
            });
    verify(dataBrokerService).createExchange(any(), any(), any());
  }

  @Test
  @DisplayName("Test deleteExchange method for Success")
  public void testDeleteExchangeSuccess(VertxTestContext vertxTestContext) {
    String exchangeID = "Dummy exchange ID";

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteExchange(any(), anyString(), any());

    managementApi
        .deleteExchange(exchangeID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteExchange(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test deleteExchange method for Failure")
  public void testDeleteExchangeFailure(VertxTestContext vertxTestContext) {
    String exchangeID = "Dummy exchange ID";
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteExchange(any(), anyString(), any());

    managementApi
        .deleteExchange(exchangeID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteExchange(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getExchangeDetails method for Success")
  public void testGetExchangeDetailsSuccess(VertxTestContext vertxTestContext) {
    String exchangeID = "Dummy exchange ID";

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listExchangeSubscribers(any(), anyString(), any());

    managementApi
        .getExchangeDetails(exchangeID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listExchangeSubscribers(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getExchangeDetails method for Failure")
  public void testGetExchangeDetailsFailure(VertxTestContext vertxTestContext) {
    String exchangeID = "Dummy exchange ID";
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listExchangeSubscribers(any(), anyString(), any());

    managementApi
        .getExchangeDetails(exchangeID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listExchangeSubscribers(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test createQueue method for Success")
  public void testCreateQueueSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createQueue(any(), anyString(), any());

    managementApi
        .createQueue(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).createQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test createQueue method for Failure")
  public void testCreateQueueFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createQueue(any(), anyString(), any());

    managementApi
        .createQueue(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).createQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test deleteQueue method for Success")
  public void testDeleteQueueSuccess(VertxTestContext vertxTestContext) {
    String queueID = "Dummy queue ID";

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteQueue(any(), anyString(), any());

    managementApi
        .deleteQueue(queueID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test deleteQueue method for Failure")
  public void testDeleteQueueFailure(VertxTestContext vertxTestContext) {
    String queueID = "Dummy queue ID";
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteQueue(any(), anyString(), any());

    managementApi
        .deleteQueue(queueID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getQueueDetails method for Success")
  public void testGetQueueDetailsSuccess(VertxTestContext vertxTestContext) {
    String queueID = "Dummy queue ID";

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listQueueSubscribers(any(), anyString(), any());

    managementApi
        .getQueueDetails(queueID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listQueueSubscribers(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getQueueDetails method for Failure")
  public void testGetQueueDetailsFailure(VertxTestContext vertxTestContext) {
    String queueID = "Dummy queue ID";
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listQueueSubscribers(any(), anyString(), any());

    managementApi
        .getQueueDetails(queueID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listQueueSubscribers(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test bindQueue2Exchange method for Success")
  public void testBindQueue2ExchangeSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .bindQueue(any(), anyString(), any());

    managementApi
        .bindQueue2Exchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).bindQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test bindQueue2Exchange method for Failure")
  public void testBindQueue2ExchangeFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .bindQueue(any(), anyString(), any());
    managementApi
        .bindQueue2Exchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).bindQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test unbindQueue2Exchange method for Success")
  public void testUnbindQueue2ExchangeSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .unbindQueue(any(), anyString(), any());

    managementApi
        .unbindQueue2Exchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).unbindQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test unbindQueue2Exchange method for Failure")
  public void testUnbindQueue2ExchangeFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .unbindQueue(any(), anyString(), any());

    managementApi
        .unbindQueue2Exchange(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).unbindQueue(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test createVHost method for Success")
  public void testCreateVHostSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createvHost(any(), any());

    managementApi
        .createVHost(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).createvHost(any(), any());
  }

  @Test
  @DisplayName("Test createVHost method for Failure")
  public void testCreateVHostFailure(VertxTestContext vertxTestContext) {

    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .createvHost(any(), any());

    managementApi
        .createVHost(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).createvHost(any(), any());
  }

  @Test
  @DisplayName("Test deleteVHost method for Success")
  public void testDeleteVHostuccess(VertxTestContext vertxTestContext) {
    String vhostID = "Dummy vhostID";
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deletevHost(any(), any());

    managementApi
        .deleteVHost(vhostID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deletevHost(any(), any());
  }

  @Test
  @DisplayName("Test deleteVHost method for Failure")
  public void testDeleteVHostFailure(VertxTestContext vertxTestContext) {
    String vhostID = "Dummy vhostID";
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deletevHost(any(), any());

    managementApi
        .deleteVHost(vhostID, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deletevHost(any(), any());
  }

  @Test
  @DisplayName("Test registerAdapter method for Success")
  public void testRegisterAdapterSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    JsonObject expectedJSON = new JsonObject();
    expectedJSON.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
    expectedJSON.put(TITLE, "Success");
    expectedJSON.put(RESULTS, new JsonArray().add(json));

    when(mockJsonObject.getString("id")).thenReturn("dummy_id");
    when(mockJsonObject.getString("name")).thenReturn("dummy_name");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(mockJsonObject));
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .registerAdaptor(any(), anyString(), any());

    managementApi
        .registerAdapter(json, dataBrokerService, cacheService, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(expectedJSON, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).registerAdaptor(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test registerAdapter method for Cache Failure")
  public void testRegisterAdapterFailureForCache(VertxTestContext vertxTestContext) {
    when(cacheService.get(any())).thenReturn(Future.failedFuture("Failed."));

    managementApi
        .registerAdapter(json, dataBrokerService, cacheService, postgresService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(
                    new JsonObject()
                        .put("type", 404)
                        .put("title", "urn:dx:rs:resourceNotFound")
                        .toString(),
                    handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test registerAdapter method for Postgres Failure")
  public void testRegisterAdapterFailureForPostgres(VertxTestContext vertxTestContext) {
    when(mockJsonObject.getString("id")).thenReturn("dummy_id");
    when(mockJsonObject.getString("name")).thenReturn("dummy_name");
    when(cacheService.get(any())).thenReturn(Future.succeededFuture(mockJsonObject));
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    managementApi
        .registerAdapter(json, dataBrokerService, cacheService, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause().getMessage());
              } else {
                assertEquals(
                    new JsonObject()
                        .put("type", 409)
                        .put("title", "urn:dx:rs:resourceAlreadyExist")
                        .toString(),
                    handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test registerAdapter method for DataBroker Failure")
  public void testRegisterAdapterFailureForBroker(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResultForBroker.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");

    when(mockJsonObject.getString("id")).thenReturn("dummy_id");
    when(mockJsonObject.getString("name")).thenReturn("dummy_name");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    when(cacheService.get(any())).thenReturn(Future.succeededFuture(mockJsonObject));
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2))
                    .handle(asyncResultForBroker);
                return null;
              }
            })
        .when(dataBrokerService)
        .registerAdaptor(any(), anyString(), any());

    managementApi
        .registerAdapter(json, dataBrokerService, cacheService, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause().getMessage());
              } else {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("Test deleteAdapter method for Success")
  public void testDeleteAdapterSuccess(VertxTestContext vertxTestContext) {
    String adapterId = "Dummy adapterId ID";
    String userId = "Dummy userId ID";
    JsonObject expectedJSON = new JsonObject();
    expectedJSON.put(TYPE, ResponseUrn.SUCCESS_URN.getUrn());
    expectedJSON.put(TITLE, "Success");
    expectedJSON.put(RESULTS, "Adapter deleted");

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteAdaptor(any(), anyString(), any());

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    managementApi
        .deleteAdapter(adapterId, userId, dataBrokerService, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(expectedJSON, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteAdaptor(any(), anyString(), any());
  }

  public static Stream<Arguments> responseData() {
    return Stream.of(
        Arguments.of(
            "{ \"type\":409 }",
            "{\"type\":409,\"title\":\"Already exists\",\"detail\":\"Already exists\"}"),
        Arguments.of(
            "{ \"type\":200, \"detail\" : \"Dummy detail\" }",
            "{\"type\":200,\"title\":\"Ok\",\"detail\":\"Dummy detail\"}"),
        Arguments.of(
            "{ \"type\":400, \"detail\" : \"Dummy detail\" }",
            "{\"type\":400,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}"),
        Arguments.of(
            "{ \"type\":404 }",
            "{\"type\":404,\"title\":\"Not Found\",\"detail\":\"Resource not found\"}"),
        Arguments.of(
            "{ \"type\":500, \"detail\" : \"Dummy detail\"  }",
            "{\"type\":400,\"title\":\"Bad Request\",\"detail\":\"Dummy detail\"}"));
  }

  @ParameterizedTest
  @MethodSource("responseData")
  @DisplayName("Test deleteAdapter method for Failure")
  public void testDeleteAdapterFailure(
      String type, String expected, VertxTestContext vertxTestContext) {
    String adapterId = "Dummy adapterId ID";
    String userId = "Dummy userId ID";
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn(type);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .deleteAdaptor(any(), anyString(), any());

    managementApi
        .deleteAdapter(adapterId, userId, dataBrokerService, postgresService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals(expected, handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).deleteAdaptor(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getAdapterDetails method for Success")
  public void testGetAdapterDetailsSuccess(VertxTestContext vertxTestContext) {
    String adapterId = "Dummy adapterId ID";
    when(asyncResult.succeeded()).thenReturn(true);
    json.remove("entities");
    json.remove(USER_ID);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listAdaptor(any(), anyString(), any());

    managementApi
        .getAdapterDetails(adapterId, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(
                    "{\"type\":\"urn:dx:rs:success\",\"title\":\"Success\",\"results\":[{\"Dummy"
                        + " key\":\"Dummy value\"}]}",
                    handler.result().toString());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listAdaptor(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test getAdapterDetails method for Failure")
  public void testGetAdapterDetailsFailure(VertxTestContext vertxTestContext) {
    String adapterId = "Dummy adapterId ID";
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .listAdaptor(any(), anyString(), any());

    managementApi
        .getAdapterDetails(adapterId, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).listAdaptor(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishHeartbeat method for Success")
  public void testPublishHeartbeatSuccess(VertxTestContext vertxTestContext) {
    json.put(JSON_TYPE, "success");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishHeartbeat(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishHeartbeat method for Failure")
  public void testPublishHeartbeatFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishHeartbeat(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDownstreamIssues method for Success")
  public void testPublishDownstreamIssuesSuccess(VertxTestContext vertxTestContext) {
    json.put(JSON_TYPE, "success");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishDownstreamIssues(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDownstreamIssues method for Failure")
  public void testPublishDownstreamIssuesFailure(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishDownstreamIssues(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDataIssue method for Success")
  public void testPublishDataIssueSuccess(VertxTestContext vertxTestContext) {
    json.put(JSON_TYPE, "success");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishDataIssue(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDataIssue method for Failure")
  public void testPublishDataIssueFailure(VertxTestContext vertxTestContext) {

    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishHeartbeat(any(), anyString(), any());

    managementApi
        .publishDataIssue(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishHeartbeat(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDataFromAdapter method for Success")
  public void testPublishDataFromAdapterSuccess(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishFromAdaptor(any(), anyString(), any());

    managementApi
        .publishDataFromAdapter(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals(json, handler.result());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishFromAdaptor(any(), anyString(), any());
  }

  @Test
  @DisplayName("Test publishDataFromAdapter method for Failure")
  public void testPublishDataFromAdapter(VertxTestContext vertxTestContext) {
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("Dummy throwable message");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg2) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg2.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishFromAdaptor(any(), anyString(), any());

    managementApi
        .publishDataFromAdapter(json, dataBrokerService)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                assertEquals("Dummy throwable message", handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    verify(dataBrokerService).publishFromAdaptor(any(), anyString(), any());
  }

  @Test
  public void testPublishAllAdapterForUser(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject().put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(mockJsonObject);
    when(mockJsonObject.getString("title")).thenReturn("success");
    when(mockJsonObject.getString("type")).thenReturn("urn:dx:rs:success");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    managementApi
        .getAllAdapterDetailsForUser(jsonObject, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals("success", handler.result().getString("title"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  public void testPublishAllAdapterForUserFailure(VertxTestContext vertxTestContext) {
    JsonObject jsonObject =
        new JsonObject().put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeQuery(anyString(), any());

    managementApi
        .getAllAdapterDetailsForUser(jsonObject, postgresService)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                assertEquals("success", handler.result().getString("title"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.failNow("");
              } else {
                assertEquals(
                    new JsonObject().put("type", 400).toString(), handler.cause().getMessage());
                vertxTestContext.completeNow();
              }
            });
  }
}

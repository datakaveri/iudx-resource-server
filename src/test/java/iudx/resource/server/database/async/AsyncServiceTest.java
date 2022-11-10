package iudx.resource.server.database.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.QueryBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import com.amazonaws.regions.Regions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.async.util.Util;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.postgres.PostgresService;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AsyncServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceTest.class);

  private static AsyncServiceImpl asyncService;
  private static AsyncServiceImpl asyncServiceSpy;
  private static Configuration config;
  private static ElasticClient client;
  private static PostgresService pgService;
  private static S3FileOpsHelper fileOpsHelper;
  private static Util util;
  private static File file;
  private static Regions clientRegion;
  private static JsonObject asyncConfig;
  private static String databaseIP;
  private static String user;
  private static String password;
  private static String timeLimit;
  private static int databasePort;
  private static String filePath;
  private static String bucketName;
  private static AsyncResult<JsonObject> asyncResult1, asyncResult2;
  static AsyncServiceImpl asyncService2;
  static AsyncFileScrollProgressListener listener;
  @Mock
  static PostgresService postgresService;
  @Mock
  Throwable throwable;
  @Mock
  JsonArray jsonArray;
  @Mock
  JsonObject jsonObject;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy async verticle")
  static void init(Vertx vertx, VertxTestContext testContext) throws MalformedURLException {
    config = new Configuration();
    asyncConfig = config.configLoader(8, vertx);
    timeLimit = asyncConfig.getString("timeLimit");
    filePath = asyncConfig.getString("filePath");

    file = mock(File.class);
    client = mock(ElasticClient.class);
    pgService = mock(PostgresService.class);
    fileOpsHelper = mock(S3FileOpsHelper.class);
    util = mock(Util.class);

    asyncService =
        new AsyncServiceImpl(vertx, client, pgService, fileOpsHelper, timeLimit, filePath);
    asyncServiceSpy = spy(asyncService);

    asyncResult1 = mock(AsyncResult.class);
    asyncResult2 = mock(AsyncResult.class);

    URL url = new URL("https://www.example.com");
    when(fileOpsHelper.generatePreSignedUrl(anyLong(), any())).thenReturn(url);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult1);
                return null;
              }
            })
        .when(pgService)
        .executeQuery(any(), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(6)).handle(asyncResult1);
                return null;
              }
            })
        .when(client)
        .scrollAsync(any(File.class), any(), any(QueryBuilder.class),any(), any(), any(),any());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult2);
                return null;
              }
            })
        .when(fileOpsHelper)
        .s3Upload(any(File.class), any(), any());

    LOGGER.info("Async Test steup complete");
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("setup test is passing");
    testContext.completeNow();
  }

  public JsonObject query() {
    JsonObject query =
        new JsonObject()
            .put(
                "id",
                new JsonArray()
                    .add(
                        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"))
            .put("time", "2020-10-10T14:20:00Z")
            .put("endtime", "2020-10-20T14:20:00Z")
            .put("timerel", "during")
            .put("searchType", "temporalSearch");

    return query;
  }

  public JsonArray record() {
    JsonArray record = new JsonArray();
    record.add(
        new JsonObject()
            .put("_id", "4c030b19-4954-4e56-868a-c36d80a77902")
                .put("search_id", "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe")
                .put("request_id", "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e")
                .put("status", "COMPLETE")
                .put("s3_url", "https://example.com")
                .put("expiry", "2022-03-02T16:08:38.495665")
                .put("user_id", "15c7506f-c800-48d6-adeb-0542b03947c6")
                .put("object_id", "b8a47206-364c-4580-8885-45205118db57")
                .put("size", 0));

    return record;
  }

  @Test
  @DisplayName("success - async search for existing request id")
  public void successfulAsyncSearchForExistingRecordTest(VertxTestContext testContext) {

    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.succeededFuture(record))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.succeededFuture()).when(asyncServiceSpy).executePGQuery(any());

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);

    verify(asyncServiceSpy, times(2)).process4ExistingRequestId(any(),any(), any(), any(), any());
    verify(asyncServiceSpy, times(2)).executePGQuery(any());
    verify(fileOpsHelper, times(2)).generatePreSignedUrl(anyLong(), any());
    testContext.completeNow();
  }

  @Test
  @DisplayName("fail - async search for existing request id")
  public void failAsyncSearchForExistingRecordTest(VertxTestContext testContext) {
    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = query();

    doAnswer(Answer -> Future.succeededFuture(record))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.failedFuture("fail")).when(asyncServiceSpy).executePGQuery(any());

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);
    testContext.completeNow();
  }

  @Test
  @DisplayName("fail download from es - async search")
  public void failDownloadForNewRequestId(Vertx vertx, VertxTestContext testContext) {
    String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonObject query = query();

    doAnswer(Answer -> Future.failedFuture("record doesn't exist"))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());

    when(asyncResult1.succeeded()).thenReturn(false);

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);
    testContext.completeNow();
  }

  @Test
  @DisplayName("fail upload to s3 - async search")
  public void failUploadForNewRequestId(Vertx vertx, VertxTestContext testContext) {
    String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonObject query = query();

    doAnswer(Answer -> Future.failedFuture("record doesn't exist"))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult2.succeeded()).thenReturn(false);

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);
    testContext.completeNow();
  }

  @Test
  @DisplayName("success - async status")
  public void successfulAsyncStatus(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.succeeded()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("async status fail - incorrect searchID")
  public void asyncStatusIncorrectSearchID(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51234";

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", new JsonArray()));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.failed()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("async status fail - incorrect user")
  public void asyncSearchIncorrectUser(VertxTestContext testContext) {

    String sub = "15c7506f-c800-48d6-adeb-0542b0394321";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService.asyncStatus(
        sub,
        searchId,
        handler -> {
          if (handler.failed()) {
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @DisplayName("success - get record for request id")
  public void testGetRecord4RequestId(VertxTestContext testContext) {
    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    JsonArray record = record();

    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(new JsonObject().put("result", record));

    asyncService
        .getRecord4RequestId(requestId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

  @Test
  @DisplayName("success - execute pg query")
  public void testExecutePGQuery(VertxTestContext testContext) {
    String query = "SELECT * FROM s3_upload_url";

    when(asyncResult1.succeeded()).thenReturn(true);

    asyncService
        .executePGQuery(query)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

  @Test
  @DisplayName("Test getRecord4RequestId method : Failure")
  public void testGetRecord4RequestIdFailure(VertxTestContext vertxTestContext) {
    when(asyncResult2.succeeded()).thenReturn(true);
    when(asyncResult2.result()).thenReturn(jsonObject);
    when(jsonObject.getJsonArray(anyString())).thenReturn(jsonArray);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(), any());
    when(jsonArray.isEmpty()).thenReturn(true);
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper, timeLimit, filePath);
    asyncService2.getRecord4RequestId("Dummy ID").onComplete(handler -> {
      if (handler.failed()) {
        assertEquals("Record doesn't exist in db for requestId.", handler.cause().getMessage());
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });

  }

  @Test
  @DisplayName("Test executePGQuery method : failure")
  public void testExecutePgQueryFailure(VertxTestContext vertxTestContext) {
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper, timeLimit, filePath);
    when(asyncResult2.succeeded()).thenReturn(false);
    when(asyncResult2.cause()).thenReturn(throwable);
    doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
        return null;
      }
    }).when(postgresService).executeQuery(anyString(), any());

    asyncService2.executePGQuery("Dummy Query").onComplete(handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        assertEquals("failed query execution throwable", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Test scrollQuery method : for Invalid Query")
  public void testScrollQueryWithInvalidQuery(VertxTestContext vertxTestContext) {
    ProgressListener progressListener = mock(ProgressListener.class);
    asyncService2 = new AsyncServiceImpl(Vertx.vertx(), client, postgresService, fileOpsHelper, timeLimit, filePath);
    when(jsonObject.put(anyString(), anyBoolean())).thenReturn(jsonObject);
    asyncService2.scrollQuery(file, jsonObject, "Dummy SearchID", progressListener, handler -> {
      if (handler.succeeded()) {
        vertxTestContext.failNow(handler.cause());
      } else {
        assertEquals("{\"type\":400,\"detail\":\"bad parameters\"}", handler.cause().getMessage());
        vertxTestContext.completeNow();
      }
    });
  }





//  @ParameterizedTest
//  @ValueSource(booleans = {true,false})
//  @DisplayName("Test updateProgress method : Different boolean values")
//  public void testUpdateProgressFailure(boolean value,VertxTestContext vertxTestContext) {
//    AsyncFileScrollProgressListener.executionCounter = mock(AsyncFileScrollProgressListener.ExecutionCounter.class);
//    when(asyncResult2.succeeded()).thenReturn(value);
//    lenient().when(asyncResult2.cause()).thenReturn(throwable);
//    lenient().doAnswer(new Answer<AsyncResult<JsonObject>>() {
//      @Override
//      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
//        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult2);
//        return null;
//      }
//    }).when(postgresService).executeQuery(anyString(), any());
//    listener = new AsyncFileScrollProgressListener("Dummy search ID", postgresService);
//    listener.updateProgress(0.55);
//    vertxTestContext.completeNow();
//  }

}

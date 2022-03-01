package iudx.resource.server.database.async;

import static iudx.resource.server.database.async.util.Constants.OBJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazonaws.regions.Regions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import iudx.resource.server.database.async.util.S3FileOpsHelper;
import iudx.resource.server.database.async.util.Utilities;
import iudx.resource.server.database.elastic.ElasticClient;
import iudx.resource.server.database.postgres.PostgresService;
import org.elasticsearch.index.query.QueryBuilder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AsyncServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceTest.class);

  private static AsyncServiceImpl asyncService;
  private static AsyncServiceImpl asyncServiceSpy;
  private static Configuration config;
  private static ElasticClient client;
  private static PostgresService pgService;
  private static S3FileOpsHelper fileOpsHelper;
  private static Utilities utilities;
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
  private static AsyncResult<JsonObject> asyncResult;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy async verticle")
  static void init(Vertx vertx, VertxTestContext testContext) throws MalformedURLException {
    config = new Configuration();
    asyncConfig = config.configLoader(9, vertx);
    timeLimit = asyncConfig.getString("timeLimit");
    filePath = asyncConfig.getString("filePath");

    file = mock(File.class);
    client = mock(ElasticClient.class);
    pgService = mock(PostgresService.class);
    fileOpsHelper = mock(S3FileOpsHelper.class);
    utilities = mock(Utilities.class);

    asyncService =
        new AsyncServiceImpl(vertx, client, pgService, fileOpsHelper, timeLimit, filePath);
    asyncServiceSpy = spy(asyncService);

    asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);

    URL url = new URL("https://www.example.com");
    when(fileOpsHelper.generatePreSignedUrl(anyLong(), any())).thenReturn(url);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
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
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(client)
        .scrollAsync(any(File.class), any(), any(QueryBuilder.class), any());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
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

  public JsonArray record() {
    JsonArray record = new JsonArray();
    record.add(
        new JsonObject()
            .put(
                "id",
                new JsonArray()
                    .add(
                        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055"))
            .put("time", "2020-10-10T14:20:00Z")
            .put("endtime", "2020-10-20T14:20:00Z")
            .put("timerel", "during")
            .put("searchType", "temporalSearch")
            .put(OBJECT_ID, "b8a47206-364c-4580-8885-45205118db57"));

    return record;
  }

  @Test
  @DisplayName("success - async search for existing request id")
  public void successfulAsyncSearchForExistingRecordTest(VertxTestContext testContext) {

    String requestId = "682a3a42aaa1c8adadea4cc9ea16d968993fc8eee4edfc299d00bccf28117965";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "18cc743b-59a4-4c26-9f54-e243986ed709";
    JsonArray record = record();
    JsonObject query = record.getJsonObject(0);

    doAnswer(Answer -> Future.succeededFuture(record))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.succeededFuture()).when(asyncServiceSpy).executePGQuery(any());

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);

    verify(asyncServiceSpy, times(1)).process4ExistingRequestId(any(), any(), any(), any());
    verify(asyncServiceSpy, times(1)).executePGQuery(any());
    verify(fileOpsHelper, times(1)).generatePreSignedUrl(anyLong(), any());
    testContext.completeNow();
  }

  @Test
  @DisplayName("success - async search for new request id")
  public void successfulAsyncSearchForNewRequestIDTest(Vertx vertx, VertxTestContext testContext) {

    String requestId = "efb0b92cd5b50d0a75a939ffa997c6e4fccdc62414ad0177a020eec98f69144e";
    String sub = "15c7506f-c800-48d6-adeb-0542b03947c6";
    String searchId = "4b25aa92-47bb-4c91-98c0-47a1c7a51fbe";
    JsonArray record = record();
    JsonObject query = record.getJsonObject(0);

    doAnswer(Answer -> Future.failedFuture("record doesn't exist"))
        .when(asyncServiceSpy)
        .getRecord4RequestId(any());
    doAnswer(Answer -> Future.succeededFuture()).when(asyncServiceSpy).executePGQuery(any());

    when(asyncResult.succeeded()).thenReturn(true);

    vertx.fileSystem().createFile(filePath + "/" + searchId + ".json");

    asyncServiceSpy.asyncSearch(requestId, sub, searchId, query);

    verify(asyncServiceSpy,times(0)).process4ExistingRequestId(any(),any(),any(),any());
    verify(fileOpsHelper, times(1)).generatePreSignedUrl(anyLong(), any());
    verify(asyncServiceSpy, times(2)).executePGQuery(any());
    testContext.completeNow();
  }
}

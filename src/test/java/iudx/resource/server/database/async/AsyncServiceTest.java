package iudx.resource.server.database.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class AsyncServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(AsyncServiceTest.class);

  private static AsyncService asyncService;
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

  @BeforeAll
  @DisplayName("Initialize vertx and deploy async verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    config = new Configuration();
    asyncConfig = config.configLoader(8, vertx);
    timeLimit = asyncConfig.getString("timeLimit");
    filePath = asyncConfig.getString("filePath");

    file = mock(File.class);
    client = mock(ElasticClient.class);
    pgService = mock(PostgresService.class);
    fileOpsHelper = mock(S3FileOpsHelper.class);

    asyncService = new AsyncServiceImpl(vertx,client,pgService,fileOpsHelper,timeLimit,filePath);

    AsyncResult<JsonObject> asyncResult = mock(AsyncResult.class);
    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
        return null;
      }
    }).when(pgService).executeQuery(any(),any());

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
        return null;
      }
    }).when(client).scrollAsync(any(File.class),any(),any(QueryBuilder.class),any());

    Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
      @SuppressWarnings("unchecked")
      @Override
      public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
        ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
        return null;
      }
    }).when(fileOpsHelper).s3Upload(any(File.class),any(),any());

    LOGGER.info("Async Test steup complete");
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("setup test is passing");
    testContext.completeNow();
  }

  @Test
  @DisplayName("success - async search")
  public void successfulAsyncSearchTest(VertxTestContext testContext) {

  }

}

package iudx.resource.server.metering;

import static iudx.resource.server.metering.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.UUID;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MeteringServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
  public static String userId;
  public static String id;
  private static Vertx vertxObj;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int databasePoolSize;
  private static String databaseTableName;
  private static MeteringServiceImpl meteringService;

  private static Configuration config;
  @Mock
  private static PgPool pool;
  private static PgConnectOptions connectOptions;
  private static String databaseIPPG;
  private static int databasePortPG;
  private static String databaseNamePG;
  private static String databaseUserNamePG;
  private static String databasePasswordPG;
  private static int poolSize;
  private static PoolOptions poolOptions;
  private static JsonObject dbConfig;
  private static  PostgresService postgresService;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    config = new Configuration();
    dbConfig = config.configLoader(6, vertx);
    databaseIP = dbConfig.getString("meteringDatabaseIP");
    databasePort = dbConfig.getInteger("meteringDatabasePort");
    databaseName = dbConfig.getString("meteringDatabaseName");
    databaseUserName = dbConfig.getString("meteringDatabaseUserName");
    databasePassword = dbConfig.getString("meteringDatabasePassword");
    databasePoolSize = dbConfig.getInteger("meteringPoolSize");
    databaseTableName = dbConfig.getString("meteringDatabaseTableName");

    userId = UUID.randomUUID().toString();
    id = "89a36273d77dac4cf38114fca1bbe64392547f86";
    vertxTestContext.completeNow();
  }

  private JsonObject readConsumerRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
    jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30[Asia/Kolkata]");
    jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30[Asia/Kolkata]");
    jsonObject.put(TIME_RELATION, DURING);
    jsonObject.put(API, "/ngsi-ld/v1/subscription");
    jsonObject.put(ENDPOINT, "/ngsi-ld/v1/consumer/audit");

    return jsonObject;
  }

  private JsonObject readProviderRequest() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(RESOURCE_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
    jsonObject.put(START_TIME, "2022-05-29T05:30:00+05:30[Asia/Kolkata]");
    jsonObject.put(END_TIME, "2022-06-04T02:00:00+05:30[Asia/Kolkata]");
    jsonObject.put(TIME_RELATION, DURING);
    jsonObject.put(API, "/ngsi-ld/v1/subscription");
    jsonObject.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias");
    jsonObject.put(CONSUMER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    jsonObject.put(IID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
    jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
    return jsonObject;
  }
  private JsonObject read() {
      JsonObject jsonObject = new JsonObject();
      jsonObject.put(START_TIME, "2022-06-20T00:00:00Z");
      jsonObject.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
      jsonObject.put(END_TIME, "2022-06-21T16:00:00Z");
      jsonObject.put(TIME_RELATION, "between");
      jsonObject.put(API, "/ngsi-ld/v1/subscription");
      jsonObject.put(PROVIDER_ID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
      jsonObject.put(ENDPOINT, "/ngsi-ld/v1/provider/audit");
      jsonObject.put(IID, "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055");
      return jsonObject;
    }

  // @AfterAll
  // public void finish(VertxTestContext testContext) {
  // logger.info("finishing");
  // vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  // }

  @Test
  @DisplayName("Testing read query with invalid time interval")
  void readFromInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
    postgresService = mock(PostgresService.class);

    meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);
    request.put(START_TIME, "2021-11-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-11-24T02:00:00+05:30[Asia/Kolkata]");
    meteringService.executeReadQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          INVALID_DATE_DIFFERENCE,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query with given Time Interval")
  void readFromValidTimeInterval(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(39);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());

    JsonObject request = readConsumerRequest();

    meteringService.executeReadQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                        LOGGER.info(response);
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for missing userId")
  void readForMissingUserId(VertxTestContext vertxTestContext) {
    JsonObject request = readConsumerRequest();
    request.remove(USER_ID);
    postgresService = mock(PostgresService.class);

    meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                          USERID_NOT_FOUND,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for missing time Relation")
  void readForMissingTimeRel(VertxTestContext vertxTestContext) {
    JsonObject request = readConsumerRequest();
    request.remove(TIME_RELATION);
    postgresService = mock(PostgresService.class);

    meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);
    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                          TIME_RELATION_NOT_FOUND,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for missing time")
  void readForMissingTime(VertxTestContext vertxTestContext) {
    JsonObject request = readConsumerRequest();
    request.remove(START_TIME);
    postgresService = mock(PostgresService.class);

    meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);
    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                          TIME_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query with invalid start/end time")
  void readForInvalidStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = readConsumerRequest();
    request.put(START_TIME, "2021-009-18T00:30:00+05:30[Asia/Kolkata]");
    postgresService = mock(PostgresService.class);

    meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);
    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                          INVALID_DATE_TIME,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for given time.")
  void readForGivenTime(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(3);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());

      JsonObject jsonObject = read();
    jsonObject.remove(RESOURCE_ID);
    jsonObject.remove(API);
    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for given time and id.")
  void readForGivenTimeAndId(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(60);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());

    JsonObject jsonObject = readConsumerRequest();
    jsonObject.remove(API);
    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for given time and api.")
  void readForGivenTimeAndApi(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(300);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.remove(RESOURCE_ID);

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }

 @Test
  @DisplayName("Testing read query for given time,api and resourceId.")
  void readForGivenTimeApiAndID(VertxTestContext vertxTestContext) {
     JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
     AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
     postgresService = mock(PostgresService.class);
     JsonObject json= mock(JsonObject.class);
     JsonArray jsonArray= mock(JsonArray.class);

     meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

     when(asyncResult.succeeded()).thenReturn(true);
     when(asyncResult.result()).thenReturn(json,responseJson);

     when(json.getJsonArray(anyString())).thenReturn(jsonArray);
     when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
     when(json.getInteger(anyString())).thenReturn(34);

     Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
         @Override
         public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
             ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
             return null;
         }
     }).when(postgresService).executeQuery(anyString(), any());
    JsonObject jsonObject = readConsumerRequest();

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing count query for given time,api and id.")
  void countForGivenTimeAndApiAndID(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(23);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.put("options", "count");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertTrue(
                        response.getJsonArray(RESULTS).getJsonObject(0).containsKey(TOTAL));
                      vertxTestContext.completeNow();
                    })));

  }

 /* @Test
  @DisplayName("Testing Write Query")
  void writeData(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias/");
    request.put(API, "/ngsi-ld/v1/subscription");
    request.put(RESPONSE_SIZE,12);
    meteringService.executeWriteQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals("Success", response.getString("title"));
                      vertxTestContext.completeNow();
                    })));

  }*/

  @Test
  @DisplayName("Testing read query with missing providerId.")
  void readForMissingProviderId(VertxTestContext vertxTestContext) {
      postgresService = mock(PostgresService.class);
      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);
    JsonObject request = readProviderRequest();
    request.remove(PROVIDER_ID);
    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                         INVALID_PROVIDER_REQUIRED,
                         new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query with invalid providerId.")
  void readForInvalidProviderId(VertxTestContext vertxTestContext) {
    JsonObject request = readProviderRequest();
    request.put(PROVIDER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-tsst-alias");

    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(
                          INVALID_PROVIDER_ID,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for given time,api and id.")
  void readForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(300);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());
    JsonObject jsonObject = readProviderRequest();

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }
    @Test
    @DisplayName("Testing read query for given time,api and id.")
    void readForGivenTimeApiIdConsumerProviderIDZero(VertxTestContext vertxTestContext) {
        JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json,responseJson);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = readProviderRequest();

        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));

    }

  @Test
  @DisplayName("Testing count query for given time,api and id.")
  void countForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(10);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());
    JsonObject jsonObject = readProviderRequest();
    jsonObject.put("options", "count");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(SUCCESS, response.getString("title"));
                      vertxTestContext.completeNow();
                    })));

  }

  @Test
  @DisplayName("Testing read query for given time,api and providerId.")
  void readForGivenTimeApiAndProviderID(VertxTestContext vertxTestContext) {
      JsonObject responseJson= new JsonObject().put(SUCCESS,"Success");
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json,responseJson);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(200);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());

    JsonObject jsonObject = readProviderRequest();
    jsonObject.remove(RESOURCE_ID);
    jsonObject.remove(CONSUMER_ID);

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                     assertEquals(SUCCESS, response.getString(SUCCESS));
                      vertxTestContext.completeNow();
                    })));

  }
  @Test
  @DisplayName("Testing count query for given time.")
  void readCountForGivenTime(VertxTestContext vertxTestContext) {
      AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
      postgresService = mock(PostgresService.class);
      JsonObject json= mock(JsonObject.class);
      JsonArray jsonArray= mock(JsonArray.class);

      meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

      when(asyncResult.succeeded()).thenReturn(true);
      when(asyncResult.result()).thenReturn(json);

      when(json.getJsonArray(anyString())).thenReturn(jsonArray);
      when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
      when(json.getInteger(anyString())).thenReturn(300);

      Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
          @Override
          public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
              ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
              return null;
          }
      }).when(postgresService).executeQuery(anyString(), any());
      JsonObject jsonObject = read();
      jsonObject.put("options","count");
      jsonObject.remove(API);
      meteringService.executeReadQuery(
              jsonObject,
              vertxTestContext.succeeding(
                      response ->
                              vertxTestContext.verify(
                                      () -> {
                                       assertEquals(SUCCESS, response.getString("title"));
                                       vertxTestContext.completeNow();
                                      })));
    }
    @Test
    @DisplayName("Testing count query for given time.")
    void readCountForGivenTimeForZero(VertxTestContext vertxTestContext) {
        AsyncResult<JsonObject> asyncResult= mock(AsyncResult.class);
        postgresService = mock(PostgresService.class);
        JsonObject json= mock(JsonObject.class);
        JsonArray jsonArray= mock(JsonArray.class);

        meteringService = new MeteringServiceImpl(dbConfig, vertxObj, postgresService);

        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(json);

        when(json.getJsonArray(anyString())).thenReturn(jsonArray);
        when(jsonArray.getJsonObject(anyInt())).thenReturn(json);
        when(json.getInteger(anyString())).thenReturn(0);

        Mockito.doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg1) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg1.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(postgresService).executeQuery(anyString(), any());
        JsonObject jsonObject = read();
        jsonObject.put("options","count");
        jsonObject.remove(API);
        meteringService.executeReadQuery(
                jsonObject,
                vertxTestContext.succeeding(
                        response ->
                                vertxTestContext.verify(
                                        () -> {
                                            assertEquals(SUCCESS, response.getString("title"));
                                            vertxTestContext.completeNow();
                                        })));
    }
}

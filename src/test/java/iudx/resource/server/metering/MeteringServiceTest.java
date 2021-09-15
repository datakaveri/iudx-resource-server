package iudx.resource.server.metering;

import static iudx.resource.server.metering.util.Constants.DETAIL;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ID;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;

@ExtendWith({VertxExtension.class})
public class MeteringServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
  private static MeteringService meteringService;
  private static Vertx vertxObj;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int databasePoolSize;
  private static Configuration config;
  public static String userId;
  public static String id;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    config = new Configuration();
    JsonObject dbConfig = config.configLoader(6, vertx);
    databaseIP = dbConfig.getString("meteringDatabaseIP");
    databasePort = dbConfig.getInteger("meteringDatabasePort");
    databaseName = dbConfig.getString("meteringDatabaseName");
    databaseUserName = dbConfig.getString("meteringDatabaseUserName");
    databasePassword = dbConfig.getString("meteringDatabasePassword");
    databasePoolSize = dbConfig.getInteger("meteringPoolSize");
    meteringService = new MeteringServiceImpl(dbConfig, vertxObj);
    userId = UUID.randomUUID().toString();
    id = "89a36273d77dac4cf38114fca1bbe64392547f86";
    vertxTestContext.completeNow();
  }

  // @AfterAll
  // public void finish(VertxTestContext testContext) {
  // logger.info("finishing");
  // vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  // }

  @Test
  @DisplayName("Testing read query with invaild time interval")
  void readFromInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(END_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(START_TIME, "1970-01-01T05:30:10+05:30[Asia/Kolkata]");

    meteringService.executeCountQuery(
        request,
        testContext.failing(
            response -> testContext.verify(
                () -> {
                  assertEquals(
                      "invalid date-time",
                      new JsonObject(response.getMessage()).getString("detail"));
                  testContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing count query with given Time Interval")
  void readFromValidTimeInterval(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();

    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");

    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response -> vertxTestContext.verify(
                () -> {
                  assertTrue(
                      response.getJsonArray("results").getJsonObject(0).containsKey("count"));
                  vertxTestContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing count query for Empty Response")
  void readForEmptyResponse(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();

    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-15T00:30:00+05:30[Asia/Kolkata]");

    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response -> vertxTestContext.verify(
                () -> {
                  assertTrue(response.getString(DETAIL).equals("Empty response"));
                  vertxTestContext.completeNow();
                })));
  }

  @Test
  @DisplayName("Testing count query for given time and email")
  void readForGivenEmailAndTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(USER_ID, userId);
    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER.info("RESPONSE" + response);
                  assertTrue(
                      response.getJsonArray("results").getJsonObject(0).containsKey("count"));
                  vertxTestContext.completeNow();
                })));
  }

  @Disabled("even if the method under test executes correctly, test will fail since it is dependent on already existing data in db")
  @Test
  @DisplayName("Testing count query for given time and resource")
  void readForGivenResourceandTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(ID, id);
    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER.info("RESPONSE" + response);
                  assertTrue(
                      response.getJsonArray("results").getJsonObject(0).containsKey("count"));
                  vertxTestContext.completeNow();
                })));
  }

  @Disabled("even if the method under test executes correctly, test will fail since it is dependent on already existing data in db")
  @Test
  @DisplayName("Testing count query for given email,time and resource")
  void readForGivenEmailResourceandTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(ID, id);
    request.put(USER_ID, userId);
    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response -> vertxTestContext.verify(
                () -> {
                  LOGGER.info("RESPONSE" + response.getString("title"));
                  assertTrue(
                      response.getJsonArray("results").getJsonObject(0).containsKey("count"));
                  vertxTestContext.completeNow();
                })));
  }

  // @Test
  // @DisplayName("Testing Write Query")
  // void writeData(VertxTestContext vertxTestContext) {
  // JsonObject request = new JsonObject();
  // request.put(EMAIL_ID, "test.data@iudx.org");
  // request.put(ID, "89a36273d77dac4cf38114fca1bbe64392547f86");
  // request.put(API, "/ngsi-ld/v1/temporal/entities");
  // meteringService.executeWriteQuery(
  // request,
  // vertxTestContext.succeeding(
  // response ->
  // vertxTestContext.verify(
  // () -> {
  // logger.info("RESPONSE" + response.getString("title"));
  // assertTrue(response.getString("title").equals("Success"));
  // vertxTestContext.completeNow();
  // })));
  // }
}

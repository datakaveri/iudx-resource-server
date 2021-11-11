package iudx.resource.server.metering;

import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.COUNT;
import static iudx.resource.server.metering.util.Constants.DETAIL;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ID;
import static iudx.resource.server.metering.util.Constants.INVALID_DATE_TIME;
import static iudx.resource.server.metering.util.Constants.RESULTS;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class})
public class MeteringServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(MeteringServiceTest.class);
  public static String userId;
  public static String id;
  private static MeteringService meteringService;
  private static Vertx vertxObj;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseName;
  private static String databaseUserName;
  private static String databasePassword;
  private static int databasePoolSize;
  private static Configuration config;

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
  @DisplayName("Testing read query with invalid time interval")
  void readFromInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(END_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(START_TIME, "1970-01-01T05:30:10+05:30[Asia/Kolkata]");

    meteringService.executeCountQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          INVALID_DATE_TIME,
                          new JsonObject(response.getMessage()).getString(DETAIL));
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
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertTrue(
                          response.getJsonArray(RESULTS).getJsonObject(0).containsKey(COUNT));
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
            response ->
                vertxTestContext.verify(
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
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info("RESPONSE" + response);
                      assertTrue(
                          response.getJsonArray(RESULTS).getJsonObject(0).containsKey(COUNT));
                      vertxTestContext.completeNow();
                    })));
  }

  //  @Disabled("even if the method under test executes correctly, test will fail since it is
  // dependent on already existing data in db")
  @Test
  @DisplayName("Testing count query for given time and resource")
  void readForGivenResourceAndTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(ID, id);
    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info("RESPONSE" + response);
                      assertTrue(
                          response.getJsonArray(RESULTS).getJsonObject(0).containsKey(COUNT));
                      vertxTestContext.completeNow();
                    })));
  }

  //  @Disabled("even if the method under test executes correctly, test will fail since it is
  // dependent on already existing data in db")
  @Test
  @DisplayName("Testing count query for given email,time and resource")
  void readForGivenEmailResourceAndTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "1970-01-01T05:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(ID, id);
    request.put(USER_ID, userId);
    meteringService.executeCountQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info("RESPONSE" + response.getString("title"));
                      assertTrue(
                          response.getJsonArray(RESULTS).getJsonObject(0).containsKey(COUNT));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query w/o start time")
  void readForMissingStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    meteringService.executeCountQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info(
                          "RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(
                          TIME_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query with invalid start time")
  void readForInvalidStartTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "2021-009-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    meteringService.executeCountQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info(
                          "RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(
                          INVALID_DATE_TIME,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query with invalid end time")
  void readForInvalidEndTime(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(START_TIME, "2021-09-18T00:30:00+05:30[Asia/Kolkata]");
    request.put(END_TIME, "2021-009-18T00:30:00+05:30[Asia/Kolkata]");
    meteringService.executeCountQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info(
                          "RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(
                          INVALID_DATE_TIME,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }
  @Test
  @DisplayName("Testing Write Query")
  void writeData(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    request.put(USER_ID, "15c7506f-c800-48d6-adeb-0542b03947c6");
    request.put(ID, "15c7506f-c800-48d6-adeb-0542b03947c6/integration-test-alias");
    request.put(API, "/ngsi-ld/v1/subscription");
    meteringService.executeWriteQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.info("RESPONSE" + response.getString("title"));
                      assertTrue(response.getString("title").equals("Success"));
                      vertxTestContext.completeNow();
                    })));
  }
}

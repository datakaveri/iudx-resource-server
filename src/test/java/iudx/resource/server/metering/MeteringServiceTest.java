package iudx.resource.server.metering;

 import static iudx.resource.server.apiserver.util.Constants.RESPONSE_SIZE;
import static iudx.resource.server.metering.util.Constants.API;
import static iudx.resource.server.metering.util.Constants.CONSUMER_ID;
import static iudx.resource.server.metering.util.Constants.DETAIL;
import static iudx.resource.server.metering.util.Constants.DURING;
import static iudx.resource.server.metering.util.Constants.ENDPOINT;
import static iudx.resource.server.metering.util.Constants.END_TIME;
import static iudx.resource.server.metering.util.Constants.ID;
import static iudx.resource.server.metering.util.Constants.IID;
import static iudx.resource.server.metering.util.Constants.INVALID_DATE_DIFFERENCE;
import static iudx.resource.server.metering.util.Constants.INVALID_DATE_TIME;
import static iudx.resource.server.metering.util.Constants.INVALID_PROVIDER_ID;
import static iudx.resource.server.metering.util.Constants.INVALID_PROVIDER_REQUIRED;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;
import static iudx.resource.server.metering.util.Constants.RESOURCE_ID;
import static iudx.resource.server.metering.util.Constants.RESULTS;
import static iudx.resource.server.metering.util.Constants.START_TIME;
import static iudx.resource.server.metering.util.Constants.SUCCESS;
import static iudx.resource.server.metering.util.Constants.TABLE_NAME;
import static iudx.resource.server.metering.util.Constants.TIME_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.TIME_RELATION;
import static iudx.resource.server.metering.util.Constants.TIME_RELATION_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.TITLE;
import static iudx.resource.server.metering.util.Constants.TOTAL;
import static iudx.resource.server.metering.util.Constants.USERID_NOT_FOUND;
import static iudx.resource.server.metering.util.Constants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
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
  private static String databaseTableName;
  private static Configuration config;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
    vertxObj = vertx;
    config = new Configuration();
    JsonObject dbConfig = config.configLoader(5, vertx);
    databaseIP = dbConfig.getString("meteringDatabaseIP");
    databasePort = dbConfig.getInteger("meteringDatabasePort");
    databaseName = dbConfig.getString("meteringDatabaseName");
    databaseUserName = dbConfig.getString("meteringDatabaseUserName");
    databasePassword = dbConfig.getString("meteringDatabasePassword");
    databasePoolSize = dbConfig.getInteger("meteringPoolSize");
    databaseTableName = dbConfig.getString("meteringDatabaseTableName");
    meteringService = new MeteringServiceImpl(dbConfig, vertxObj);
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

  // @AfterAll
  // public void finish(VertxTestContext testContext) {
  // logger.info("finishing");
  // vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  // }

  @Test
  @DisplayName("Testing read query with invalid time interval")
  void readFromInvalidTimeInterval(VertxTestContext testContext) {
    JsonObject request = readConsumerRequest();
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
    JsonObject request = readConsumerRequest();

    meteringService.executeReadQuery(
        request,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      assertEquals(SUCCESS, response.getString(TITLE));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for missing userId")
  void readForMissingUserId(VertxTestContext vertxTestContext) {
    JsonObject request = readConsumerRequest();
    request.remove(USER_ID);

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

    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug(
                          "RESPONSE " + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(
                          INVALID_DATE_TIME,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time.")
  void readForGivenTime(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.remove(RESOURCE_ID);
    jsonObject.remove(API);
    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time and id.")
  void readForGivenTimeAndId(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.remove(API);
    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time and api.")
  void readForGivenTimeAndApi(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.remove(RESOURCE_ID);

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time,api and resourceId.")
  void readForGivenTimeApiAndID(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time,api and id.")
  void readForGivenTimeApiAndIDEmptyResponse(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.put(START_TIME, "2021-11-19T05:30:00+05:30[Asia/Kolkata]");
    jsonObject.put(END_TIME, "2021-11-21T02:00:00+05:30[Asia/Kolkata]");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug(
                          "RESPONSE " + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(SUCCESS, new JsonObject(response.getMessage()).getString(TITLE));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query for given time,api and id.")
  void countForGivenTimeApiAndIDEmptyResponse(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.put(START_TIME, "2021-11-19T05:30:00+05:30[Asia/Kolkata]");
    jsonObject.put(END_TIME, "2021-11-21T02:00:00+05:30[Asia/Kolkata]");
    jsonObject.put("options", "count");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug(
                          "RESPONSE " + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(SUCCESS, new JsonObject(response.getMessage()).getString(TITLE));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query for given time,api and id.")
  void countForGivenTimeAndApiAndID(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readConsumerRequest();
    jsonObject.put("options", "count");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(
                          response.getJsonArray(RESULTS).getJsonObject(0).containsKey(TOTAL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
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
                      LOGGER.debug("RESPONSE" + response.getString("title"));
                      assertTrue(response.getString("title").equals("Success"));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query with missing providerId.")
  void readForMissingProviderId(VertxTestContext vertxTestContext) {
    JsonObject request = readProviderRequest();
    request.remove(PROVIDER_ID);
    meteringService.executeReadQuery(
        request,
        vertxTestContext.failing(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug(
                          "RESPONSE " + new JsonObject(response.getMessage()).getString(DETAIL));
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
                      LOGGER.debug(
                          "RESPONSE " + new JsonObject(response.getMessage()).getString(DETAIL));
                      assertEquals(
                          INVALID_PROVIDER_ID,
                          new JsonObject(response.getMessage()).getString(DETAIL));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time,api and id.")
  void readForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readProviderRequest();

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing count query for given time,api and id.")
  void countForGivenTimeApiIdConsumerProviderID(VertxTestContext vertxTestContext) {
    JsonObject jsonObject = readProviderRequest();
    jsonObject.put("options", "count");

    meteringService.executeReadQuery(
        jsonObject,
        vertxTestContext.succeeding(
            response ->
                vertxTestContext.verify(
                    () -> {
                      LOGGER.debug("RESPONSE" + response);
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing read query for given time,api and providerId.")
  void readForGivenTimeApiAndProviderID(VertxTestContext vertxTestContext) {
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
                      assertTrue(response.getString(TITLE).equals(SUCCESS));
                      vertxTestContext.completeNow();
                    })));
  }
}

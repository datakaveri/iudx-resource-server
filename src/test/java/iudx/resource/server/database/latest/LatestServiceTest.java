package iudx.resource.server.database.latest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

@ExtendWith({VertxExtension.class})
public class LatestServiceTest {
  private static final Logger logger = LogManager.getLogger(LatestDataService.class);
  private static Vertx vertxObj;
  private static RedisClient client;
  private static Configuration config;
  private static JsonObject attributeList;

  private static RedisClient redisClient;
  private static LatestDataService latest;

  @BeforeAll
  @DisplayName("Deploying Latest Test Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    config = new Configuration();
    JsonObject redisConfig = config.configLoader(5, vertx);
    attributeList = redisConfig.getJsonObject("attributeList");
    new RedisClient(vertx, redisConfig).start().onSuccess(handler -> {
      redisClient = handler;
      latest = new LatestDataServiceImpl(redisClient, attributeList);
      testContext.completeNow();
    }).onFailure(handler -> {
      testContext.failNow("fail to start vertx");
    });
  }


  /**
   * resource-id query resource-group aqm rg flood rg itms
   */

  @Test
  @DisplayName("Testing Latest Data at resource level- flood")
  void searchLatestResourceflood(VertxTestContext testContext) {
    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR055";
    JsonObject request =
        new JsonObject()
            .put("id", new JsonArray().add(id))
            .put("searchType", "latestSearch");

    latest.getLatestData(request, handler -> {
      if (handler.succeeded()) {
        logger.debug("Got the data!");
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("Testing Latest Data at resource level- flood (id not present in redis)")
  void searchLatestResourcefloodIdnotPresent(VertxTestContext testContext) {
    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR018";
    JsonObject request =
        new JsonObject()
            .put("id", new JsonArray().add(id))
            .put("searchType", "latestSearch");

    latest.getLatestData(request, handler -> {
      if (handler.succeeded()) {
        testContext.failNow(handler.cause());
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  @DisplayName("Testing Latest Data at resource level- itms")
  void searchLatestResourceItms(VertxTestContext testContext) {
    String id =
        "suratmunicipal.org/6db486cb4f720e8585ba1f45a931c63c25dbbbda/rs.iudx.org.in/surat-itms-realtime-info/surat-itms-live-eta";
    JsonObject request =
        new JsonObject()
            .put("id", new JsonArray().add(id))
            .put("searchType", "latestSearch");

    latest.getLatestData(request, handler -> {
      if (handler.succeeded()) {
        logger.debug("Got the data!");
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }


  @Test
  @DisplayName("Testing Basic Exceptions (No id key)")
  void searchWithNoId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("options", "id");

    latest.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No id found", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (id is empty)")
  void searchEmptyId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("options", "id");

    latest.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("resource-id is empty", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

}

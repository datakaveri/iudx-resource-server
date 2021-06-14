package iudx.resource.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.configuration.Configuration;
import iudx.resource.server.database.latest.LatestDataService;
import iudx.resource.server.database.latest.LatestDataServiceImpl;
import iudx.resource.server.database.latest.RedisClient;

@ExtendWith({VertxExtension.class})
public class LatestServiceTest {
  private static Logger logger = LoggerFactory.getLogger(LatestDataService.class);
  private static LatestDataService latestService;
  private static Vertx vertxObj;
  private static RedisClient client;
  private static String redisHost, user, password;
  private static int port;
  private static Configuration config;
  private static JsonObject attributeList;
  // private static String connectionString;

  /* TODO Need to update params to use contants */
  @BeforeAll
  @DisplayName("Deploying Latest Test Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    config = new Configuration();
    JsonObject redisConfig = config.configLoader(5, vertx);

    /* Read the configuration and set the rabbitMQ server properties. */
    /* Configuration setup */
    redisHost = redisConfig.getString("redisHost");
    port = redisConfig.getInteger("redisPort");
    user = redisConfig.getString("redisUser");
    password = redisConfig.getString("redisPassword");
    attributeList = redisConfig.getJsonObject("attributeList");
    // connectionString =
    // "redis://:".concat(user).concat(":").concat(password).concat("@").concat(redisHost)
    // .concat(":").concat(String.valueOf(port));
    // connectionString = "redis://:@https://database.iudx.io:28734/1";

    // logger.debug("Redis ConnectionString: " + connectionString);
    logger.debug("Host: " + redisHost + "Port: " + port);
    client = new RedisClient(io.vertx.core.Vertx.vertx(), redisHost, port);
    latestService = new LatestDataServiceImpl(client, attributeList);
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    logger.info("Finishing the tests....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  // Functional Testing

  /**
   * resource-id query resource-group aqm rg flood rg itms
   */

  //@Test
  @DisplayName("Testing Latest Data at resource level- flood")
  void searchLatestResourceflood(VertxTestContext testContext) {
    String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/" +
        "rs.iudx.io/pune-env-flood/FWR018";
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(id))
            // .put("options", "id")
            .put("searchType", "latestSearch");

    latestService.getLatestData(request, handler -> {
      if (handler.succeeded()) {
        logger.debug("Got the data!");
        assertEquals(id, handler.result().getJsonArray("results").getJsonObject(0)
            .getString("id"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  //@Test
  @DisplayName("Testing Latest Data at resource level- itms")
  void searchLatestResourceItms(VertxTestContext testContext) {
    String id =
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information"
            +
            "/surat-itms-live-eta";
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(id))
            // .put("options", "id")
            .put("searchType", "latestSearch");

    latestService.getLatestData(request, handler -> {
      if (handler.succeeded()) {
        logger.debug("Got the data!");
        assertEquals(id, handler.result().getJsonArray("results").getJsonObject(0)
            .getString("id"));
        testContext.completeNow();
      } else {
        testContext.failNow(handler.cause());
      }
    });
  }

  /**
   * Group level testing --> aqm --> flood --> itms
   */

  // @Test
  // @DisplayName("Testing Latest Data at group level- aqm")
  // void searchLatestGroupAQM(VertxTestContext testContext) {
  // String id = "varanasismartcity.gov.in/62d1f729edd3d2a1a090cb1c6c89356296963d55" +
  // "/rs.iudx.io/varanasi-env-aqm";
  // JsonObject request =
  // new JsonObject()
  // .put("id",
  // new JsonArray().add(id))
  // .put("options", "group").put("searchType","latestSearch");
  //
  // latestService.getLatestData(request, result -> {
  // if (result.succeeded()) {
  // logger.debug("Got the data!");
  // assertEquals(id, result.result().getJsonArray("results").getJsonObject(0)
  // .getString("id"));
  // testContext.completeNow();
  // } else {
  // logger.error("Error: " + result.cause());
  // testContext.failNow(result.cause());
  // }
  // });
  // }

  // @Test
  // @DisplayName("Testing Latest Data at group level- flood")
  // void searchLatestGroupFlood(VertxTestContext testContext) {
  // String id = "datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/" +
  // "rs.iudx.io/pune-env-flood";
  // JsonObject request =
  // new JsonObject()
  // .put("id",
  // new JsonArray().add(id))
  // .put("options", "group").put("searchType","latestSearch");
  //
  // latestService.getLatestData(request, result -> {
  // if (result.succeeded()) {
  // assertEquals(id, result.result().getJsonArray("results").getJsonObject(0)
  // .getString("id"));
  // testContext.completeNow();
  // } else {
  // logger.error("Error: " + result.cause());
  // testContext.failNow(result.cause());
  // }
  // });
  // }

  // @Test
  // @DisplayName("Testing Latest Data at group level- itms")
  // void searchLatestGroupITMS(VertxTestContext testContext) {
  // String id = "suratmunicipal.org/6db486cb4f720e8585ba1f45a931c63c25dbbbda/" +
  // "rs.iudx.io/surat-itms-realtime-info/surat-itms-live-eta";
  // JsonObject request =
  // new JsonObject()
  // .put("id",
  // new JsonArray().add(id))
  // .put("options", "group").put("searchType","latestSearch");
  //
  // latestService.getLatestData(request, result -> {
  // if (result.succeeded()) {
  // assertEquals(id, result.result().getJsonArray("results").getJsonObject(0)
  // .getString("id"));
  // testContext.completeNow();
  // } else {
  // logger.error("Error: " + result.cause());
  // testContext.failNow(result.cause());
  // }
  // });
  // }

  // Exception Testing

  /**
   * id not present in JSONObject from Verticle options not present in JSONObject id is empty
   * options is empty options not equal to group/id id not present in the Redis
   **/

  @Test
  @DisplayName("Testing Basic Exceptions (No id key)")
  void searchWithNoId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("options", "id");

    latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No id found", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

  // @Test
  // @DisplayName("Testing Basic Exceptions (No options key)")
  // void searchWithNoResourceId(VertxTestContext testContext) {
  // JsonObject request = new JsonObject().put("id", new JsonArray().add("<some actual id>"));
  //
  // latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
  // assertEquals("options not found", new JsonObject(response.getMessage()).getString(
  // "detail"));
  // testContext.completeNow();
  // })));
  // }

  @Test
  @DisplayName("Testing Basic Exceptions (id is empty)")
  void searchEmptyId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("options", "id");

    latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("resource-id is empty", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

  // @Test
  // @DisplayName("Testing Basic Exceptions (options is empty)")
  // void searchEmptyOptions(VertxTestContext testContext) {
  // JsonObject request =
  // new JsonObject().put("id", new JsonArray().add("<some actual id>")).put("options", "");
  //
  // latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
  // assertEquals("options is empty", new JsonObject(response.getMessage()).getString(
  // "detail"));
  // testContext.completeNow();
  // })));
  // }

  // @Test
  // @DisplayName("Testing Basic Exceptions (invalid options parameters)")
  // void searchInvalidOptions(VertxTestContext testContext) {
  // JsonObject request =
  // new JsonObject().put("id", new JsonArray().add("<some actual id>")).put("options", "invalid!");
  //
  // latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
  // assertEquals("invalid options for latest", new JsonObject(response.getMessage()).getString(
  // "detail"));
  // testContext.completeNow();
  // })));
  // }

  @Test
  @DisplayName("Testing Basic Exceptions (id not present in Database)")
  void searchIdNotInRedis(VertxTestContext testContext) {
    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86" +
        "/rs.iudx.io/surat-itms-realtime-information/surat-itms-live";
    JsonObject request =
        new JsonObject().put("id", new JsonArray().add(id))
            .put("options", "id")
            .put("searchType", "latestSearch");

    latestService.getLatestData(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Not found", new JsonObject(response.getMessage())
          .getString("detail"));
      testContext.completeNow();
    })));
  }

}

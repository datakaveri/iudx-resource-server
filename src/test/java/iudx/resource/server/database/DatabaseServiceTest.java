package iudx.resource.server.database;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.core.logging.Logger;
import io.vertx.junit5.VertxTestContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({VertxExtension.class})
public class DatabaseServiceTest {
  private static Logger logger = LoggerFactory.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static RestClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP;
  private static int databasePort;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream("config.properties");
      properties.load(inputstream);

      databaseIP = properties.getProperty("databaseIP");
      databasePort = Integer.parseInt(properties.getProperty("databasePort"));

    } catch (Exception ex) {

      logger.info(ex.toString());

    }

    client = RestClient.builder(new HttpHost(databaseIP, databasePort, "http")).build();
    vertx.deployVerticle(new DatabaseVerticle(), testContext.succeeding(id -> {
      logger.info("Successfully deployed Verticle for Test");
      dbService = new DatabaseServiceImpl(client);
      testContext.completeNow();
    }));
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    logger.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "geoSearch_").put("lon", 82.987988).put("lat", 25.319768)
            .put("radius", "100").put("isTest", true);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(82.98797, response.getJsonObject(0).getDouble("longitude"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No resource-id key)")
  void searchWithNoResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No id found", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (resource-id is empty)")
  void searchEmptyResourceId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("resource-id is empty", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"));

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No searchType found", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [lat,lon,radius])")
  void searchMissingGeoParamsCircle(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "geoSearch_").put("isTest", true);
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid geo parameters", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-Polygon query")
  void searchGeoPolygon(VertxTestContext testContext) {
    /**
     * coordinates should look like this
     * [[[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5],[lo1,la1]]]
     */

    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("geometry", "polygon").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(new JsonArray().add(82.9735).add(25.3703))
                    .add(new JsonArray().add(83.0053).add(25.3567))
                    .add(new JsonArray().add(82.9766).add(25.3372))
                    .add(new JsonArray().add(82.95).add(25.3519))
                    .add(new JsonArray().add(82.936).add(25.3722))
                    .add(new JsonArray().add(82.9735).add(25.3703))))
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_")
            .put("isTest", true);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(82.93 <= response.getJsonObject(0).getDouble("longitude")
          && response.getJsonObject(0).getDouble("longitude") <= 83.006);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty, georel)")
  void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "geoSearch_").put("isTest", true).put("geometry", "polygon");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid geo parameters", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
  void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("geometry", "polygon").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(new JsonArray().add(82.9735).add(25.3703))
                    .add(new JsonArray().add(83.0053).add(25.3567))
                    .add(new JsonArray().add(82.9766).add(25.3372))
                    .add(new JsonArray().add(82.95).add(25.3519))
                    .add(new JsonArray().add(82.936).add(25.3722))))
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_")
            .put("isTest", true);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Coordinate mismatch (Polygon)", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]]
     */

    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("geometry", "linestring").put("georel", "intersects")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(82.9735).add(25.3352))
                    .add(new JsonArray().add(82.9894).add(25.3452))
                    .add(new JsonArray().add(82.99).add(25.34)))
            .put("geoproperty", "geoJsonLocation").put("isTest", true)
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(82.9735 <= response.getJsonObject(2).getDouble("longitude")
          && response.getJsonObject(0).getDouble("longitude") <= 82.9894);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */

    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(82.95).add(25.3567))
                    .add(new JsonArray().add(83.0053).add(25)))
            .put("geoproperty", "geoJsonLocation").put("isTest", true)
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(82.9 <= response.getJsonObject(0).getDouble("longitude")
          && response.getJsonObject(0).getDouble("longitude") <= 83.1);
      testContext.completeNow();
    })));

  }

  @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /**
     * coordinates should look like this [[lo1,la1],[lo3,la3]]
     */

    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates",
                new JsonArray().add(new JsonArray().add(82).add(25.33))
                    .add(new JsonArray().add(82.01).add(25.317)))
            .put("geoproperty", "geoJsonLocation").put("isTest", true)
            .put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Empty response", response.getMessage());
      testContext.completeNow();
    })));

  }

  @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "responseFilter_")
            .put("attrs", new JsonArray().add("resource-id").add("latitude").add("longitude"))
            .put("isTest", true);
    Set<String> attrs = new HashSet<>();
    attrs.add("resource-id");
    attrs.add("latitude");
    attrs.add("longitude");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "responseFilter_").put("isTest", true);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid responseFilter parameters", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "responseFilter_geoSearch_")
            .put("attrs", new JsonArray().add("resource-id").add("latitude").add("longitude"))
            .put("isTest", true).put("lon", 82.987988).put("lat", 25.319768).put("radius", "100");
    Set<String> attrs = new HashSet<>();
    attrs.add("resource-id");
    attrs.add("latitude");
    attrs.add("longitude");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertTrue(82.987988 > response.getJsonObject(0).getDouble("longitude"));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
}

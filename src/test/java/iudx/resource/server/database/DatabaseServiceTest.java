package iudx.resource.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

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
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.database.archives.DatabaseServiceImpl;
import iudx.resource.server.database.archives.elastic.ElasticClient;

@ExtendWith({VertxExtension.class})
public class DatabaseServiceTest {
  private static final Logger logger = LogManager.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String databaseIP, user, password;
  private static int databasePort;
  private static Configuration config;
  private static String timeLimit;

  /** Values for test */
  private static String idOpen;
  private static String idClose;
  private static String temporalStartDate;
  private static String temporalEndDate;

  /* TODO Need to update params to use contants */
  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj=vertx;
    config = new Configuration();
    JsonObject dbConfig = config.configLoader(0, vertx);

    /* Read the configuration and set the rabbitMQ server properties. */
    /* Configuration setup */
    databaseIP = dbConfig.getString("databaseIP");
    databasePort = dbConfig.getInteger("databasePort");
    user = dbConfig.getString("dbUser");
    password = dbConfig.getString("dbPassword");
    timeLimit = dbConfig.getString("timeLimit");

    idOpen = dbConfig.getString("testIdOpen");
    idClose = dbConfig.getString("testIdSecure");
    temporalStartDate = dbConfig.getString("temporalStartDate");
    temporalEndDate = dbConfig.getString("temporalEndDate");

    logger.debug("Info : DB creds " + databaseIP + ", " + databasePort + ", " + user + ", "
        + password + ", " + timeLimit);

    client = new ElasticClient(databaseIP, databasePort, user, password);
    dbService = new DatabaseServiceImpl(client, timeLimit);
    testContext.completeNow();

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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "geoSearch_").put("lon", 72.8296).put("lat", 21.2)
            .put("radius", 500)
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
//      assertEquals(72.833759, response.getJsonArray("results").getJsonObject(0)
//          .getJsonObject("location").getJsonArray("coordinates").getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No resource-id key)")
  void searchWithNoResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No id found", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (resource-id is empty)")
  void searchEmptyResourceId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject().put("id", new JsonArray()).put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("resource-id is empty", new JsonObject(response.getMessage()).getString(
          "detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"));

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("No searchType found", new JsonObject(response.getMessage()).getString(
          "detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "geoSearch_");
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid geo parameters", new JsonObject(response.getMessage())
          .getString("detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("geometry", "polygon").put("georel", "within")
            .put("coordinates",
                "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]")
            .put("geoproperty", "location").put("searchType", "geoSearch_")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(72.5 <= response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonArray("coordinates").getDouble(0)
          && response.getJsonArray("results").getJsonObject(0).getJsonObject("location")
              .getJsonArray("coordinates").getDouble(1) <= 73);
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "geoSearch_").put("geometry", "polygon");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid geo parameters", new JsonObject(response.getMessage())
          .getString("detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("geometry", "polygon").put("georel", "within")
            .put("coordinates",
                "[[[82.9735,25.3703],[83.0053,25.3567],[82.9766,25.3372],[82.95,25.3519],"
                    + "[82.936,25.3722]]]")
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Coordinate mismatch (Polygon)", new JsonObject(response.getMessage())
          .getString("detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("geometry", "linestring").put("georel", "intersects")
            .put("coordinates",
                "[[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20]]")
            .put("geoproperty", "location").put("searchType", "geoSearch_")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(72.5 <= response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonArray("coordinates").getDouble(0)
          && response.getJsonArray("results").getJsonObject(0)
              .getJsonObject("location").getJsonArray("coordinates").getDouble(0) <= 72.9);
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
            .put("geoproperty", "location").put("searchType", "geoSearch_")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(72.8 <= response.getJsonArray("results").getJsonObject(0)
          .getJsonObject("location").getJsonArray("coordinates").getDouble(0)
          && response.getJsonArray("results").getJsonObject(0)
              .getJsonObject("location").getJsonArray("coordinates").getDouble(0) <= 73);
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("geometry", "bbox").put("georel", "within")
            .put("coordinates", "[[82,25.33],[82.01,25.317]]")
            .put("geoproperty", "geoJsonLocation")
            .put("searchType", "geoSearch_")
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Empty response", new JsonObject(response.getMessage())
          .getString("detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "responseFilter_")
            .put("attrs", new JsonArray().add("id").add("license_plate").add("speed"))
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("license_plate");
    attrs.add("speed");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray("results")) {
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "responseFilter_");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Missing/Invalid responseFilter parameters", new JsonObject(response
          .getMessage()).getString("detail"));
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
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "responseFilter_geoSearch_")
            .put("attrs", new JsonArray().add("id").add("location").add("speed"))
            .put("lon", 72.8296).put("lat", 21.2).put("radius", 500)
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("location");
    attrs.add("speed");
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray("results")) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertTrue(72.8 < response.getJsonArray("results").getJsonObject(0)
            .getJsonObject("location").getJsonArray("coordinates").getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Count Geo-Circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("searchType", "geoSearch_")
        .put("lon", 72.8296)
        .put("lat", 21.2)
        .put("radius", 500)
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_").put("timerel", "during")
            .put("time", temporalStartDate).put("endtime", temporalEndDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    OffsetDateTime end = OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonArray("results")
          .getJsonObject(5).getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      logger.info("#### response Date " + resDateUtc);
      assertTrue(!(resDateUtc.isBefore(start) || resDateUtc.isAfter(end)));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (Before)")
  void searchBeforeTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_").put("timerel", "before")
            .put("time", temporalEndDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonArray("results")
          .getJsonObject(6).getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      logger.info("#### response Date " + resDateUtc);
      assertTrue(resDateUtc.isBefore(start));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (After)")
  void searchAfterTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_").put("timerel", "after")
            .put("time", temporalStartDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate =
          OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
              .getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      logger.info("#### response Date " + resDateUtc);
      assertTrue(resDateUtc.isAfter(start) || resDateUtc.isEqual(start));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-Polygon query")
  void countGeoPolygon(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("geometry", "polygon")
        .put("georel", "within")
        .put("coordinates",
            "[[[72.719,21],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]")
        .put("geoproperty", "location")
        .put("searchType", "geoSearch_")
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (TEquals)")
  void searchTequalsTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idOpen))
            .put("searchType", "temporalSearch_").put("timerel", "tequals")
            .put("time", temporalStartDate);

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate =
          OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(0)
              .getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      logger.info("#### response Date " + resDateUtc);
      assertTrue(resDateUtc.isEqual(start));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-Linestring query")
  void countGeoLineString(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("geometry", "linestring")
        .put("georel", "intersects")
        .put("coordinates", "[[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20]]")
        .put("geoproperty", "location")
        .put("searchType", "geoSearch_")
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (Before) with IST date format")
  void searchBeforeTemporalIST(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "temporalSearch_").put("timerel", "before")
            .put("time", "2020-09-29T10:00:00+05:30");

    ZonedDateTime start = ZonedDateTime.parse("2020-10-19T10:00:00+05:30");
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      ZonedDateTime resDate = ZonedDateTime.parse(response.getJsonArray("results")
          .getJsonObject(6).getString("observationDateTime"));
      logger.info("#### response Date " + resDate);
      assertTrue(resDate.isBefore(start));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Exceptions (invalid date)")
  void searchTemporalInvalidDate(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "temporalSearch_").put("timerel", "tequals")
            .put("time", "Invalid date");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Invalid date format", new JsonObject(response.getMessage())
          .getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-Bbox query")
  void countGeoBbox(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("geometry", "bbox")
        .put("georel", "within")
        .put("coordinates", "[[72.8296,21.2],[72.8297,21.15]]")
        .put("geoproperty", "location")
        .put("searchType", "geoSearch_")
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(0).containsKey("count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex Queries (Geo + Temporal + Response Filter)")
  void searchComplexPart2(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idClose))
            .put("searchType", "temporalSearch_geoSearch_responseFilter_")
            .put("timerel", "before")
            .put("time", temporalStartDate)
            .put("lon", 72.8296)
            .put("lat", 21.2)
            .put("radius", 500)
            .put("attrs", new JsonArray()
                .add("id").add("location")
                .add("observationDateTime"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("observationDateTime");
    attrs.add("location");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalStartDate, dateTimeFormatter);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate =
          OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
              .getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      // assertEquals(72.833759, response.getJsonArray("results").getJsonObject(0).getJsonObject(
      // "location").getJsonArray("coordinates").getDouble(0));
      assertTrue(resDateUtc.isBefore(start));
      assertEquals(attrs, response.getJsonArray("results").getJsonObject(2).fieldNames());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("searchType", "responseFilter_").put("attrs", new JsonArray().add("id")
            .add("latitude").add("longitude"));

    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Count is not supported with filtering", new JsonObject(response
          .getMessage()).getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Partial Complex Queries (Geo + Temporal + invalid-Response Filter)")
  void searchPartialComplex(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(idClose))
            .put("searchType", "temporalSearch_geoSearch_response@KSf_")
            .put("timerel", "before")
            .put("time", temporalEndDate)
            .put("lon", 72.8296)
            .put("lat", 21.2)
            .put("radius", 500)
            .put("attrs", new JsonArray()
                .add("id")
                .add("longitude")
                .add("observationDateTime"));
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse(temporalEndDate, dateTimeFormatter);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate =
          OffsetDateTime.parse(response.getJsonArray("results").getJsonObject(3)
              .getString("observationDateTime"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      // assertEquals(72.833759, response.getJsonArray("results").getJsonObject(0).getJsonObject(
      // "location").getJsonArray("coordinates").getDouble(0));
      assertTrue(resDateUtc.isBefore(start) || resDateUtc.isEqual(start));
      testContext.completeNow();
    })));
  }


  @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("searchType", "response!@$_geoS241");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Invalid search request",
          new JsonObject(response.getMessage()).getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is greater than)")
  void searchAttributeGt(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", ">").put("value", "2")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(4).getDouble("referenceLevel") > 2);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is lesser than)")
  void searchAttributeLt(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", "<").put("value", "5")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(2).getDouble("referenceLevel") < 5);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is greater than equal)")
  void searchAttributeGte(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", ">=").put("value", "3")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(
          response.getJsonArray("results").getJsonObject(3).getDouble("referenceLevel") >= 3);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is lesser than equal)")
  void searchAttributeLte(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", "<=").put("value", "5")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(
          response.getJsonArray("results").getJsonObject(7).getDouble("referenceLevel") <= 5);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is between)")
  void searchAttributeBetween(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType", "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject()
                                                  .put("attribute", "referenceLevel")
                                                  .put("operator", "<==>")
                                                  .put("valueLower", "3").put("valueUpper","5")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getJsonArray("results").getJsonObject(9).getDouble("referenceLevel") > 3
          && response.getJsonArray("results").getJsonObject(9).getDouble("referenceLevel") < 5);
      testContext.completeNow();
    })));
  }

  /*
   * TODO: Need to understand operator parameter of JsonObject from the APIServer would look like.
   */

  // @Test
  // @DisplayName("Testing Attribute Search (property is like)")
  // void searchAttributeLike(VertxTestContext testContext){
  // JsonObject request = new JsonObject().put("id", new JsonArray()
  // .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
  // "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
  // "attribute", "CO2").put("operator", "like").put("value", "500")));
  // dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
  // assertTrue(response.getJsonObject(4).getDouble("CO2") > 500);
  // testContext.completeNow();
  // })));
  // }

  @Test
  @DisplayName("Testing Attribute Search (property is equal)")
  void searchAttributeEqual(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", "==").put("value", "4.2")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(response.getJsonArray("results").getJsonObject(0)
          .getDouble("referenceLevel"), 4.2);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is not equal)")
  void searchAttributeNe(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR013"))
        .put("searchType",
            "attributeSearch_")
        .put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "referenceLevel").put("operator", "!=").put("value", "5")))
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(
          response.getJsonArray("results").getJsonObject(4).getDouble("referenceLevel") != 5);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Latest Search")
  @Disabled
  void latestSearch(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR018"))
        .put("searchType", "latestSearch");
    JsonArray id = request.getJsonArray("id");
    JsonArray idFromResponse = new JsonArray();
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      for (Object o : response.getJsonArray("results")) {
        JsonObject doc = (JsonObject) o;
        idFromResponse.add(doc.getString("id"));
      }
      assertEquals(id, idFromResponse);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Latest Search with Response Filter")
  @Disabled
  void latestSearchFiltered(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("datakaveri.org/04a15c9960ffda227e9546f3f46e629e1fe4132b/rs.iudx.io/pune-env-flood/FWR018"))
        .put("searchType", "latestSearch")
        .put("attrs", new JsonArray().add("id")
            .add("observationDateTime"));
    Set<String> attrs = new HashSet<>();
    attrs.add("id");
    attrs.add("observationDateTime");
    JsonArray id = request.getJsonArray("id");
    JsonArray idFromResponse = new JsonArray();
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray("results")) {
        JsonObject jsonObj = (JsonObject) obj;
        idFromResponse.add(jsonObj.getString("id"));
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(id, idFromResponse);
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing auto index selection with malformed id")
  void malformedResourceId(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray().add("malformed-id")).put(
        "searchType", "latestSearch");
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Malformed Id [\"malformed-id\"]", new JsonObject(response.getMessage())
          .getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing auto index selection with Invalid resource-id")
  void autoIndexInvalidId(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "ii/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx"
                        + ".io/surat-itms-realtime-information/surat-itms-live-eta"))
            .put("searchType", "geoSearch_").put("lon", 72.8296).put("lat", 21.2)
            .put("radius", 500)
            .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      System.out.println(response.getMessage());
      assertEquals("Invalid resource id",
          new JsonObject(response.getMessage()).getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing search empty response with 204")
  void searchEmptyResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("searchType", "geoSearch_").put("lon", 72.8296).put("lat", 23.2)
        .put("radius", 10)
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Empty response", new JsonObject(response.getMessage()).getString("detail"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing count empty response with 204")
  void countEmptyResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add(
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta"))
        .put("searchType", "geoSearch_").put("lon", 72.8296).put("lat", 23.2)
        .put("radius", 10)
        .put("applicableFilters", new JsonArray().add("ATTR").add("TEMPORAL").add("SPATIAL"));
    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Empty response", new JsonObject(response.getMessage()).getString("detail"));
      testContext.completeNow();
    })));
  }
}



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
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
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
  private static ElasticClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP, user, password;
  private static int databasePort;
  
  /* TODO Need to update params to use contants */
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
      user = properties.getProperty("dbUser");
      password = properties.getProperty("dbPassword");

    } catch (Exception ex) {

      logger.info(ex.toString());

    }

    // TODO : Need to enable TLS using xpack security

    client = new ElasticClient(databaseIP, databasePort);
    dbService = new DatabaseServiceImpl(client);
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
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "geoSearch_").put("lon", 82.987988).put("lat", 25.319768)
            .put("radius", 100);

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
            .put("searchType", "geoSearch_");
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
            .put("coordinates","[[[82.9735,25.3703],[83.0053,25.3567],[82.9766,25.3372],[82.95,25.3519],"
                + "[82.936,25.3722],[82.9735,25.3703]]]")
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_")
            ;

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
            .put("searchType", "geoSearch_").put("geometry", "polygon");

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
            .put("coordinates","[[[82.9735,25.3703],[83.0053,25.3567],[82.9766,25.3372],[82.95,25.3519],"
                + "[82.936,25.3722]]]")
            .put("geoproperty", "geoJsonLocation").put("searchType", "geoSearch_")
            ;

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
            .put("coordinates","[[82.9735,25.3352],[82.9894,25.3452],[82.99,25.34]]")
            .put("geoproperty", "geoJsonLocation")
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
            .put("coordinates","[[82.95,25.3567],[83.0053,25]]")
            .put("geoproperty", "geoJsonLocation")
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
            .put("coordinates","[[82,25.33],[82.01,25.317]]")
            .put("geoproperty", "geoJsonLocation")
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
            ;
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
            .put("searchType", "responseFilter_");

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
            .put("lon", 82.987988).put("lat", 25.319768).put("radius", 100);
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

  @Test
  @DisplayName("Testing Count Geo-Circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id",new JsonArray()
            .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("searchType","geoSearch_").put("lon",82.987988).put("lat",25.319768)
        .put("radius",100).put("isTest",true);

    dbService.countQuery(request, testContext.succeeding(response-> testContext.verify(()->{
      assertEquals(3146,response.getInteger("Count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Temporal Queries (During)")
  void searchDuringTemporal(VertxTestContext testContext) throws ParseException {
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_").put("timerel", "during")
            .put("time","2020-06-01T14:20:00Z").put("endtime","2020-06-03T15:00:00Z");

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z", dateTimeFormatter);
    OffsetDateTime end = OffsetDateTime.parse("2020-06-03T15:00:00Z", dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(5)
          .getString("time"), dateTimeFormatter);
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
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_").put("timerel", "before")
            .put("time","2020-06-01T14:20:00Z");

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z",dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(6)
          .getString("time"), dateTimeFormatter);
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
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_").put("timerel", "after")
            .put("time","2020-06-01T14:20:00Z");

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z",dateTimeFormatter);
    logger.info("### start date: " + start);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(3)
          .getString("time"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      logger.info("#### response Date " + resDateUtc);
      assertTrue(resDateUtc.isAfter(start));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-Polygon query")
  void countGeoPolygon(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("geometry","polygon").put("georel","within")
        .put("coordinates","[[[82.9735,25.3703],[83.0053,25.3567],[82.9766,25.3372],[82.95,25.3519],"
            + "[82.936,25.3722],[82.9735,25.3703]]]")
        .put("geoproperty","geoJsonLocation").put("searchType","geoSearch_").put("isTest",true);

    dbService.countQuery(request, testContext.succeeding(response-> testContext.verify(()->{
      assertEquals(91870,response.getInteger("Count"));
      testContext.completeNow();
    })));
  }
  
    @Test
    @DisplayName("Testing Temporal Queries (TEquals)")
    void searchTequalsTemporal(VertxTestContext testContext) throws ParseException {
      JsonObject request =
          new JsonObject()
              .put("id",
                  new JsonArray().add(
                      "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
              .put("searchType", "temporalSearch_").put("timerel", "tequals")
              .put("time","2020-06-01T14:20:00Z");
  
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
      OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z",dateTimeFormatter);
      logger.info("### start date: " + start);
  
      dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
        OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(0)
            .getString("time"), dateTimeFormatter);
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
            .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("geometry","linestring").put("georel","intersects")
        .put("coordinates","[[82.9735,25.3352],[82.9894,25.3452],[82.99,25.34]]")
        .put("geoproperty","geoJsonLocation").put("isTest",true).put("searchType","geoSearch_");

    dbService.countQuery(request, testContext.succeeding(response-> testContext.verify(()->{
      assertEquals(207,response.getInteger("Count"));
      testContext.completeNow();
    })));
  }
  @Test
  @DisplayName("Testing Temporal Exceptions (invalid date)")
  void searchTemporalInvalidDate(VertxTestContext testContext){
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_").put("timerel", "tequals")
            .put("time","Invalid date");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      assertEquals("Invalid date format", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-Bbox query")
  void countGeoBbox(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("id", new JsonArray()
            .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("geometry","bbox").put("georel","within")
        .put("coordinates","[[82.95,25.3567],[83.0053,25]]")
        .put("geoproperty","geoJsonLocation").put("isTest",true).put("searchType","geoSearch_");

    dbService.countQuery(request, testContext.succeeding(response-> testContext.verify(()->{
      assertEquals(2194856,response.getInteger("Count"));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex Queries (Geo + Temporal + Response Filter)")
  void searchComplexPart2(VertxTestContext testContext){
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_geoSearch_responseFilter_")
            .put("timerel", "before").put("time","2020-06-01T14:20:00Z").put("lon", 82.987988)
            .put("lat", 25.319768).put("radius", 100).put("attrs", new JsonArray()
            .add("resource-id").add("longitude").add("time"));
    Set<String> attrs = new HashSet<>();
    attrs.add("resource-id");
    attrs.add("time");
    attrs.add("longitude");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z",dateTimeFormatter);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(3)
          .getString("time"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      assertEquals(82.98797, response.getJsonObject(0).getDouble("longitude"));
      assertTrue(resDateUtc.isBefore(start));
      assertEquals(attrs,response.getJsonObject(2).fieldNames());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("searchType","responseFilter_").put("attrs", new JsonArray().add("resource-id")
            .add("latitude").add("longitude"))
        .put("isTest",true);

    dbService.countQuery(request, testContext.failing(response-> testContext.verify(()->{
      assertEquals("Count is not supported with filtering", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Partial Complex Queries (Geo + Temporal + invalid-Response Filter)")
  void searchPartialComplex(VertxTestContext testContext){
    JsonObject request =
        new JsonObject()
            .put("id",
                new JsonArray().add(
                    "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
            .put("searchType", "temporalSearch_geoSearch_response@KSf_")
            .put("timerel", "before").put("time","2020-06-01T14:20:00Z").put("lon", 82.987988)
            .put("lat", 25.319768).put("radius", 100).put("attrs", new JsonArray()
            .add("resource-id").add("longitude").add("time"));
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXXXX");
    OffsetDateTime start = OffsetDateTime.parse("2020-06-01T14:20:00Z",dateTimeFormatter);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      OffsetDateTime resDate = OffsetDateTime.parse(response.getJsonObject(3)
          .getString("time"), dateTimeFormatter);
      OffsetDateTime resDateUtc = resDate.withOffsetSameInstant(ZoneOffset.UTC);
      assertEquals(82.98797, response.getJsonObject(0).getDouble("longitude"));
      assertTrue(resDateUtc.isBefore(start));
      testContext.completeNow();
    })));
  }


  @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live"))
        .put("searchType","response!@$_geoS241");

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(()->{
      assertEquals("Invalid search request", response.getMessage());
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is greater than)")
  void searchAttributeGt(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
            "attribute", "CO2").put("operator", ">").put("value", "500")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(4).getDouble("CO2") > 500);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is lesser than)")
  void searchAttributeLt(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", "<").put("value", "500")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(2).getDouble("CO2") < 500);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is greater than equal)")
  void searchAttributeGte(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", ">=").put("value", "500")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(3).getDouble("CO2") >= 500);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is lesser than equal)")
  void searchAttributeLte(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", "<=").put("value", "500")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(7).getDouble("CO2") <= 500);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is between)")
  void searchAttributeBetween(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", "<==>").put("valueLower", "500").put("valueUpper",
        "1000")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(9).getDouble("CO2") > 500);
      testContext.completeNow();
    })));
  }

  /* TODO: Need to understand operator parameter of JsonObject from the APIServer would
  look like. */

  //  @Test
  //  @DisplayName("Testing Attribute Search (property is like)")
  //  void searchAttributeLike(VertxTestContext testContext){
  //    JsonObject request = new JsonObject().put("id", new JsonArray()
  //        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
  //        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
  //        "attribute", "CO2").put("operator", "like").put("value", "500")));
  //    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
  //      assertTrue(response.getJsonObject(4).getDouble("CO2") > 500);
  //      testContext.completeNow();
  //    })));
  //  }

  @Test
  @DisplayName("Testing Attribute Search (property is equal)")
  void searchAttributeEqual(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", "==").put("value", "501.11")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertEquals(response.getJsonObject(0).getDouble("CO2"), 501.11);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Attribute Search (property is not equal)")
  void searchAttributeNe(VertxTestContext testContext){
    JsonObject request = new JsonObject().put("id", new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-aqm/EM_01_0103_01")).put("searchType",
        "attributeSearch_").put("attr-query", new JsonArray().add(new JsonObject().put(
        "attribute", "CO2").put("operator", "!=").put("value", "500")));
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(()->{
      assertTrue(response.getJsonObject(4).getDouble("CO2") != 500);
      testContext.completeNow();
    })));
  }
}


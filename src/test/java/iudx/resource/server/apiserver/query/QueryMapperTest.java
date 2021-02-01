package iudx.resource.server.apiserver.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.util.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;



@ExtendWith(VertxExtension.class)
public class QueryMapperTest {

  private QueryMapper qm;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    qm = new QueryMapper();
    testContext.completeNow();
  }

  @Test
  public void testGetQueryTerms(Vertx vertx, VertxTestContext testContext) {
    String q = "speed>=300";
    System.out.println(qm);
    JsonObject json = qm.getQueryTerms(q);
    assertEquals("speed", json.getString(Constants.JSON_ATTRIBUTE));
    assertEquals(">=", json.getString(Constants.JSON_OPERATOR));
    assertEquals("300", json.getString(Constants.JSON_VALUE));
    testContext.completeNow();

  }

  @Test
  public void testToJson(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(Constants.NGSILDQUERY_ID, "id1");
    map.add(Constants.NGSILDQUERY_ATTRIBUTE, "attr1");
    NGSILDQueryParams params = new NGSILDQueryParams(map);
    System.out.println(qm);
    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(Constants.NGSILDQUERY_ID));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.getJsonArray(Constants.NGSILDQUERY_ID) instanceof JsonArray);
    assertTrue(json.getJsonArray(Constants.NGSILDQUERY_ATTRIBUTE) instanceof JsonArray);
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for Circle geo-query")
  public void testToJson4CircleQuery(Vertx vertx, VertxTestContext testContext) {
    // georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(Constants.NGSILDQUERY_ID, "id1");
    map.add(Constants.NGSILDQUERY_ATTRIBUTE, "attr1");
    map.add(Constants.NGSILDQUERY_GEOREL, "near;maxdistance=360");
    map.add(Constants.NGSILDQUERY_GEOMETRY, "point");
    map.add(Constants.NGSILDQUERY_GEOPROPERTY,"location");
    map.add(Constants.NGSILDQUERY_COORDINATES, "[8.68462,49.40606]");
    NGSILDQueryParams params = new NGSILDQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(Constants.JSON_LAT));
    assertTrue(json.containsKey(Constants.JSON_LON));
    assertTrue(json.containsKey(Constants.JSON_RADIUS));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_TIMEREL));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_TIME));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for geo-query")
  public void testToJson4GeoQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(Constants.NGSILDQUERY_ID, "id1,id2");
    map.add(Constants.NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(Constants.NGSILDQUERY_GEOREL, "within");
    map.add(Constants.NGSILDQUERY_GEOMETRY, "Polygon");
    map.add(Constants.NGSILDQUERY_GEOPROPERTY,"location");
    map.add(Constants.NGSILDQUERY_COORDINATES,
        "[[[8.68462,49.40606],[8.68550,49.40622],[8.68545,49.40634],[8.68457,49.40617],[8.68462,49.40606]]]");
    NGSILDQueryParams params = new NGSILDQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(Constants.NGSILDQUERY_ID));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_COORDINATES));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_GEOREL));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_GEOMETRY));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_TIMEREL));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_TIME));
    assertFalse(json.containsKey(Constants.NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for temporal queries")
  public void testToJson4TemporalQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(Constants.NGSILDQUERY_ID, "id1,id2");
    map.add(Constants.NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(Constants.NGSILDQUERY_TIMEREL, "during");
    map.add(Constants.NGSILDQUERY_TIME, "2020-01-23T14:20:00Z");
    map.add(Constants.NGSILDQUERY_ENDTIME, "2020-01-24T14:40:00Z");
    NGSILDQueryParams params = new NGSILDQueryParams(map);

    JsonObject json = qm.toJson(params, true);

    assertTrue(json.containsKey(Constants.NGSILDQUERY_ID));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_TIMEREL));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_TIME));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_ENDTIME));
    testContext.completeNow();
  }

  @Test
  @Description("QueryMapper test for simple attribute query")
  public void testToJson4SimpleAttributeQuery(Vertx vertx, VertxTestContext testContext) {
    MultiMap map = MultiMap.caseInsensitiveMultiMap();
    map.add(Constants.NGSILDQUERY_ID, "id1,id2");
    map.add(Constants.NGSILDQUERY_ATTRIBUTE, "attr1,attr2");
    map.add(Constants.NGSILDQUERY_Q, "speed>=300;temprature==35");
    NGSILDQueryParams params = new NGSILDQueryParams(map);

    JsonObject json = qm.toJson(params, false);

    assertTrue(json.containsKey(Constants.NGSILDQUERY_ID));
    assertTrue(json.containsKey(Constants.NGSILDQUERY_ATTRIBUTE));
    assertTrue(json.containsKey(Constants.JSON_ATTR_QUERY));
    assertTrue(json.getJsonArray(Constants.JSON_ATTR_QUERY) instanceof JsonArray);
    assertEquals(json.getJsonArray(Constants.JSON_ATTR_QUERY).size(), 2);
    testContext.completeNow();
  }

  @AfterEach
  public void teardown() {

  }

}

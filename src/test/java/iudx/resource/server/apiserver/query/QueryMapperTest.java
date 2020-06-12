package iudx.resource.server.apiserver.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.ApiServerVerticle;

@ExtendWith(VertxExtension.class)
public class QueryMapperTest {

	private QueryMapper qm;

	@BeforeEach
	public void setup(Vertx vertx, VertxTestContext testContext) {
		vertx.deployVerticle(new ApiServerVerticle(), testContext.completing());
		qm = new QueryMapper();
	}

	@Test
	public void testGetQueryTerms(Vertx vertx, VertxTestContext testContext) {
		String q = "speed>=300";
		JsonObject json = qm.getQueryTerms(q);
		assertEquals("speed", json.getString("attribute"));
		assertEquals(">=", json.getString("operator"));
		assertEquals("300", json.getString("value"));
		testContext.completeNow();

	}

	@Test
	public void testToJson(Vertx vertx, VertxTestContext testContext) {
		MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("id", "id1");
		map.add("attrs", "attr1");
		NGSILDQueryParams params = new NGSILDQueryParams(map);
		System.out.println(qm);
		JsonObject json = qm.toJson(params, false);

		assertTrue(json.containsKey("id"));
		assertTrue(json.containsKey("attribute-filter"));
		assertTrue(json.getJsonArray("id") instanceof JsonArray);
		assertTrue(json.getJsonArray("attribute-filter") instanceof JsonArray);
		testContext.completeNow();
	}

	@Test
	@Description("QueryMapper test for Circle geo-query")
	public void testToJson4CircleQuery(Vertx vertx, VertxTestContext testContext) {
		// georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D
		MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("id", "id1");
		map.add("attrs", "attr1");
		map.add("georel", "near;maxdistance=360");
		map.add("geometry", "point");
		map.add("coordinates", "[8.684628009796143,49.406062179606515]");
		NGSILDQueryParams params = new NGSILDQueryParams(map);

		JsonObject json = qm.toJson(params, false);

		assertTrue(json.containsKey("lat"));
		assertTrue(json.containsKey("lon"));
		assertTrue(json.containsKey("radius"));
		assertFalse(json.containsKey("timerel"));
		assertFalse(json.containsKey("date"));
		assertFalse(json.containsKey("endtime"));
		testContext.completeNow();
	}

	@Test
	@Description("QueryMapper test for geo-query")
	public void testToJson4GeoQuery(Vertx vertx, VertxTestContext testContext) {
		MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("id", "id1,id2");
		map.add("attrs", "attr1,attr2");
		map.add("georel", "within");
		map.add("geometry", "Polygon");
		map.add("coordinates",
				"[[[8.684628009796143,49.406062179606515],[8.685507774353027,49.4062262372493],[8.68545413017273,49.40634491690448],[8.684579730033875,49.40617736907259],[8.684628009796143,49.406062179606515]]]");
		NGSILDQueryParams params = new NGSILDQueryParams(map);

		JsonObject json = qm.toJson(params, false);

		assertTrue(json.containsKey("id"));
		assertTrue(json.containsKey("attribute-filter"));
		assertTrue(json.containsKey("coordinates"));
		assertTrue(json.containsKey("georel"));
		assertTrue(json.containsKey("geometry"));
		assertFalse(json.containsKey("timerel"));
		assertFalse(json.containsKey("date"));
		assertFalse(json.containsKey("endtime"));
		testContext.completeNow();
	}

	@Test
	@Description("QueryMapper test for temporal queries")
	public void testToJson4TemporalQuery(Vertx vertx, VertxTestContext testContext) {
		MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("id", "id1,id2");
		map.add("attrs", "attr1,attr2");
		map.add("timerel", "between");
		map.add("time", "2020-01-23T14:20:00Z");
		map.add("endtime", "2020-01-24T14:40:00Z");
		NGSILDQueryParams params = new NGSILDQueryParams(map);

		JsonObject json = qm.toJson(params, true);

		assertTrue(json.containsKey("id"));
		assertTrue(json.containsKey("attribute-filter"));
		assertTrue(json.containsKey("timerel"));
		assertTrue(json.containsKey("time"));
		assertTrue(json.containsKey("endtime"));
		testContext.completeNow();
	}

	@Test
	@Description("QueryMapper test for simple attribute query")
	public void testToJson4SimpleAttributeQuery(Vertx vertx, VertxTestContext testContext) {
		MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("id", "id1,id2");
		map.add("attrs", "attr1,attr2");
		map.add("q", "speed>=300;temprature==35");
		NGSILDQueryParams params = new NGSILDQueryParams(map);

		JsonObject json = qm.toJson(params, false);

		assertTrue(json.containsKey("id"));
		assertTrue(json.containsKey("attribute-filter"));
		assertTrue(json.containsKey("attr-query"));
		assertTrue(json.getJsonArray("attr-query") instanceof JsonArray);
		assertEquals(json.getJsonArray("attr-query").size(), 2);
		testContext.completeNow();
	}

	@AfterEach
	public void teardown() {

	}

}

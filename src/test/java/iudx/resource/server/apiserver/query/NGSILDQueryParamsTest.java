package iudx.resource.server.apiserver.query;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static iudx.resource.server.apiserver.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class NGSILDQueryParamsTest {
    NGSILDQueryParams ngsildQueryParams;
    JsonObject jsonObject;

    @ParameterizedTest
    @ValueSource(strings = {NGSILDQUERY_MAXDISTANCE, NGSILDQUERY_MINDISTANCE})
    @DisplayName("Test create method : with JsonObject input ")
    public void test_create_with_json_input(String distance, VertxTestContext vertxTestContext) throws URISyntaxException {
        jsonObject = new JsonObject();
        JsonObject dummyJson = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        dummyJson.put("geometry", "Dummy geometry value");
        dummyJson.put("coordinates", jsonArray);
        dummyJson.put("georel", "Dummy;" + distance + "=1000");
        dummyJson.put("timerel", "Dummy timerel value");
        dummyJson.put("time", "Dummy time value");
        dummyJson.put("endtime", "Dummy endtime value");

        jsonObject.put("Dummy_key", "Dummy_value");
        jsonObject.put(NGSILDQUERY_Q, "Dummy textQuery");
        jsonObject.put(NGSILDQUERY_ATTRIBUTE, "Dummy textQuery");
        jsonObject.put(NGSILDQUERY_TYPE, "Dummy textQuery");
        jsonObject.put("geoQ", dummyJson.put("geoQ", "Dummy value"));
        jsonObject.put(NGSILDQUERY_Q, "Dummy textQuery");
        jsonObject.put("temporalQ", dummyJson.put("temporalQ", "Dummy textQuery"));
        jsonObject.put("entities", "[{\"id\":\"dummy_id\",\"idPattern\":\"dummy_idPattern\"}]");
        jsonObject.put(IUDXQUERY_OPTIONS, "Dummy textQuery");
        jsonObject.put(NGSILDQUERY_FROM, "Dummy textQuery");
        jsonObject.put(NGSILDQUERY_SIZE, "Dummy textQuery");
        ngsildQueryParams = new NGSILDQueryParams();
        ngsildQueryParams = new NGSILDQueryParams(jsonObject);

        List<String> expected_string_list = new ArrayList<>();
        expected_string_list.add("Dummy textQuery");

        List<URI> uriList = new ArrayList<>();
        List<String> expected_pattern = new ArrayList<>();
        expected_pattern.add("dummy_idPattern");
        uriList.add(new URI("dummy_id"));
        assertEquals(expected_pattern, ngsildQueryParams.getIdPattern());
        assertEquals(uriList, ngsildQueryParams.getId());
        assertEquals("TemporalRelation [endTime=Dummy endtime value, temprel=Dummy timerel value, time=Dummy time value]"
                , ngsildQueryParams.getTemporalRelation().toString());
        assertEquals(expected_string_list, ngsildQueryParams.getAttrs());
        assertEquals("Dummy textQuery", ngsildQueryParams.getQ());
        Assertions.assertEquals(expected_string_list, ngsildQueryParams.getType());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test create method : when input is Multimap params")
    public void test_create_with_multimap_params(VertxTestContext vertxTestContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        map.add(IUDXQUERY_OPTIONS, "Dummy value");
        map.add(NGSILDQUERY_SIZE, "Dummy size");
        map.add(NGSILDQUERY_FROM, "Dummy value");
        map.add(NGSILDQUERY_GEOREL, "dummy;" + NGSILDQUERY_MINDISTANCE + "=3");
        ngsildQueryParams = new NGSILDQueryParams(map);
        assertEquals("Dummy value", ngsildQueryParams.getOptions());
        assertEquals("Dummy size", ngsildQueryParams.getPageSize());
        assertEquals("Dummy value", ngsildQueryParams.getPageFrom());
        vertxTestContext.completeNow();
    }


    @Test
    @DisplayName("Test setOptions method")
    public void test_setOptions(VertxTestContext vertxTestContext) {
        ngsildQueryParams = new NGSILDQueryParams();
        ngsildQueryParams.setOptions("Dummy option");
        assertEquals("Dummy option", ngsildQueryParams.getOptions());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setQ method")
    public void test_setQ(VertxTestContext vertxTestContext) {
        ngsildQueryParams = new NGSILDQueryParams();
        ngsildQueryParams.setQ("Dummy Q");
        assertEquals("Dummy Q", ngsildQueryParams.getQ());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setIdPattern method")
    public void test_setIdPattern(VertxTestContext vertxTestContext) {
        ngsildQueryParams = new NGSILDQueryParams();
        List<String> expected_pattern = new ArrayList<>();
        expected_pattern.add("dummy_idPattern");
        ngsildQueryParams.setIdPattern(expected_pattern);
        assertEquals(expected_pattern, ngsildQueryParams.getIdPattern());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setAttrs method")
    public void test_setAttrs(VertxTestContext vertxTestContext) {
        ngsildQueryParams = new NGSILDQueryParams();
        List<String> expected_attribute = new ArrayList<>();
        expected_attribute.add("dummy_attribute");
        ngsildQueryParams.setAttrs(expected_attribute);
        assertEquals(expected_attribute, ngsildQueryParams.getAttrs());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setType method")
    public void test_setType(VertxTestContext vertxTestContext) {
        ngsildQueryParams = new NGSILDQueryParams();
        List<String> expected_type = new ArrayList<>();
        expected_type.add("dummy_type");
        ngsildQueryParams.setType(expected_type);
        assertEquals(expected_type, ngsildQueryParams.getType());
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setId method")
    public void test_setId(VertxTestContext vertxTestContext) throws URISyntaxException {
        ngsildQueryParams = new NGSILDQueryParams();
        List<URI> uri = new ArrayList<>();
        uri.add(new URI("Dummy_URI"));
        ngsildQueryParams.setId(uri);
        assertEquals(uri, ngsildQueryParams.getId());
        vertxTestContext.completeNow();
    }
}

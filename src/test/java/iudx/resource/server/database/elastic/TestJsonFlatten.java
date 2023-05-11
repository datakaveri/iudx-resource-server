package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestJsonFlatten {
  private JsonFlatten jsonFlatten;
  @Mock private JsonNode node;
  @Mock private Map<String, ValueNode> map;
  private ObjectMapper objectMapper;
  private String json;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) throws JsonProcessingException {
    objectMapper = new ObjectMapper();
    json =
        "{\n"
            + "\"Residents\" : [\n"
            + "{\n"
            + "\"ID\":78542894753894535,\n"
            + "\"isOwner\":true,\n"
            + "\"name\":\"Somebody\",\n"
            + "\"flatNumber\":24,\n"
            + "\"amountToPay\":23.45,\n"
            + "\"region\":\"A\",\n"
            + "\"observationDateTime\":\"2020-10-10T20:45:00+05:30\"\n"
            + "},\n"
            + "{\n"
            + "\"ID\":78542894753894767,\n"
            + "\"isOwner\":false,\n"
            + "\"name\":\"Someone\",\n"
            + "\"flatNumber\":25,\n"
            + "\"amountToPay\":23.45,\n"
            + "\"region\":\"B\",\n"
            + "\"observationDateTime\":\"2021-10-10T20:45:00+05:30\"\n"
            + "},\n"
            + "{\n"
            + "\"ID\":78542894753894636,\n"
            + "\"isOwner\":true,\n"
            + "\"name\":\"Anybody\",\n"
            + "\"flatNumber\":26,\n"
            + "\"amountToPay\":23.45,\n"
            + "\"region\":\"C\",\n"
            + "\"observationDateTime\":\"2022-10-10T20:45:00+05:30\"\n"
            + "}\n"
            + "]\n"
            + "}";
    JsonNode jsonNode = objectMapper.readTree(json);
    jsonFlatten = new JsonFlatten(jsonNode);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test flatten method ")
  public void testFlatten(VertxTestContext vertxTestContext) {
    LinkedHashMap<String, Object> result = jsonFlatten.flatten();
    assertEquals("78542894753894535", result.get("Residents.0.ID").toString());
    assertEquals("78542894753894767", result.get("Residents.1.ID").toString());
    assertEquals("78542894753894636", result.get("Residents.2.ID").toString());

    assertTrue((Boolean) result.get("Residents.0.isOwner"));
    assertFalse((Boolean) result.get("Residents.1.isOwner"));
    assertTrue((Boolean) result.get("Residents.2.isOwner"));

    assertEquals("Somebody", result.get("Residents.0.name"));
    assertEquals("Someone", result.get("Residents.1.name"));
    assertEquals("Anybody", result.get("Residents.2.name"));

    assertEquals(24, result.get("Residents.0.flatNumber"));
    assertEquals(25, result.get("Residents.1.flatNumber"));
    assertEquals(26, result.get("Residents.2.flatNumber"));

    assertEquals(23.45, result.get("Residents.0.amountToPay"));
    assertEquals(23.45, result.get("Residents.1.amountToPay"));
    assertEquals(23.45, result.get("Residents.2.amountToPay"));

    assertEquals("A", result.get("Residents.0.region"));
    assertEquals("B", result.get("Residents.1.region"));
    assertEquals("C", result.get("Residents.2.region"));

    assertTrue(result.containsKey("Residents.0.observationDateTime"));
    assertTrue(result.containsKey("Residents.0.observationDateTime"));
    assertTrue(result.containsKey("Residents.0.observationDateTime"));

    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test flattenJson method : Failure")
  public void testFlattenJson(VertxTestContext vertxTestContext) {
    assertThrows(RuntimeException.class, () -> JsonFlatten.flattenJson(node, "dummy_parent", map));
    vertxTestContext.completeNow();
  }
}

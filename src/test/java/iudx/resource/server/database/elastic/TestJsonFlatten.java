package iudx.resource.server.database.elastic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
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
    String expectedCsv =
        "Residents.0.ID=78542894753894535, Residents.0.isOwner=true, Residents.0.name=Somebody, Residents.0.flatNumber=24, Residents.0.amountToPay=23.45, Residents.0.region=A, Residents.0.observationDateTime=2020-10-10 20:45:00.0, Residents.1.ID=78542894753894767, Residents.1.isOwner=false, Residents.1.name=Someone, Residents.1.flatNumber=25, Residents.1.amountToPay=23.45, Residents.1.region=B, Residents.1.observationDateTime=2021-10-10 20:45:00.0, Residents.2.ID=78542894753894636, Residents.2.isOwner=true, Residents.2.name=Anybody, Residents.2.flatNumber=26, Residents.2.amountToPay=23.45, Residents.2.region=C, Residents.2.observationDateTime=2022-10-10 20:45:00.0";
    assertEquals(expectedCsv, jsonFlatten.flatten().toString().replace("{", "").replace("}", ""));
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test flattenJson method : Failure")
  public void testFlattenJson(VertxTestContext vertxTestContext) {
    assertThrows(RuntimeException.class, () -> JsonFlatten.flattenJson(node, "dummy_parent", map));
    vertxTestContext.completeNow();
  }
}

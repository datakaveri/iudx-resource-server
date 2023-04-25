package iudx.resource.server.database.elastic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import com.github.sisyphsu.dateparser.DateParserUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonFlatten {

    private final Map<String, ValueNode> json = new LinkedHashMap<>();
    private final LinkedHashMap<String, Object> jsonObj = new LinkedHashMap<>();
    private final JsonNode root;

    public JsonFlatten(JsonNode node) {
        this.root = Objects.requireNonNull(node);
    }

    public static void flattenJson(JsonNode node, String parent, Map<String, ValueNode> map) {
        if (node instanceof ValueNode) {
            map.put(parent, (ValueNode) node);
        } else {
            String prefix = parent == null ? "" : parent + ".";
            if (node instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) node;
                for (int i = 0; i < arrayNode.size(); i++) {
                    flattenJson(arrayNode.get(i), prefix + i, map);
                }
            } else if (node instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) node;
                for (Iterator<Map.Entry<String, JsonNode>> it = objectNode.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> field = it.next();
                    flattenJson(field.getValue(), prefix + field.getKey(), map);
                }
            } else {
                throw new RuntimeException("unknown json node");
            }
        }
    }

    public LinkedHashMap<String, Object> flatten() {
        flattenJson(root, null, json);
        for (String key : json.keySet()) {
            if (Objects.equals(key, "observationDateTime")) {
                LocalDateTime localDateTime = DateParserUtils.parseDateTime(json.get(key).asText());
                jsonObj.put(key, Timestamp.valueOf(localDateTime));
                continue;
            }

            if (json.get(key).isInt()) jsonObj.put(key, json.get(key).asInt());
            if (json.get(key).isLong()) jsonObj.put(key, json.get(key).asLong());
            if (json.get(key).isFloat()) jsonObj.put(key, json.get(key).asDouble());
            if (json.get(key).isDouble()) jsonObj.put(key, json.get(key).asDouble());
            if (json.get(key).isBoolean()) jsonObj.put(key, json.get(key).asBoolean());
            if (json.get(key).isTextual()) jsonObj.put(key, json.get(key).asText());
        }
        return jsonObj;
    }
}
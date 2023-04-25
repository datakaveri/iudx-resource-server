package iudx.resource.server.apiserver.validation;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.validation.types.JsonSchemaTypeValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
class JsonSchemaTypeValidatorTest {

  JsonSchemaTypeValidator jsonSchemaTypeValidator;

  @Mock ValidationException validationException;

  String jsonSchema;
  JsonObject json;
  Schema schema;

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    jsonSchema =
        "{\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"id\": {\n"
            + "      \"type\": \"string\"\n"
            + "    },\n"
            + "    \"server-url\": {\n"
            + "      \"type\": \"string\"\n"
            + "    },\n"
            + "    \"server-port\": {\n"
            + "      \"type\": \"integer\"\n"
            + "    },\n"
            + "    \"isSecure\": {\n"
            + "      \"type\": \"boolean\"\n"
            + "    }\n"
            + "  },\n"
            + "  \"required\": [\n"
            + "    \"id\",\n"
            + "    \"server-url\",\n"
            + "    \"server-port\",\n"
            + "    \"isSecure\"\n"
            + "  ]\n"
            + "}";

    json =
        new JsonObject()
            .put(
                "id",
                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood/FWR1059,,")
            .put("server-url", "www.abc.com")
            .put("server-port", 1234)
            .put("isSecure", true);

    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);

    try {
      schema = schemaParser.parse(new JsonObject(jsonSchema));
    } catch (Exception ex) {

      testContext.failNow("fail to create schema from json " + ex);
    }

    testContext.completeNow();
  }

  @Test
  public void test(VertxTestContext vertxTestContext) {
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(json, schema);
    assertTrue(jsonSchemaTypeValidator.isValid());
    vertxTestContext.completeNow();
  }

  @Test
  public void testInvalidJson(VertxTestContext vertxTestContext) {
    json.put("server-port", "asdd");
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(json, schema);
    Assertions.assertThrows(DxRuntimeException.class, () -> jsonSchemaTypeValidator.isValid());
    vertxTestContext.completeNow();
  }

  @Test
  public void checkFailureCode(VertxTestContext vertxTestContext) {
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(json, schema);
    assertEquals(400, jsonSchemaTypeValidator.failureCode());
    vertxTestContext.completeNow();
  }

  @Test
  public void checkFailureMessage(VertxTestContext vertxTestContext) {
    jsonSchemaTypeValidator = new JsonSchemaTypeValidator(json, schema);
    assertEquals(
        "Invalid json format in post request [schema mismatch]",
        jsonSchemaTypeValidator.failureMessage());
    vertxTestContext.completeNow();
  }
}

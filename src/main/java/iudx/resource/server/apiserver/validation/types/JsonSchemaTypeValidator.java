package iudx.resource.server.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.NoSyncValidationException;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.ValidationException;

public class JsonSchemaTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(JsonSchemaTypeValidator.class);
  private JsonObject value;
  private boolean required;
  private Schema schema;

  public JsonSchemaTypeValidator(JsonObject value, boolean required, Schema schema) {
    this.value = value;
    this.required = required;
    this.schema = schema;
  }

  @Override
  public boolean isValid() {
    try {
      schema.validateSync(value);
    } catch (ValidationException e) {
      LOGGER.error("Validation error :" + e.getMessage());
      return false;
    } catch (NoSyncValidationException e) {
      LOGGER.error("Validation error :" + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Json schema validation failed.";
  }

}

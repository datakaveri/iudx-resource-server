package iudx.resource.server.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class OptionsTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(OptionsTypeValidator.class);

  private String value;
  private boolean required;

  public OptionsTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!value.equals("count")) {
      LOGGER.error("Validation error : count is only allowed value for options parameter");
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
    return "Invalid options value";
  }
}

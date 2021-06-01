package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TimeRelTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<Object> allowedValues = List.of("after", "before", "during", "between");

  private String value;
  private boolean required;

  public TimeRelTypeValidator(String value, boolean required) {
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
    if (!allowedValues.contains(value)) {
      LOGGER.error("Validation error : Value " + value + " " + "is not allowed");
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
    return "Invalid time relation";
  }
}

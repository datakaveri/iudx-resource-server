package iudx.resource.server.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringTypeValidator implements Validator{
  private static final Logger LOGGER = LogManager.getLogger(StringTypeValidator.class);
  
  private String value;
  private boolean required;

  public StringTypeValidator(String value, boolean required) {
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
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        return false;
      }
    }
    if(value.length()>100) {
      LOGGER.error("Validation error : length >100 not allowed");
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
    return "Invalid string";
  }

}

package iudx.resource.server.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OptionsHeaderValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(OptionsHeaderValidator.class);

  private String value;
  private boolean required;

  public OptionsHeaderValidator(String value, boolean required) {
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
        LOGGER.error("Validation error :  blank value passed");
        return false;
      }
    }
    if (!value.equals("streaming")) {
      LOGGER.error("Validation error : streaming is only allowed value for options parameter");
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

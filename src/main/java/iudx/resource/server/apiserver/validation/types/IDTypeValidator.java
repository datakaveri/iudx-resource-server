package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class IDTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(IDTypeValidator.class);

  private Integer minLength = VALIDATION_ID_MIN_LEN;
  private Integer maxLength = VALIDATION_ID_MAX_LEN;
  private static final Pattern regexIDPattern =
      Pattern.compile(
          "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");


  private String value;
  private boolean required;

  public IDTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  public boolean isvalidIUDXId(String value) {
    return regexIDPattern.matcher(value).matches();
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
    if (value.length() > maxLength) {
      LOGGER.error("Validation error : Value exceed max character limit.");
      return false;
    }
    if (!isvalidIUDXId(value)) {
      LOGGER.error("Validation error : Invalid id.");
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
    return "Invalid id.";
  }

}

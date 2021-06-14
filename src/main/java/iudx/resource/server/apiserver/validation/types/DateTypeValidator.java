package iudx.resource.server.apiserver.validation.types;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DateTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(DateTypeValidator.class);


  private String value;
  private boolean required;

  public DateTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidDate(String value) {
    String dateString = value.trim().replaceAll("\\s", "+");// since + is treated as space in uri
    // params
    try {
      ZonedDateTime.parse(dateString);
      return true;
    } catch (DateTimeParseException e) {
      LOGGER.error("Validation error : Invalid Date format.");
      return false;
    }
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
    return isValidDate(value);
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid date";
  }

}

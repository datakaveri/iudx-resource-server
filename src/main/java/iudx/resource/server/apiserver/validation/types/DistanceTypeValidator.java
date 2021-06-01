package iudx.resource.server.apiserver.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DistanceTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(DistanceTypeValidator.class);
  private double allowedDistance = 1000.0;

  private String value;
  private boolean required;

  public DistanceTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }


  private boolean isValidDistance(String distance) {
    try {
      Double distanceValue = Double.parseDouble(distance);
      if (distanceValue > Integer.MAX_VALUE) {
        LOGGER.error("Validation error : Invalid integer value (Integer overflow).");
        return false;
      }
      if (distanceValue > allowedDistance || distanceValue < 1) {
        LOGGER.error("Validation error : Distance outside (1,1000)m range not allowed");
        return false;
      }
    } catch (NumberFormatException ex) {
      LOGGER.error("Validation error : Number format error ( not a valid distance)");
      return false;
    }
    return true;
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
    return isValidDistance(value);
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid distance.";
  }
}

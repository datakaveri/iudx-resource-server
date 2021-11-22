package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.response.ResponseUrn.INVALID_GEO_PARAM;
import static iudx.resource.server.apiserver.response.ResponseUrn.INVALID_GEO_VALUE;
import static iudx.resource.server.apiserver.util.Constants.VALIDATION_ALLOWED_DIST;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.util.HttpStatusCode;

public final class DistanceTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(DistanceTypeValidator.class);
  
  private final String value;
  private final boolean required;

  public DistanceTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }


  private boolean isValidDistance(final String distance) {
    try {
      Double distanceValue = Double.parseDouble(distance);
      if (distanceValue > Integer.MAX_VALUE) {
        LOGGER.error("Validation error : Invalid integer value (Integer overflow).");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
      }
      if (distanceValue > VALIDATION_ALLOWED_DIST || distanceValue < 1) {
        LOGGER.error("Validation error : Distance outside (1,1000)m range not allowed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
      }
    } catch (NumberFormatException ex) {
      LOGGER.error("Validation error : Number format error ( not a valid distance)");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
    }
    return true;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_PARAM, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
      }
    }
    return isValidDistance(value);
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_GEO_VALUE.getMessage();
  }
}

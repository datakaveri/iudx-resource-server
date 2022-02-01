package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.VALIDATION_ALLOWED_DIST;
import static iudx.resource.server.common.ResponseUrn.INVALID_GEO_PARAM_URN;
import static iudx.resource.server.common.ResponseUrn.INVALID_GEO_VALUE_URN;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;

public final class DistanceTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(DistanceTypeValidator.class);
  
  private final String value;
  private final boolean required;
  private final boolean noMaxDistanceLimit;

  public DistanceTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
    noMaxDistanceLimit = false;
  }

  public DistanceTypeValidator(final String value, final boolean required, final boolean noMaxLimit) {
    this.value = value;
    this.required = required;
    this.noMaxDistanceLimit = noMaxLimit;
  }


  private boolean isValidDistance(final String distance) {
    try {
      Double distanceValue = Double.parseDouble(distance);
      if (distanceValue > Integer.MAX_VALUE) {
        LOGGER.error("Validation error : Invalid integer value (Integer overflow).");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
      }
      if(noMaxDistanceLimit)
        return true;
      if ( !noMaxDistanceLimit && (distanceValue > VALIDATION_ALLOWED_DIST || distanceValue < 1)) {
        LOGGER.error("Validation error : Distance outside (1,1000)m range not allowed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
      }
    } catch (NumberFormatException ex) {
      LOGGER.error("Validation error : Number format error ( not a valid distance)");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_PARAM_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
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
    return INVALID_GEO_VALUE_URN.getMessage();
  }
}

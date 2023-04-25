package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;

public final class GeometryTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeometryTypeValidator.class);

  private final String value;
  private final boolean required;

  public GeometryTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
      }
    }
    if (!VALIDATION_ALLOWED_GEOM.contains(value)) {
      LOGGER.error("Validation error : Value " + value + " " + "is not allowed");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE_URN, failureMessage(value));
    }
    return true;
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

package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;

public final class GeoPropertyTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoPropertyTypeValidator.class);

  private final String value;
  private final boolean required;

  public GeoPropertyTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
      }
    }
    if (!VALIDATION_ALLOWED_GEOPROPERTY.contains(value)) {
      LOGGER.error("Validation error : Only location is allowed for geoproperty");
      throw new DxRuntimeException(failureCode(), INVALID_GEO_VALUE, failureMessage(value));
    }
    return true;
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

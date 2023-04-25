package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.common.ResponseUrn.*;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;

public final class GeoRelTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<String> allowedValues = List.of("within", "intersects", "near");

  private final String value;
  private final boolean required;

  public GeoRelTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      throw new DxRuntimeException(failureCode(), INVALID_GEO_REL_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_GEO_REL_URN, failureMessage(value));
      }
    }
    String[] geoRelationValues = value.split(";");
    if (!allowedValues.contains(geoRelationValues[0])) {
      throw new DxRuntimeException(failureCode(), INVALID_GEO_REL_URN, failureMessage(value));
    }
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_GEO_REL_URN.getMessage();
  }
}

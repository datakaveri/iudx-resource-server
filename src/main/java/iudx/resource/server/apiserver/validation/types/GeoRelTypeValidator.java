package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.response.ResponseUrn.*;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;


public class GeoRelTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<String> allowedValues = List.of("within", "intersects", "near");

  private String value;
  private boolean required;

  public GeoRelTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  @Override
  public boolean isValid() {
    if (required && (value == null || value.isBlank())) {  
      throw new DxRuntimeException(failureCode(), INVALID_GEO_REL, failureMessage());
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    String[] geoRelationValues = value.split(";");
    if (!allowedValues.contains(geoRelationValues[0])) {
      throw new DxRuntimeException(failureCode(), INVALID_GEO_REL, failureMessage(value));
    }
    return true;
  }


  @Override
  public int failureCode() {
    return 400;
  }


  @Override
  public String failureMessage() {
    return INVALID_GEO_REL.getMessage();
  }

  private String failureMessage(String value) {
    return failureMessage() + "[" + value + "]";
  }
}

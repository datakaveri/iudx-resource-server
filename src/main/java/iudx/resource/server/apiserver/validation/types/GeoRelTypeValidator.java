package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class GeoRelTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<String> allowedValues = List.of("within", "intersects", "near");

  public ParameterTypeValidator create() {
    ParameterTypeValidator geoRelTypeValidator = new GeoRelValidator();
    return geoRelTypeValidator;
  }


  class GeoRelValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if(value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("Empty value not allowed for parameter.");
      }
      String[] geoRelationValues = value.split(";");
      if (!allowedValues.contains(geoRelationValues[0])) {
        throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(
            "Value " + value + " " + "is not allowed");

      }
      return RequestParameter.create(value);
    }
  }
}

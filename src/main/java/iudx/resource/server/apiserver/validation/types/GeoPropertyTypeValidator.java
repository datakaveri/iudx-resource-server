package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator.GeoRelValidator;

public class GeoPropertyTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(GeoPropertyValidator.class);

  private List<Object> allowedValues = List.of("location", "Location");

  public ParameterTypeValidator create() {
    ParameterTypeValidator geoRelTypeValidator = new GeoPropertyValidator();
    return geoRelTypeValidator;
  }


  class GeoPropertyValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (!allowedValues.contains(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Only location is allowed for geoproperty");

      }
      return RequestParameter.create(value);
    }
  }
}

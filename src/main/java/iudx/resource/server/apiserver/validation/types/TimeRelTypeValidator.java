package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class TimeRelTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<Object> allowedValues = List.of("after", "before", "during","between");

  public ParameterTypeValidator create() {
    ParameterTypeValidator timeRelValidator=new TimeRelValidator();
    return timeRelValidator;
  }
  
  
  class TimeRelValidator implements ParameterTypeValidator {
    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if(value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("Empty value not allowed for parameter.");
      }
      if (!allowedValues.contains(value)) {
        throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(
            "Value " + value + " " + "is not allowed");

      }
      return RequestParameter.create(value);
    }
  }
}

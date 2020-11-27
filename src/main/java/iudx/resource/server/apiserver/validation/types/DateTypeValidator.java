package iudx.resource.server.apiserver.validation.types;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class DateTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(DateTypeValidator.class);

  public ParameterTypeValidator create() {
    ParameterTypeValidator dateTypeValidator = new DateValidator();
    return dateTypeValidator;
  }

  class DateValidator implements ParameterTypeValidator {


    private boolean isValidDate(String value) {
      try {
        //ZonedDateTime.parse(value); //TODO : validate date -time for UTC and IST
        return true;
      } catch (DateTimeParseException e) {
        return false;
      }
    }


    @Override
    public RequestParameter isValid(String value) throws ValidationException {

      if (!isValidDate(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Invalid Date format.");
      }
      return RequestParameter.create(value);
    }

  }

}

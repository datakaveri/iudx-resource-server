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
      String dateString = value.trim().replaceAll("\\s", "+");//since + is treated as space in uri params
      try {
        ZonedDateTime.parse(dateString);
        return true;
      } catch (DateTimeParseException e) {
        System.out.println(e);
        return false;
      }
    }


    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if(value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("Empty values are not allowed in parameter.");
      }
      if (!isValidDate(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Invalid Date format.");
      }
      return RequestParameter.create(value);
    }

  }

}

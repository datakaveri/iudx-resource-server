package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class IDTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(IDTypeValidator.class);

  private String pattern =
      "^[a-zA-Z0-9.]+/+[a-zA-Z0-9.]+/+[a-zA-Z.]+/+[a-zA-Z-_.]+/+[a-zA-Z0-9-_.]+$";
  private Integer minLength = VALIDATION_ID_MIN_LEN;
  private Integer maxLength = VALIDATION_ID_MAX_LEN;


  public ParameterTypeValidator create() {
    LOGGER.debug("creating ParameterTypeValidator for ID ");
    IDValidator idValidator = new IDValidator();
    return idValidator;
  }

  class IDValidator implements ParameterTypeValidator {


    public boolean isvalidIUDXId(String value) {
      return Pattern.compile(pattern).matcher(value).matches();
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty value not allowed.");
      }
      if (value.length() > maxLength) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Value exceed max character limit.");
      }
      if (!isvalidIUDXId(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Invalid id.");
      }

      return RequestParameter.create(value);
    }
  }

}

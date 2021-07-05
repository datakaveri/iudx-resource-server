package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class HeaderTokenValidation {
  
  private static final Logger LOGGER = LogManager.getLogger(HeaderTokenValidation.class);
  
  
  public ParameterTypeValidator create() {
    LOGGER.debug("creating ParameterTypeValidator for ID ");
    HeaderToken tokenValidator = new HeaderToken();
    return tokenValidator;
  }
  
  
  class HeaderToken implements ParameterTypeValidator{

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty value not allowed.");
      }
      if (value.length() > VALIDATION_TOKEN_MAX_LEN) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Value exceed max character limit.");
      }
      return RequestParameter.create(value);
    }
    
  }

}

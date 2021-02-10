package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class OptionsTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(OptionsTypeValidator.class);


  public ParameterTypeValidator create() {
    ParameterTypeValidator optionsTypeValidator = new OptionsPropertyValidator();
    return optionsTypeValidator;
  }
  
  
  class OptionsPropertyValidator implements ParameterTypeValidator{

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if(!value.equals("count")) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("count is only allowed value for options parameter");
      }
      return RequestParameter.create(value);
    }
    
  }
}

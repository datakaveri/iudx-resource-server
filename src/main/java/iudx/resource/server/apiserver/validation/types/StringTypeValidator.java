package iudx.resource.server.apiserver.validation.types;

import java.util.regex.Pattern;
import static iudx.resource.server.apiserver.response.ResponseUrn.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.util.HttpStatusCode;

public final class StringTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(StringTypeValidator.class);
  
  private Pattern regexPattern;
  private final String value;
  private final boolean required;
  
  public StringTypeValidator(String value, boolean required,Pattern regexPattern) {
    this.value = value;
    this.required = required;
    this.regexPattern=regexPattern;
  }


  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE, failureMessage());
      }
    }
    
    if(regexPattern!=null && !regexPattern.matcher(value).matches()) {
      LOGGER.error("Validation error :  doesn't passed regex [ "+regexPattern.pattern() +" ] test");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE, failureMessage(value));
    }
    
    return true;
  }

  @Override
  public int failureCode() {
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PARAM_VALUE.getMessage();
  }

}

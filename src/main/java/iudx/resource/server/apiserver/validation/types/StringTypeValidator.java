package iudx.resource.server.apiserver.validation.types;

import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringTypeValidator implements Validator{
  private static final Logger LOGGER = LogManager.getLogger(StringTypeValidator.class);
  
  private String value;
  private boolean required;
  private Pattern regexPattern;

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
      return false;
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        return false;
      }
    }
    
    if(regexPattern!=null && !regexPattern.matcher(value).matches()) {
      LOGGER.error("Validation error :  doesn't passed regex [ "+regexPattern.pattern() +" ] test");
      return false;
    }
    
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return "Invalid string";
  }

}

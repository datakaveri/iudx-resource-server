package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class AttrsTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(AttrsTypeValidator.class);

  private Integer maxAttrsItems = VALIDATION_MAX_ATTRS;
  private Integer maxAttrLength=VALIDATIONS_MAX_ATTR_LENGTH;

  public ParameterTypeValidator create() {
    AttributeValidator attributeValidator=new AttributeValidator();
    return attributeValidator;
  }
  
  class AttributeValidator implements ParameterTypeValidator {
    
    private boolean isValidAttributesFilterCount(String value) {
      String[] attrs=value.split(",");
      if(attrs.length>maxAttrsItems) {
        return false;
      }
      return true;
    }
    
    
    private boolean isValidAttributeLength(String value) {
      String[] attrs=value.split(",");
      for(String attr:attrs) {
        if(attr.length()>maxAttrLength) {
          return false;
        }
      }
      return true;
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if(value.trim().isBlank()) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("Empty value not allowed for parameter.");
      }
      if(!isValidAttributesFilterCount(value)) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("More than "+maxAttrsItems+" attributes are not allowed.");
      }
      if(!isValidAttributeLength(value)) {
        throw ValidationException.ValidationExceptionFactory
        .generateNotMatchValidationException("One of the attribute exceeds allowed characters(only 100 characters allowed).");
      }
      
      return RequestParameter.create(value);
    }
    
  }
}

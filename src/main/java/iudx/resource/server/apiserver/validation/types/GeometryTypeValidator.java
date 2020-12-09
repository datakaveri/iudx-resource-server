package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class GeometryTypeValidator {
private static final Logger LOGGER = LogManager.getLogger(GeometryTypeValidator.class);
  
  private List<Object> allowedValues=List.of("Point","point","Polygon","polygon","LineString","linestring","bbox");
  
  public ParameterTypeValidator create() {
    ParameterTypeValidator geometryTypeValidator=new GeomTypeValidator();
    return geometryTypeValidator;
  }
  
  class GeomTypeValidator implements ParameterTypeValidator {

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
     if(value.isBlank()) {
       throw ValidationException.ValidationExceptionFactory
       .generateNotMatchValidationException("Empty value not allowed for parameter.");
     }
     if(!allowedValues.contains(value)) {
       throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(
           "Value " + value + " " + "in not inside enum list " + allowedValues.toString());
     }
      return RequestParameter.create(value);
    }
    
  }

}

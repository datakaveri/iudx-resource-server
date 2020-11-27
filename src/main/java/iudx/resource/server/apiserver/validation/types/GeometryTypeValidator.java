package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class GeometryTypeValidator {
private static final Logger LOGGER = LogManager.getLogger(GeometryTypeValidator.class);
  
  private List<Object> allowedValues=List.of("Point","point","Polygon","polygon","LineString","linestring","bbox");
  
  public ParameterTypeValidator create() {
    ParameterTypeValidator innerValidator=ParameterTypeValidator.createStringTypeValidator(".*", "");
    ParameterTypeValidator geometryTypeValidator=ParameterTypeValidator.createEnumTypeValidatorWithInnerValidator(allowedValues, innerValidator);
    return geometryTypeValidator;
  }

}

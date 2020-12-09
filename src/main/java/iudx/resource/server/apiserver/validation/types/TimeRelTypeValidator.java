package iudx.resource.server.apiserver.validation.types;

import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class TimeRelTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(GeoRelTypeValidator.class);

  private List<Object> allowedValues = List.of("after", "before", "during","between");

  public ParameterTypeValidator create() {
    ParameterTypeValidator innerValidator=ParameterTypeValidator.createStringTypeValidator(".*", "");
    ParameterTypeValidator timeRelValidator=ParameterTypeValidator.createEnumTypeValidatorWithInnerValidator(allowedValues, innerValidator);
    return timeRelValidator;
  }
}

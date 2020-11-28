package iudx.resource.server.apiserver.validation.types;

import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class DistanceTypeValidator {

  public ParameterTypeValidator create() {
    ParameterTypeValidator distanceTypeValidator = ParameterTypeValidator.createIntegerTypeValidator(10000.0, 0.0, null, 0);
    return distanceTypeValidator;
  }
}

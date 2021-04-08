package iudx.resource.server.apiserver.validation.types;

import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

public class DistanceTypeValidator {

  private double allowedDistance = 1000.0;

  public ParameterTypeValidator create() {
    ParameterTypeValidator distanceTypeValidator = new DistanceValidator();
    return distanceTypeValidator;
  }

  class DistanceValidator implements ParameterTypeValidator {

    private boolean isValidDistance(String distance) {
      try {
        Double distanceValue = Double.parseDouble(distance);
        if (distanceValue > Integer.MAX_VALUE) {
          throw ValidationException.ValidationExceptionFactory
              .generateNotMatchValidationException("Invalid integer value (Integer overflow).");
        }
        if (distanceValue > 1000.0) {
          return false;
        }
      } catch (NumberFormatException ex) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Number format error ( not a valid distance)");
      }
      return true;
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty values are not allowed in parameter.");
      }
      if (!isValidDistance(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException(
                "Distance greater than " + allowedDistance + " not allowed");
      }
      return RequestParameter.create(value);
    }

  }
}

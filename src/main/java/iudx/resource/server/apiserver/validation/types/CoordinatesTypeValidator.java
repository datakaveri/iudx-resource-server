package iudx.resource.server.apiserver.validation.types;

import java.text.DecimalFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;

// TODO : A better way to validate coordinates,
// current method works but not very efficient,
// it assumes in a , separated array odd index value will be a longitude and,
// even index value will be a latitude
public class CoordinatesTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(CoordinatesTypeValidator.class);

  private static final String LATITUDE_PATTERN =
      "^(\\+|-)?(?:90(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-8][0-9])(?:(?:\\.[0-9]{1,6})?))$";
  private static final String LONGITUDE_PATTERN =
      "^(\\+|-)?(?:180(?:(?:\\.0{1,6})?)|(?:[0-9]|[1-9][0-9]|1[0-7][0-9])(?:(?:\\.[0-9]{1,6})?))$";



  public ParameterTypeValidator create() {
    ParameterTypeValidator coordinatesValidator = new CoordinatesValidator();
    return coordinatesValidator;
  }

  class CoordinatesValidator implements ParameterTypeValidator {
    private DecimalFormat df = new DecimalFormat("#.######");

    private boolean isValidLatitude(String latitude) {
      Float latitudeValue = Float.parseFloat(latitude);
      if (!df.format(latitudeValue).matches(LATITUDE_PATTERN)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("invalid latitude value " + latitude);
      }
      return true;
    }

    private boolean isValidLongitude(String longitude) {
      Float longitudeValue = Float.parseFloat(longitude);
      if (!df.format(longitudeValue).matches(LONGITUDE_PATTERN)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("invalid longitude value " + longitude);
      }
      return true;
    }

    private boolean isValidCoordinates(String value) {
      String coordinates = value.replaceAll("\\[", "").replaceAll("\\]", "");
      String[] coordinatesArray = coordinates.split(",");
      boolean checkLongitudeFlag = false;
      for (String coordinate : coordinatesArray) {
        // check for length of coordinate must not be greater than 10 ###.######
        if (coordinate.length() > 10) {
          throw ValidationException.ValidationExceptionFactory
              .generateNotMatchValidationException("invalid coordinate" + coordinate);
        }
        if (checkLongitudeFlag) {
          isValidLatitude(coordinate);
        } else {
          isValidLongitude(coordinate);
        }
        checkLongitudeFlag = !checkLongitudeFlag;
      }
      return true;
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (!isValidCoordinates(value)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("invalid coordinate" + value);
      }
      return RequestParameter.create(value);

    }

  }

  public static void main(String[] args) {
    String coordinates =
        "[[[72.719,90],[72.842,21.2],[72.923,20.8],[72.74,20.34],[72.9,20.1],[72.67,20],[72.719,21]]]";
    ParameterTypeValidator test = new CoordinatesTypeValidator().create();
    System.out.println(test.isValid(coordinates) + "");
  }

}

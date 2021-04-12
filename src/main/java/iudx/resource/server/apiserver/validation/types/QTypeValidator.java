package iudx.resource.server.apiserver.validation.types;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;
import iudx.resource.server.apiserver.util.Constants;
import static iudx.resource.server.apiserver.util.Constants.*;

public class QTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(QTypeValidator.class);

  List<String> allowedOperators = List.of(">", "==", "<", "!=", "<=", ">=");
  // TODO : put valid regex for IUDX id
  private final String idRegex = ".*";

  public ParameterTypeValidator create() {
    ParameterTypeValidator qTypeValidator = new QValidator();
    return qTypeValidator;
  }

  class QValidator implements ParameterTypeValidator {

    private boolean isValidAttribute(String value) {
      return true;
    }

    private boolean isValidOperator(String value) {
      return allowedOperators.contains(value);
    }

    private boolean isValidValue(String value) {
      try {
        Float.parseFloat(value);
        return true;
      } catch (NumberFormatException ex) {
        LOGGER.info("Passes value in q parameter in not float");
        return false;
      }
    }

    private boolean isValidID(JsonObject json) {
      if (json.containsKey("id")) {
        String id = json.getString(JSON_VALUE);
        Pattern pattern = Pattern.compile(idRegex);
        Matcher matcher = pattern.matcher(id);
        return matcher.matches();
      } else {
        return true;
      }
    }

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      if (value.isBlank()) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Empty value not allowed for parameter.");
      }
      if (value.length() > 512) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Exceeding max length(512 characters) criteria");
      }

      JsonObject qJson = getQueryTerms(value);
      if (!isValidAttribute(qJson.getString(JSON_ATTRIBUTE))) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid attribute in <<q>> query");
      }
      if (!isValidOperator(qJson.getString(JSON_OPERATOR))) {
        throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(
            "Not a valid Operator in <<q>> query");
      }

      if (!isValidID(qJson)) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid id query");
      }

      // NOTE : committed till filter work is not completed.
      // Now value is not restricted to only float
      /*
       * if (!isValidValue(qJson.getString(JSON_VALUE))) { throw
       * ValidationException.ValidationExceptionFactory
       * .generateNotMatchValidationException("Not a valid Float value in <<q>> query"); }
       */
      return RequestParameter.create(value);
    }
  }

  JsonObject getQueryTerms(String queryTerms) {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    List<String> allowedOperators = Arrays.asList(">", "=", "<", ">=", "<=", "==", "!=");
    int startIndex = 0;
    boolean specialCharFound = false;
    for (int i = 0; i < length; i++) {
      Character c = queryTerms.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
        if (allowedSpecialCharacter.contains(c)) {
          json.put(Constants.JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else {
          LOGGER.info("Ignore " + c.toString());
          throw ValidationException.ValidationExceptionFactory
              .generateNotMatchValidationException("Operator not allowed.");
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put(Constants.JSON_OPERATOR, queryTerms.substring(startIndex, i));
          json.put(Constants.JSON_VALUE, queryTerms.substring(i));
          break;
        }
      }

    }
    if (!allowedOperators.contains(json.getString(Constants.JSON_OPERATOR))) {
      throw ValidationException.ValidationExceptionFactory
          .generateNotMatchValidationException("Operator not allowed.");
    }
    return json;
  }

}

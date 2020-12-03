package iudx.resource.server.apiserver.validation.types;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.ValidationException;


import static iudx.resource.server.apiserver.util.Constants.*;

public class QTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(QTypeValidator.class);

  List<String> allowedOperators = List.of(">", "==", "<", "!=", "<=", ">=");

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

    @Override
    public RequestParameter isValid(String value) throws ValidationException {
      JsonObject qJson = getQueryTerms(value);
      if (!isValidAttribute(qJson.getString(JSON_ATTRIBUTE))) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid attribute in <<q>> query");
      }
      if (!isValidOperator(qJson.getString(JSON_OPERATOR))) {
        throw ValidationException.ValidationExceptionFactory.generateNotMatchValidationException(
            "Not a valid Operator in <<q>> query, only " + allowedOperators + "  allowed");
      }
      if (!isValidValue(qJson.getString(JSON_VALUE))) {
        throw ValidationException.ValidationExceptionFactory
            .generateNotMatchValidationException("Not a valid Float value in <<q>> query");
      }
      return RequestParameter.create(value);
    }
  }

  JsonObject getQueryTerms(String queryTerms) {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    int startIndex = 0;
    boolean specialCharFound = false;
    for (int i = 0; i < length; i++) {
      Character c = queryTerms.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
        if (allowedSpecialCharacter.contains(c)) {
          json.put(JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else {
          LOGGER.info("Ignore " + c.toString());
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put(JSON_OPERATOR, queryTerms.substring(startIndex, i));
          json.put(JSON_VALUE, queryTerms.substring(i));
          break;
        }
      }

    }
    return json;
  }
}

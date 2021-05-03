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
  private static final Pattern regexIDPattern =
      Pattern.compile(
          "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");
  private static final String qAttributeRegex = "^[a-zA-Z0-9_]{1,100}+$";

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
        Matcher matcher = regexIDPattern.matcher(id);
        return matcher.matches();
      } else {
        return true;
      }
    }

    private boolean isValidAttributeValue(String value) {
      return qAttributeRegex.matches(value);
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

//      if (!isValidAttributeValue(qJson.getString(JSON_VALUE))) {
//        throw ValidationException.ValidationExceptionFactory
//            .generateNotMatchValidationException("Not a valid attribute value in <<q>> query");
//      }

      return RequestParameter.create(value);
    }
  }

  JsonObject getQueryTerms(String queryTerms) {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    List<Character> allowedSpecialCharAttribValue=Arrays.asList('_','-');
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
        }else if(allowedSpecialCharAttribValue.contains(c)) {
          //do nothing
        }else {
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

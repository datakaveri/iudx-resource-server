package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;

public class QTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(QTypeValidator.class);

  List<String> allowedOperators = List.of(">", "=", "<", ">=", "<=", "==", "!=");
  // TODO : put valid regex for IUDX id
  private static final Pattern regexIDPattern =
      Pattern.compile(
          "^[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z0-9.]{4,100}/{1}[a-zA-Z.]{4,100}/{1}[a-zA-Z-_.]{4,100}/{1}[a-zA-Z0-9-_.]{4,100}$");
  private static final String qAttributeRegex = "^[a-zA-Z0-9_]{1,100}+$";

  private String value;
  private boolean required;

  public QTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

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



  JsonObject getQueryTerms(String queryTerms) throws Exception {
    JsonObject json = new JsonObject();
    int length = queryTerms.length();
    List<Character> allowedSpecialCharacter = Arrays.asList('>', '=', '<', '!');
    List<Character> allowedSpecialCharAttribValue = Arrays.asList('_', '-');
    int startIndex = 0;
    boolean specialCharFound = false;
    for (int i = 0; i < length; i++) {
      Character c = queryTerms.charAt(i);
      if (!(Character.isLetter(c) || Character.isDigit(c)) && !specialCharFound) {
        if (allowedSpecialCharacter.contains(c)) {
          json.put(Constants.JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else if (allowedSpecialCharAttribValue.contains(c)) {
          // do nothing
        } else {
          LOGGER.info("Ignore " + c.toString());
          throw new Exception("Operator not allowed.");
        }
      } else {
        if (specialCharFound && (Character.isLetter(c) || Character.isDigit(c))) {
          json.put(Constants.JSON_OPERATOR, queryTerms.substring(startIndex, i));
          json.put(Constants.JSON_VALUE, queryTerms.substring(i));
          break;
        }
      }

    }
    return json;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + " required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        return false;
      }
    }
    if (value.length() > 512) {
      LOGGER.error("Validation error : Exceeding max length(512 characters) criteria");
      return false;
    }
    JsonObject qJson;
    try {
      qJson = getQueryTerms(value);
    } catch (Exception ex) {
      LOGGER.error("Validation error : Operator not allowed.");
      return false;
    }
    if (!isValidAttribute(qJson.getString(JSON_ATTRIBUTE))) {
      LOGGER.error("Validation error : Not a valid attribute in <<q>> query");
      return false;
    }
    if (!isValidOperator(qJson.getString(JSON_OPERATOR))) {
      LOGGER.error("Validation error : Not a valid Operator in <<q>> query");
      return false;
    }
    // if (!isValidAttributeValue(qJson.getString(JSON_VALUE))) {
    // throw ValidationException.ValidationExceptionFactory
    // .generateNotMatchValidationException("Not a valid attribute value in <<q>> query");
    // }

    if (!isValidID(qJson)) {
      return false;
    }
    return true;
  }

  @Override
  public int failureCode() {
    return 400;
  }

  @Override
  public String failureMessage() {
    return null;
  }

}

package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;

public final class QTypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(QTypeValidator.class);

  private final String value;
  private final boolean required;

  public QTypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidAttribute(final String value) {
    return true;
  }

  private boolean isValidOperator(final String value) {
    return VALIDATION_ALLOWED_OPERATORS.contains(value);
  }

  public boolean isValidValue(final String value) {
    try {
      Float.parseFloat(value);
      return true;
    } catch (NumberFormatException ex) {
      LOGGER.info("Passed value in q parameter is not float");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
  }

  private boolean isValidID(final JsonObject json) {
    if (json.containsKey("id")) {
      String id = json.getString(JSON_VALUE);
      Matcher matcher = VALIDATION_ID_PATTERN.matcher(id);
      return matcher.matches();
    } else {
      return true;
    }
  }

  public boolean isValidAttributeValue(final String value) {
    return VALIDATION_Q_ATTR_PATTERN.matches(value);
  }

  JsonObject getQueryTerms(final String queryTerms) throws Exception {
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
          json.put(JSON_ATTRIBUTE, queryTerms.substring(startIndex, i));
          startIndex = i;
          specialCharFound = true;
        } else if (allowedSpecialCharAttribValue.contains(c)) {
          // do nothing
        } else {
          LOGGER.error("Ignore " + c.toString());
          throw new DxRuntimeException(
              failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
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

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + " required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
    } else {
      if (value == null) {
        return true;
      }
      if (value.isBlank()) {
        LOGGER.error("Validation error :  blank value for passed");
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage());
      }
    }
    if (value.length() > 512) {
      LOGGER.error("Validation error : Exceeding max length(512 characters) criteria");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    JsonObject qJson;
    try {
      qJson = getQueryTerms(value);
    } catch (Exception ex) {
      LOGGER.error("Validation error : Operator not allowed.");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    if (!isValidAttribute(qJson.getString(JSON_ATTRIBUTE))) {
      LOGGER.error("Validation error : Not a valid attribute in <<q>> query");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    if (!isValidOperator(qJson.getString(JSON_OPERATOR))) {
      LOGGER.error("Validation error : Not a valid Operator in <<q>> query");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
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
    return HttpStatusCode.BAD_REQUEST.getValue();
  }

  @Override
  public String failureMessage() {
    return INVALID_PARAM_VALUE_URN.getMessage();
  }
}

package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import static iudx.resource.server.common.ResponseUrn.*;

import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.HttpStatusCode;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QtypeValidator implements Validator {

  private static final Logger LOGGER = LogManager.getLogger(QtypeValidator.class);

  private final String value;
  private final boolean required;

  public QtypeValidator(final String value, final boolean required) {
    this.value = value;
    this.required = required;
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

  private boolean isValidId(final JsonObject json) {
    if (json.containsKey("id")) {
      String id = json.getString(JSON_VALUE);
      Matcher matcher = VALIDATION_ID_PATTERN.matcher(id);
      return matcher.matches();
    } else {
      return true;
    }
  }

  public boolean isValidAttributeValue(final String value) {
    LOGGER.debug("value,{},{}", value, VALIDATION_Q_ATTR_PATTERN.matcher(value).matches());
    return VALIDATION_Q_ATTR_PATTERN.matcher(value).matches();
  }

  JsonObject getQueryTerms(final String queryTerms) {
    JsonObject json = new JsonObject();

    String[] attributes = queryTerms.split(";");
    LOGGER.debug(attributes);

    for (String attr : attributes) {

      String[] attributeQueryTerms =
          attr.split("((?=>)|(?<=>)|(?=<)|(?<=<)|(?==)|(?<==)|(?=!)|(?<=!))");
      LOGGER.debug(Arrays.stream(attributeQueryTerms).collect(Collectors.toList()));
      LOGGER.debug(attributeQueryTerms.length);
      if (attributeQueryTerms.length == 3) {
        json.put(JSON_OPERATOR, attributeQueryTerms[1]).put(JSON_VALUE, attributeQueryTerms[2]);
      } else if (attributeQueryTerms.length == 4) {
        json.put(JSON_OPERATOR, attributeQueryTerms[1].concat(attributeQueryTerms[2]))
            .put(JSON_VALUE, attributeQueryTerms[3]);
      } else {
        throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
      }
      json.put(JSON_ATTRIBUTE, attributeQueryTerms[0]);
    }

    LOGGER.debug(json);

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
    JsonObject qjson;
    try {
      qjson = getQueryTerms(value);
    } catch (Exception ex) {
      LOGGER.error("Validation error : Operator not allowed.");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    if (!isValidAttributeValue(qjson.getString(JSON_ATTRIBUTE))) {
      LOGGER.error("Validation error : Not a valid attribute in <<q>> query");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    if (!isValidOperator(qjson.getString(JSON_OPERATOR))) {
      LOGGER.error("Validation error : Not a valid Operator in <<q>> query");
      throw new DxRuntimeException(failureCode(), INVALID_PARAM_VALUE_URN, failureMessage(value));
    }
    // if (!isValidAttributeValue(qJson.getString(JSON_VALUE))) {
    // throw ValidationException.ValidationExceptionFactory
    // .generateNotMatchValidationException("Not a valid attribute value in <<q>> query");
    // }

    if (!isValidId(qjson)) {
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

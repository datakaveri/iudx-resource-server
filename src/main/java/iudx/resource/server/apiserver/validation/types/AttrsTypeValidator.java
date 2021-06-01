package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.VALIDATIONS_MAX_ATTR_LENGTH;
import static iudx.resource.server.apiserver.util.Constants.VALIDATION_MAX_ATTRS;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AttrsTypeValidator implements Validator {
  private static final Logger LOGGER = LogManager.getLogger(AttrsTypeValidator.class);

  private Integer maxAttrsItems = VALIDATION_MAX_ATTRS;
  private Integer maxAttrLength = VALIDATIONS_MAX_ATTR_LENGTH;
  private static final Pattern attrsValueRegex = Pattern.compile("^[a-zA-Z0-9_]+");


  private String value;
  private boolean required;

  public AttrsTypeValidator(String value, boolean required) {
    this.value = value;
    this.required = required;
  }

  private boolean isValidAttributesCount(String value) {
    String[] attrs = value.split(",");
    if (attrs.length > maxAttrsItems) {
      LOGGER.error("Validation error : Invalid numbers of attrs passed [ " + value + " ]");
      return false;
    }
    return true;
  }


  private boolean isValidAttributeValue(String value) {
    String[] attrs = value.split(",");
    for (String attr : attrs) {
      if (attr.length() > maxAttrLength) {
        LOGGER.error(
            "Validation error : One of the attribute exceeds allowed characters(only 100 characters allowed).");
        return false;
      }
      if (!attrsValueRegex.matcher(attr).matches()) {
        LOGGER.error("Validation error : Invalid attribute value. [ " + value + " ]");
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isValid() {
    LOGGER.debug("value : " + value + "required : " + required);
    if (required && (value == null || value.isBlank())) {
      LOGGER.error("Validation error : null or blank value for required mandatory field");
      return false;
    } else {
      if (value == null || value.isBlank()) {
        return true;
      }
    }
    if (!isValidAttributesCount(value)) {
      return false;
    }
    if (!isValidAttributeValue(value)) {
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
    return "Invalid attrs value";
  }
}

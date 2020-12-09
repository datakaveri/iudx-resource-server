package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class IDTypeValidator {

  private static final Logger LOGGER = LogManager.getLogger(IDTypeValidator.class);
  
  private  String pattern = VALIDATION_ID_PATTERN;
  private  Integer minLength = VALIDATION_ID_MIN_LEN;
  private  Integer maxLength = VALIDATION_ID_MAX_LEN;


  public ParameterTypeValidator create() {
    LOGGER.debug("creating ParameterTypeValidator for ID ");
    ParameterTypeValidator idTypeValidator =
        ParameterTypeValidator.createStringTypeValidator(pattern, minLength, maxLength, "");
    return ParameterTypeValidator.createArrayTypeValidator(idTypeValidator, "csv", 5, 1);
  }

}

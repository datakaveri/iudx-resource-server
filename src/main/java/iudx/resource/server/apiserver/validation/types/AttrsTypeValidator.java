package iudx.resource.server.apiserver.validation.types;

import static iudx.resource.server.apiserver.util.Constants.VALIDATION_MAX_ATTRS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;

public class AttrsTypeValidator {
  private static final Logger LOGGER = LogManager.getLogger(AttrsTypeValidator.class);

  private Integer maxAttrsItems = VALIDATION_MAX_ATTRS;

  public ParameterTypeValidator create() {
    LOGGER.debug("creating ParameterTypeValidator for attrs ");
    ParameterTypeValidator attrsTypeValidator =
        ParameterTypeValidator.createStringTypeValidator(".*", 1, 200, "");
    return ParameterTypeValidator.createArrayTypeValidator(attrsTypeValidator, "csv", maxAttrsItems,
        1);
  }
}

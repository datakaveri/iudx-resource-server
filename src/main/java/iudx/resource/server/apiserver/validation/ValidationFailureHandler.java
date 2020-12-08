package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;
import static iudx.resource.server.apiserver.util.Constants.JSON_DETAIL;
import static iudx.resource.server.apiserver.util.Constants.JSON_TITLE;
import static iudx.resource.server.apiserver.util.Constants.JSON_TYPE;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;

public class ValidationFailureHandler implements Handler<RoutingContext>{

  private static final Logger LOGGER = LogManager.getLogger(ValidationFailureHandler.class);
  @Override
  public void handle(RoutingContext context) {
    Throwable failure = context.failure();
    LOGGER.error("error :"+failure);
    if (failure instanceof ValidationException) { 
      // Something went wrong during validation!
      String failedParameter=((ValidationException) failure).parameterName();
      LOGGER.error("error :" +failure.getMessage()+" param : "+failedParameter);
      String validationErrorMessage = "<<"+failedParameter+">> "+failure.getMessage();
      context.response()
        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(HttpStatus.SC_BAD_REQUEST)
        .end(validationFailureReponse(validationErrorMessage).toString());
    }
    context.next();
    return;
  }
  
  private JsonObject validationFailureReponse(String message) {
    return new JsonObject()
        .put(JSON_TYPE, HttpStatus.SC_BAD_REQUEST)
        .put(JSON_TITLE, "Bad Request")
        .put(JSON_DETAIL, message);
  }

}

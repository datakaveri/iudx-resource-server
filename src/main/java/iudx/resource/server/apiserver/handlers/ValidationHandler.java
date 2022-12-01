package iudx.resource.server.apiserver.handlers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.apiserver.validation.ValidatorsHandlersFactory;
import iudx.resource.server.apiserver.validation.types.Validator;

import static iudx.resource.server.apiserver.util.Constants.HEADER_PUBLIC_KEY;

public class ValidationHandler implements Handler<RoutingContext> {

  private static final Logger LOGGER = LogManager.getLogger(ValidationHandler.class);

  private RequestType requestType;
  private Vertx vertx;

  public ValidationHandler(Vertx vertx, RequestType apiRequestType) {
    this.vertx = vertx;
    this.requestType = apiRequestType;
  }

  @Override
  public void handle(RoutingContext context) {
    LOGGER.debug("inside validation");
    ValidatorsHandlersFactory validationFactory = new ValidatorsHandlersFactory();
    MultiMap parameters = context.request().params();
    MultiMap headers = context.request().headers();
    RequestBody requestBody=context.body();
    JsonObject body=null;
    if(requestBody!=null) {
      if(requestBody.asJsonObject()!=null) {
        body=requestBody.asJsonObject().copy();
      }
    }
    Map<String, String> pathParams = context.pathParams();
    parameters.set(HEADER_PUBLIC_KEY,context.request().getHeader(HEADER_PUBLIC_KEY));
    parameters.addAll(pathParams);

    List<Validator> validations = validationFactory.build(vertx, requestType, parameters, headers, body);
    for (Validator validator : Optional.ofNullable(validations).orElse(Collections.emptyList())) {
      LOGGER.debug("validator :" + validator.getClass().getName());
      validator.isValid();
    }
    context.next();
    return;
  }

}

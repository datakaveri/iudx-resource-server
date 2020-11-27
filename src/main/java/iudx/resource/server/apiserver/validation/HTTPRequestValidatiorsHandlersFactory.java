package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import iudx.resource.server.apiserver.validation.types.AttrsTypeValidator;
import iudx.resource.server.apiserver.validation.types.DateTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoPropertyTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeometryTypeValidator;
import iudx.resource.server.apiserver.validation.types.IDTypeValidator;
import iudx.resource.server.apiserver.validation.types.QTypeValidator;
import iudx.resource.server.apiserver.validation.types.TimeRelTypeValidator;

public class HTTPRequestValidatiorsHandlersFactory {

  private static final Logger LOGGER =
      LogManager.getLogger(HTTPRequestValidatiorsHandlersFactory.class);

  public HTTPRequestValidationHandler getValidation4Context(String requestType) {
    LOGGER.debug("getValidation4Context() started for :" + requestType);
    if (requestType.equalsIgnoreCase("ENTITY")) {
      return getEntityRequestValidations();
    }
    if (requestType.equalsIgnoreCase("TEMPORAL")) {
      return getTemporalRequestValidations();
    }
    return null;
  }


  private HTTPRequestValidationHandler getEntityRequestValidations() {
    return HTTPRequestValidationHandler.create()
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ID, new IDTypeValidator().create(), true,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ATTRIBUTE,
            new AttrsTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOREL,
            new GeoRelTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOMETRY,
            new GeometryTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOPROPERTY,
            new GeoPropertyTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_Q, new QTypeValidator().create(), false,
            false);

  }

  private HTTPRequestValidationHandler getTemporalRequestValidations() {
    return HTTPRequestValidationHandler.create()
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ID, new IDTypeValidator().create(), true,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ATTRIBUTE,
            new AttrsTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_Q, new QTypeValidator().create(), false,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_TIMEREL,
            new TimeRelTypeValidator().create(), false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_TIME, new DateTypeValidator().create(), false,false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ENDTIME, new DateTypeValidator().create(), false,false);

  }

}

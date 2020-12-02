package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_COORDINATES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENDTIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOMETRY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ID;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_MAXDISTANCE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEREL;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import iudx.resource.server.apiserver.validation.types.AttrsTypeValidator;
import iudx.resource.server.apiserver.validation.types.CoordinatesTypeValidator;
import iudx.resource.server.apiserver.validation.types.DateTypeValidator;
import iudx.resource.server.apiserver.validation.types.DistanceTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoPropertyTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeometryTypeValidator;
import iudx.resource.server.apiserver.validation.types.IDTypeValidator;
import iudx.resource.server.apiserver.validation.types.OptionsTypeValidator;
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
    if (requestType.equalsIgnoreCase("LATEST")) {
      return getLatestRequestValidations();
    }
    if (requestType.equalsIgnoreCase("POST")) {
      return getPostRequestValidations();
    }
    return null;
  }

  private final ParameterTypeValidator idTypeValidator = new IDTypeValidator().create();
  private final ParameterTypeValidator attributeTypeValidator = new AttrsTypeValidator().create();
  private final ParameterTypeValidator georelTypeValidator = new GeoRelTypeValidator().create();
  private final ParameterTypeValidator geometryTypeValidator = new GeometryTypeValidator().create();
  private final ParameterTypeValidator geoPropertyValidator =
      new GeoPropertyTypeValidator().create();
  private final ParameterTypeValidator qTypeValidator = new QTypeValidator().create();
  private final ParameterTypeValidator distanceTypeValidator = new DistanceTypeValidator().create();
  private final ParameterTypeValidator optionsTypeValidator = new OptionsTypeValidator().create();
  private final ParameterTypeValidator coordinatesTypeValidator =
      new CoordinatesTypeValidator().create();
  private final ParameterTypeValidator timeRelTypeValidator = new TimeRelTypeValidator().create();
  private final ParameterTypeValidator dateTypeValidator = new DateTypeValidator().create();



  private HTTPRequestValidationHandler getEntityRequestValidations() {
    final HTTPRequestValidationHandler validator = HTTPRequestValidationHandler.create()
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ID, idTypeValidator, true, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ATTRIBUTE, attributeTypeValidator, false,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOREL, georelTypeValidator, false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOMETRY, geometryTypeValidator, false,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOPROPERTY, geoPropertyValidator, false,
            false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_Q, qTypeValidator, false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_MAXDISTANCE, distanceTypeValidator, false,
            false)
        .addQueryParamWithCustomTypeValidator("maxDistance", distanceTypeValidator, false,
            false)
        .addQueryParamWithCustomTypeValidator("options", optionsTypeValidator, false, false)
        .addQueryParamWithCustomTypeValidator(NGSILDQUERY_COORDINATES, coordinatesTypeValidator,
            false, false);
    return validator;

  }

  private HTTPRequestValidationHandler getTemporalRequestValidations() {
    final HTTPRequestValidationHandler validator =
        HTTPRequestValidationHandler.create()
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ID, idTypeValidator, true, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ATTRIBUTE, attributeTypeValidator,
                false, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_Q, qTypeValidator, false, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_TIMEREL, timeRelTypeValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_TIME, dateTypeValidator, false, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_ENDTIME, dateTypeValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator("options", optionsTypeValidator, false, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOREL, georelTypeValidator, false, false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOMETRY, geometryTypeValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_GEOPROPERTY, geoPropertyValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_MAXDISTANCE, distanceTypeValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator("maxDistance", distanceTypeValidator, false,
                false)
            .addQueryParamWithCustomTypeValidator(NGSILDQUERY_COORDINATES, coordinatesTypeValidator,
                false, false);
    return validator;
  }


  private HTTPRequestValidationHandler getLatestRequestValidations() {
    final ParameterTypeValidator pathParamsTypeValidator =
        ParameterTypeValidator.createStringTypeValidator(".*", 1, 100, "");

    final HTTPRequestValidationHandler validator = HTTPRequestValidationHandler.create()
        .addPathParamWithCustomTypeValidator("domain", pathParamsTypeValidator, false)
        .addPathParamWithCustomTypeValidator("userSha", pathParamsTypeValidator, false)
        .addPathParamWithCustomTypeValidator("resourceServer", pathParamsTypeValidator, false)
        .addPathParamWithCustomTypeValidator("resourceGroup", pathParamsTypeValidator, false)
        .addPathParamWithCustomTypeValidator("resourceName", pathParamsTypeValidator, false);

    return validator;
  }

  private HTTPRequestValidationHandler getPostRequestValidations() {
    HTTPRequestValidationHandler validator = null;
    String jsonSchema = null;

    try {
      jsonSchema = loadJson();
    } catch (Exception ex) {
      LOGGER.error(ex);
      return validator;
    }
    validator = HTTPRequestValidationHandler.create().addJsonBodySchema(jsonSchema);
    return validator;
  }


  private String loadJson() {
    String jsonStr = null;
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("post_request_schema.json")) {
      jsonStr = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
    } catch (IOException e) {
      LOGGER.error(e);
      return jsonStr;
    }
    return jsonStr;
  }

}

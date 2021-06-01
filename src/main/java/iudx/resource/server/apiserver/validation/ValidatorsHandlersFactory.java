package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import iudx.resource.server.apiserver.query.GeoRelation;
import iudx.resource.server.apiserver.util.RequestType;
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
import iudx.resource.server.apiserver.validation.types.StringTypeValidator;
import iudx.resource.server.apiserver.validation.types.TimeRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.Validator;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER =
      LogManager.getLogger(ValidatorsHandlersFactory.class);

  public List<Validator> build(final RequestType requestType, final MultiMap parameters,
      final MultiMap headers) {
    LOGGER.debug("getValidation4Context() started for :" + requestType);
    LOGGER.debug("type :" + requestType);
    List<Validator> validator = null;

    switch (requestType) {
      case ENTITY:
        validator = getEntityRequestValidations(parameters, headers);
        break;
      case TEMPORAL:
        validator = getTemporalRequestValidations(parameters, headers);
        break;
      case LATEST:
        validator = getLatestRequestValidations(parameters, headers);
        break;
      case POST:
        break;
      default:
        break;
    }

    return validator;
  }

  // private final ParameterTypeValidator idTypeValidator = new IDTypeValidator().create();
  // //private final ParameterTypeValidator attributeTypeValidator = new
  // AttrsTypeValidator().create();
  // private final ParameterTypeValidator georelTypeValidator = new GeoRelTypeValidator().create();
  // private final ParameterTypeValidator geometryTypeValidator = new
  // GeometryTypeValidator().create();
  // private final ParameterTypeValidator geoPropertyValidator =
  // new GeoPropertyTypeValidator().create();
  // private final ParameterTypeValidator qTypeValidator = new QTypeValidator().create();
  // private final ParameterTypeValidator distanceTypeValidator = new
  // DistanceTypeValidator().create();
  // private final ParameterTypeValidator optionsTypeValidator = new
  // OptionsTypeValidator().create();
  // //private final ParameterTypeValidator coordinatesTypeValidator =
  // // new CoordinatesTypeValidator().create();
  // private final ParameterTypeValidator timeRelTypeValidator = new
  // TimeRelTypeValidator().create();
  // private final ParameterTypeValidator dateTypeValidator = new DateTypeValidator().create();



  private List<Validator> getEntityRequestValidations(final MultiMap parameters,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSILDQUERY_Q), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false));
    validators.add(new OptionsTypeValidator(parameters.get("options"), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));

    return validators;

  }

  private List<Validator> getTemporalRequestValidations(final MultiMap parameters,
      final MultiMap headers) {

    List<Validator> validators = new ArrayList<>();

    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSILDQUERY_Q), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false));
    validators.add(new OptionsTypeValidator(parameters.get("options"), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_TIME), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_ENDTIME), false));

    return validators;
  }


  private List<Validator> getLatestRequestValidations(final MultiMap parameters,
      final MultiMap headers) {


    List<Validator> validators = new ArrayList<>();
    validators.add(new StringTypeValidator(parameters.get("domain"), true));
    validators.add(new StringTypeValidator(parameters.get("userSha"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceServer"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceGroup"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceName"), true));

    return validators;
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

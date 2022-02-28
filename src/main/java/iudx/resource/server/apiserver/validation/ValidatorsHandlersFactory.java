package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.DOMAIN;
import static iudx.resource.server.apiserver.util.Constants.HEADER_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.ID_DOMAIN_REGEX;
import static iudx.resource.server.apiserver.util.Constants.ID_RG_REGEX;
import static iudx.resource.server.apiserver.util.Constants.ID_RN_REGEX;
import static iudx.resource.server.apiserver.util.Constants.ID_RS_REGEX;
import static iudx.resource.server.apiserver.util.Constants.ID_USERSHA_REGEX;
import static iudx.resource.server.apiserver.util.Constants.IUDXQUERY_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_COORDINATES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENDTIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_FROM;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOMETRY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ID;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_MAXDISTANCE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_SIZE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEREL;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_GROUP;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_NAME;
import static iudx.resource.server.apiserver.util.Constants.RESOURCE_SERVER;
import static iudx.resource.server.apiserver.util.Constants.USERSHA;
import static iudx.resource.server.common.ResponseUrn.SCHEMA_READ_ERROR_URN;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Schema;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.apiserver.validation.types.AttrsTypeValidator;
import iudx.resource.server.apiserver.validation.types.CoordinatesTypeValidator;
import iudx.resource.server.apiserver.validation.types.DateTypeValidator;
import iudx.resource.server.apiserver.validation.types.DistanceTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoPropertyTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeoRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.GeometryTypeValidator;
import iudx.resource.server.apiserver.validation.types.IDTypeValidator;
import iudx.resource.server.apiserver.validation.types.JsonSchemaTypeValidator;
import iudx.resource.server.apiserver.validation.types.OptionsHeaderValidator;
import iudx.resource.server.apiserver.validation.types.OptionsTypeValidator;
import iudx.resource.server.apiserver.validation.types.PaginationLimitTypeValidator;
import iudx.resource.server.apiserver.validation.types.PaginationOffsetTypeValidator;
import iudx.resource.server.apiserver.validation.types.QTypeValidator;
import iudx.resource.server.apiserver.validation.types.StringTypeValidator;
import iudx.resource.server.apiserver.validation.types.TimeRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.Validator;
import iudx.resource.server.common.HttpStatusCode;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER =
      LogManager.getLogger(ValidatorsHandlersFactory.class);

  private static final Pattern UUID_PATTERN = Pattern
      .compile(".*[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

  public List<Validator> build(final Vertx vertx, final RequestType requestType,
      final MultiMap parameters,
      final MultiMap headers, final JsonObject body) {
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
      case POST_TEMPORAL:
        validator = getPostEntitiesValidations(vertx, parameters, headers, body, requestType);
        break;
      case POST_ENTITIES:
        validator = getPostTemporalValidations(vertx, parameters, headers, body, requestType);
        break;
      case SUBSCRIPTION:
        validator = getSubscriptionsValidations(vertx, body, headers);
        break;
      case ASYNC_SEARCH:
        validator = getAsyncRequestValidations(parameters, headers);
        break;
      case ASYNC_STATUS:
        validator = getAsyncStatusRequestValidator(parameters, headers);
        break;
      default:
        break;
    }

    return validator;
  }


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
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));

    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));

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
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_TIME), true));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_ENDTIME), false));

    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));

    return validators;
  }


  private List<Validator> getLatestRequestValidations(final MultiMap parameters,
      final MultiMap headers) {

    List<Validator> validators = new ArrayList<>();
    validators.add(new StringTypeValidator(parameters.get(DOMAIN), true, ID_DOMAIN_REGEX));
    validators.add(new StringTypeValidator(parameters.get(USERSHA), true, ID_USERSHA_REGEX));
    validators.add(new StringTypeValidator(parameters.get(RESOURCE_SERVER), true, ID_RS_REGEX));
    validators.add(new StringTypeValidator(parameters.get(RESOURCE_GROUP), true, ID_RG_REGEX));
    validators.add(new StringTypeValidator(parameters.get(RESOURCE_NAME), true, ID_RN_REGEX));

    return validators;
  }

  private List<Validator> getPostEntitiesValidations(Vertx vertx, final MultiMap parameters,
      final MultiMap headers, final JsonObject body, final RequestType requestType) {

    List<Validator> validators = new ArrayList<>();
    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));
    // request body validators.
    validators.addAll(getRequestSchemaValidator(vertx, body, requestType));

    return validators;
  }


  private List<Validator> getPostTemporalValidations(Vertx vertx, final MultiMap parameters,
      final MultiMap headers, final JsonObject body, final RequestType requestType) {

    List<Validator> validators = new ArrayList<>();
    // pagination optional fields
    validators.add(new PaginationLimitTypeValidator(parameters.get(NGSILDQUERY_SIZE), false));
    validators.add(new PaginationOffsetTypeValidator(parameters.get(NGSILDQUERY_FROM), false));
    // request body validators.
    validators.addAll(getRequestSchemaValidator(vertx, body, requestType));

    return validators;
  }


  private List<Validator> getSubscriptionsValidations(final Vertx vertx, final JsonObject body,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new OptionsHeaderValidator(headers.get(HEADER_OPTIONS), true));
    validators.addAll(getRequestSchemaValidator(vertx, body, RequestType.SUBSCRIPTION));
    return validators;
  }

  private List<Validator> getAsyncRequestValidations(
      final MultiMap parameters, final MultiMap headers) {

    List<Validator> validators = new ArrayList<>();
    validators.add(new IDTypeValidator(parameters.get(NGSILDQUERY_ID), true));
    validators.add(new AttrsTypeValidator(parameters.get(NGSILDQUERY_ATTRIBUTE), false));
    validators.add(new OptionsTypeValidator(parameters.get(IUDXQUERY_OPTIONS), false));
    // geo fields
    validators.add(new GeoRelTypeValidator(parameters.get(NGSILDQUERY_GEOREL), false));
    validators.add(new GeometryTypeValidator(parameters.get(NGSILDQUERY_GEOMETRY), false));
    validators.add(new GeoPropertyTypeValidator(parameters.get(NGSILDQUERY_GEOPROPERTY), false));
    validators.add(new QTypeValidator(parameters.get(NGSILDQUERY_Q), false));
    validators.add(new DistanceTypeValidator(parameters.get(NGSILDQUERY_MAXDISTANCE), false, true));
    validators.add(new DistanceTypeValidator(parameters.get("maxDistance"), false, true));
    validators.add(new CoordinatesTypeValidator(parameters.get(NGSILDQUERY_COORDINATES), false));
    // temporal fields
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), false));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_TIME), false));
    validators.add(new DateTypeValidator(parameters.get(NGSILDQUERY_ENDTIME), false));

    return validators;
  }

  private List<Validator> getAsyncStatusRequestValidator(final MultiMap parameters,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new StringTypeValidator(parameters.get("searchId"), true, UUID_PATTERN));
    return validators;
  }

  private static Map<String, String> jsonSchemaMap = new HashMap<>();

  private List<Validator> getRequestSchemaValidator(Vertx vertx, JsonObject body,
      RequestType requestType) {
    List<Validator> validators = new ArrayList<>();
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
    String jsonSchema = null;

    try {
      jsonSchema = loadJson(requestType.getFilename());
      Schema schema = schemaParser.parse(new JsonObject(jsonSchema));
      validators.add(new JsonSchemaTypeValidator(body, schema));
    } catch (Exception ex) {
      LOGGER.error(ex);
      throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(), SCHEMA_READ_ERROR_URN);
    }

    return validators;
  }

  private String loadJson(String filename) {
    String jsonStr = null;
    if (jsonSchemaMap.containsKey(filename)) {
      jsonStr = jsonSchemaMap.get(filename);
    } else {
      try (InputStream inputStream =
          getClass().getClassLoader().getResourceAsStream(filename)) {
        jsonStr = CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        jsonSchemaMap.put(filename, jsonStr);
      } catch (IOException e) {
        LOGGER.error(e);
        throw new DxRuntimeException(HttpStatusCode.BAD_REQUEST.getValue(), SCHEMA_READ_ERROR_URN);
      }
    }
    return jsonStr;
  }
}

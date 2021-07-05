package iudx.resource.server.apiserver.validation;

import static iudx.resource.server.apiserver.util.Constants.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import iudx.resource.server.apiserver.validation.types.PaginationOffsetTypeValidator;
import iudx.resource.server.apiserver.validation.types.PaginationLimitTypeValidator;
import iudx.resource.server.apiserver.validation.types.QTypeValidator;
import iudx.resource.server.apiserver.validation.types.StringTypeValidator;
import iudx.resource.server.apiserver.validation.types.TimeRelTypeValidator;
import iudx.resource.server.apiserver.validation.types.Validator;

public class ValidatorsHandlersFactory {

  private static final Logger LOGGER =
      LogManager.getLogger(ValidatorsHandlersFactory.class);

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
        validator = getPostRequestValidator(vertx, body,requestType);
        break;
      case POST_ENTITIES:
        validator = getPostRequestValidator(vertx, body,requestType);
        break;
      case SUBSCRIPTION:
        validator = getSubscriptionsValidations(vertx, body, headers);
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
    validators.add(new TimeRelTypeValidator(parameters.get(NGSILDQUERY_TIMEREL), true, false));
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
    validators.add(new StringTypeValidator(parameters.get("domain"), true));
    validators.add(new StringTypeValidator(parameters.get("userSha"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceServer"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceGroup"), true));
    validators.add(new StringTypeValidator(parameters.get("resourceName"), true));

    return validators;
  }
  

  private List<Validator> getSubscriptionsValidations(final Vertx vertx, final JsonObject body,
      final MultiMap headers) {
    List<Validator> validators = new ArrayList<>();
    validators.add(new OptionsHeaderValidator(headers.get(HEADER_OPTIONS), false));
    return validators;
  }


  private static Map<String, String> jsonSchemaMap = new HashMap<>();

  private List<Validator> getTemporalPostRequestValidations(Vertx vertx, JsonObject body) {
    List<Validator> validators = new ArrayList<>();
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
    String jsonSchema = null;

    try {
      jsonSchema = loadJson(RequestType.POST_TEMPORAL.getFilename());
      Schema schema = schemaParser.parse(new JsonObject(jsonSchema));
      validators.add(new JsonSchemaTypeValidator(body, true, schema));
    } catch (Exception ex) {
      LOGGER.error(ex);
    }
    return validators;
  }


  private List<Validator> getPostRequestValidator(Vertx vertx, JsonObject body,RequestType requestType) {
    List<Validator> validators = new ArrayList<>();
    SchemaRouter schemaRouter = SchemaRouter.create(vertx, new SchemaRouterOptions());
    SchemaParser schemaParser = SchemaParser.createOpenAPI3SchemaParser(schemaRouter);
    String jsonSchema = null;

    try {
      jsonSchema = loadJson(requestType.getFilename());
      Schema schema = schemaParser.parse(new JsonObject(jsonSchema));
      validators.add(new JsonSchemaTypeValidator(body, true, schema));
    } catch (Exception ex) {
      LOGGER.error(ex);
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
        return jsonStr;
      }
    }
    return jsonStr;
  }

}

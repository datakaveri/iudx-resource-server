package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.util.Constants.HEADER_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.HEADER_TOKEN;
import static iudx.resource.server.apiserver.util.Constants.IUDXQUERY_OPTIONS;
import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_PARAM;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ATTRIBUTE;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_COORDINATES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENDTIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ENTITIES;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOMETRY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOQ;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_GEOREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_ID;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_IDPATTERN;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_Q;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TEMPORALQ;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEPROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIMEREL;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TIME_PROPERTY;
import static iudx.resource.server.apiserver.util.Constants.NGSILDQUERY_TYPE;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Coordinates;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.service.CatalogueService;

/**
 * This class is used to validate NGSI-LD request and request parameters.
 *
 */
public class Validator {

  private static final Logger LOGGER = LogManager.getLogger(Validator.class);

  private static Set<String> validParams = new HashSet<String>();
  private static Set<String> validHeaders = new HashSet<String>();
  private CatalogueService catalogueService;

  public Validator(CatalogueService catalogueService) {
    this.catalogueService = catalogueService;
  }

  static {
    validParams.add(NGSILDQUERY_TYPE);
    validParams.add(NGSILDQUERY_ID);
    validParams.add(NGSILDQUERY_IDPATTERN);
    validParams.add(NGSILDQUERY_ATTRIBUTE);
    validParams.add(NGSILDQUERY_Q);
    validParams.add(NGSILDQUERY_GEOREL);
    validParams.add(NGSILDQUERY_GEOMETRY);
    validParams.add(NGSILDQUERY_COORDINATES);
    validParams.add(NGSILDQUERY_GEOPROPERTY);
    validParams.add(NGSILDQUERY_TIMEPROPERTY);
    validParams.add(NGSILDQUERY_TIME);
    validParams.add(NGSILDQUERY_TIMEREL);
    validParams.add(NGSILDQUERY_TIME);
    validParams.add(NGSILDQUERY_ENDTIME);
    validParams.add(NGSILDQUERY_ENTITIES);
    validParams.add(NGSILDQUERY_GEOQ);
    validParams.add(NGSILDQUERY_TEMPORALQ);
    // Need to check with the timeProperty in Post Query property for NGSI-LD release v1.3.1
    validParams.add(NGSILDQUERY_TIME_PROPERTY);

    // for IUDX count query
    validParams.add(IUDXQUERY_OPTIONS);
  }

  static {
    validHeaders.add(HEADER_OPTIONS);
    validHeaders.add(HEADER_TOKEN);
    validHeaders.add("User-Agent");
    validHeaders.add("Content-Type");
  }

  /**
   * Validate a http request.
   * 
   * @param parameterMap parameters map of request query
   * @param response HttpServerResponse object
   */
  private boolean validateParams(MultiMap parameterMap) {
    final List<Entry<String, String>> entries = parameterMap.entries();
    for (final Entry<String, String> entry : entries) {
      // System.out.println(entry.getKey());
      if (!validParams.contains(entry.getKey())) {
        return false;
      }
    }
    return true;
  }


  private boolean validateHeader(MultiMap headerMap) {
    final List<Entry<String, String>> entries = headerMap.entries();
    for (final Entry<String, String> entry : entries) {
      // System.out.println(entry.getKey());
      /*
       * if (!validHeaders.contains(entry.getKey())) { return false; }
       */
    }
    return true;
  }

  /**
   * validate request parameters.
   * 
   * @param paramsMap map of request parameters
   * @return Future future JsonObject
   */
  public Future<Boolean> validate(MultiMap paramsMap) {
    Promise<Boolean> promise = Promise.promise();
    if (validateParams(paramsMap)) {
      isValidQueryWithFilters(paramsMap).onComplete(handler -> {
        if (handler.succeeded()) {
          // validation for geometry and coordinates.
          String geom = paramsMap.get(NGSILDQUERY_GEOMETRY);
          String coords = paramsMap.get(NGSILDQUERY_COORDINATES);
          if (geom != null && coords != null && !isValidCoordinatesForGeometry(geom, coords)) {
            promise.fail("Invalid geometry coordinates.");
          } else {
            promise.complete(true);
          }
        } else {
          promise.fail(handler.cause().getMessage());
        }
      });
    } else {
      promise.fail(MSG_INVALID_PARAM);
    }
    return promise.future();
  }

  /**
   * validate request parameters.
   * 
   * @param JsonObject requestJson of request parameters
   * @return Future future JsonObject
   */
  public Future<Boolean> validate(JsonObject requestJson) {
    Promise<Boolean> promise = Promise.promise();
    MultiMap paramsMap = MultiMap.caseInsensitiveMultiMap();

    requestJson.forEach(entry -> {
      if (entry.getKey().equalsIgnoreCase("geoQ") || entry.getKey().equalsIgnoreCase("temporalQ")) {
        JsonObject innerObject = (JsonObject) entry.getValue();
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
        innerObject.forEach(innerentry -> {
          paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
        });
      } else if (entry.getKey().equalsIgnoreCase("entities")) {
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
        JsonArray array = (JsonArray) entry.getValue();
        JsonObject innerObject = array.getJsonObject(0);
        innerObject.forEach(innerentry -> {
          paramsMap.add(innerentry.getKey().toString(), innerentry.getValue().toString());
        });
      } else {
        paramsMap.add(entry.getKey().toString(), entry.getValue().toString());
      }

    });
    
    validate(paramsMap).onComplete(handler->{
      if(handler.succeeded()) {
        promise.complete(true);
      }else {
        promise.fail(handler.cause().getMessage());
      }
    });
    return promise.future();
  }


  private Future<Boolean> isValidQueryWithFilters(MultiMap paramsMap) {
    Promise<Boolean> promise = Promise.promise();
    Future<List<String>> filtersFuture = catalogueService.getApplicableFilters(paramsMap.get("id"));
    filtersFuture.onComplete(handler -> {
      if (handler.succeeded()) {
        List<String> filters = filtersFuture.result();
        if (isTemporalQuery(paramsMap) && !filters.contains("TEMPORAL")) {
          promise.fail("Temporal parameters are not supported by RS group/Item.");
          return;
        }
        if (isSpatialQuery(paramsMap) && !filters.contains("SPATIAL")) {
          promise.fail("Spatial parameters are not supported by RS group/Item.");
          return;
        }
        if (isAttributeQuery(paramsMap) && !filters.contains("ATTR")) {
          promise.fail("Attribute parameters are not supported by RS group/Item.");
          return;
        }
        promise.complete(true);
      } else {
        promise.fail("fail to get filters for validation");
      }
    });
    return promise.future();
  }


  private Boolean isTemporalQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_TIMEREL) || params.contains(NGSILDQUERY_TIME)
        || params.contains(NGSILDQUERY_ENDTIME) || params.contains(NGSILDQUERY_TIME_PROPERTY);

  }

  private Boolean isSpatialQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_GEOREL) || params.contains(NGSILDQUERY_GEOMETRY)
        || params.contains(NGSILDQUERY_GEOPROPERTY) || params.contains(NGSILDQUERY_COORDINATES);

  }

  private Boolean isAttributeQuery(MultiMap params) {
    return params.contains(NGSILDQUERY_ATTRIBUTE);

  }

  private boolean isValidCoordinatesForGeometry(String geom, String coordinates) {
    JsonObject json = new JsonObject();
    json.put("coordinates", new JsonArray(coordinates));
    if (geom.equalsIgnoreCase("point")) {
      json.put("type", "Point");
      return isValidCoordinates(json.toString());
    } else if (geom.equalsIgnoreCase("polygon")) {
      json.put("type", "Polygon");
      return isValidCoordinates(json.toString());
    } else if (geom.equalsIgnoreCase("linestring")) {
      json.put("type", "LineString");
      return isValidCoordinates(json.toString());
    } else if (geom.equalsIgnoreCase("bbox")) {
      // NOTE : since bbox is not completely supported by jts2geojson, an alternative to check 2
      // coordinates and validate as a linestring
      String[] bboxEdges = coordinates.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
      if (bboxEdges.length != 4) {
        return false;
      }
      json.put("type", "LineString");
      return isValidCoordinates(json.toString());
    } else {
      return false;
    }
  }

  private boolean isValidCoordinates(String geoJson) {
    boolean isValid = false;
    try {
      GeoJSONReader reader = new GeoJSONReader();
      org.locationtech.jts.geom.Geometry geom = reader.read(geoJson);
      boolean  coordsCount=false;
      if(("Polygon").equalsIgnoreCase(geom.getGeometryType())) {
        Coordinate[] coords=geom.getCoordinates();
        coordsCount=coords.length<11;
      }
      isValid = geom.isValid() && coordsCount;
    } catch (Exception ex) {
      LOGGER.error("Invalid geom/coordinates passed");
    }
    return isValid;
  }
}

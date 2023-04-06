package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WrapperQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.exception.ESQueryException;

public class GeoQueryFiltersDecorator implements ElasticsearchQueryDecorator {

  private static final Logger LOGGER = LogManager.getLogger(GeoQueryFiltersDecorator.class);
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;
  private String geoQuery =
      "{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }";

  public GeoQueryFiltersDecorator(Map<FilterType, List<Query>> queryFilters,
      JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    Query geoWrapperQuery;
    if (requestQuery.containsKey(LON) && requestQuery.containsKey(LAT)
        && requestQuery.containsKey(GEO_RADIUS)) {
      // circle
      requestQuery.put(GEOMETRY, "Point");
      String relation = requestQuery.containsKey(GEOREL) ? requestQuery.getString(GEOREL) : WITHIN;
      String query = String.format(geoQuery, "location", getGeoJson(requestQuery), relation);
      String encodedString = Base64.getEncoder().encodeToString(query.getBytes());
      geoWrapperQuery = WrapperQuery.of(w -> w.query(encodedString))._toQuery();

    } else if (requestQuery.containsKey(GEOMETRY)
        && (requestQuery.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
            || requestQuery.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
        && requestQuery.containsKey(GEOREL) && requestQuery.containsKey(COORDINATES_KEY)
        && requestQuery.containsKey(GEO_PROPERTY)) {
      // polygon & linestring
      String relation = requestQuery.getString(GEOREL);

      if (!isValidCoordinates(requestQuery.getString(GEOMETRY),
          new JsonArray(requestQuery.getString("coordinates")))) {
        throw new ESQueryException("Coordinate mismatch (Polygon)");
      }
      String query = String.format(geoQuery, "location", getGeoJson(requestQuery), relation);
      String encodedString = Base64.getEncoder().encodeToString(query.getBytes());
      geoWrapperQuery = WrapperQuery.of(w -> w.query(encodedString))._toQuery();

    } else if (requestQuery.containsKey(GEOMETRY)
        && requestQuery.getString(GEOMETRY).equalsIgnoreCase(BBOX)
        && requestQuery.containsKey(GEOREL) && requestQuery.containsKey(COORDINATES_KEY)
        && requestQuery.containsKey(GEO_PROPERTY)) {
      // bbox
      String relation = requestQuery.getString(GEOREL);
      String query = String.format(geoQuery, "location", getGeoJson(requestQuery), relation);
      String encodedString = Base64.getEncoder().encodeToString(query.getBytes());
      geoWrapperQuery = WrapperQuery.of(w -> w.query(encodedString))._toQuery();
    } else {
      throw new ESQueryException("Missing/Invalid geo parameters");
    }
    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(geoWrapperQuery);
    return queryFilters;
  }

  private JsonObject getGeoJson(JsonObject json) {
    LOGGER.debug(json);
    JsonObject geoJson = new JsonObject();
    String geom;
    JsonArray coordinates;
    if ("Point".equalsIgnoreCase(json.getString(GEOMETRY))) {
      double lat = json.getDouble(LAT);
      double lon = json.getDouble(LON);
      geom = "Circle";
      geoJson.put("radius", json.getString("radius") + "m");
      coordinates = new JsonArray().add(lon).add(lat);
    } else if ("bbox".equalsIgnoreCase(json.getString(GEOMETRY))) {
      geom = "envelope";
      coordinates = new JsonArray(json.getString("coordinates"));
    } else {
      geom = json.getString(GEOMETRY);
      coordinates = new JsonArray(json.getString("coordinates"));
    }
    geoJson.put("type", geom);
    geoJson.put("coordinates", coordinates);
    LOGGER.debug(geoJson);
    return geoJson;
  }

  private boolean isValidCoordinates(String geometry, JsonArray coordinates) {
    int length = coordinates.getJsonArray(0).size();
    if (geometry.equalsIgnoreCase(POLYGON)
        && !coordinates
            .getJsonArray(0)
              .getJsonArray(0)
              .getDouble(0)
              .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
        && !coordinates
            .getJsonArray(0)
              .getJsonArray(0)
              .getDouble(1)
              .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
      return false;

    }
    return true;
  }

}

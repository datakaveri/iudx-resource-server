package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.BBOX;
import static iudx.resource.server.database.archives.Constants.COORDINATES_KEY;
import static iudx.resource.server.database.archives.Constants.GEOMETRY;
import static iudx.resource.server.database.archives.Constants.GEOREL;
import static iudx.resource.server.database.archives.Constants.GEO_PROPERTY;
import static iudx.resource.server.database.archives.Constants.GEO_RADIUS;
import static iudx.resource.server.database.archives.Constants.LAT;
import static iudx.resource.server.database.archives.Constants.LINESTRING;
import static iudx.resource.server.database.archives.Constants.LON;
import static iudx.resource.server.database.archives.Constants.POLYGON;
import static iudx.resource.server.database.archives.Constants.WITHIN;

import iudx.resource.server.database.elastic.exception.ESQueryDecodeException;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class GeoQueryParser implements QueryParser {

  private BoolQueryBuilder builder;
  private JsonObject json;

  public GeoQueryParser(BoolQueryBuilder builder, JsonObject json) {
    this.builder = builder;
    this.json = json;
  }

  @Override
  public BoolQueryBuilder parse() {

    String relation;

    if (json.containsKey(LON) && json.containsKey(LAT) && json.containsKey(GEO_RADIUS)) {

      relation = json.containsKey(GEOREL) ? json.getString(GEOREL) : WITHIN;
      json.put("geometry", "point");// TODO : replace and pass geom directly in query from api verticle
      builder.filter(QueryBuilders.wrapperQuery(String
          .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
              "location",
              getGeoJson(json),
              ShapeRelation.getRelationByName(relation).getRelationName())));

    } else if (json.containsKey(GEOMETRY)
        && (json.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
            || json.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
        && json.containsKey(GEOREL) && json.containsKey(COORDINATES_KEY)
        && json.containsKey(GEO_PROPERTY)) {

      if (!isValidCoordinates(json.getString(GEOMETRY), new JsonArray(json.getString("coordinates")))) {
        throw new ESQueryDecodeException("Coordinate mismatch (Polygon)");
      }

      relation = json.getString(GEOREL);

      builder.filter(QueryBuilders.wrapperQuery(String
          .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
              "location",
              getGeoJson(json),
              ShapeRelation.getRelationByName(relation).getRelationName())));

    } else if (json.containsKey(GEOMETRY) && json.getString(GEOMETRY).equalsIgnoreCase(BBOX)
        && json.containsKey(GEOREL) && json.containsKey(COORDINATES_KEY)
        && json.containsKey(GEO_PROPERTY)) {
      relation = json.getString(GEOREL);
      builder.filter(QueryBuilders.wrapperQuery(String
          .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
              "location",
              getGeoJson(json),
              ShapeRelation.getRelationByName(relation).getRelationName())));

    } else {
      throw new ESQueryDecodeException("Missing/Invalid geo parameters");
    }
    return builder;
  }

  private JsonObject getGeoJson(JsonObject json) {
    JsonObject geoJson = new JsonObject();
    String geom;
    JsonArray coordinates;
    if ("Point".equalsIgnoreCase(json.getString(GEOMETRY))) {
      double lat = json.getDouble(LAT);
      double lon = json.getDouble(LON);
      geom = "Circle";
      geoJson.put("radius", json.getString("radius") + "m");
      coordinates = (new JsonArray().add(lon).add(lat));
    } else if ("bbox".equalsIgnoreCase(json.getString(GEOMETRY))) {
      geom = "envelope";
      coordinates = (new JsonArray(json.getString("coordinates")));
    } else {
      geom = json.getString(GEOMETRY);
      coordinates = (new JsonArray(json.getString("coordinates")));
    }
    geoJson.put("type", geom);
    geoJson.put("coordinates", coordinates);
    return geoJson;
  }

  private boolean isValidCoordinates(String geometry, JsonArray coordinates) {
    int length = coordinates.getJsonArray(0).size();
    if (geometry.equalsIgnoreCase(POLYGON)
        && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
        && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
      return false;

    }
    return true;
  }

}

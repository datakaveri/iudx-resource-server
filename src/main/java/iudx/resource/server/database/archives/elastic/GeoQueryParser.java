package iudx.resource.server.database.archives.elastic;

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

      relation = json.getString(GEOREL);
      // coordinates = new JsonArray(json.getString(COORDINATES_KEY));

      builder.filter(QueryBuilders.wrapperQuery(String
          .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
              "location",
              getGeoJson(json),
              ShapeRelation.getRelationByName(relation).getRelationName())));

    } else if (json.containsKey(GEOMETRY) && json.getString(GEOMETRY).equalsIgnoreCase(BBOX)
        && json.containsKey(GEOREL) && json.containsKey(COORDINATES_KEY)
        && json.containsKey(GEO_PROPERTY)) {
      relation = json.getString(GEOREL);
      // coordinates = new JsonArray(json.getString(COORDINATES_KEY));
      builder.filter(QueryBuilders.wrapperQuery(String
          .format("{ \"geo_shape\": { \"%s\": { \"shape\": %s, \"relation\": \"%s\" } } }",
              "location",
              getGeoJson(json),
              ShapeRelation.getRelationByName(relation).getRelationName())));

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


  public static void main(String[] args) {
    JsonObject json = new JsonObject(
        "{\"id\":[\"iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/surat-itms-realtime-information/surat-itms-live-eta\"],\"geometry\":\"bbox\",\"coordinates\":\"[[72.8296,21.2],[72.8297,21.15]]\",\"georel\":\"within\",\"geoproperty\":\"location\",\"searchType\":\"latestSearch_geoSearch\",\"instanceID\":\"localhost:8443\"}");

    GeoQueryParser geoQueryParser = new GeoQueryParser(new BoolQueryBuilder(), json);
    BoolQueryBuilder bool = geoQueryParser.parse();
    System.out.println(bool.toString());
  }

}

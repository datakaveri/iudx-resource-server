package iudx.resource.server.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import org.apache.commons.codec.digest.DigestUtils;

import static iudx.resource.server.database.Constants.*;

public class QueryDecoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryDecoder.class);

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   * 
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public JsonObject queryDecoder(JsonObject request) {

    String searchType = request.getString(SEARCH_TYPE);
    Boolean match = false;
    JsonObject elasticQuery = new JsonObject();
    JsonArray id = request.getJsonArray(ID);
    JsonArray filterQuery = new JsonArray();
    String queryGeoShape = null;

    JsonObject boolObject = new JsonObject().put(BOOL_KEY, new JsonObject());
    filterQuery.add(new JsonObject(
        TERMS_QUERY.replace("$1", RESOURCE_ID_KEY).replace("$2", id.encode())));

    /* TODO: Pagination for large result set */
    if (request.containsKey(SEARCH_KEY) && request.getBoolean(SEARCH_KEY)) {
      elasticQuery.put(SIZE_KEY, 10000);
    }

    /* Latest Search */
    if (LATEST_SEARCH.equalsIgnoreCase(searchType)) {
      JsonArray sourceFilter = null;
      if (request.containsKey(RESPONSE_ATTRS)) {
        sourceFilter = request.getJsonArray(RESPONSE_ATTRS);
      }
      JsonObject latestQuery = new JsonObject();
      JsonArray docs = new JsonArray();
      for (Object o : id) {
        String resourceString = (String) o;
        String sha1String = DigestUtils.sha1Hex(resourceString);
        JsonObject json = new JsonObject().put(DOC_ID, sha1String);
        if (sourceFilter != null) {
          json.put(SOURCE_FILTER_KEY, sourceFilter);
        }
        docs.add(json);
      }
      return latestQuery.put(DOCS_KEY, docs);
    }

    /* Geo-Spatial Search */
    if (searchType.matches(GEOSEARCH_REGEX)) {

      LOGGER.debug("Info: Geo Search block");

      match = true;

      String relation;
      JsonArray coordinates = null;

      if (request.containsKey(LON) && request.containsKey(LAT) && request.containsKey(GEO_RADIUS)) {

        double lat = request.getDouble(LAT);
        double lon = request.getDouble(LON);
        double radius = request.getDouble(GEO_RADIUS);
        relation = request.containsKey(GEOREL) ? request.getString(GEOREL) : WITHIN;

        coordinates = new JsonArray().add(lon).add(lat);
        String radiusStr = ",\"radius\": \"$1m\"".replace("$1", Double.toString(radius));

        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", GEO_CIRCLE)
            .replace("$2", coordinates.toString().concat(radiusStr)).replace("$3", relation)
            .replace("$4", GEO_KEY);

      } else if (request.containsKey(GEOMETRY)
          && (request.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
              || request.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
          && request.containsKey(GEOREL) && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEO_PROPERTY)) {

        String geometry = request.getString(GEOMETRY);
        relation = request.getString(GEOREL);
        coordinates = new JsonArray(request.getString(COORDINATES_KEY));
        int length = coordinates.getJsonArray(0).size();

        if (geometry.equalsIgnoreCase(POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(ERROR, COORDINATE_MISMATCH);

        }

        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", geometry)
            .replace("$2", coordinates.toString()).replace("$3", relation).replace("$4", GEO_KEY);

      } else if (request.containsKey(GEOMETRY) && request.getString(GEOMETRY).equalsIgnoreCase(BBOX)
          && request.containsKey(GEOREL) && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEO_PROPERTY)) {
        relation = request.getString(GEOREL);
        coordinates = new JsonArray(request.getString(COORDINATES_KEY));

        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", GEO_BBOX)
            .replace("$2", coordinates.toString()).replace("$3", relation).replace("$4", GEO_KEY);

      } else {
        return new JsonObject().put(ERROR, MISSING_GEO_FIELDS);
      }

      filterQuery.add(new JsonObject(queryGeoShape));
    }

    /* Temporal Search */
    if (searchType.matches(TEMPORAL_SEARCH_REGEX) && request.containsKey(REQ_TIMEREL)
        && request.containsKey(TIME_KEY)) {

      LOGGER.info("Info: Temporal Search block");

      match = true;
      String timeRelation = request.getString(REQ_TIMEREL);
      String time = request.getString(TIME_KEY);
      
      /* check if the time is valid based on ISO 8601 format. */

      try {
        ZonedDateTime zdt = ZonedDateTime.parse(time);
        LOGGER.debug("Parsed time: " + zdt.toString());
      } catch (DateTimeParseException e) {
        LOGGER.error("Invalid Date exception: " + e.getMessage());
        return new JsonObject().put(ERROR, INVALID_DATE);
      }

      String rangeTimeQuery = "";
      if (DURING.equalsIgnoreCase(timeRelation)) {
        String endTime = request.getString(END_TIME);
        String endTemp = "\",\"lte\":" + "\"" + endTime + "\"";
        rangeTimeQuery =
            TIME_QUERY.replace("$1", GREATER_THAN_EQ).replace("$2\"", time.concat(endTemp));

      } else if (BEFORE.equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery = TIME_QUERY.replace("$1", LESS_THAN).replace("$2", time);

      } else if (AFTER.equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery = TIME_QUERY.replace("$1", GREATER_THAN).replace("$2", time);

      } else if (TEQUALS.equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery = TERM_QUERY.replace("$1", TIME_FIELD_DB).replace("$2", time);

      } else {
        return new JsonObject().put(ERROR, MISSING_TEMPORAL_FIELDS);

      }
      System.out.println(rangeTimeQuery);
      filterQuery.add(new JsonObject(rangeTimeQuery));
    }

    /* Attribute Search */
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {

      LOGGER.debug("Info: Attribute Search block");

      match = true;
      JsonArray attrQuery;

      if (request.containsKey(ATTRIBUTE_QUERY_KEY)) {
        attrQuery = request.getJsonArray(ATTRIBUTE_QUERY_KEY);

        /* Multi-Attribute */
        for (Object obj : attrQuery) {
          JsonObject attrObj = (JsonObject) obj;
          String attrElasticQuery = "";

          try {
            String attribute = attrObj.getString(ATTRIBUTE_KEY);
            String operator = attrObj.getString(OPERATOR);
            String attributeValue = attrObj.getString(VALUE);

            if (GREATER_THAN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", GREATER_THAN)
                  .replace("$3", attributeValue);

            } else if (LESS_THAN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", LESS_THAN)
                  .replace("$3", attributeValue);

            } else if (GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", GREATER_THAN_EQ)
                  .replace("$3", attributeValue);

            } else if (LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = RANGE_QUERY.replace("$1", attribute).replace("$2", LESS_THAN_EQ)
                  .replace("$3", attributeValue);

            } else if (EQUAL_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = TERM_QUERY.replace("$1", attribute).replace("$2", attributeValue);

            } else if (BETWEEN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery = RANGE_QUERY_BW.replace("$1", attribute)
                  .replace("$2", GREATER_THAN_EQ).replace("$3", attrObj.getString(VALUE_LOWER))
                  .replace("$4", LESS_THAN_EQ).replace("$5", attrObj.getString(VALUE_UPPER));

            } else if (NOT_EQUAL_OP.equalsIgnoreCase(operator)) {

              boolObject.getJsonObject(BOOL_KEY).put(MUST_NOT,
                  new JsonObject(TERM_QUERY.replace("$1", attribute)
                      .replace("$2", attributeValue)));

            } else {
              return new JsonObject().put(ERROR, INVALID_OPERATOR);
            }

            if (!attrElasticQuery.isBlank()) {
              filterQuery.add(new JsonObject(attrElasticQuery));
            }

          } catch (NullPointerException e) {
            LOGGER.error("Fail: " + MISSING_ATTRIBUTE_FIELDS + ";" + e.getMessage());
            return new JsonObject().put(ERROR, MISSING_ATTRIBUTE_FIELDS);
          }
        }
      }
    }

    /* Response Filtering */
    if (searchType.matches(RESPONSE_FILTER_REGEX)) {

      LOGGER.debug("Info: Adding responseFilter");

      match = true;
      if (!request.getBoolean(SEARCH_KEY)) {
        return new JsonObject().put(ERROR, COUNT_UNSUPPORTED);
      }
      if (request.containsKey(RESPONSE_ATTRS)) {
        JsonArray sourceFilter = request.getJsonArray(RESPONSE_ATTRS);
        elasticQuery.put(SOURCE_FILTER_KEY, sourceFilter);
      } else {
        return new JsonObject().put(ERROR, MISSING_RESPONSE_FILTER_FIELDS);
      }
    }

    /* checks if any valid search requests have matched */
    if (!match) {
      return new JsonObject().put(ERROR, INVALID_SEARCH);
    } else {
      /* return fully formed elastic query */
      boolObject.getJsonObject(BOOL_KEY).put(FILTER_KEY, filterQuery);
      return elasticQuery.put(QUERY_KEY, boolObject);
    }
  }
}

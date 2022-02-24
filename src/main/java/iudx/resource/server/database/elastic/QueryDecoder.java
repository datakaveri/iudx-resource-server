package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.COUNT_UNSUPPORTED;
import static iudx.resource.server.database.archives.Constants.ERROR;
import static iudx.resource.server.database.archives.Constants.GEOSEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.INVALID_SEARCH;
import static iudx.resource.server.database.archives.Constants.MISSING_RESPONSE_FILTER_FIELDS;
import static iudx.resource.server.database.archives.Constants.PROD_INSTANCE;
import static iudx.resource.server.database.archives.Constants.QUERY_KEY;
import static iudx.resource.server.database.archives.Constants.REQ_TIMEREL;
import static iudx.resource.server.database.archives.Constants.RESPONSE_ATTRS;
import static iudx.resource.server.database.archives.Constants.RESPONSE_FILTER_REGEX;
import static iudx.resource.server.database.archives.Constants.SEARCH_KEY;
import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static iudx.resource.server.database.archives.Constants.SOURCE_FILTER_KEY;
import static iudx.resource.server.database.archives.Constants.TEMPORAL_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.TEST_INSTANCE;
import static iudx.resource.server.database.archives.Constants.TIME_KEY;
import static iudx.resource.server.database.archives.Constants.TIME_LIMIT;

import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  public JsonObject getESquery(JsonObject json) {

    String searchType = json.getString(SEARCH_TYPE);
    Boolean isValidQuery = false;
    JsonObject elasticQuery = new JsonObject();

    boolean temporalQuery = false;

    JsonArray id = json.getJsonArray(ID);

    String timeLimit = json.getString(TIME_LIMIT).split(",")[1];
    int numDays = Integer.valueOf(json.getString(TIME_LIMIT).split(",")[2]);

    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
    boolQuery.filter(QueryBuilders.termsQuery(ID, id.getString(0)));

    if (searchType.matches(GEOSEARCH_REGEX)) {
      boolQuery = new GeoQueryParser(boolQuery, json).parse();
      isValidQuery = true;
    }

    if (searchType.matches(TEMPORAL_SEARCH_REGEX) && json.containsKey(REQ_TIMEREL)
        && json.containsKey(TIME_KEY)) {
      boolQuery = new TemporalQueryParser(boolQuery, json).parse();
      temporalQuery = true;
      isValidQuery = true;
    }

    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      boolQuery = new AttributeQueryParser(boolQuery, json).parse();
      isValidQuery = true;
    }

    JsonArray responseFilters = null;
    if (searchType.matches(RESPONSE_FILTER_REGEX)) {
      LOGGER.debug("Info: Adding responseFilter");
      isValidQuery = true;
      if (!json.getBoolean(SEARCH_KEY)) {
        return new JsonObject().put(ERROR, COUNT_UNSUPPORTED);
      }
      if (json.containsKey(RESPONSE_ATTRS)) {
        responseFilters = json.getJsonArray(RESPONSE_ATTRS);
      } else {
        return new JsonObject().put(ERROR, MISSING_RESPONSE_FILTER_FIELDS);
      }
    }

    /* checks if any valid search jsons have matched */
    if (!isValidQuery) {
      return new JsonObject().put(ERROR, INVALID_SEARCH);
    } else {
      if (!temporalQuery && json.getJsonArray("applicableFilters").contains("TEMPORAL")) {
        if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(PROD_INSTANCE)) {
          boolQuery
              .filter(QueryBuilders.rangeQuery("observationDateTime")
                  .gte("now-" + timeLimit + "d/d"));

        } else if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(TEST_INSTANCE)) {
          String endTime = json.getString(TIME_LIMIT).split(",")[1];
          ZonedDateTime endTimeZ = ZonedDateTime.parse(endTime);
          ZonedDateTime startTime = endTimeZ.minusDays(numDays);

          boolQuery
              .filter(QueryBuilders.rangeQuery("observationDateTime")
                  .lte(endTime)
                  .gte(startTime));

        }

      }
    }

    elasticQuery.put(QUERY_KEY, new JsonObject(boolQuery.toString()));

    if (responseFilters != null) {
      elasticQuery.put(SOURCE_FILTER_KEY, responseFilters);
    }

    return elasticQuery;
  }

}

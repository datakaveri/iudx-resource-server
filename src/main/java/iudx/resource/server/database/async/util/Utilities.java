package iudx.resource.server.database.async.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.AttributeQueryParser;
import iudx.resource.server.database.elastic.GeoQueryParser;
import iudx.resource.server.database.elastic.TemporalQueryParser;
import iudx.resource.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import java.time.ZonedDateTime;
import java.util.UUID;

import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static iudx.resource.server.database.archives.Constants.TIME_LIMIT;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.GEOSEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.TEMPORAL_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.REQ_TIMEREL;
import static iudx.resource.server.database.archives.Constants.TIME_KEY;
import static iudx.resource.server.database.archives.Constants.ATTRIBUTE_SEARCH_REGEX;
import static iudx.resource.server.database.archives.Constants.RESPONSE_FILTER_REGEX;
import static iudx.resource.server.database.archives.Constants.SEARCH_KEY;
import static iudx.resource.server.database.archives.Constants.RESPONSE_ATTRS;
import static iudx.resource.server.database.archives.Constants.PROD_INSTANCE;
import static iudx.resource.server.database.archives.Constants.TEST_INSTANCE;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_PENDING_SQL;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_READY_SQL;
import static iudx.resource.server.database.postgres.Constants.UPDATE_S3_URL_SQL;
import static iudx.resource.server.database.postgres.Constants.DELETE_S3_PENDING_SQL;

public class Utilities {

  private static final Logger LOGGER = LogManager.getLogger(Utilities.class);
  private final PostgresService pgService;

  public Utilities(PostgresService pgService) {
    this.pgService = pgService;
  }

  public Future<Void> writeToDB(StringBuilder query) {
    Promise<Void> promise = Promise.promise();

    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete();
          } else {
            LOGGER.error("op on DB failed");
            promise.fail("operation fail");
          }
        });
    return promise.future();
  }

  public Future<Void> writeToDB(String searchID, String requestID, String sub) {

    StringBuilder query =
        new StringBuilder(
            INSERT_S3_PENDING_SQL
                .replace("$1", UUID.randomUUID().toString())
                .replace("$2", searchID)
                .replace("$3", requestID)
                .replace("$4", sub)
                .replace("$5", "Pending"));

    return writeToDB(query);
  }

  public Future<Void> writeToDB(String sub, JsonObject result) {

    StringBuilder query =
        new StringBuilder(
            INSERT_S3_READY_SQL
                .replace("$1", UUID.randomUUID().toString())
                .replace("$2", UUID.randomUUID().toString())
                .replace("$3", result.getString("request_id"))
                .replace("$4", result.getString("status"))
                .replace("$5", result.getString("s3_url"))
                .replace("$6", result.getString("expiry"))
                .replace("$7", sub)
                .replace("$8", result.getString("object_id")));

    return writeToDB(query);
  }

  public Future<Void> writeToDB(String sub, String s3_url, String expiry, JsonObject result) {

    result.remove("s3_url");
    result.remove("expiry");
    result.put("s3_url", s3_url);
    result.put("expiry", expiry);

    return writeToDB(sub, result);
  }

  public Future<Void> updateDBRecord(
      String searchID, String s3_url, String expiry, String objectKey) {

    StringBuilder query =
        new StringBuilder(
            UPDATE_S3_URL_SQL
                .replace("$1", s3_url)
                .replace("$2", expiry)
                .replace("$3", searchID)
                .replace("$4", objectKey));

    return writeToDB(query);
  }

  public QueryBuilder getESquery1(JsonObject json, boolean scrollRequest) {
    LOGGER.debug(json);
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

    if (searchType.matches(TEMPORAL_SEARCH_REGEX)
        && json.containsKey(REQ_TIMEREL)
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
        return null;
      }
      if (json.containsKey(RESPONSE_ATTRS)) {
        responseFilters = json.getJsonArray(RESPONSE_ATTRS);
      } else {
        return null;
      }
    }

    /* checks if any valid search jsons have matched */
    if (!isValidQuery) {
      return null;
    } else {
      if (!temporalQuery && json.getJsonArray("applicableFilters").contains("TEMPORAL")) {
        if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(PROD_INSTANCE)) {
          boolQuery.filter(
              QueryBuilders.rangeQuery("observationDateTime").gte("now-" + timeLimit + "d/d"));

        } else if (json.getString(TIME_LIMIT).split(",")[0].equalsIgnoreCase(TEST_INSTANCE)) {
          String endTime = json.getString(TIME_LIMIT).split(",")[1];
          ZonedDateTime endTimeZ = ZonedDateTime.parse(endTime);
          ZonedDateTime startTime = endTimeZ.minusDays(numDays);

          boolQuery.filter(
              QueryBuilders.rangeQuery("observationDateTime").lte(endTime).gte(startTime));
        }
      }
    }

    //    elasticQuery.put(QUERY_KEY, new JsonObject(boolQuery.toString()));
    //
    //    if (responseFilters != null) {
    //      elasticQuery.put(SOURCE_FILTER_KEY, responseFilters);
    //    }

    return boolQuery;
  }

  public Future<Void> deleteEntry(String searchID) {
    StringBuilder query = new StringBuilder(DELETE_S3_PENDING_SQL.replace("$1", searchID));

    return writeToDB(query);
  }
}

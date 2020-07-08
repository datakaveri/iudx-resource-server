package iudx.resource.server.database;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Resource Server implements
 * the definitions of the {@link iudx.resource.server.database.DatabaseService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);
  private final RestClient client;
  private JsonObject query;

  public DatabaseServiceImpl(RestClient client) {
    this.client = client;
  }

  /**
   * Performs a ElasticSearch search query using the low level REST client.
   * 
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return database response in case of success and
   *                appropriate error message in case of failure
   */

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {
    /* TODO Need to update params to use contants */
    logger.info("Inside searchQuery<DatabaseService> block-------- " + request.toString());
    request.put("search", true);
    // TODO : only for testing comment after testing.
    request.put("isTest", true);

    if (!request.containsKey("id")) {
      logger.info("No id found");
      handler.handle(Future.failedFuture("No id found"));
      return null;
    }
    if (request.getJsonArray("id").isEmpty()) {
      logger.info("resource-id is empty");
      handler.handle(Future.failedFuture("resource-id is empty"));
      return null;
    }
    if (!request.containsKey("searchType")) {
      logger.info("No searchType found");
      handler.handle(Future.failedFuture("No searchType found"));
      return null;
    }
    // TODO: Need to automate the Index flow using the instanceID field.
    // Need to populate a HashMap containing the instanceID and the indexName
    // We need to discuss if we need to have a single index or an index per group to
    // avoid any
    // dependency
    // String resourceGroup = ""; //
    // request.getJsonArray("id").getString(0).split("/")[3];
    String resourceServer = request.getJsonArray("id").getString(0).split("/")[0];
    logger.info("Resource Server instanceID is " + resourceServer);
    Request elasticRequest = new Request("GET", Constants.VARANASI_TEST_SEARCH_INDEX);
    elasticRequest.addParameter(Constants.FILTER_PATH, Constants.FILTER_PATH_VAL);
    query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.error("Query returned with an error: " + query.getString("Error"));
      handler.handle(Future.failedFuture(query.getString("Error")));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    elasticRequest.setJsonEntity(query.toString());
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        JsonArray dbResponse = new JsonArray();
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (!responseJson.containsKey("hits")) {
            handler.handle(Future.failedFuture("Empty response"));
            return;
          }
          JsonArray responseHits = responseJson.getJsonObject("hits").getJsonArray("hits");
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject("_source"));
          }
          handler.handle(Future.succeededFuture(dbResponse));
        } catch (IOException e) {
          logger.error("DB ERROR:\n");
          e.printStackTrace();
          handler.handle(Future.failedFuture("DB ERROR"));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.error("DB request has failed. ERROR: \n");
        e.printStackTrace();
        handler.handle(Future.failedFuture("DB ERROR"));
      }
    });
    return this;
  }

  /**
   * Performs a ElasticSearch count query using the low level REST client.
   * 
   * @param request Json object received from the ApiServerVerticle
   * @param handler Handler to return database response in case of success and
   *                appropriate error message in case of failure
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    logger.info("Inside countQuery<DatabaseService> block-------- " + request.toString());
    request.put("search", false);

    if (!request.containsKey("id")) {
      logger.info("No id found");
      handler.handle(Future.failedFuture("No id found"));
      return null;
    }
    if (request.getJsonArray("id").isEmpty()) {
      logger.info("resource-id is empty");
      handler.handle(Future.failedFuture("resource-id is empty"));
      return null;
    }
    if (!request.containsKey("searchType")) {
      logger.info("No searchType found");
      handler.handle(Future.failedFuture("No searchType found"));
      return null;
    }
    // String resourceGroup = ""; //
    // request.getJsonArray("id").getString(0).split("/")[3];
    query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.error("Query returned with an error " + query.getString("Error"));
      handler.handle(Future.failedFuture(query.getString("Error")));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    Request elasticRequest = new Request("GET", Constants.VARANASI_TEST_COUNT_INDEX);
    elasticRequest.setJsonEntity(query.toString());
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          handler.handle(Future
              .succeededFuture(new JsonObject().put("Count", responseJson.getInteger("count"))));
        } catch (IOException e) {
          logger.error("DB ERROR:\n");
          e.printStackTrace();
          handler.handle(Future.failedFuture("DB ERROR"));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.error("DB request has failed. ERROR:\n");
        e.printStackTrace();
        handler.handle(Future.failedFuture("DB ERROR"));
      }
    });
    return this;

  }

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the
   * parameters passed in the request.
   * 
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */

  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString(Constants.SEARCH_TYPE);
    Boolean match = false;
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject().put(Constants.BOOL_KEY, new JsonObject());
    JsonArray id = request.getJsonArray(Constants.ID);
    JsonArray filterQuery = new JsonArray();
    JsonObject termQuery = new JsonObject().put(Constants.TERMS_KEY,
        new JsonObject().put(Constants.RESOURCE_ID_KEY + ".keyword", id));

    filterQuery.add(termQuery);
    /* TODO: Pagination for large result set */
    if (request.containsKey("search") && request.getBoolean("search")) {
      elasticQuery.put(Constants.SIZE_KEY, 10);
    }
    /* Geo-Spatial Search */
    if (searchType.matches("(.*)geoSearch(.*)")) {
      match = true;
      logger.info("In geoSearch block---------");
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      if (request.containsKey(Constants.LON) && request.containsKey(Constants.LAT)
          && request.containsKey(Constants.GEO_RADIUS)) {
        double lat = request.getDouble(Constants.LAT);
        double lon = request.getDouble(Constants.LON);
        double radius = request.getDouble(Constants.GEO_RADIUS);
        relation = request.containsKey(Constants.GEOREL) ? request.getString(Constants.GEOREL)
            : Constants.WITHIN;
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_CIRCLE)
                    .put(Constants.COORDINATES_KEY, new JsonArray().add(lon).add(lat))
                    .put(Constants.GEO_RADIUS, radius + "m"))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && (request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POLYGON)
              || request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.LINESTRING))
          && request.containsKey(Constants.GEOREL) && request.containsKey(Constants.COORDINATES_KEY)
          && request.containsKey(Constants.GEO_PROPERTY)) {
        String geometry = request.getString(Constants.GEOMETRY);
        relation = request.getString(Constants.GEOREL);
        coordinates = new JsonArray(request.getString(Constants.COORDINATES_KEY));
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase(Constants.POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put("Error", Constants.COORDINATE_MISMATCH);

        }
        shapeJson
            .put(Constants.SHAPE_KEY, new JsonObject().put(Constants.TYPE_KEY, geometry)
                .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.BBOX)
          && request.containsKey(Constants.GEOREL) && request.containsKey(Constants.COORDINATES_KEY)
          && request.containsKey(Constants.GEO_PROPERTY)) {
        relation = request.getString(Constants.GEOREL);
        coordinates = new JsonArray(request.getString(Constants.COORDINATES_KEY));
        shapeJson = new JsonObject();
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_BBOX)
                    .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put("Error", Constants.MISSING_GEO_FIELDS);
      }
      geoSearch.put(Constants.GEO_SHAPE_KEY, new JsonObject().put(Constants.GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);
    }
    /* Response Filtering */
    if (searchType.matches("(.*)responseFilter(.*)")) {
      match = true;
      logger.info("In responseFilter block---------");
      if (!request.getBoolean("search")) {
        return new JsonObject().put("Error", Constants.COUNT_UNSUPPORTED);
      }
      if (request.containsKey(Constants.RESPONSE_ATTRS)) {
        JsonArray sourceFilter = request.getJsonArray(Constants.RESPONSE_ATTRS);
        elasticQuery.put(Constants.SOURCE_FILTER_KEY, sourceFilter);
      } else {
        return new JsonObject().put("Error", Constants.MISSING_RESPONSE_FILTER_FIELDS);
      }
    }
    /* Temporal Search */
    if (searchType.matches("(.*)temporalSearch(.*)") && request.containsKey(Constants.REQ_TIMEREL)
        && request.containsKey("time")) {
      match = true;
      logger.info("In temporalSearch block---------");
      String timeRelation = request.getString(Constants.REQ_TIMEREL);
      String time = request.getString(Constants.TIME_KEY);
      /* check if the time is valid based on the format. Supports both UTC and IST. */
      try {
        DateFormat formatTimeUtc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        DateFormat formatTimeIst = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatTimeUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        formatTimeIst.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        Date parsedDateTimeUtc = formatTimeUtc.parse(time);
        Date parsedDateTimeIst = formatTimeIst.parse(time);
        logger.info("Requested date: #UTC- " + formatTimeUtc.format(parsedDateTimeUtc) + "\n#IST- "
            + formatTimeIst.format(parsedDateTimeIst));
      } catch (ParseException e) {
        e.printStackTrace();
        return new JsonObject().put("Error", Constants.INVALID_DATE);
      }
      JsonObject rangeTimeQuery = new JsonObject();
      if (Constants.DURING.equalsIgnoreCase(timeRelation)) {
        String endTime = request.getString(Constants.END_TIME);
        rangeTimeQuery.put(Constants.RANGE_KEY,
            new JsonObject().put(Constants.TIME_KEY, new JsonObject()
                .put(Constants.GREATER_THAN_EQ, time).put(Constants.LESS_THAN_EQ, endTime)));
      } else if (Constants.BEFORE.equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery.put(Constants.RANGE_KEY, new JsonObject().put(Constants.TIME_KEY,
            new JsonObject().put(Constants.LESS_THAN, time)));
      } else if (Constants.AFTER.equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery.put(Constants.RANGE_KEY, new JsonObject().put(Constants.TIME_KEY,
            new JsonObject().put(Constants.GREATER_THAN, time)));
      } else if ("tequals".equalsIgnoreCase(timeRelation)) {
        rangeTimeQuery.put(Constants.TERM_KEY, new JsonObject().put(Constants.TIME_KEY, time));
      } else {
        return new JsonObject().put("Error", Constants.MISSING_TEMPORAL_FIELDS);
      }
      filterQuery.add(rangeTimeQuery);
    }
    /* Attribute Search */
    if (searchType.matches("(.*)attributeSearch(.*)")) {
      match = true;
      JsonArray attrQuery;
      logger.info("In attributeFilter block---------");
      if (request.containsKey(Constants.ATTRIBUTE_QUERY_KEY)) {
        attrQuery = request.getJsonArray(Constants.ATTRIBUTE_QUERY_KEY);
        /* Multi-Attribute */
        for (Object obj : attrQuery) {
          JsonObject attrObj = (JsonObject) obj;
          JsonObject attrElasticQuery = new JsonObject();
          try {
            String attribute = attrObj.getString(Constants.ATTRIBUTE_KEY);
            String operator = attrObj.getString(Constants.OPERATOR);
            if (Constants.GREATER_THAN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.RANGE_KEY,
                  new JsonObject().put(attribute, new JsonObject().put(Constants.GREATER_THAN,
                      Double.valueOf(attrObj.getString(Constants.VALUE)))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.LESS_THAN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.RANGE_KEY,
                  new JsonObject().put(attribute, new JsonObject().put(Constants.LESS_THAN,
                      Double.valueOf(attrObj.getString(Constants.VALUE)))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.GREATER_THAN_EQ_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.RANGE_KEY,
                  new JsonObject().put(attribute, new JsonObject().put(Constants.GREATER_THAN_EQ,
                      Double.valueOf(attrObj.getString(Constants.VALUE)))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.LESS_THAN_EQ_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.RANGE_KEY,
                  new JsonObject().put(attribute, new JsonObject().put(Constants.LESS_THAN_EQ,
                      Double.valueOf(attrObj.getString(Constants.VALUE)))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.EQUAL_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.TERM_KEY, new JsonObject().put(attribute,
                  Double.valueOf(attrObj.getString(Constants.VALUE))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.BETWEEN_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.RANGE_KEY,
                  new JsonObject().put(attribute,
                      new JsonObject()
                          .put(Constants.GREATER_THAN_EQ,
                              Double.valueOf(attrObj.getString(Constants.VALUE_LOWER)))
                          .put(Constants.LESS_THAN_EQ,
                              Double.valueOf(attrObj.getString(Constants.VALUE_UPPER)))));
              filterQuery.add(attrElasticQuery);
            } else if (Constants.NOT_EQUAL_OP.equalsIgnoreCase(operator)) {
              attrElasticQuery.put(Constants.TERM_KEY, new JsonObject().put(attribute,
                  Double.valueOf(attrObj.getString(Constants.VALUE))));
              boolObject.getJsonObject(Constants.BOOL_KEY).put(Constants.MUST_NOT,
                  attrElasticQuery);

              /*
               * TODO: Need to understand operator parameter of JsonObject from the APIServer
               * would look like.
               */
              // else if ("like".equalsIgnoreCase(operator)) {}
            } else {
              return new JsonObject().put("Error", Constants.INVALID_OPERATOR);
            }
          } catch (NullPointerException e) {
            e.printStackTrace();
            return new JsonObject().put("Error", Constants.MISSING_ATTRIBUTE_FIELDS);
          }
        }
      }
    }
    /* checks if any valid search requests have matched */
    if (!match) {
      return new JsonObject().put("Error", Constants.INVALID_SEARCH);
    } else {
      /* return fully formed elastic query */
      boolObject.getJsonObject(Constants.BOOL_KEY).put(Constants.FILTER_KEY, filterQuery);
      return elasticQuery.put(Constants.QUERY_KEY, boolObject);
    }
  }
}

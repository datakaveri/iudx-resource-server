package iudx.resource.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;


/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Resource Server implements the definitions of the
 * {@link iudx.resource.server.database.DatabaseService}.
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
   * @param handler Handler to return database response in case of success and appropriate error
   *        message in case of failure
   */

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {
    Request elasticRequest;
    logger.info("Inside searchQuery<DatabaseService> block-------- " + request.toString());
    request.put("search", true);

    if (!request.containsKey("id")) {
      handler.handle(Future.failedFuture("No id found"));
      return null;
    }
    if (request.getJsonArray("id").isEmpty()) {
      handler.handle(Future.failedFuture("resource-id is empty"));
      return null;
    }
    if (!request.containsKey("searchType")) {
      handler.handle(Future.failedFuture("No searchType found"));
      return null;
    }
    // TODO: Need to automate the Index flow using the instanceID field.
    // Need to populate a HashMap containing the instanceID and the indexName
    // We need to discuss if we need to have a single index or an index per group to avoid any
    // dependency
    String resourceGroup = ""; // request.getJsonArray("id").getString(0).split("/")[3];
    logger.info("Resource Group is " + resourceGroup);
    String resourceServer = request.getJsonArray("id").getString(0).split("/")[0];
    logger.info("Resource Server instanceID is " + resourceServer);
    if (request.getBoolean("isTest")) {
      elasticRequest = new Request("GET", VARANASI_TEST_SEARCH_INDEX);
    } else if ("varanasi-swm-vehicles".equalsIgnoreCase(resourceGroup)) {
      elasticRequest = new Request("GET", VARANASI_SWM_SEARCH_INDEX);
    } else {
      elasticRequest = new Request("GET", VARANASI_OTHER_SEARCH_INDEX);
    }

    elasticRequest.addParameter("filter_path", "took,hits.hits._source");
    query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.error("Query returned with an error");
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
   * @param handler Handler to return database response in case of success and appropriate error
   *        message in case of failure
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request elasticRequest;
    logger.info("Inside countQuery<DatabaseService> block-------- " + request.toString());
    request.put("search", false);

    if (!request.containsKey("id")) {
      handler.handle(Future.failedFuture("No id found"));
      return null;
    }
    if (request.getJsonArray("id").isEmpty()) {
      handler.handle(Future.failedFuture("resource-id is empty"));
      return null;
    }
    if (!request.containsKey("searchType")) {
      handler.handle(Future.failedFuture("No searchType found"));
      return null;
    }
    String resourceGroup = ""; // request.getJsonArray("id").getString(0).split("/")[3];
    if (request.getBoolean("isTest")) {
      elasticRequest = new Request("GET", VARANASI_TEST_COUNT_INDEX);
    } else if ("varanasi-swm-vehicles".equalsIgnoreCase(resourceGroup)) {
      elasticRequest = new Request("GET", VARANASI_SWM_COUNT_INDEX);
    } else {
      elasticRequest = new Request("GET", VARANASI_OTHER_COUNT_INDEX);
    }
    query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.error("Query returned with an error");
      handler.handle(Future.failedFuture(query.getString("Error")));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
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
          handler.handle(Future.succeededFuture(new JsonObject()
              .put("Count", responseJson.getInteger("count"))));
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
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   * 
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */

  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString("searchType");
    JsonObject elasticQuery = new JsonObject();
    JsonArray id = request.getJsonArray("id");
    JsonArray filterQuery = new JsonArray();
    JsonObject termQuery =
        new JsonObject().put("terms", new JsonObject().put(RESOURCE_ID_KEY + ".keyword", id));

    filterQuery.add(termQuery);
    if (request.containsKey("search") && request.getBoolean("search")) {
      elasticQuery.put("size", 10);
    }
    if (searchType.matches("(.*)geoSearch(.*)")) {
      logger.info("In geoSearch block---------");
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      if (request.containsKey("lon") && request.containsKey("lat")
          && request.containsKey("radius")) {
        double lat = request.getDouble("lat");
        double lon = request.getDouble("lon");
        String radius = request.getString("radius");
        shapeJson.put(SHAPE_KEY, new JsonObject().put(TYPE_KEY, GEO_CIRCLE)
            .put(COORDINATES_KEY, new JsonArray().add(lon).add(lat)).put(GEO_RADIUS, radius + "m"))
            .put(GEO_RELATION_KEY, "within");
      } else if (request.containsKey("geometry")
          && (request.getString("geometry").equalsIgnoreCase("polygon")
              || request.getString("geometry").equalsIgnoreCase("linestring"))
          && request.containsKey("georel") && request.containsKey("coordinates")
          && request.containsKey("geoproperty")) {
        String geometry = request.getString("geometry");
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase("polygon") && !coordinates.getJsonArray(0).getJsonArray(0)
            .getDouble(0).equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put("Error", "Coordinate mismatch (Polygon)");
        }
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, geometry).put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey("geometry")
          && request.getString("geometry").equalsIgnoreCase("bbox") && request.containsKey("georel")
          && request.containsKey("coordinates") && request.containsKey("geoproperty")) {
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        shapeJson = new JsonObject();
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_BBOX).put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put("Error", "Missing/Invalid geo parameters");
      }
      geoSearch.put(GEO_SHAPE_KEY, new JsonObject().put(GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);
    }
    if (searchType.matches("(.*)responseFilter(.*)")) {
      logger.info("In responseFilter block---------");
      if (!request.getBoolean("search")) {
        return new JsonObject().put("Error", "Count is not supported with filtering");
      }
      if (request.containsKey("attrs")) {
        JsonArray sourceFilter = request.getJsonArray("attrs");
        elasticQuery.put(SOURCE_FILTER_KEY, sourceFilter);
      } else {
        return new JsonObject().put("Error", "Missing/Invalid responseFilter parameters");
      }
    }

    elasticQuery.put(QUERY_KEY,
        new JsonObject().put(BOOL_KEY, new JsonObject().put(FILTER_KEY, filterQuery)));

    return elasticQuery;
  }

}

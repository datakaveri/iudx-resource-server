package iudx.resource.server.database;

import com.fasterxml.jackson.core.JsonParseException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.units.qual.A;
import org.elasticsearch.client.*;

import java.io.IOException;

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


  public DatabaseServiceImpl(RestClient client){
    this.client = client;
  }
  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {
    Request elasticRequest;
    logger.info("JRequest @ Service: "+request.toString());
    try{
      if(!request.containsKey("id")){
        handler.handle(Future.failedFuture("No id found"));
        logger.info("Here");
      }
      if(request.getJsonArray("id").isEmpty()){
        handler.handle(Future.failedFuture("resource-id is empty"));
        return null;
      }
      if(!request.containsKey("search-type")) {
        handler.handle(Future.failedFuture("No search-type found"));
        return null;
      }
      String resourceGroup = "";// request.getJsonArray("id").getString(0).split("/")[3];
      if(request.getBoolean("isTest")){
        elasticRequest = new Request("GET",VARANASI_TEST_SEARCH_INDEX);
      }else if("varanasi-swm-vehicles".equalsIgnoreCase(resourceGroup)){
        elasticRequest = new Request("GET",VARANASI_SWM_VEHICLES_SEARCH_INDEX);
      }else{
        elasticRequest = new Request("GET",VARANASI_OTHER_SEARCH_INDEX);
      }
      query = queryDecoder(request);
      if(query == null){
        handler.handle(Future.failedFuture("Query returned was null"));
        return null;
      }
      logger.info("Query constructed: " + query.toString());
      elasticRequest.setJsonEntity(query.toString());
      Cancellable cancellable = client.performRequestAsync(elasticRequest, new ResponseListener() {
        @Override
        public void onSuccess(Response response) {
          logger.info("Successful DB request");
	  JsonArray dbresponse = new JsonArray();
          try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200 && statusCode != 204){
              handler.handle(Future.failedFuture("Status code is not 2xx"));
            }
            JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
            JsonArray responseHits = responseJson.getJsonObject("hits").getJsonArray("hits");
	    for(Object json : responseHits){
	      JsonObject jsonTemp = (JsonObject) json;
	      dbresponse.add(jsonTemp.getJsonObject("_source"));
	    }
            handler.handle(Future.succeededFuture(dbresponse));
          } catch (IOException e) {
            e.printStackTrace();
	    handler.handle(Future.failedFuture(e.getMessage()));
          }
        }
        @Override
        public void onFailure(Exception e) {
          logger.info("DB request has failed");
          handler.handle(Future.failedFuture(e.getMessage()));
        }
      });
    }catch (Exception e){
      logger.info("Database Service<searchQuery>-- "+e.getMessage());
      handler.handle(Future.failedFuture(e.getMessage()));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString("search-type");
    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put("size",10);
    JsonArray id = request.getJsonArray("id");
    JsonArray filterQuery = new JsonArray();

    JsonObject termQuery = new JsonObject().put("terms",
            new JsonObject().put(RESOURCE_ID_KEY+".keyword",id));

    filterQuery.add(termQuery);

    if ("geoSearch".equalsIgnoreCase(searchType)){
      JsonObject shapeJson = new JsonObject() ,geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      if (request.containsKey("lon")
              && request.containsKey("lat")
              && request.containsKey("radius")){
        double lat = Double.parseDouble(request.getString("lat"));
        double lon = Double.parseDouble(request.getString("lon"));
        String radius = request.getString("radius");
        shapeJson.put(SHAPE_KEY,new JsonObject()
                .put(TYPE_KEY,GEO_CIRCLE)
                .put(COORDINATES_KEY, new JsonArray()
                        .add(lon)
                        .add(lat))
                .put(GEO_RADIUS,radius+"m"))
          .put(GEO_RELATION_KEY,"within");
      }
      else if (request.containsKey("geometry")
              && (request.getString("geometry").equalsIgnoreCase("polygon")
              || request.getString("geometry").equalsIgnoreCase("linestring"))
              && request.containsKey("georel")
              && request.containsKey("coordinates")
              && request.containsKey("geoproperty")){
        String geometry = request.getString("geometry");
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        shapeJson.put(SHAPE_KEY,new JsonObject()
                .put(TYPE_KEY,geometry)
                .put(COORDINATES_KEY,coordinates))
          .put(GEO_RELATION_KEY,relation);
      }
      else if (request.containsKey("geometry")
              && request.getString("geometry").equalsIgnoreCase("bbox")
              && request.containsKey("georel")
              && request.containsKey("coordinates")
              && request.containsKey("geoproperty")){
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        shapeJson = new JsonObject();
        shapeJson.put(SHAPE_KEY,new JsonObject()
                .put(TYPE_KEY,GEO_BBOX)
                .put(COORDINATES_KEY,coordinates))
          .put(GEO_RELATION_KEY,relation);

      }else{
        return null;
      }
      geoSearch.put(GEO_SHAPE_KEY, new JsonObject()
        .put(GEO_KEY,shapeJson));
      filterQuery.add(geoSearch);
    }else{
      return null;
    }

    elasticQuery.put(QUERY_KEY, new JsonObject()
            .put(BOOL_KEY,new JsonObject()
                    .put(FILTER_KEY, filterQuery)));

    return elasticQuery;
  }

}

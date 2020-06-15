package iudx.resource.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.client.RestClient;

/**
 * The Database Service.
 * <h1>Database Service</h1>
 * <p>
 * The Database Service in the IUDX Resource Server defines the operations to be
 * performed with the IUDX Database server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface DatabaseService {

  String GEO_KEY = "geoJsonLocation";
  String GEO_CIRCLE = "circle";
  String GEO_BBOX = "envelope";
  String COORDINATES_KEY = "coordinates";
  String GEO_RELATION_KEY = "relation";
  String TYPE_KEY = "type";
  String GEO_SHAPE_KEY = "geo_shape";
  String GEO_RADIUS = "radius";
  String SHAPE_KEY = "shape";
  String QUERY_KEY = "query";
  String FILTER_KEY = "filter";
  String BOOL_KEY = "bool";
  String RESOURCE_ID_KEY = "resource-id";
  String VARANASI_SWM_VEHICLES_SEARCH_INDEX = "varanasi-swm-vehicles/_search";
  String VARANASI_OTHER_SEARCH_INDEX = "varanasi-other/_search";
  String VARANASI_TEST_SEARCH_INDEX = "varanasi/_search";

  /**
   * The searchQuery implements the search operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler);

  /**
   * The countQuery implements the count operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * 
   * @param vertx   which is the vertx instance
   * @param address which is the proxy address
   * @return DatabaseServiceVertxEBProxy which is a service proxy
   */

  @GenIgnore
  static DatabaseService create(RestClient client){
    return new DatabaseServiceImpl(client);
  }

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}

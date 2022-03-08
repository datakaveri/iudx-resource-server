package iudx.resource.server.database.archives;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.elastic.ElasticClient;

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

  /**
   * The searchQuery implements the search operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

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
   * The create implements the count operation with the database.
   * @param client RestClient to perform ES queries.
   * @return DatabaseService object.
   */

  @GenIgnore
  static DatabaseService create(ElasticClient client, String timeLimit) {
    return new DatabaseServiceImpl(client, timeLimit);
  }

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return DatabaseServiceVertxEBProxy which is a service proxy 
   */

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}

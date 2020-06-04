package iudx.resource.server.media;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The Media Service.
 * <h1>Media Service</h1>
 * <p>
 * The Media Service in the IUDX Resource Server defines the operations to be performed
 * with the IUDX Media (or) Video server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface MediaService {

  @Fluent
  MediaService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler);

  @Fluent
  MediaService countQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler);

  @GenIgnore
  static MediaService createProxy(Vertx vertx, String address) {
    return new MediaServiceVertxEBProxy(vertx, address);
  }
}

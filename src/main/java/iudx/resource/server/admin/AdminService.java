package iudx.resource.server.admin;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.callback.CallbackService;

@VertxGen
@ProxyGen
public interface AdminService {

  @Fluent
  AdminService setUniqueResourceAttribute(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  AdminService deleteUniqueResourceAttribute(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @GenIgnore
  static CallbackService createProxy(Vertx vertx, String address) {
    return new AdminServiceVertxEBProxy(vertx, address);
  }
}

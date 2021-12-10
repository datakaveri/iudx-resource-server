package iudx.resource.server.database.postgres;

import java.util.List;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;


@VertxGen
@ProxyGen
public interface PostgresService {

  @Fluent
  PostgresService executeQuery(final String query, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  PostgresService executePreparedQuery(final String query, final List<String> queryparams,
      Handler<AsyncResult<JsonObject>> handler);


  @GenIgnore
  static PostgresService createProxy(Vertx vertx, String address) {
    return new PostgresServiceVertxEBProxy(vertx, address);
  }

}

package iudx.resource.server.client;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface ClientService {

    @GenIgnore
    static ClientService createProxy(Vertx vertx, String address) {
        return new ClientServiceVertxEBProxy(vertx, address);
    }

    @Fluent
    ClientService clientRegistration(JsonObject request, Handler<AsyncResult<JsonObject>> handler);



}

package iudx.resource.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Latest Data Service.
 * <h1>Latest Data Service</h1>
 * <p>
 * The Latest Data Service in the IUDX Resource Server retrieves the latest data belonging to an ID.
 * </p>
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface LatestDataService {

    /**
     * The getLatestData retrieves the latest data.
     *
     * @param request which is a JsonObject
     * @param handler which is a Request Handler
     * @return LatestDataService which is a Service
     */

    @Fluent
    LatestDataService getLatestData(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

    @GenIgnore
    static LatestDataService create(RedisClient client, JsonObject attributeList) {
        return new LatestDataServiceImpl(client, attributeList);
    }

    /**
     * The createProxy helps the code generation blocks to generate proxy code.
     * @param vertx which is the vertx instance
     * @param address which is the proxy address
     * @return LatestDataServiceVertxEBProxy which is a service proxy
     */

    @GenIgnore
    static LatestDataService createProxy(Vertx vertx, String address) {
        return new iudx.resource.server.database.LatestDataServiceVertxEBProxy(vertx, address);
    }
}

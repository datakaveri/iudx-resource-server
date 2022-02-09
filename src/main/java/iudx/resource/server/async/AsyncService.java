package iudx.resource.server.async;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Async Service.
 * <h1>Async Service</h1>
 * <p>
 *   The Async Service in the IUDX Resource Server defines the operations to be
 *   performed with the IUDX Async Server.
 * </p>
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2022-02-08
 */

@VertxGen
@ProxyGen
public interface AsyncService {

	/**
	 * The scrollQuery implements the async scroll search with the database.
	 *
	 * @param request which is a JsonObject
	 * @param handler which is a Request Handler
	 * @return AsyncService which is a service
	 */

	@Fluent
	AsyncService scrollQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

	/**
	 * The createProxy helps the code generation blocks to generate proxy code.
	 * @param vertx which is the vertx instance
	 * @param address which is the proxy address
	 * @return AsyncServiceVertxEBProxy which is a service proxy
	 */

	@GenIgnore
	static AsyncService createProxy(Vertx vertx, String address) {
		return new AsyncServiceVertxEBProxy(vertx, address);
	}
}

package iudx.resource.server.metering;

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
public interface MeteringService {

	//save and read
	
//	 @Fluent
//	  MeteringService saveToDataBase(JsonObject request,
//	      Handler<AsyncResult<JsonObject>> handler);
//	
	  @Fluent
	  MeteringService connect(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
	@GenIgnore
	static MeteringService createProxy(Vertx vertx, String address) {
		return new MeteringServiceVertxEBProxy(vertx, address);
	}

	
}

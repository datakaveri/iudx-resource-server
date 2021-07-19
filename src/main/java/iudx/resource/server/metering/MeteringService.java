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
	  MeteringService readFromDatabase( Handler<AsyncResult<JsonObject>> handler);
	  
	  @Fluent
	  MeteringService writeInDatabase(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	  
	  
	  @Fluent
	  MeteringService readWithTime(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	  @Fluent
	  MeteringService readWithEmail(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	  @Fluent
	  MeteringService readWithResourceId(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	  @Fluent
	  MeteringService readWithEmailandTime(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	  @Fluent
	  MeteringService readWithEmailandResourceId(JsonObject request,Handler<AsyncResult<JsonObject>> handler);

	  @Fluent
	  MeteringService readWithTimeandResourceId(JsonObject request,Handler<AsyncResult<JsonObject>> handler);

	  @Fluent
	  MeteringService readWithTimeEmailandResourceId(JsonObject request,Handler<AsyncResult<JsonObject>> handler);
	  
	@GenIgnore
	static MeteringService createProxy(Vertx vertx, String address) {
		return new MeteringServiceVertxEBProxy(vertx, address);
	}


	
}

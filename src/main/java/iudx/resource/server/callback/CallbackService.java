package iudx.resource.server.callback;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * <h1>Callback Service</h1>
 * The Callback Service.
 * <p>
 * The Callback Service in the IUDX Resource Server defines the operations to be
 * performed with the IUDX Callback server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0 
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface CallbackService {

	/**
	 * The getQueueMessage implements for getting message from "callback.data" queue.
	 * 
	 * @param request which is a JsonObject
	 * @param handler which is a Request Handler
	 * @return CallbackService which is a Service
	 */
	@Fluent
	CallbackService getMessageFromCallbackNotificationQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
	
	/**
	 * The getQueueMessage implements for getting message from "callback.notification" queue.
	 * 
	 * @param request which is a JsonObject
	 * @param handler which is a Request Handler
	 * @return CallbackService which is a Service
	 */
	@Fluent
	CallbackService getMessageFromCallbackDataQueue(JsonObject request, Handler<AsyncResult<JsonObject>> handler);
	
	/**
	 * The queryCallBackDataBase implements for the query callBack database.
	 * 
	 * @param request which is a JsonObject
	 * @param handler which is a Request Handler
	 * @return CallbackService which is a Service
	 */
	@Fluent
	CallbackService queryCallBackDataBase(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

	/**
	 * <h1>The sendDataToCallBackSubscriber implements for sending data to callback database
	 * update info.</h1>
	 * 
	 * @param request JSON Object
	 * @param handler Handler to return response in case of success and appropriate
	 *                error message in case of failure.
	 */
	@Fluent
	CallbackService sendDataToCallBackSubscriber(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

	
	
	
}
package iudx.resource.server.callback;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * <h1>Callback Service</h1> The Callback Service.
 * <p>
 * The Callback Service in the IUDX Resource Server defines the operations to be performed with the
 * IUDX Callback server.
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
   * The connectToCallbackNotificationQueue implements for getting message from
   * "callback.notification" queue.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  @Fluent
  Future<JsonObject> connectToCallbackNotificationQueue(JsonObject request);

  /**
   * The connectToCallbackDataQueue implements for getting message from "callback.data" queue.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  @Fluent
  Future<JsonObject> connectToCallbackDataQueue(JsonObject request);

  /**
   * The queryCallBackDataBase implements for the query callBack database.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  @Fluent
  Future<JsonObject> queryCallBackDataBase(JsonObject request);

  /**
   * The sendDataToCallBackSubscriber implements for sending data to callback database update info.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  @Fluent
  Future<JsonObject> sendDataToCallBackSubscriber(JsonObject request);
}

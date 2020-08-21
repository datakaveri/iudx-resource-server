package iudx.resource.server.callback;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * <h1>Callback Service</h1> The Callback Service.
 * <p>
 * The Callback Service in the IUDX Resource Server defines the operations to be performed with the
 * IUDX Callback server.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public interface CallbackService {

  /**
   * The connectToCallbackNotificationQueue implements for getting message from
   * "callback.notification" queue.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  Future<JsonObject> connectToCallbackNotificationQueue(JsonObject request);

  /**
   * The connectToCallbackDataQueue implements for getting message from "callback.data" queue.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  Future<JsonObject> connectToCallbackDataQueue(JsonObject request);

  /**
   * The queryCallBackDataBase implements for the query callBack database.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  Future<JsonObject> queryCallBackDataBase(JsonObject request);

  /**
   * The sendDataToCallBackSubscriber implements for sending data to callback database update info.
   * 
   * @param json containing queueName.
   * @return Future object
   */
  Future<JsonObject> sendDataToCallBackSubscriber(JsonObject request);
}

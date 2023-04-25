package iudx.resource.server.callback;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 *
 *
 * <h1>Callback Service</h1>
 *
 * The Callback Service.
 *
 * <p>The Callback Service in the IUDX Resource Server defines the operations to be performed with
 * the IUDX Callback server.
 *
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
   * @param request containing queueName.
   * @return CallbackService which is a Service
   */
  @Fluent
  CallbackService connectToCallbackNotificationQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The connectToCallbackDataQueue implements for getting message from "callback.data" queue.
   *
   * @param request containing queueName.
   * @return CallbackService which is a Service
   */
  @Fluent
  CallbackService connectToCallbackDataQueue(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The queryCallBackDataBase implements for the query callBack database.
   *
   * @param request containing queueName.
   * @return CallbackService which is a Service
   */
  @Fluent
  CallbackService queryCallBackDataBase(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The sendDataToCallBackSubscriber implements for sending data to callback database update info.
   *
   * @param request containing queueName.
   * @return CallbackService which is a Service
   */
  @Fluent
  CallbackService sendDataToCallBackSubscriber(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @GenIgnore
  static CallbackService createProxy(Vertx vertx, String address) {
    return new CallbackServiceVertxEBProxy(vertx, address);
  }
}

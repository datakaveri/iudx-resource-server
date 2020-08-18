package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * interface to define all subscription related operation.
 *
 */
public interface Subscription {
  /**
   * get a subscription by id.
   * 
   * @param json json containing subscription id.
   * @return Future object
   */
  Future<JsonObject> get(JsonObject json);

  /**
   * create a subscription.
   * 
   * @param subscription subscription json.
   * @return Future object
   */
  Future<JsonObject> create(JsonObject subscription);

  /**
   * update a subscription.
   * 
   * @param subscription subscription body
   * @return
   */
  Future<JsonObject> update(JsonObject subscription);

  /**
   * append a subscription with new values.
   * 
   * @param subscription subscription vlaues to be updated
   * @return Future object
   */
  Future<JsonObject> append(JsonObject subscription);

  /**
   * delete a subscription request.
   * 
   * @param json json containing id for sub to delete
   * @return Future object
   */
  Future<JsonObject> delete(JsonObject json);

}

package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class containing methods to create callback subscriptions in system.
 * 
 *
 */
public class CallbackSuscription implements Subscription {

  JsonObject json = new JsonObject();

  {
    json.put(Constants.JSON_TYPE, "501");
    json.put(Constants.JSON_TITLE, "not implemented yet..");
    json.put(Constants.JSON_DETAIL, "specified endpoint for callback not implemented yet..");
  }
  private DataBrokerService databroker;
  private DatabaseService dbService;

  public CallbackSuscription(DataBrokerService databroker, DatabaseService dbService) {
    this.databroker = databroker;
    this.dbService = dbService;
  }

  /**
   * create a callback subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> create(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.registerCallbackSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * update a callback subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> update(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.updateCallbackSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * append a callback subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> append(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    promise.complete(json);
    return promise.future();
  }

  /**
   * delete a callback subscription.
   * 
   * {@inheritDoc}
   * 
   */
  @Override
  public Future<JsonObject> delete(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.deleteStreamingSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * get a callback subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> get(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.listCallbackSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

}

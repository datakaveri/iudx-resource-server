package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class containing methods to create a streaming subscription in system.
 * 
 */
public class StreamingSubscription implements Subscription {

  JsonObject json = new JsonObject();

  {
    json.put(Constants.JSON_TYPE, "501");
    json.put(Constants.JSON_TITLE, "not implemented yet..");
    json.put(Constants.JSON_DETAIL, "specified endpoint for streaming sub not implemented yet..");
  }

  private DataBrokerService databroker;
  private DatabaseService dbService;

  public StreamingSubscription(DataBrokerService databroker, DatabaseService dbService) {
    this.databroker = databroker;
    this.dbService = dbService;
  }

  /**
   * create a streaming subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> create(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.registerStreamingSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * update a streaming subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> update(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.updateStreamingSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * append a streaming subscription.
   * 
   * {@inheritDoc}}
   */
  @Override
  public Future<JsonObject> append(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    promise.complete(json);
    return promise.future();
  }

  /**
   * delete a streaming subscription.
   * 
   * {@inheritDoc}}
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
   * get a streaming subscription.
   * 
   * {@inheritDoc}
   */
  @Override
  public Future<JsonObject> get(JsonObject subscription) {
    Promise<JsonObject> promise = Promise.promise();
    databroker.listStreamingSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

}

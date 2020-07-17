package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class containing methods to create callback subscriptions in system.
 * 
 *
 */
public class CallbackSubscription implements Subscription {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(CallbackSubscription.class);

  //TODO : delete when all methods are implemented
  JsonObject json = new JsonObject();
  {
    json.put(Constants.JSON_TYPE, "501");
    json.put(Constants.JSON_TITLE, "not implemented yet..");
    json.put(Constants.JSON_DETAIL, "specified endpoint for callback not implemented yet..");
  }
  private DataBrokerService databroker;
  private DatabaseService dbService;

  public CallbackSubscription(DataBrokerService databroker, DatabaseService dbService) {
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
    LOGGER.info("callback create() method started");
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
    LOGGER.info("callback update() method started");
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
    LOGGER.info("callback append() method started");
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
    LOGGER.info("callback delete() method started");
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
    LOGGER.info("callback get() method started");
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

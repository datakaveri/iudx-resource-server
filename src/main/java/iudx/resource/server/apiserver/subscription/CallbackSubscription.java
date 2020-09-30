package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import iudx.resource.server.database.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

/**
 * class containing methods to create callback subscriptions in system.
 * 
 *
 */
public class CallbackSubscription implements Subscription {

  private static final Logger LOGGER = LogManager.getLogger(CallbackSubscription.class);

  private DataBrokerService databroker;
  private DatabaseService dbService;

  private static volatile CallbackSubscription instance = null;

  private CallbackSubscription(DataBrokerService databroker, DatabaseService dbService) {
    this.databroker = databroker;
    this.dbService = dbService;
  }

  public static CallbackSubscription getInstance(DataBrokerService databroker,
      DatabaseService dbService) {
    if (instance == null) {
      synchronized (CallbackSubscription.class) {
        if (instance == null)
          instance = new CallbackSubscription(databroker, dbService);
      }
    }
    return instance;
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
        promise.fail(handler.cause().getMessage());
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
        promise.fail(handler.cause().getMessage());
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
    databroker.updateCallbackSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause().getMessage());
      }
    });
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
        promise.fail(handler.cause().getMessage());
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
        promise.fail(handler.cause().getMessage());
      }
    });
    return promise.future();
  }

}

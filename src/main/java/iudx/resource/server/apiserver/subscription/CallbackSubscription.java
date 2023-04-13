package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** class containing methods to create callback subscriptions in system. */
public class CallbackSubscription implements Subscription {

  private static final Logger LOGGER = LogManager.getLogger(CallbackSubscription.class);

  private DataBrokerService databroker;

  public CallbackSubscription(DataBrokerService databroker) {
    this.databroker = databroker;
  }

  /**
   * create a callback subscription.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> create(JsonObject subscription) {
    LOGGER.info("callback create() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.registerCallbackSubscription(
        subscription,
        handler -> {
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
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> update(JsonObject subscription) {
    LOGGER.info("callback update() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.updateCallbackSubscription(
        subscription,
        handler -> {
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
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> append(JsonObject subscription) {
    LOGGER.info("callback append() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.updateCallbackSubscription(
        subscription,
        handler -> {
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
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> delete(JsonObject subscription) {
    LOGGER.info("callback delete() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.deleteStreamingSubscription(
        subscription,
        handler -> {
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
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> get(JsonObject subscription) {
    LOGGER.info("callback get() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.listCallbackSubscription(
        subscription,
        handler -> {
          if (handler.succeeded()) {
            promise.complete(handler.result());
          } else {
            promise.fail(handler.cause().getMessage());
          }
        });
    return promise.future();
  }
}

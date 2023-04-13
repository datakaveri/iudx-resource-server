package iudx.resource.server.apiserver.subscription;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** class containing methods to create a streaming subscription in system. */
public class StreamingSubscription implements Subscription {

  private static final Logger LOGGER = LogManager.getLogger(StreamingSubscription.class);

  private DataBrokerService databroker;

  public StreamingSubscription(DataBrokerService databroker) {
    this.databroker = databroker;
  }

  //  public static StreamingSubscription getInstance(DataBrokerService databroker,
  //      PostgresService pgService) {
  //    if (instance == null) {
  //      synchronized (StreamingSubscription.class) {
  //        if (instance == null)
  //          instance = new StreamingSubscription(databroker, pgService);
  //      }
  //    }
  //    return instance;
  //  }

  /**
   * create a streaming subscription.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> create(JsonObject subscription) {
    LOGGER.info("streaming create() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.registerStreamingSubscription(
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
   * update a streaming subscription.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> update(JsonObject subscription) {
    LOGGER.info("streaming update() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.updateStreamingSubscription(
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
   * append a streaming subscription.
   *
   * <p>{@inheritDoc}}
   */
  @Override
  public Future<JsonObject> append(JsonObject subscription) {
    LOGGER.info("streaming append() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.appendStreamingSubscription(
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
   * delete a streaming subscription.
   *
   * <p>{@inheritDoc}}
   */
  @Override
  public Future<JsonObject> delete(JsonObject subscription) {
    LOGGER.info("streaming delete() method started");
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
   * get a streaming subscription.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public Future<JsonObject> get(JsonObject subscription) {
    LOGGER.info("streaming get() method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscription.getString(Constants.SUBSCRIPTION_ID));
    databroker.listStreamingSubscription(
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

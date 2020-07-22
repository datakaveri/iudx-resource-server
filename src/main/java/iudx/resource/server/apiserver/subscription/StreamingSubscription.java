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
 * class containing methods to create a streaming subscription in system.
 * 
 */
public class StreamingSubscription implements Subscription {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamingSubscription.class);

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
    LOGGER.info("streaming create() method started");
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
    LOGGER.info("streaming update() method started");
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
    LOGGER.info("streaming append() method started");
    Promise<JsonObject> promise = Promise.promise();
    databroker.appendStreamingSubscription(subscription, handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail(handler.cause());
      }
    });
    return promise.future();
  }

  /**
   * delete a streaming subscription.
   * 
   * {@inheritDoc}}
   */
  @Override
  public Future<JsonObject> delete(JsonObject subscription) {
    LOGGER.info("streaming delete() method started");
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
    LOGGER.info("streaming get() method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.info("sub id :: " + subscription.getString(Constants.SUBSCRIPTION_ID));
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

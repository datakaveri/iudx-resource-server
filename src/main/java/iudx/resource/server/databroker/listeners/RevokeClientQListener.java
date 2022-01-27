package iudx.resource.server.databroker.listeners;

import static iudx.resource.server.common.Constants.TOKEN_INVALID_Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import iudx.resource.server.cache.CacheService;
import iudx.resource.server.cache.cacheImpl.CacheType;

public class RevokeClientQListener implements RMQListeners {

  private static final Logger LOGGER = LogManager.getLogger(RevokeClientQListener.class);

  private final RabbitMQClient client;
  private final CacheService cache;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public RevokeClientQListener(RabbitMQClient client, CacheService cache) {
    this.client = client;
    this.cache = cache;
  }


  @Override
  public void start() {
    client
        .start()
        .onSuccess(handler -> {
          LOGGER.trace("starting Q listener for revoked clients");
          client.basicConsumer(TOKEN_INVALID_Q, options, rmqConsumer -> {
            if (rmqConsumer.succeeded()) {
              RabbitMQConsumer mqConsumer = rmqConsumer.result();
              mqConsumer.handler(message -> {
                Buffer body = message.body();
                if (body != null) {
                  JsonObject invalidClientJson = new JsonObject(body);
                  LOGGER.debug("received message from revoked-client Q :" + invalidClientJson);
                  String key = invalidClientJson.getString("sub");
                  String value = invalidClientJson.getString("expiry");
                  LOGGER.info("message received from RMQ : " + invalidClientJson);
                  JsonObject cacheJson = new JsonObject();
                  cacheJson.put("type", CacheType.REVOKED_CLIENT);
                  cacheJson.put("key", key);
                  cacheJson.put("value", value);

                  cache.refresh(cacheJson, cacheHandler -> {
                    if (cacheHandler.succeeded()) {
                      LOGGER.debug("revoked client message published to Cache Verticle");
                    } else {
                      LOGGER.debug("revoked client message published to Cache Verticle fail");
                    }
                  });
                } else {
                  LOGGER.error("Empty json received from revoke_token queue");
                }
              });
            }
          });
        })
        .onFailure(handler -> {
          LOGGER.error("Rabbit client startup failed.");
        });
  }

}

package iudx.resource.server.cache;

import static iudx.resource.server.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.cache.cacheImpl.CatalogueCacheImpl;
import iudx.resource.server.database.postgres.PostgresService;

public class CacheVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);

  private MessageConsumer<JsonObject> consumer;
  private ServiceBinder binder;

  private CacheService cacheService;
  private PostgresService pgService;
  private CatalogueCacheImpl catalogueCache;

  @Override
  public void start() throws Exception {

    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);
    catalogueCache = new CatalogueCacheImpl(vertx, config());
    cacheService = new CacheServiceImpl(vertx, pgService, catalogueCache);

    binder = new ServiceBinder(vertx);
    consumer = binder.setAddress(CACHE_SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

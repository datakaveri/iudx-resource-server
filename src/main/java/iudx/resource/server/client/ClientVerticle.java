package iudx.resource.server.client;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.database.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.common.Constants.PG_SERVICE_ADDRESS;

public class ClientVerticle extends AbstractVerticle {

    private static final String CLIENT_SERVICE_ADDRESS = "iudx.rs.client.service";
    private static final Logger LOGGER = LogManager.getLogger(ClientVerticle.class);
    private ClientService clientService;
    private ServiceBinder binder;
    private MessageConsumer<JsonObject> consumer;
    private PostgresService postgresService;

    @Override
    public void start() throws Exception {
        binder = new ServiceBinder(vertx);
        postgresService = PostgresService.createProxy(vertx,PG_SERVICE_ADDRESS);
        clientService  = new ClientServiceImpl(vertx, postgresService);
        consumer = binder.setAddress(CLIENT_SERVICE_ADDRESS).register(ClientService.class, clientService);
        LOGGER.info("Client Verticle deployed");
    }
    @Override
    public void stop() {
        binder.unregister(consumer);
    }
}

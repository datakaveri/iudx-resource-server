package iudx.resource.server.client;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;

import static iudx.resource.server.common.Constants.BROKER_SERVICE_ADDRESS;

public class ClientServiceImpl implements ClientService {
    private final Vertx vertx;
    private PostgresService postgresService;
    public static DataBrokerService rmqService;

    public ClientServiceImpl(Vertx vertx, PostgresService postgresService) {
        this.vertx = vertx;
        this.postgresService = postgresService;
        this.rmqService = DataBrokerService.createProxy(vertx, BROKER_SERVICE_ADDRESS);
    }

    @Override
    public ClientService clientRegistration(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
        return null;
    }
}

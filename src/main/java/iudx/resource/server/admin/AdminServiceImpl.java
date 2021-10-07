package iudx.resource.server.admin;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.PostgresClient;

public class AdminServiceImpl implements AdminService {

  private DatabaseService databaseService;

  public AdminServiceImpl(PostgresClient client) {
    this.databaseService = new DatabaseService(client);
  }

  @Override
  public AdminService setUniqueResourceAttribute(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.setUniqueAttribute(request)
        .onSuccess(ar -> {
          handler.handle(Future.succeededFuture(ar));
        })
        .onFailure(ar -> {
          handler.handle(Future.failedFuture(ar.getLocalizedMessage()));
        });
    return this;
  }

  @Override
  public AdminService deleteUniqueResourceAttribute(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.deleteUniqueAttribute(request)
        .onSuccess(ar -> {
          handler.handle(Future.succeededFuture(ar));
        })
        .onFailure(ar -> {
          handler.handle(Future.failedFuture(ar.getLocalizedMessage()));
        });
    return this;
  }
}

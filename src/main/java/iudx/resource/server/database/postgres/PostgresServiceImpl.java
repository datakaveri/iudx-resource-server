package iudx.resource.server.database.postgres;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;

public final class PostgresServiceImpl implements PostgresService {

  private final PgPool client;


  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public PostgresService executeQuery(String query, Handler<AsyncResult<JsonObject>> handler) {
    client
        .withConnection(connection -> connection.query(query).execute())
        .onSuccess(successHandler -> {
          handler.handle(Future.succeededFuture());
        })
        .onFailure(failureHandler -> {
          Future.failedFuture(failureHandler);
        });
    return this;
  }

}

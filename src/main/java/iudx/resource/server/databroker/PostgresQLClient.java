package iudx.resource.server.databroker;

import java.awt.event.FocusEvent.Cause;
import java.util.List;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PostgresQLClient {

  private PgPool pgclient;

  public PostgresQLClient(Vertx vertx, PgConnectOptions pgConnectOptions,
      PoolOptions connectionPoolOptions) {
    this.pgclient = PgPool.pool(vertx, pgConnectOptions, connectionPoolOptions);
  }


  public Future<RowSet<Row>> executeAsync(String preparedQuerySQL, List<Object> args) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgclient.preparedQuery(preparedQuerySQL).execute(Tuple.of(args.toArray()), executeHandler -> {
      if (executeHandler.succeeded()) {
        promise.complete(executeHandler.result());
      } else {
        promise.fail(executeHandler.cause());
      }
    });
    return promise.future();
  }

  public Future<RowSet<Row>> executeAsync(String preparedQuerySQL, Object... args) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgclient.preparedQuery(preparedQuerySQL).execute(Tuple.of(args), executeHandler -> {
      if (executeHandler.succeeded()) {
        promise.complete(executeHandler.result());
      } else {
        promise.fail(executeHandler.cause());
      }
    });
    return promise.future();
  }

  public Future<RowSet<Row>> executeAsync(String preparedQuerySQL) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgclient.preparedQuery(preparedQuerySQL).execute(executeHandler -> {
      if (executeHandler.succeeded()) {
        promise.complete(executeHandler.result());
      } else {
        promise.fail(executeHandler.cause());
      }
    });
    return promise.future();
  }

}

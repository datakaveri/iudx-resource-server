package iudx.resource.server.metering;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

public class MeteringServiceImpl implements MeteringService {

	private static final Logger LOGGER = LogManager.getLogger(MeteringServiceImpl.class);

	private Vertx vertx;
	private String databaseIP;
	private int databasePort;
	private String databaseName;
	private String databaseUserName;
	private String databasePassword;
	private int databasePoolSize;
	PgConnectOptions connectOptions;
	PoolOptions poolOptions;
	PgPool pool;
	SqlClient client;

	public MeteringServiceImpl(JsonObject propObj, Vertx vertxInstance) {

		LOGGER.info("inside MerteringService");
		if (propObj != null && !propObj.isEmpty()) {
			databaseIP = propObj.getString("meteringDatabaseIP");
			databasePort = propObj.getInteger("meteringDatabasePort");
			databaseName = propObj.getString("meteringDatabaseName");
			databaseUserName = propObj.getString("meteringDatabaseUserName");
			databasePassword = propObj.getString("meteringDatabasePassword");
			databasePoolSize = propObj.getInteger("meteringpoolSize");
		}

		this.connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP).setDatabase(databaseName)
				.setUser(databaseUserName).setPassword(databasePassword);

		this.poolOptions = new PoolOptions().setMaxSize(databasePoolSize);

		this.pool = PgPool.pool(vertxInstance, connectOptions, poolOptions);
//		this.client=PgPool.client(vertxInstance, connectOptions, poolOptions);
		System.out.println(propObj);
		this.vertx = vertxInstance;

		LOGGER.info("IP: " + databaseIP);
		LOGGER.info("Port: " + databasePort);
		LOGGER.info("database: " + databaseName);
		LOGGER.info("user: " + databaseUserName);
		LOGGER.info("pass: " + databasePassword);

	}

	@Override
	public MeteringService readFromDatabase(Handler<AsyncResult<JsonObject>> handler) {

		Future<JsonObject> result = readFromDatabase();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readFromDatabase() {
		LOGGER.debug("Trying to connect");
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		pool.getConnection().compose(connection -> connection.query("SELECT * FROM immudbtest ").execute())
				.onComplete(rows -> {
					if (rows.succeeded()) {
						RowSet<Row> result = rows.result();
						print(result);

						promise.complete();
					} else {

						LOGGER.info("Something went wrong " + rows.cause().getMessage());
					}
				});
		return promise.future();

	}

	private void print(RowSet<Row> result) {
		for (Row rs : result) {
			LOGGER.debug("TIME: " + (rs.getLong("(metering.immudbtest.time)")));
			LOGGER.debug("Email: " + rs.getString("(metering.immudbtest.email)"));
			LOGGER.debug("Api: " + rs.getString("(metering.immudbtest.api)"));
			LOGGER.debug("Resource: " + rs.getString("(metering.immudbtest.resource)"));
		}

	}

	@Override
	public MeteringService writeInDatabase(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

		Future<JsonObject> result = writeInDatabase(request);
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;

	}

	private Future<JsonObject> writeInDatabase(JsonObject request) {
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		int time = 3;
		String email = ",'email@test3'";
		String api = ",'api@test3'";
		String resource = ",'resource@test3'";
		LOGGER.debug("Trying to write");
		pool.getConnection().compose(connection -> connection.query(
				"UPSERT INTO immudbtest (time,email,api,resource) VALUES (" + time + email + api + resource + ")")
				.execute()).onComplete(rows -> {
					RowSet<Row> result = rows.result();
					LOGGER.info("Insert users, now the number of users is " + result.size());
					promise.complete();
				});
		return promise.future();

	}

	@Override
	public MeteringService readWithEmailandTime(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

		Future<JsonObject> result = readWithEmailandTime();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithEmailandTime() {

		LOGGER.debug("Trying to readWithEmailandTime");
		String email = "'email@test'";
		int starttime = 1;
		int endtime = 10;
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection().compose(connection -> connection.query("SELECT count() FROM immudbtest where email="
				+ email + " and time>=" + starttime + " and time<=" + endtime).execute()).onComplete(rows -> {
					RowSet<Row> result = rows.result();

//					print(result);
					LOGGER.info("Count for given email and time: " + result.size());
					promise.complete();
				});
		return promise.future();
	}

	@Override
	public MeteringService readWithTime(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithTime();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;

	}

	private Future<JsonObject> readWithTime() {

		LOGGER.debug("Trying to readWithTime");

		int starttime = 1;
		int endtime = 10;
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection()
				.compose(connection -> connection
						.query("SELECT * FROM immudbtest where time>=" + starttime + " and time<=" + endtime).execute())
				.onComplete(rows -> {
					if (rows.succeeded()) {
						RowSet<Row> result = rows.result();
						LOGGER.info("count for given time interval: " + result.size());

						promise.complete();
					} else {

						LOGGER.info("Something went wrong " + rows.cause().getMessage());
					}
				});
		return promise.future();

	}

	@Override
	public MeteringService readWithEmail(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithEmail();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithEmail() {
		LOGGER.debug("Trying to readWithEmail");

		String email = "'public.data@iudx.org'";
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection()
				.compose(
						connection -> connection.query("SELECT count() FROM immudbtest where email=" + email).execute())
				.onComplete(rows -> {
					RowSet<Row> result = rows.result();
//					print(result);
					LOGGER.info("Count for given email: " + result.size());
					promise.complete();
				});
		return promise.future();
	}

	@Override
	public MeteringService readWithResourceId(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithResourceId();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithResourceId() {
		LOGGER.debug("Trying to readWithResourceId");

		String resourceId = "'89a36273d77dac4cf38114fca1bbe64392547f86'";
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection().compose(
				connection -> connection.query("SELECT count() FROM immudbtest where resource=" + resourceId).execute())
				.onComplete(rows -> {
					RowSet<Row> result = rows.result();
//					print(result);
					LOGGER.info("Count for given resourceId: " + result.size());
					promise.complete();
				});
		return promise.future();
	}

	@Override
	public MeteringService readWithEmailandResourceId(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithEmailandResourceId();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithEmailandResourceId() {
		LOGGER.debug("Trying to readWithEmailandResourceId");

		String resourceId = "'89a36273d77dac4cf38114fca1bbe64392547f86'";
		String email = "'public.data@iudx.org'";
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection()
				.compose(connection -> connection
						.query("SELECT count() FROM immudbtest where resource=" + resourceId + " and email=" + email)
						.execute())
				.onComplete(rows -> {
					RowSet<Row> result = rows.result();
//					print(result);
					LOGGER.info("Count for given resourceId and email : " + result.size());
					promise.complete();
				});
		return promise.future();
	}

	@Override
	public MeteringService readWithTimeandResourceId(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithTimeandResourceId();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithTimeandResourceId() {
		LOGGER.debug("Trying to readWithTimeandResourceId");

		String resourceId = "'resource@test'";
		int starttime = 1;
		int endtime = 10;
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection().compose(connection -> connection.query("SELECT count() FROM immudbtest where resource="
				+ resourceId + " and time>=" + starttime + " and time<=" + endtime).execute()).onComplete(rows -> {
					RowSet<Row> result = rows.result();
//					print(result);
					LOGGER.info("Count for given resourceId and time interval : " + result.size());
					promise.complete();
				});
		return promise.future();
	}

	@Override
	public MeteringService readWithTimeEmailandResourceId(JsonObject request,
			Handler<AsyncResult<JsonObject>> handler) {
		Future<JsonObject> result = readWithTimeEmailandResourceId();
		result.onComplete(resultHandler -> {
			if (resultHandler.succeeded()) {
				handler.handle(Future.succeededFuture(resultHandler.result()));
			} else if (resultHandler.failed()) {
				LOGGER.error("failed ::" + resultHandler.cause());
				handler.handle(Future.failedFuture(resultHandler.cause().getMessage()));
			}
		});

		return this;
	}

	private Future<JsonObject> readWithTimeEmailandResourceId() {
		LOGGER.debug("Trying to readWithTimeEmailandResourceId");

		String resourceId = "'resource@test'";
		int starttime = 1;
		int endtime = 10;
		String email = "'api@test'";
		Promise<JsonObject> promise = Promise.promise();
		pool.getConnection()
				.compose(connection -> connection.query("SELECT count() FROM immudbtest where resource=" + resourceId
						+ " and time>=" + starttime + " and time<=" + endtime + " and email=" + email).execute())
				.onComplete(rows -> {
					RowSet<Row> result = rows.result();
//					print(result);
					LOGGER.info("Count for given resourceId,email and time interval : " + result.size());
					promise.complete();
				});
		return promise.future();
	}

}

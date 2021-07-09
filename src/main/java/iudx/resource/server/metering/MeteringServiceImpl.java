package iudx.resource.server.metering;

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

		LOGGER.info("inside merteringService");
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

		System.out.println(propObj);
		this.vertx = vertxInstance;

		LOGGER.info("IP: " + databaseIP);
		LOGGER.info("Port: " + databasePort);
		LOGGER.info("database: " + databaseName);
		LOGGER.info("user: " + databaseUserName);
		LOGGER.info("pass: " + databasePassword);

	}

	@Override
	public MeteringService connect(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

		Future<JsonObject> result = connect();
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

	private Future<JsonObject> connect() {
		LOGGER.debug("Trying to connect");
		JsonObject response = new JsonObject();
		Promise<JsonObject> promise = Promise.promise();

		pool.getConnection().compose(conn -> {
			System.out.println("Got a connection from the pool");

			// All operations execute on the same connection
			return conn.query("SELECT * FROM immudbtest").execute()
					.compose(res -> conn.query("SELECT * FROM immudbtest").execute()).onComplete(ar -> {
						// Release the connection to the pool
						conn.close();
					});
		}).onComplete(rows -> {
			if (rows.succeeded()) {
				RowSet<Row> result = rows.result();
				for (Row rs : result) {
					LOGGER.debug("TIME: " + (rs.getLong("(metering.immudbtest.time)")));
					LOGGER.debug("Email: " + rs.getString("(metering.immudbtest.email)"));
					LOGGER.debug("Api: " + rs.getString("(metering.immudbtest.api)"));
					LOGGER.debug("Resource: " + rs.getString("(metering.immudbtest.resource)"));
					response.put("email", rs.getString("(metering.immudbtest.email)"));
					response.put("api", rs.getString("(metering.immudbtest.api)"));
					response.put("resourceId", rs.getString("(metering.immudbtest.resource)"));
					response.put("time", (rs.getLong("(metering.immudbtest.time)")));
				}
				promise.complete(response);

			} else {

				LOGGER.info("Something went wrong " + rows.cause().getMessage());
			}
		});

		return promise.future();
	}

}

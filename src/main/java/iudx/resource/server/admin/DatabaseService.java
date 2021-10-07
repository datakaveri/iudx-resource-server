package iudx.resource.server.admin;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.databroker.PostgresClient;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.time.OffsetDateTime;

import static iudx.resource.server.admin.util.Constants.*;

public class DatabaseService {

  private static final Logger logger = LogManager.getLogger(DatabaseService.class);
  private PostgresClient postgresClient;

  public DatabaseService(PostgresClient client) {
    this.postgresClient = client;
  }

  public Future<JsonObject> setUniqueAttribute(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();

    String id = request.getString("id");
    String uniqueAttr = request.getString("uniqueAttribute");
    OffsetDateTime time = OffsetDateTime.now();
    String query = INSERT_UNIQUE_ATTRIBUTE.replace("$1", id)
        .replace("$2", uniqueAttr)
        .replace("$3", time.toString())
        .replace("$4", time.toString());
    JsonObject response = new JsonObject()
        .put("attr", uniqueAttr)
        .put("id", id);

    postgresClient.executeAsync(query)
        .onSuccess(ar -> {
          logger.debug("Inserted unique Attribute");
          promise.complete(response);
        })
        .onFailure(ar -> {
          logger.error("Could not set unique Attribute due to: {}", ar.getCause());
          promise.fail(ar.getCause());
        });
    return promise.future();
  }

  public Future<JsonObject> deleteUniqueAttribute(JsonObject request) {
    Promise<JsonObject> promise = Promise.promise();
    String id = request.getString("id");
    JsonObject response = new JsonObject()
        .put("id", id);

    String query = DELETE_UNIQUE_ATTRIBUTE.replace("$1", id);
    postgresClient.executeAsync(query)
        .onSuccess(ar -> {
          logger.debug("Deleted unique Attribute");
          promise.complete(response);
        })
        .onFailure(ar -> {
          logger.error("Could not set unique Attribute due to: {}", ar.getCause());
          promise.fail(ar.getCause());
        });
    return promise.future();
  }
}

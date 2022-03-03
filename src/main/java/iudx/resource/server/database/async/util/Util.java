package iudx.resource.server.database.async.util;

import static iudx.resource.server.database.archives.Constants.EMPTY_RESOURCE_ID;
import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.ID_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.SEARCHTYPE_NOT_FOUND;
import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static iudx.resource.server.database.postgres.Constants.INSERT_S3_PENDING_SQL;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.postgres.PostgresService;

public class Util {

  private static final Logger LOGGER = LogManager.getLogger(Util.class);
  private final PostgresService pgService;

  public Util(PostgresService pgService) {
    this.pgService = pgService;
  }

  public Future<Void> writeToDB(StringBuilder query) {
    Promise<Void> promise = Promise.promise();

    pgService.executeQuery(
        query.toString(),
        pgHandler -> {
          if (pgHandler.succeeded()) {
            promise.complete();
          } else {
            LOGGER.error("op on DB failed");
            promise.fail("operation fail");
          }
        });
    return promise.future();
  }

  public Future<Void> writeToDB(String searchID, String requestID, String sub) {

    StringBuilder query =
        new StringBuilder(
            INSERT_S3_PENDING_SQL
                .replace("$1", UUID.randomUUID().toString())
                .replace("$2", searchID)
                .replace("$3", requestID)
                .replace("$4", sub)
                .replace("$5", QueryProgress.IN_PROGRESS.toString()));

    return writeToDB(query);
  }

  public boolean isValidQuery(JsonObject query) {
    if (!query.containsKey(ID)) {
      LOGGER.debug("Info: " + ID_NOT_FOUND);
      return false;
    }
    if (query.getJsonArray(ID).isEmpty()) {
      LOGGER.debug("Info: " + EMPTY_RESOURCE_ID);
      return false;
    }
    if (!query.containsKey(SEARCH_TYPE)) {
      LOGGER.debug("Info: " + SEARCHTYPE_NOT_FOUND);
      return false;
    }
    if (query.getJsonArray(ID).getString(0).split("/").length != 5) {
      LOGGER.error("Malformed ID: " + query.getJsonArray(ID).getString(0));
      return false;
    }

    return true;
  }
}

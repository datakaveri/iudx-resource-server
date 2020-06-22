package iudx.resource.server.database;

import java.util.logging.Handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Database Service Implementation.
 * <h1>Database Service Implementation</h1>
 * <p>
 * The Database Service implementation in the IUDX Resource Server implements
 * the definitions of the {@link iudx.resource.server.database.DatabaseService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {
//      added for testing.
//      JsonArray jsonArray = new JsonArray();
//      handler.handle(Future.succeededFuture(jsonArray));
    return this;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {
    JsonObject elasticQuery = new JsonObject();
    return elasticQuery;
  }

}

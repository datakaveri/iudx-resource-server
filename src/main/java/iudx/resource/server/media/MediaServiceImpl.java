package iudx.resource.server.media;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Media Service Implementation.
 * <h1>Media Service Implementation</h1>
 * <p>
 * The Media Service implementation in the IUDX Resource Server implements the
 * definitions of the {@link iudx.resource.server.media.MediaService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class MediaServiceImpl implements MediaService {

  private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

  /**
   * {@inheritDoc}
   */

  @Override
  public MediaService searchQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */

  @Override
  public MediaService countQuery(JsonObject request, Handler<AsyncResult<JsonArray>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {
    JsonObject mediaQuery = new JsonObject();
    return mediaQuery;
  }

}

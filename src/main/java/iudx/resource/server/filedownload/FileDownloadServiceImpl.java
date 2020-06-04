package iudx.resource.server.filedownload;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The File Download Service Implementation.
 * <h1>File Download Service Implementation</h1>
 * <p>
 * The File Download Service implementation in the IUDX Resource Server implements the definitions
 * of the {@link iudx.resource.server.filedownload.FileDownloadService}.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class FileDownloadServiceImpl implements FileDownloadService {

  private static final Logger logger = LoggerFactory.getLogger(FileDownloadServiceImpl.class);

  /**
   * {@inheritDoc}
   */

  @Override
  public FileDownloadService fileDownload(JsonObject request,
      Handler<AsyncResult<JsonArray>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   * 
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */

  public JsonObject queryDecoder(JsonObject request) {
    JsonObject fileDownloadQuery = new JsonObject();
    return fileDownloadQuery;
  }

}

package iudx.resource.server.filedownload;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The File Download Service.
 * <h1>File Download Service</h1>
 * <p>
 * The File Download Service in the IUDX Resource Server defines the operations to be performed with
 * the IUDX File server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface FileDownloadService {

  @Fluent
  FileDownloadService fileDownload(JsonObject request, Handler<AsyncResult<JsonArray>> handler);

  @GenIgnore
  static FileDownloadService createProxy(Vertx vertx, String address) {
    return new FileDownloadServiceVertxEBProxy(vertx, address);
  }
}

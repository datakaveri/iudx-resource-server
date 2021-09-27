package iudx.resource.server.databroker;

import static iudx.resource.server.databroker.util.Constants.REQUEST_DELETE;
import static iudx.resource.server.databroker.util.Constants.REQUEST_GET;
import static iudx.resource.server.databroker.util.Constants.REQUEST_POST;
import static iudx.resource.server.databroker.util.Constants.REQUEST_PUT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class RabbitWebClient {
  private static final Logger LOGGER = LogManager.getLogger(RabbitWebClient.class);

  private WebClient webClient;
  private String username;
  private String password;

  RabbitWebClient(Vertx vertx,WebClientOptions webClientOptions,JsonObject propJson) {
    this.username = propJson.getString("userName");
    this.password = propJson.getString("password");
    this.webClient = getRabbitMQWebClient(vertx, webClientOptions);
  }

  private WebClient getRabbitMQWebClient(Vertx vertx, WebClientOptions webClientOptions) {
    return WebClient.create(vertx, webClientOptions);
  }

  public Future<HttpResponse<Buffer>> requestAsync(String requestType, String url,
      JsonObject requestJson) {
    LOGGER.debug("Info : RabbitMQClientImpl#requestAsync() started");
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    HttpRequest<Buffer> webRequest = createRequest(requestType, url);
    webRequest.sendJsonObject(requestJson, ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        promise.complete(response);
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  public Future<HttpResponse<Buffer>> requestAsync(String requestType, String url) {
    LOGGER.debug("Info : RabbitMQClientImpl#requestAsync() started");
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    HttpRequest<Buffer> webRequest = createRequest(requestType, url);
    webRequest.send(ar -> {
      if (ar.succeeded()) {
        HttpResponse<Buffer> response = ar.result();
        promise.complete(response);
      } else {
        promise.fail(ar.cause());
      }
    });
    return promise.future();
  }

  private HttpRequest<Buffer> createRequest(String requestType, String url) {
    HttpRequest<Buffer> webRequest = null;
    switch (requestType) {
      case REQUEST_GET:
        webRequest = webClient.get(url).basicAuthentication(username, password);
        break;
      case REQUEST_POST:
        webRequest = webClient.post(url).basicAuthentication(username, password);
        break;
      case REQUEST_PUT:
        webRequest = webClient.put(url).basicAuthentication(username, password);
        break;
      case REQUEST_DELETE:
        webRequest = webClient.delete(url).basicAuthentication(username, password);
        break;
      default:
        break;
    }
    return webRequest;
  }
}
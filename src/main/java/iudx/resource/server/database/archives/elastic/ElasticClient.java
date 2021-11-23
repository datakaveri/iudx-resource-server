package iudx.resource.server.database.archives.elastic;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.ResponseBuilder;

import static iudx.resource.server.database.archives.Constants.*;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

public class ElasticClient {

  private final RestClient client;
  private ResponseBuilder responseBuilder;
  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);
  /**
   * ElasticClient - Elastic Low level wrapper.
   * 
   * @param databaseIP IP of the ElasticDB
   * @param databasePort Port of the ElasticDB
   */

  public ElasticClient(String databaseIP, int databasePort, String user, String password) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
    client = RestClient.builder(new HttpHost(databaseIP, databasePort)).setHttpClientConfigCallback(
        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials)).build();
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   * 
   * @param index Index to search on
   * @param query Query
   * @param searchHandler JsonObject result {@link AsyncResult}
   */
  public ElasticClient searchAsync(String index, String filterPathValue, String query,
      Handler<AsyncResult<JsonObject>> searchHandler) {

    Request queryRequest = new Request(REQUEST_GET, index);
    queryRequest.addParameter(FILTER_PATH, filterPathValue);
    queryRequest.setJsonEntity(query);

    client.performRequestAsync(queryRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        JsonArray dbResponse = new JsonArray();
        JsonObject jsonTemp;
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (!responseJson.containsKey(HITS) && !responseJson.containsKey(DOCS_KEY)) {
            responseBuilder =
                new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
            searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return;
          }
          responseBuilder = new ResponseBuilder(SUCCESS).setTypeAndTitle(200);
          JsonArray responseHits = new JsonArray();
          if (responseJson.containsKey(HITS)) {
            responseHits = responseJson.getJsonObject(HITS).getJsonArray(HITS);
          } else if (responseJson.containsKey(DOCS_KEY)) {
            responseHits = responseJson.getJsonArray(DOCS_KEY);
          }
          for (Object json : responseHits) {
            jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject(SOURCE_FILTER_KEY));
          }
          responseBuilder.setMessage(dbResponse);
          searchHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
        } catch (IOException e) {
          LOGGER.error("IO Execption from Database: " + e.getMessage());
          JsonObject ioError = new JsonObject(e.getMessage());
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
          searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.error(e.getLocalizedMessage());
        try {
          String error = e.getMessage().substring(e.getMessage().indexOf("{"),
              e.getMessage().lastIndexOf("}") + 1);
          JsonObject dbError = new JsonObject(error);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
          searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (DecodeException jsonError) {
          LOGGER.error("Json parsing exception: " + jsonError);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(BAD_PARAMETERS);
          searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }
    });
    return this;
  }

  /**
   * countAsync - Wrapper around elasticsearch async count requests.
   * 
   * @param index Index to search on
   * @param query Query
   * @param countHandler JsonObject result {@link AsyncResult}
   */
  public ElasticClient countAsync(String index, String query,
      Handler<AsyncResult<JsonObject>> countHandler) {

    Request queryRequest = new Request(REQUEST_GET, index);
    queryRequest.setJsonEntity(query);

    client.performRequestAsync(queryRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            countHandler.handle(Future.failedFuture(DB_ERROR_2XX));
            responseBuilder =
                new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(DB_ERROR_2XX);
            countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return;
          }

          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getInteger(COUNT) == 0) {
            responseBuilder =
                new ResponseBuilder(FAILED).setTypeAndTitle(204).setMessage(EMPTY_RESPONSE);
            countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            return;
          }
          responseBuilder =
              new ResponseBuilder(SUCCESS).setTypeAndTitle(200)
                  .setCount(responseJson.getInteger(COUNT));
          countHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
        } catch (IOException e) {
          LOGGER.error("IO Execption from Database: " + e.getMessage());
          JsonObject ioError = new JsonObject(e.getMessage());
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        LOGGER.error(e.getLocalizedMessage());
        try {
          String error = e.getMessage().substring(e.getMessage().indexOf("{"),
              e.getMessage().lastIndexOf("}") + 1);
          JsonObject dbError = new JsonObject(error);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        } catch (DecodeException jsonError) {
          LOGGER.error("Json parsing exception: " + jsonError);
          responseBuilder = new ResponseBuilder(FAILED).setTypeAndTitle(400)
              .setMessage(BAD_PARAMETERS);
          countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
        }
      }
    });
    return this;
  }
}

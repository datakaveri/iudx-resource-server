package iudx.resource.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

import static iudx.resource.server.database.Constants.*;

public class ElasticClient {

  private final RestClient client;

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
      Handler<AsyncResult<JsonArray>> searchHandler) {

    Request queryRequest = new Request(REQUEST_GET, index);
    queryRequest.addParameter(FILTER_PATH, filterPathValue);
    queryRequest.setJsonEntity(query);

    client.performRequestAsync(queryRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        JsonArray dbResponse = new JsonArray();

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            searchHandler.handle(Future.failedFuture(DB_ERROR_2XX));
            return;
          }

          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (!responseJson.containsKey(HITS) && !responseJson.containsKey(DOCS_KEY)) {
            searchHandler.handle(Future.failedFuture(EMPTY_RESPONSE));
            return;
          }
          JsonArray responseHits = new JsonArray();
          if (responseJson.containsKey(HITS)) {
            responseHits = responseJson.getJsonObject(HITS).getJsonArray(HITS);
          } else if (responseJson.containsKey(DOCS_KEY)) {
            responseHits = responseJson.getJsonArray(DOCS_KEY);
          }
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject(SOURCE_FILTER_KEY));
          }
          searchHandler.handle(Future.succeededFuture(dbResponse));
        } catch (IOException e) {
          searchHandler.handle(Future.failedFuture(e));
        }
      }

      @Override
      public void onFailure(Exception e) {
        searchHandler.handle(Future.failedFuture(e));
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
            return;
          }

          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          countHandler.handle(Future.succeededFuture(
              new JsonObject().put(StringUtils.capitalize(COUNT), responseJson.getInteger(COUNT))));

        } catch (IOException e) {
          countHandler.handle(Future.failedFuture(e));
        }
      }

      @Override
      public void onFailure(Exception e) {
        countHandler.handle(Future.failedFuture(e));
      }
    });
    return this;
  }
}

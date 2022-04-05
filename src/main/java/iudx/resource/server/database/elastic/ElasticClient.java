package iudx.resource.server.database.elastic;

import static iudx.resource.server.database.archives.Constants.BAD_PARAMETERS;
import static iudx.resource.server.database.archives.Constants.COUNT;
import static iudx.resource.server.database.archives.Constants.DB_ERROR_2XX;
import static iudx.resource.server.database.archives.Constants.DOCS_KEY;
import static iudx.resource.server.database.archives.Constants.EMPTY_RESPONSE;
import static iudx.resource.server.database.archives.Constants.FAILED;
import static iudx.resource.server.database.archives.Constants.FILTER_PATH;
import static iudx.resource.server.database.archives.Constants.HITS;
import static iudx.resource.server.database.archives.Constants.REQUEST_GET;
import static iudx.resource.server.database.archives.Constants.SOURCE_FILTER_KEY;
import static iudx.resource.server.database.archives.Constants.SUCCESS;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.resource.server.database.archives.ResponseBuilder;

public class ElasticClient {

  private final RestClient client;
  private final RestHighLevelClient highLevelClient;
  private ResponseBuilder responseBuilder;
  private String filePath;
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
    RestClientBuilder restClientBuilder =
        RestClient.builder(new HttpHost(databaseIP, databasePort))
            .setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials));
    client = restClientBuilder.build();
    highLevelClient = new RestHighLevelClient(restClientBuilder);
  }

  public ElasticClient(
      String databaseIP, int databasePort, String user, String password, String filePath) {
    this(databaseIP, databasePort, user, password);
    this.filePath = filePath;
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param index Index to search on
   * @param query Query
   * @param searchHandler JsonObject result {@link AsyncResult}
   */
  public ElasticClient searchAsync(
      String index,
      String filterPathValue,
      String query,
      Handler<AsyncResult<JsonObject>> searchHandler) {

    Request queryRequest = new Request(REQUEST_GET, index);
    queryRequest.addParameter(FILTER_PATH, filterPathValue);
    queryRequest.setJsonEntity(query);

    client.performRequestAsync(
        queryRequest,
        new ResponseListener() {
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
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
              searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            }
          }

          @Override
          public void onFailure(Exception e) {
            LOGGER.error(e.getLocalizedMessage());
            try {
              String error =
                  e.getMessage()
                      .substring(e.getMessage().indexOf("{"), e.getMessage().lastIndexOf("}") + 1);
              JsonObject dbError = new JsonObject(error);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
              searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            } catch (DecodeException jsonError) {
              LOGGER.error("Json parsing exception: ", jsonError);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(BAD_PARAMETERS);
              searchHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            } catch (Exception ex) {
              LOGGER.error("elastic exception: ", ex);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(BAD_PARAMETERS);
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
  public ElasticClient countAsync(
      String index, String query, Handler<AsyncResult<JsonObject>> countHandler) {

    Request queryRequest = new Request(REQUEST_GET, index);
    queryRequest.setJsonEntity(query);

    client.performRequestAsync(
        queryRequest,
        new ResponseListener() {
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
                  new ResponseBuilder(SUCCESS)
                      .setTypeAndTitle(200)
                      .setCount(responseJson.getInteger(COUNT));
              countHandler.handle(Future.succeededFuture(responseBuilder.getResponse()));
            } catch (IOException e) {
              LOGGER.error("IO Execption from Database: ", e.getMessage());
              JsonObject ioError = new JsonObject(e.getMessage());
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(ioError);
              countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            }
          }

          @Override
          public void onFailure(Exception e) {
            LOGGER.error(e.getLocalizedMessage());
            try {
              String error =
                  e.getMessage()
                      .substring(e.getMessage().indexOf("{"), e.getMessage().lastIndexOf("}") + 1);
              JsonObject dbError = new JsonObject(error);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(dbError);
              countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            } catch (DecodeException jsonError) {
              LOGGER.error("Json parsing exception: ", jsonError);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(BAD_PARAMETERS);
              countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            }catch (Exception ex) {
              LOGGER.error("elastic exception: ", ex);
              responseBuilder =
                  new ResponseBuilder(FAILED).setTypeAndTitle(400).setMessage(BAD_PARAMETERS);
              countHandler.handle(Future.failedFuture(responseBuilder.getResponse().toString()));
            }
          }
        });
    return this;
  }

  public ElasticClient scrollAsync(
      File file, String index, QueryBuilder query, Handler<AsyncResult<JsonObject>> scrollHandler) {

    String scrollId = null;
    try {
      SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
      searchSourceBuilder.query(query);
      searchSourceBuilder.fetchSource(new String[] {}, new String[] {"_index", "_type", "_score"});
      searchSourceBuilder.size(10000);

      SearchRequest searchRequest = new SearchRequest();
      // searchRequest.addParameter(FILTER_PATH, "took,hits.hits._source");
      searchRequest.indices(index);
      searchRequest.source(searchSourceBuilder);
      searchRequest.scroll(TimeValue.timeValueMinutes(5L));

      RequestOptions rqo = RequestOptions.DEFAULT;
      rqo.toBuilder().addParameter(FILTER_PATH, "took,hits.hits._source");

      SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
      scrollId = searchResponse.getScrollId();

      LOGGER.debug("total hits" + searchResponse.getHits().getTotalHits().value);

      SearchHit[] searchHits = searchResponse.getHits().getHits();

      LOGGER.debug(file.getAbsolutePath());

      FileWriter filew = new FileWriter(file);
      int totalFiles = 0;
      filew.write('[');

      boolean appendComma = false;

      while (searchHits != null && searchHits.length > 0) {
        LOGGER.debug("results={} ({} new)", totalFiles += searchHits.length, searchHits.length);

        for (SearchHit sh : searchHits) {
          if (appendComma) {
            filew.write("," + sh.getSourceAsString());
          } else {
            filew.write(sh.getSourceAsString());
          }
          appendComma = true;
        }

        SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(TimeValue.timeValueMinutes(5L));
        searchResponse = highLevelClient.scroll(scrollRequest, rqo);
        scrollId = searchResponse.getScrollId();
        searchHits = searchResponse.getHits().getHits();
      }
      filew.write(']');
      filew.close();
      scrollHandler.handle(Future.succeededFuture());
    } catch (IOException ex) {
      LOGGER.error(ex);
      LOGGER.error(ex.getMessage());
      scrollHandler.handle(Future.failedFuture(ex));
    } catch (Exception ex) {
      LOGGER.error(ex);
      LOGGER.error(ex.getMessage());
      scrollHandler.handle(Future.failedFuture(ex));
    } finally {
      if (scrollId != null) {
        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.addScrollId(scrollId);
        try {
          ClearScrollResponse clearScrollResponse =
              highLevelClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
          LOGGER.error(e);
          LOGGER.error(e.getMessage());
          scrollHandler.handle(Future.failedFuture(e));
        }
      }
    }
    return this;
  }
}

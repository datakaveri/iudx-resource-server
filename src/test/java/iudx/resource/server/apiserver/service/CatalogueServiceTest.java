package iudx.resource.server.apiserver.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class CatalogueServiceTest {

  @Mock Vertx vertxObj;
  JsonObject config;
  CatalogueService catalogueService;

  @Mock HttpRequest<Buffer> httpRequest;
  @Mock AsyncResult<HttpResponse<Buffer>> asyncResult;
  @Mock HttpResponse<Buffer> httpResponse;
  @Mock CacheService cache;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    config = new JsonObject();
    config.put("catServerHost", "guest");
    config.put("catServerPort", 8443);
    JsonObject jsonObject = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray1 = new JsonArray();
    JsonObject jsonObject1 = new JsonObject();
    jsonObject1.put("id", "abcd/abcd/abcd/abcd");
    jsonObject1.put("iudxResourceAPIs", jsonArray1);
    jsonArray.add(jsonObject1);
    jsonObject.put("results", jsonArray);
    //        CatalogueService.catWebClient = mock(WebClient.class);

    //
    // when(CatalogueService.catWebClient.get(anyInt(),anyString(),anyString())).thenReturn(httpRequest);
    //        when(httpRequest.addQueryParam(anyString(),anyString())).thenReturn(httpRequest);
    //        when(httpRequest.expect(any())).thenReturn(httpRequest);
    //        when(asyncResult.succeeded()).thenReturn(true);
    //        when(asyncResult.result()).thenReturn(httpResponse);
    //        when(httpResponse.bodyAsJsonObject()).thenReturn(jsonObject);
    //        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
    //            @Override
    //            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws
    // Throwable {
    //
    //
    // ((Handler<AsyncResult<HttpResponse<Buffer>>>)arg0.getArgument(0)).handle(asyncResult);
    //                return null;
    //            }
    //        }).when(httpRequest).send(any());
    catalogueService = new CatalogueService(vertxObj, config, cache);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success [Group level resource]")
  public void testGetApplicableFiltersSuccessGroup(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(cacheReply))
        .thenReturn(Future.succeededFuture(new JsonObject()));

    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(2)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test getApplicableFilters method Success [Resource level]")
  public void testGetApplicableFiltersSuccessItem(VertxTestContext vertxTestContext) {
    String id = "abcd/abcd/abcd";
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    when(cache.get(any()))
        .thenReturn(Future.succeededFuture(new JsonObject()))
        .thenReturn(Future.succeededFuture(cacheReply));

    catalogueService
        .getApplicableFilters(id)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(2)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Testing Success for isItemExist method with List of String IDs")
  public void testIsItemExistSuccess(VertxTestContext vertxTestContext) {
    List<String> idList = new ArrayList<>();
    idList.add("abcd/abcd/abcd/abcd");
    idList.add("efgh/efgh/efgh/efgh");
    idList.add("asdf/asdf/asfd/asdf");
    //        JsonObject responseJSonObject = new JsonObject();
    //        responseJSonObject.put("type","urn:dx:cat:Success");
    //        responseJSonObject.put("totalHits", 10);
    //        when(httpResponse.bodyAsJsonObject()).thenReturn(responseJSonObject);
    JsonObject cacheReply = new JsonObject();
    cacheReply.put("iudxResourceAPIs", new JsonArray());
    when(cache.get(any())).thenReturn(Future.succeededFuture(cacheReply));

    catalogueService
        .isItemExist(idList)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                // assertTrue(handler.result());
                verify(cache, times(3)).get(any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
    //        verify(CatalogueService.catWebClient,times(4)).get(anyInt(),anyString(),anyString());
    //        verify(httpRequest,times(6)).addQueryParam(anyString(),anyString());
    //        verify(httpRequest,times(4)).send(any());

  }

  @Test
  @DisplayName("Testing Failure for isItemExist method with List of String IDs")
  public void testIsItemExistFailure(VertxTestContext vertxTestContext) {
    List<String> idList = new ArrayList<>();
    idList.add("abcd/abcd/abcd/abcd");
    idList.add("efgh/efgh/efgh/efgh");
    idList.add("asdf/asdf/asfd/asdf");
    //        when(asyncResult.succeeded()).thenReturn(false);

    when(cache.get(any())).thenReturn(Future.failedFuture(""));

    catalogueService
        .isItemExist(idList)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(cache, times(3)).get(any());
                vertxTestContext.failNow(handler.cause());
              } else {
                vertxTestContext.completeNow();
              }
            });
    //        verify(CatalogueService.catWebClient,times(4)).get(anyInt(),anyString(),anyString());
    //        verify(httpRequest,times(6)).addQueryParam(anyString(),anyString());
    //        verify(httpRequest,times(4)).send(any());
  }
}

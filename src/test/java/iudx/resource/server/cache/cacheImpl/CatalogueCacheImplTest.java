package iudx.resource.server.cache.cacheImpl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class CatalogueCacheImplTest {
    @Mock
    Vertx vertxObj;
    @Mock
    JsonObject jsonObject;
    @Mock
    HttpRequest<Buffer> httpRequest;
    @Mock
    HttpResponse<Buffer> httpResponse;
    @Mock
    AsyncResult<HttpResponse<Buffer>> asyncResult;
    CatalogueCacheImpl CatalogueCacheImpl;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        JsonObject config = new JsonObject();
        config.put("catServerHost", "guest");
        config.put("catServerPort", 8443);
        config.put("dxCatalogueBasePath", "/iudx/cat/v1");
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        JsonArray jsonArray1 = new JsonArray();
        JsonObject jsonObject1 = new JsonObject();
        jsonObject1.put("id", "abcd/abcd/abcd/abcd");
        jsonObject1.put("iudxResourceAPIs", jsonArray1);
        jsonArray.add(jsonObject1);
        jsonObject.put("results", jsonArray);

        CatalogueCacheImpl.catWebClient = mock(WebClient.class);
        when(CatalogueCacheImpl.catWebClient.get(anyInt(), anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.addQueryParam(anyString(), anyString())).thenReturn(httpRequest);
        when(httpRequest.expect(any())).thenReturn(httpRequest);
        when(asyncResult.succeeded()).thenReturn(true);
        when(asyncResult.result()).thenReturn(httpResponse);
        when(httpResponse.bodyAsJsonObject()).thenReturn(jsonObject);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {

                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
            }
        }).when(httpRequest).send(any());

        CatalogueCacheImpl = new CatalogueCacheImpl(vertxObj, config);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Testing get for empty value")
    void test1(VertxTestContext vertxTestContext) {
        assertNotNull(CatalogueCacheImpl.get("")) ;
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Testing get for id")
    void test2(VertxTestContext vertxTestContext) {
       assertNotNull(CatalogueCacheImpl.get("abcd/abcd/abcd/abcd")) ;
        vertxTestContext.completeNow();
    }


}

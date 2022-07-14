package iudx.resource.server.database.async.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.database.postgres.PostgresService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.resource.server.database.archives.Constants.ID;
import static iudx.resource.server.database.archives.Constants.SEARCH_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestUtil {
    @Mock
    PostgresService pgService;
    Util util;
    @Mock
    JsonObject jsonObjectFuture;
    @Mock
    AsyncResult<JsonObject> asyncResult;
    @Mock
    Throwable throwable;

    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        util = new Util(pgService);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test writeToDB method : failure")
    public void test_writeToDB_failure(VertxTestContext vertxTestContext)
    {
       String searchID = "Dummy Search ID";
       String requestID = "Dummy requestID";
        String sub = "Dummy value";
        StringBuilder query = new StringBuilder();
        query.append("Select * from dummyTable");
        when(asyncResult.succeeded()).thenReturn(false);
        when(asyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<JsonObject>>() {
            @Override
            public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
            }
        }).when(pgService).executeQuery(anyString(), any());
        util.writeToDB(searchID,requestID,sub).onComplete(handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                assertEquals("failed query executionthrowable", handler.cause().getMessage());
                verify(asyncResult,times(1)).cause();
                vertxTestContext.completeNow();
            }
        });
    }


    @Test
    @DisplayName("Test isValidQuery method : with invalid query")
    public void test_isValidQuery_failure(VertxTestContext vertxTestContext)
    {
        JsonObject query = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0,"dummy/value/abcd/abcd/abcd/abcd");
        query.put(ID, jsonArray);
        query.put(SEARCH_TYPE,"abcd");
        assertFalse(util.isValidQuery(query));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test isValidQuery method : with SEARCHTYPE_NOT_FOUND")
    public void test_isValidQuery_with_SEARCHTYPE_NOT_FOUND(VertxTestContext vertxTestContext)
    {

        JsonObject query = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(0,"dummy/value/abcd/abcd/abcd/abcd");
        query.put(ID, jsonArray);
        assertFalse(util.isValidQuery(query));
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test isValidQuery method : with empty resource id")
    public void test_isValidQuery_with_empty_resource_id(VertxTestContext vertxTestContext)
    {
        JsonObject query = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        query.put(ID, jsonArray);
        assertFalse(util.isValidQuery(query));
        vertxTestContext.completeNow();
    }
}

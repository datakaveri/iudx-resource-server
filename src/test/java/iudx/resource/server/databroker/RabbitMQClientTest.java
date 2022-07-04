package iudx.resource.server.databroker;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import iudx.resource.server.databroker.util.PermissionOpType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RabbitMQClientTest {

    RabbitClient rabbitClient;
    String userID;
    String password;
    @Mock
    RabbitMQOptions rabbitConfigs;
    Vertx vertxObj;
    @Mock
    RabbitWebClient webClient;
    @Mock
    PostgresClient pgSQLClient;
    @Mock
    JsonObject configs;
    @Mock
    Future<HttpResponse<Buffer>> httpResponseFuture;
    @Mock
    Future<RowSet<Row>> rowSetFuture;
    @Mock
    AsyncResult<HttpResponse<Buffer>> httpResponseAsyncResult;
    @Mock
    HttpResponse<Buffer> bufferHttpResponse;
    @Mock
    Buffer buffer;
    @Mock
    AsyncResult<RowSet<Row>> rowSetAsyncResult;
    @Mock
    Throwable throwable;
    PermissionOpType type;
    JsonObject request;
    JsonArray jsonArray;
    String vHost;
    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext) {
        userID = "Dummy UserID";
        password = "Dummy password";
        vertxObj = Vertx.vertx();
        vHost = "Dummy vHost";
        when(configs.getString(anyString())).thenReturn("Dummy string");
        when(configs.getInteger(anyString())).thenReturn(400);
        when(rabbitConfigs.setVirtualHost(anyString())).thenReturn(rabbitConfigs);
        request = new JsonObject();
        jsonArray = new JsonArray();
        jsonArray.add(0,"{\"Dummy key\" : \"Dummy value\"}");
        request.put("exchangeName", "Dummy exchangeName");
        request.put("queueName","Dummy Queue name");
        request.put("id","Dummy ID");
        request.put("vHost","Dummy vHost");
        request.put("entities",jsonArray);
        rabbitClient = new RabbitClient(vertxObj, rabbitConfigs, webClient, pgSQLClient, configs);
        vertxTestContext.completeNow();
    }




    @Test
    @DisplayName("Test resetPwdInDb method : Failure")
    public void testResetPwdInDbFailure(VertxTestContext vertxTestContext)
    {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.resetPwdInDb(userID,password).onComplete(handler -> {
            if(handler.failed())
            {
                assertEquals("Error : Write to database failed",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
    @Test
    @DisplayName("Test resetPwdInDb method : Success")
    public void testResetPwdInDbSuccess(VertxTestContext vertxTestContext)
    {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(true);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
        @Override
        public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
            ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
            return null;
        }
    }).when(rowSetFuture).onComplete(any());
        rabbitClient.resetPwdInDb(userID,password).onComplete(handler -> {
            if(handler.succeeded())
            {
               assertEquals("{\"status\":\"success\"}",handler.result().toString());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test testGetUserInDb method : Success")
    public void testGetUserInDbSuccess(VertxTestContext vertxTestContext)
    {
        RowSet<Row> rowSet = mock(RowSet.class);
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(true);
        when(rowSetAsyncResult.result()).thenReturn(rowSet);

        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.getUserInDb(userID).onComplete(handler -> {
            if(handler.succeeded())
            {
                assertEquals("{\"apiKey\":null}", handler.result().toString());
                vertxTestContext.completeNow();

            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test testGetUserInDb method : Failure")
    public void testGetUserInDbFailure(VertxTestContext vertxTestContext)
    {
        when(pgSQLClient.executeAsync(anyString())).thenReturn(rowSetFuture);
        when(rowSetAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<RowSet<Row>>>() {
            @Override
            public AsyncResult<RowSet<Row>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<RowSet<Row>>>) arg0.getArgument(0)).handle(rowSetAsyncResult);
                return null;
            }
        }).when(rowSetFuture).onComplete(any());
        rabbitClient.getUserInDb(userID).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());

            }
            else
            {
                assertEquals("Error : Get ID from database failed",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    static Stream<Arguments> inputStatusCode()
    {
        return Stream.of(
          Arguments.of(201,"{\"type\":200,\"title\":\"topic_permissions\",\"detail\":\"topic permission set\"}"),
          Arguments.of(204,"{\"type\":200,\"title\":\"topic_permissions\",\"detail\":\"topic permission already set\"}"),
          Arguments.of(400,"{\"type\":500,\"title\":\"topic_permissions\",\"detail\":\"Error in setting Topic permissions\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("inputStatusCode")
    @DisplayName("Test setTopicPermissions method : with different status code")
    public void testSetTopicPermissions(int statusCode, String expected,VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(statusCode);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.setTopicPermissions("Dummy vHost", "Dummy adaptorID", userID).onComplete(handler -> {
            if(handler.succeeded())
            {
                assertEquals(expected, handler.result().toString());
            }
            else
            {
                assertEquals(expected,handler.cause().getMessage());
            }
        });
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test setTopicPermissions method : Failure")
    public void testSetTopicPermissionsFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.setTopicPermissions("Dummy vHost", "Dummy adaptorID", userID).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":500,\"title\":\"topic_permissions\",\"detail\":\"Error in setting Topic permissions\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }
    @Test
    @DisplayName("Test createExchange method : for status code 400")
    public void testCreateExchangeForBadRequest(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createExchange(request,"Dummy vHost").onComplete(handler -> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":400,\"title\":\"failure\",\"detail\":\"Exchange already exists with different properties\"}", handler.result().toString());
                vertxTestContext.completeNow();

            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createExchange method : Failure")
    public void testCreateExchangeFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);


        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createExchange(request,"Dummy vHost").onComplete(handler -> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Creation of Exchange failed\"}", handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : For status code 404")
    public void testGetExchangeForSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchange(request,"Dummy vHost").onComplete(handler-> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"Exchange not found\"}",handler.result().toString());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : For status code 401")
    public void testGetExchangeForEXCHANGE_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(401);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchange(request,"Dummy vHost").onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("getExchange_statusthrowable",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : With null request")
    public void testGetExchangeFailure(VertxTestContext vertxTestContext)
    {
        rabbitClient.getExchange(new JsonObject(),"Dummy vHost").onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("exchangeName not provided",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }


    @Test
    @DisplayName("Test deleteExchange : Failure")
    public void testDeleteExchange(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deleteExchange(request,"Dummy vHost").onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":500,\"title\":\"error\",\"detail\":\"Deletion of Exchange failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
        });
    }

    @Test
    @DisplayName("Test listExchangeSubscribers method : For status code 404")
    public void testListExchangeSubscribersForSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.listExchangeSubscribers(request,"Dummy vHost").onComplete(handler-> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"Exchange not found\"}",handler.result().toString());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
    @Test
    @DisplayName("Test listExchangeSubscribers method : Failure")
    public void testListExchangeSubscribersFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.listExchangeSubscribers(request,"Dummy vHost").onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Internal server error\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createQueue method : when status code is 400")
    public void testCreateQueue(VertxTestContext vertxTestContext)
    {

        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.createQueue(request,"Dummy vHost").onComplete(handler -> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":400,\"title\":\"failure\",\"detail\":\"Queue already exists with different properties\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
            else {
                    vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createQueue method : when status code is 400")
    public void testCreateQueueFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);
        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());

        rabbitClient.createQueue(request,"Dummy vHost").onComplete(handler -> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Creation of Queue failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deleteQueue method : Failure")
    public void testDeleteQueueFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deleteQueue(request,"Dummy vHost").onComplete(handler-> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Deletion of Queue failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }
    @Test
    @DisplayName("Test bindQueue method : Failure")
    public void testBindQueueFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString(),any())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.bindQueue(request,"Dummy vHost").onComplete(handler-> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"error in queue binding with adaptor\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Test
    @DisplayName("Test unbindQueue method : status code 404")
    public void testUnbindQueueForSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.unbindQueue(request,"Dummy vHost").onComplete(handler-> {
            if(handler.succeeded())
            {
               assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"Queue/Exchange/Routing Key does not exist\"}",handler.result().toString());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test unbindQueue method : Failure")
    public void testUnbindQueueFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.unbindQueue(request,"Dummy vHost").onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"error in queue binding with adaptor\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createvHost method : when status code is 204 ")
    public void testCreatevHostWhenSC_NO_CONTENT(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(204);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createvHost(request).onComplete(handler-> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":409,\"title\":\"failure\",\"detail\":\"vHost already exists\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test createvHost method : Failure")
    public void testCreatevHostFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.createvHost(request).onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Creation of vHost failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deletevHost method : when status code is 400 ")
    public void testDeletevHostWhenSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(400);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deletevHost(request).onComplete(handler-> {
            if(handler.succeeded())
            {
                assertEquals("{}",handler.result().toString());
                vertxTestContext.completeNow();

            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test deletevHost method : Failure")
    public void testDeletevHostFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deletevHost(request).onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Deletion of vHost failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listvHost method : for status code 404 ")
    public void testListvHostWhenSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listvHost(request).onComplete(handler -> {
            if(handler.succeeded())
            {
              assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"No vhosts found\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listvHost method : Failure")
    public void testListvHostHostFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listvHost(request).onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Listing of vHost failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listQueueSubscribers method : for status code 404 ")
    public void testListQueueSubscribersWhenSC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(404);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listQueueSubscribers(request,vHost).onComplete(handler -> {
            if(handler.succeeded())
            {
                assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"Queue does not exist\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test listQueueSubscribers method : Failure")
    public void testListQueueSubscribersFailure(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.listQueueSubscribers(request,vHost).onComplete(handler-> {
            if(handler.failed())
            {
                assertEquals("{\"type\":500,\"title\":\"failure\",\"detail\":\"Listing of Queue failed\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @Test
    @DisplayName("Test getExchange method : error")
    public void test_getExchange_with_error(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(false);
        when(httpResponseAsyncResult.cause()).thenReturn(throwable);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.getExchange(request,vHost).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("getExchange_errorthrowable",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }
    @Test
    @DisplayName("Test getExchange method : when SC_NOT_FOUND")
    public void test_deletevHost_when_SC_NOT_FOUND(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deletevHost(request).onComplete(handler -> {
            if(handler.failed())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":404,\"title\":\"failure\",\"detail\":\"No vhosts found\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
        });
    }

    @Test
    @DisplayName("Test registerAdapter method : with empty adaptorID")
    public void test_registerAdapter_empty_empty_adaptorID(VertxTestContext vertxTestContext)
    {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add("");
        request.put("entities", jsonArray);
      rabbitClient.registerAdapter(request,vHost).onComplete(handler -> {
            if(handler.succeeded())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":400,\"title\":\"Bad Request data\",\"detail\":\"Invalid/Missing Parameters\"}",handler.cause().getMessage());
                vertxTestContext.completeNow();

            }
        });
    }

    @Test
    @DisplayName("Test deleteAdapter method : Success ")
    public void test_deleteAdapter_success(VertxTestContext vertxTestContext)
    {
        when(webClient.requestAsync(anyString(),anyString())).thenReturn(httpResponseFuture);
        when(httpResponseAsyncResult.succeeded()).thenReturn(true);
        when(httpResponseAsyncResult.result()).thenReturn(bufferHttpResponse);
        when(bufferHttpResponse.statusCode()).thenReturn(200);

        doAnswer(new Answer<AsyncResult<HttpResponse<Buffer>>>() {
            @Override
            public AsyncResult<HttpResponse<Buffer>> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<HttpResponse<Buffer>>>) arg0.getArgument(0)).handle(httpResponseAsyncResult);
                return null;
            }
        }).when(httpResponseFuture).onComplete(any());
        rabbitClient.deleteAdapter(request,vHost).onComplete(handler -> {
            if(handler.failed())
            {
                vertxTestContext.failNow(handler.cause());
            }
            else
            {
                assertEquals("{\"type\":200,\"title\":\"success\",\"detail\":\"adaptor deleted\"}",handler.result().toString());
                vertxTestContext.completeNow();

            }
        });
    }


}

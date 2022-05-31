package iudx.resource.server.apiserver.handlers;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.exceptions.DxRuntimeException;
import iudx.resource.server.common.ResponseUrn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class FailureHandlerTest {
    @Mock
    DxRuntimeException dxRuntimeException;
    @Mock
    RoutingContext routingContext;
    @Mock
    HttpServerResponse httpServerResponse;
    @Mock
    ResponseUrn responseUrn;
    @Mock
    Future<Void> voidFuture;

    @Test
    @DisplayName("Test handle method with RuntimeException")
    public void testHandle(VertxTestContext vertxTestContext)
    {
        when(routingContext.failure()).thenReturn(dxRuntimeException);
        when(dxRuntimeException.getUrn()).thenReturn(responseUrn);
        when(dxRuntimeException.getStatusCode()).thenReturn(400);
        when(responseUrn.getUrn()).thenReturn("Dummy URN");
        when(dxRuntimeException.getMessage()).thenReturn("Dummy Message");
        when(routingContext.response()).thenReturn(httpServerResponse);
        when(httpServerResponse.putHeader(anyString(),anyString())).thenReturn(httpServerResponse);
        when(httpServerResponse.setStatusCode(anyInt())).thenReturn(httpServerResponse);
        when(httpServerResponse.end(anyString())).thenReturn(voidFuture);

        FailureHandler failureHandler = new FailureHandler();
        failureHandler.handle(routingContext);

        verify(routingContext,times(2)).response();
        verify(httpServerResponse, times(2)).putHeader(anyString(),anyString());
        verify(httpServerResponse, times(2)).setStatusCode(400);
        verify(httpServerResponse, times(2)).end(anyString());


        assertEquals("Dummy Message", dxRuntimeException.getMessage());
        assertEquals("Dummy URN",dxRuntimeException.getUrn().getUrn());
        vertxTestContext.completeNow();
    }

}

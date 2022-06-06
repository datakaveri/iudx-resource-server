package iudx.resource.server.apiserver.handlers;


import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.util.RequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ValidationHandlerTest {
    RequestType requestType;
    @Mock
    RoutingContext routingContext;
    @Mock
    HttpServerRequest httpServerRequest;

    ValidationHandler validationHandler;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(RequestType.ENTITY),
                Arguments.of(RequestType.TEMPORAL),
                Arguments.of(RequestType.LATEST),
                Arguments.of(RequestType.POST_TEMPORAL),
                Arguments.of(RequestType.POST_ENTITIES),
                Arguments.of(RequestType.SUBSCRIPTION),
                Arguments.of(RequestType.ASYNC_SEARCH),
                Arguments.of(RequestType.ASYNC_STATUS)
        );
    }

    @DisplayName("Test handle method")
    @ParameterizedTest
    @MethodSource("data")
    public void testHandle(RequestType value, VertxTestContext vertxTestContext) {
        MultiMap map = MultiMap.caseInsensitiveMultiMap();
        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("Dummy key1", "Dummy value1");
        stringMap.put("Dummy key 2", "Dummy value 2");
        requestType = value;
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("Dummy key", "Dummy value");

        when(routingContext.getBodyAsJson()).thenReturn(jsonObject);
        when(routingContext.request()).thenReturn(httpServerRequest);
        when(httpServerRequest.params()).thenReturn(map);
        when(httpServerRequest.headers()).thenReturn(map);
        when(routingContext.pathParams()).thenReturn(stringMap);

        validationHandler = new ValidationHandler(Vertx.vertx(), requestType);
        validationHandler.handle(routingContext);
        verify(routingContext, times(2)).request();
        verify(httpServerRequest).params();
        verify(routingContext).getBodyAsJson();
        verify(routingContext).pathParams();
        vertxTestContext.completeNow();
    }
}

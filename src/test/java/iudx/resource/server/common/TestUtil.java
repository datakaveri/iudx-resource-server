package iudx.resource.server.common;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.resource.server.apiserver.util.Constants.MSG_INVALID_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({VertxExtension.class, MockitoExtension.class})

public class TestUtil {

    @DisplayName("Test isValidName")
    @Test
    public void test_isValidName_success(VertxTestContext vertxTestContext)
    {
        Util.isValidName("some_name").onComplete(handler -> {
            if (handler.succeeded())
            {
                assertTrue(handler.result());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }

    @DisplayName("Test isValidName : with invalid name")
    @Test
    public void test_isValidName_failure(VertxTestContext vertxTestContext)
    {
        Util.isValidName("###").onComplete(handler -> {
            if (handler.failed())
            {
                assertEquals(MSG_INVALID_NAME,handler.cause().getMessage());
                vertxTestContext.completeNow();
            }
            else
            {
                vertxTestContext.failNow(handler.cause());
            }
        });
    }
}

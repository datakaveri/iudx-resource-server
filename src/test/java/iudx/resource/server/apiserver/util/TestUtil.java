package iudx.resource.server.apiserver.util;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.common.HttpStatusCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class TestUtil {
    @Test
    public void test(VertxTestContext vertxTestContext){
        assertNotNull(Util.errorResponse(HttpStatusCode.BAD_REQUEST));
        vertxTestContext.completeNow();
    }
}

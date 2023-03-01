package iudx.resource.server.apiserver.util;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.response.ResponseUtil;
import iudx.resource.server.common.HttpStatusCode;
import iudx.resource.server.common.ResponseUrn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class ResponseUtilTest {
    @Test
    public void test(VertxTestContext vertxTestContext){
        assertNotNull(ResponseUtil.generateResponse(HttpStatusCode.BAD_REQUEST, ResponseUrn.BAD_REQUEST_URN,"Bad Request"));
        vertxTestContext.completeNow();
    }
    @Test
    public void test2(VertxTestContext vertxTestContext){
        assertNotNull(ResponseUtil.generateResponse(HttpStatusCode.BAD_REQUEST, ResponseUrn.BAD_REQUEST_URN));
        vertxTestContext.completeNow();
    }
}

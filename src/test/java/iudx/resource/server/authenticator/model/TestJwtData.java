package iudx.resource.server.authenticator.model;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class TestJwtData {
    JwtData jwtData;
    @BeforeEach
    public void setUp(VertxTestContext vertxTestContext)
    {
        jwtData = new JwtData();
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test toJson method")
    public void test_toJson(VertxTestContext vertxTestContext)
    {
        JsonObject actual = jwtData.toJson();
        assertNotNull(actual);
        assertEquals(new JsonObject(),actual);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test getAccess_token method")
    public void test_getAccess_token(VertxTestContext vertxTestContext)
    {
        String actual = jwtData.getAccess_token();
        assertNull(actual);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test getIat method")
    public void test_getIat(VertxTestContext vertxTestContext)
    {
        Integer actual = jwtData.getIat();
        assertNull(actual);
        vertxTestContext.completeNow();
    }
}

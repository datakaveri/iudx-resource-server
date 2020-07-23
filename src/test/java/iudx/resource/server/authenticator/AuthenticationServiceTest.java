package iudx.resource.server.authenticator;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Properties;

@ExtendWith(VertxExtension.class)
public class AuthenticationServiceTest {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceTest.class);
    private static final Properties properties = new Properties();
    private static Vertx vertxObj;
    private static AuthenticationService authenticationService;

    @BeforeAll
    @DisplayName("Initialize Vertx and deploy Auth Verticle")
    static void initialize(Vertx vertx, VertxTestContext testContext) {
        vertxObj = vertx;
        authenticationService = new AuthenticationServiceImpl();
        logger.info("Auth tests setup complete");
        testContext.completeNow();
    }

    @Test
    @DisplayName("Testing setup")
    public void shouldSucceed(VertxTestContext testContext) {
        logger.info("Default test is passing");
        testContext.completeNow();
    }
}

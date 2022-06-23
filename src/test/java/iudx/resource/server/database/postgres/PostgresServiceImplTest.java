package iudx.resource.server.database.postgres;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class PostgresServiceImplTest {

    static PostgresServiceImpl pgService;
    @Container
    static PostgreSQLContainer container = new PostgreSQLContainer<>("postgres:12.11")
            .withInitScript("pg_test_schema.sql");


    @BeforeAll
    public static void  setUp(VertxTestContext vertxTestContext) {
        // Now we have an address and port for Postgresql, no matter where it is running
        Integer port = container.getFirstMappedPort();
        String host = container.getHost();
        String db = container.getDatabaseName();
        String user = container.getUsername();
        String password = container.getPassword();

        PgConnectOptions connectOptions = new PgConnectOptions()
                .setPort(port)
                .setHost(host)
                .setDatabase(db)
                .setUser(user)
                .setPassword(password);

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(10);

        Vertx vertxObj = Vertx.vertx();

        PgPool pool = PgPool.pool(vertxObj, connectOptions, poolOptions);

        pgService = new PostgresServiceImpl(pool);
        vertxTestContext.completeNow();
    }

    @Test
    @DisplayName("Test executeQuery method : Success")
    public void testExecuteQuerySuccess(VertxTestContext vertxTestContext) {
        StringBuilder stringBuilder = new StringBuilder(Constants.INSERT_REVOKE_TOKEN_SQL.replace("$1", UUID.randomUUID().toString()).replace("$2", LocalDateTime.now().toString()));

        String expected = "{\"type\":\"urn:dx:rs:success\",\"title\":\"successful operations\",\"result\":[]}";
        pgService.executeQuery(stringBuilder.toString(), handler -> {
            if (handler.succeeded()) {
                assertEquals(expected, handler.result().toString());
                assertTrue(handler.result().containsKey("type"));
                assertTrue(handler.result().containsKey("title"));
                assertTrue(handler.result().containsKey("result"));
                assertEquals("successful operations", handler.result().getString("title"));
                assertEquals("urn:dx:rs:success", handler.result().getString("type"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

    }

    @Test
    @DisplayName("Test executeQuery method : with Invalid unique_attribute")
    public void testExecuteQueryFailure(VertxTestContext vertxTestContext) {
        StringBuilder stringBuilder = new StringBuilder(Constants.UPDATE_UNIQUE_ATTR_SQL.replace("$2", UUID.randomUUID().toString()).replace("$1", "abcd"));
        pgService.executeQuery(stringBuilder.toString(), handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject expected = new JsonObject(handler.cause().getMessage());

                assertTrue(handler.cause().getMessage().contains("type"));
                assertTrue(handler.cause().getMessage().contains("status"));
                assertTrue(handler.cause().getMessage().contains("detail"));
                assertEquals("urn:dx:rs:DatabaseError", expected.getString("type"));
                assertEquals(400, expected.getInteger("status"));
                vertxTestContext.completeNow();
            }
        });

    }

    @Test
    @DisplayName("Test executeQuery method : Success")
    public void testExecutePreparedQuerySuccess(VertxTestContext vertxTestContext) {
        StringBuilder stringBuilder = new StringBuilder(Constants.INSERT_REVOKE_TOKEN_SQL.replace("$1", UUID.randomUUID().toString()).replace("$2", LocalDateTime.now().toString()));

        pgService.executePreparedQuery(stringBuilder.toString(), new JsonObject(), handler -> {
            if (handler.succeeded()) {
                assertTrue(handler.result().containsKey("result"));
                vertxTestContext.completeNow();
            } else {
                vertxTestContext.failNow(handler.cause());
            }
        });

    }

    @Test
    @DisplayName("Test executeQuery method : with invalid tuple")
    public void testExecutePreparedQueryFailure(VertxTestContext vertxTestContext) {
        StringBuilder stringBuilder = new StringBuilder(Constants.INSERT_REVOKE_TOKEN_SQL.replace("$1", UUID.randomUUID().toString()).replace("$2", LocalDateTime.now().toString()));

        JsonObject jsonObject = new JsonObject();
        jsonObject.put("id1", "dummy id1");
        jsonObject.put("id2", "Dummy id2");

        pgService.executePreparedQuery(stringBuilder.toString(), jsonObject, handler -> {
            if (handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
            } else {
                JsonObject expected = new JsonObject(handler.cause().getMessage());
                assertTrue(handler.cause().getMessage().contains("type"));
                assertTrue(handler.cause().getMessage().contains("status"));
                assertTrue(handler.cause().getMessage().contains("detail"));
                assertEquals("urn:dx:rs:DatabaseError", expected.getString("type"));
                assertEquals(400, expected.getInteger("status"));
                vertxTestContext.completeNow();
            }
        });
    }

}

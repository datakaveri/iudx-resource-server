package iudx.resource.server.database.postgres;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import iudx.resource.server.configuration.Configuration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class PostgresServiceImplTest {

    private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImplTest.class);
    private static PostgresServiceImpl pgService;
    private static PostgreSQLContainer<?> postgresContainer;
    public static String CONTAINER = "postgres:12.11";
    public static String database = "iudx";
    private static Configuration config;
    private static JsonObject dbConfig;

    @BeforeAll
    static void setup(Vertx vertx, VertxTestContext testContext) {

        config = new Configuration();
        dbConfig = config.configLoader(2, vertx);

        dbConfig.put("databaseIp", "localhost");
        dbConfig.put("databasePort", 5432);
        dbConfig.put("databaseName", database);
        dbConfig.put("databaseUserName", "iudx_user");
        dbConfig.put("databasePassword", "pg@postgres.dk");
        dbConfig.put("poolSize", 25);

        postgresContainer = new PostgreSQLContainer<>(CONTAINER).withInitScript("pg_test_schema.sql");

        postgresContainer.withUsername(dbConfig.getString("databaseUserName"));
        postgresContainer.withPassword(dbConfig.getString("databasePassword"));
        postgresContainer.withDatabaseName(dbConfig.getString("databaseName"));
        postgresContainer.withExposedPorts(dbConfig.getInteger("databasePort"));


        postgresContainer.start();
        if (postgresContainer.isRunning()) {
            dbConfig.put("databasePort", postgresContainer.getFirstMappedPort());

            PgConnectOptions connectOptions =
                    new PgConnectOptions()
                            .setPort(dbConfig.getInteger("databasePort"))
                            .setHost(dbConfig.getString("databaseIp"))
                            .setDatabase(dbConfig.getString("databaseName"))
                            .setUser(dbConfig.getString("databaseUserName"))
                            .setPassword(dbConfig.getString("databasePassword"))
                            .setReconnectAttempts(2)
                            .setReconnectInterval(1000);


            PoolOptions poolOptions = new PoolOptions().setMaxSize(dbConfig.getInteger("poolSize"));
            PgPool pool = PgPool.pool(vertx, connectOptions, poolOptions);

            pgService = new PostgresServiceImpl(pool);
            testContext.completeNow();
        }
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

package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.starter.ResourceServerStarter;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiServerVerticleTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiServerVerticleTest.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "127.0.0.1";

  private static WebClient client;

  ApiServerVerticleTest() {
  }

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);

    ResourceServerStarter starter = new ResourceServerStarter();
    Future<JsonObject> result = starter.startServer();

    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        testContext.completeNow();
      }
    });
  }

  @Test
  @Order(1)
  @Description("calling /entities endpoint")
  public void testHandleEntitiesQuery(Vertx vertx, VertxTestContext testContext)
      throws InterruptedException {
    Thread.sleep(30000);
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    // TODO : Need to update the ID to check with the actual database response.
    client.get(PORT, BASE_URL, apiURL + "?id=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

  @Test
  @Order(2)
  @Description("calling /temporal/entities endpoint")
  public void testHandleTemporalQuery(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_TEMPORAL_URL;
    // TODO : Need to update the ID to check with the actual database response.
    client.get(PORT, BASE_URL, apiURL + "?id=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

  // TODO : need to refactor code for Promise and Future.
  // @Test
  // @Order(3)
  // @Description("calling /entities endpoint with invalid parameters")
  public void testEntitiesBadRequestParam(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiURL + "?id2=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        assertTrue(res.containsKey("type"));
        assertTrue(res.containsKey("title"));
        assertTrue(res.containsKey("details"));

        assertEquals(res.getInteger("type"), 400);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

  @Test
  @Order(3)
  @Description("testing management api '/exchange' to create a exchange")
  public void testCreateExchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
    JsonObject request = new JsonObject();
    request.put("exchangeName", "abcdfef");
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
        assertEquals(res.getString("exchange"), request.getString("exchangeName"));
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

}

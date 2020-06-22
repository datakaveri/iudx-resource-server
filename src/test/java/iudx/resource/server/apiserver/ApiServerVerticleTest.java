package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

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
import iudx.resource.server.deploy.helper.ResourceServerDeployer;

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
    WebClientOptions clientOptions = new WebClientOptions().setSsl(false).setVerifyHost(false);
    client = WebClient.create(vertx, clientOptions);

    new Thread(() -> {
      ResourceServerDeployer.main(new String[] {});
    }).start();

    vertx.setTimer(20000, id -> {
      testContext.completeNow();
    });

  }

  @Test
  @Order(1)
  @Description("calling /entities endpoint")
  public void testHandleEntitiesQuery(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiURL + "?id=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        assertEquals(ar.result().statusCode(), ResponseType.Ok.getCode());
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
    client.get(PORT, BASE_URL, apiURL + "?id=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        assertEquals(ar.result().statusCode(), ResponseType.Ok.getCode());
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

  @Test
  @Order(3)
  @Description("calling /entities endpoint with invalid parameters")
  public void testEntitiesBadRequestParam(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiURL + "?id2=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ar.result().statusCode(), ResponseType.BadRequestData.getCode());
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

}

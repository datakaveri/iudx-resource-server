package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.cli.annotations.Description;
import io.vertx.core.json.JsonArray;
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
  private static String exchangeName;
  private static String queueName;
  private static String vhost;
  private static JsonArray entities;

  private static WebClient client;

  ApiServerVerticleTest() {
  }

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    WebClientOptions clientOptions = new WebClientOptions().setSsl(false).setVerifyHost(false)
        .setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);

    ResourceServerStarter starter = new ResourceServerStarter();
    Future<JsonObject> result = starter.startServer();

    result.onComplete(resultHandler -> {
      if (resultHandler.succeeded()) {
        testContext.completeNow();
      }
    });

    exchangeName = UUID.randomUUID().toString().replaceAll("-", "");
    queueName = UUID.randomUUID().toString().replaceAll("-", "");
    vhost = UUID.randomUUID().toString().replaceAll("-", "");
    entities = new JsonArray().add("id1").add("id2");

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

  @Test
  @Order(19)
  @Description("calling /entities endpoint with invalid parameters")
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
    request.put("exchangeName", exchangeName);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
        assertEquals(res.getString("exchangeName"), exchangeName);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(4)
  @Description("testing management api '/exchange' to create a already existing exchange")
  public void testCreateExchange400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
    JsonObject request = new JsonObject();
    request.put("exchange", exchangeName);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
        assertEquals(res.getString("title"), "Failure");
        assertEquals(res.getString("detail"), "Exchange already exists");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(5)
  @Description("testing management api '/queue' to create a queue")
  public void testCreateQueue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL;
    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
        assertEquals(res.getString("queue"), queueName);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(6)
  @Description("testing management api '/queue' to create a already existing queue")
  public void testCreateQueue400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL;
    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
        assertEquals(res.getString("title"), "Failure");
        assertEquals(res.getString("detail"), "Queue already exists");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(7)
  @Description("testing management api '/bind' to bind exchange to queue")
  public void testBindQueue2Exchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_BIND_URL;

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);
    request.put("queueName", queueName);
    request.put("entities", entities);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, handler -> {
      if (handler.succeeded()) {
        JsonObject res = new JsonObject();
        assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
        assertEquals(res.getString("exchange"), exchangeName);
        assertEquals(res.getString("queue"), queueName);
        assertEquals(res.getJsonArray("entities"), entities);
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(8)
  @Description("testing management api /exchange to get exchange details")
  public void testGetExchangeDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/" + exchangeName;
    client.get(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        assertEquals(response.getString(queueName), entities);
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  // TODO : correct according to type
  @Test
  @Order(9)
  @Description("testing management api '/queue' to get queue details")
  public void testGetQueueDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.get(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());

      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(10)
  @Description("testing management api '/unbind' to unbind exchange to queue")
  public void testUnbindQueue2Exchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_UNBIND_URL;
    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);
    request.put("queueName", queueName);
    request.put("entities", entities);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, handler -> {
      if (handler.succeeded()) {
        JsonObject res = new JsonObject();
        assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
        assertEquals(res.getString("exchange"), exchangeName);
        assertEquals(res.getString("queue"), queueName);
        assertEquals(res.getJsonArray("entities"), entities);
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(11)
  @Description("testing management api '/queue' to delete a queue")
  public void testDeleteQueue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.delete(PORT, BASE_URL, apiUrl).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        assertEquals(res.getString("queue"), queueName);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(12)
  @Description("testing management api '/queue' to delete a queue when no queue exist")
  public void testDeleteQueue404(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.delete(PORT, BASE_URL, apiUrl).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString("title"), "Failure");
        assertEquals(res.getString("detail"), "Queue does not exist");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(13)
  @Description("testing management api '/exchange' to delete a exchange")
  public void testDeleteExchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + exchangeName;
    client.delete(PORT, BASE_URL, apiUrl).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
        assertEquals(res.getString("exchange"), exchangeName);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(14)
  @Description("testing management api '/exchange' to delete a exchange when no exchange exist")
  public void testDeleteExchange404(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + exchangeName;
    client.delete(PORT, BASE_URL, apiUrl).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString("title"), "Failure");
        assertEquals(res.getString("detail"), "Exchange does not exist");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(15)
  @Description("testing management api '/vhost' to create a vhost")
  public void testCreateVhost(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL;
    JsonObject request = new JsonObject();
    request.put("vHost", vhost);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
        assertEquals(res.getString("vHost"), vhost);
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(16)
  @Description("testing management api '/vhost' to create a vhost which already exist")
  public void testCreateVhost400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL;
    JsonObject request = new JsonObject();
    request.put("vHost", vhost);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(request, handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
        assertEquals(res.getString("title"), "Failure");
        assertEquals(res.getString("detail"), "vHost already exists");
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(17)
  @Description("testing management api '/vhost' to delete a vhost")
  public void testDeleteVhost(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL + "/" + vhost;
    client.delete(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        assertEquals(res.getString("vHost"), vhost);
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(18)
  @Description("testing management api '/vhost' to delete a vhost when no vhost exist ")
  public void testDeleteVhost400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL + "/" + vhost;
    client.delete(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
        assertEquals(res.getString("title"), "Failure");
        // assertEquals(res.getString("detail"), "vHost already exists"); //TODO : wrong
        // message in databroker service
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

}

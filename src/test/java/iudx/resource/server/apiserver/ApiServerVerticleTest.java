package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
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
  private static String subscriptionId;
  private static String fakeToken;
  private static String adapterId;

  private static WebClient client;

  ApiServerVerticleTest() {
  }

  @BeforeAll
  public static void setup(Vertx vertx, VertxTestContext testContext) {
    WebClientOptions clientOptions = new WebClientOptions().setSsl(true).setVerifyHost(false)
        .setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);

    /*
     * ResourceServerStarter starter = new ResourceServerStarter();
     * Future<JsonObject> result = starter.startServer();
     * 
     * result.onComplete(resultHandler -> { if (resultHandler.succeeded()) {
     * testContext.completeNow(); } });
     * 
     */

    /*
     * result.onComplete(resultHandler -> { if (resultHandler.succeeded()) {
     * testContext.completeNow(); } });
     */
    exchangeName = UUID.randomUUID().toString().replaceAll("-", "");
    queueName = UUID.randomUUID().toString().replaceAll("-", "");
    vhost = UUID.randomUUID().toString().replaceAll("-", "");
    entities = new JsonArray()
        .add("rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live");
    fakeToken = UUID.randomUUID().toString();
    adapterId = UUID.randomUUID().toString();
    testContext.completeNow();
  }

  // @Test
  @Order(1)
  @DisplayName("test /entities endpoint")
  public void testHandleEntitiesQuery(Vertx vertx, VertxTestContext testContext)
      throws InterruptedException {
    // Thread.sleep(30000);
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    // TODO : Need to update the ID to check with the actual database response.
    client
        .get(PORT, BASE_URL,
            apiURL + "?id=rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .send(ar -> {
          if (ar.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failed();
          }
        });
  }

  // @Test
  @Order(2)
  @DisplayName("test /temporal/entities endpoint")
  public void testHandleTemporalQuery(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_TEMPORAL_URL;
    // TODO : Need to update the ID to check with the actual database response.
    client
        .get(PORT, BASE_URL,
            apiURL + "?id=rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .send(ar -> {
          if (ar.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failed();
          }
        });
  }

  // @Test
  @Order(3)
  @DisplayName("test /entities endpoint with invalid parameters")
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

  // @Test
  @Order(4)
  @DisplayName("test /entities endpoint for a circle geometry")
  public void testEntities4CircleGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("georel", "within")
        .addQueryParam("coordinates", "%5B82.987988%2C25.319768%5D").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(5)
  @DisplayName("test /entities endpoint for polygon geometry")
  public void testEntities4PolygonGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("georel", "within").addQueryParam("geometry", "polygon")
        .addQueryParam("coordinates",
            "%5B%5B114.78515624999999%2C66.26685631430843%5D%2C%5B114.78544056415559%2C66.26677642907407%5D%5D")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(6)
  @DisplayName("test /entities endpoint for linestring geometry")
  public void testEntities4LineStringGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("georel", "intersect").addQueryParam("geometry", "linestring")
        .addQueryParam("coordinates",
            "%5B%5B82.9735%2C25.3352%5D%5B82.9894%2C25.3452%5D%5B82.99%2C25.34%5D%5D")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(7)
  @DisplayName("test /entities endpoint for response filter query")
  public void testResponseFilterQuery(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("georel", "within").addQueryParam("geometry", "polygon")
        .addQueryParam("attr", "latitude,longitude,resource-id").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(8)
  @DisplayName("test /entities endpoint for bbox geometry")
  public void testEntities4BoundingBoxGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("georel", "within").addQueryParam("geometry", "bbox")
        .addQueryParam("coordinates", "%5B%5B82%2C25.33%5D%5B82.01%2C25.317%5D%5D")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(9)
  @DisplayName("test /entities for geo + responseFilter(attrs) ")
  public void testGeo_ResponseFilter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl)
        .addQueryParam("id",
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam("attrs", "latitude,longitude,resource-id").addQueryParam("georel", "within")
        .addQueryParam("geometry", "polygon")
        .addQueryParam("coordinates",
            "%5B%5B114.78515624999999%2C66.26685631430843%5D%2C%5B114.78544056415559%2C66.26677642907407%5D%5D")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(10)
  @DisplayName("test /entities for empty id")
  public void testBadRequestForEntities(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.post(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  /** Subscription API test **/

  // @Test
  @Order(100)
  @DisplayName("test /subscription endpoint to create a subscription")
  public void testCreateStreamingSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put("name", "test-streaming-name");
    json.put("type", "streaming");
    json.put("entities", entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", "testing-token").sendJsonObject(json,
        handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            subscriptionId = handler.result().bodyAsJsonObject().getString("subscriptionID");
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(101)
  @DisplayName("test /subscription endpoint to create subscription without token")
  public void testCreateStreaming401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put("name", "test-streaming-name");
    json.put("type", "streaming");
    json.put("entities", entities);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(json, handler -> {
      if (handler.succeeded()) {
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  // @Test
  @Order(103)
  @DisplayName("test /subscription endpoint to get a subscription")
  public void testGetSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    client.get(PORT, BASE_URL, apiUrl).putHeader("token", "testing-token").send(handler -> {
      if (handler.succeeded()) {
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  // @Test
  @Order(104)
  @DisplayName("test /subscription endpoint to delete a subscription")
  public void testDeleteSubs(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", "testing-token").send(handler -> {
      if (handler.succeeded()) {
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  /** Management API test cases **/

  @Test
  @Order(200)
  @DisplayName(" management api /exchange to create a exchange")
  public void testCreateExchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        ar -> {
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
  @Order(201)
  @DisplayName(" management api /exchange to create a already existing exchange")
  public void testCreateExchange400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL;
    JsonObject request = new JsonObject();
    request.put("exchange", exchangeName);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
            assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString("title"), "failure");
            assertEquals(res.getString("detail"), "Exchange already exists");
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failNow(ar.cause());
          }
        });
  }

  @Test
  @Order(203)
  @DisplayName(" management api /queue to create a queue")
  public void testCreateQueue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL;
    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        ar -> {
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
  @Order(204)
  @DisplayName(" management api /queue to create a already existing queue")
  public void testCreateQueue400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL;
    JsonObject request = new JsonObject();
    request.put("queueName", queueName);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
            assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString("title"), "failure");
            assertEquals(res.getString("detail"), "Queue already exists");
            testContext.completeNow();
          } else if (ar.failed()) {
            testContext.failNow(ar.cause());
          }
        });
  }

  @Test
  @Order(205)
  @DisplayName(" management api /bind to bind exchange to queue")
  public void testBindQueue2Exchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_BIND_URL;

    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);
    request.put("queueName", queueName);
    request.put("entities", entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertEquals(exchangeName, res.getString("exchange"));
            assertEquals(queueName, res.getString("queue"));
            assertEquals(entities, res.getJsonArray("entities"));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(206)
  @DisplayName(" management api /exchange to get exchange details")
  public void testGetExchangeDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/" + exchangeName;
    client.get(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        assertEquals(entities, response.getJsonArray(queueName));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  // TODO : correct according to type
  @Test
  @Order(207)
  @DisplayName(" management api /queue to get queue details")
  public void testGetQueueDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.get(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(208)
  @DisplayName(" management api /unbind to unbind exchange to queue")
  public void testUnbindQueue2Exchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_UNBIND_URL;
    JsonObject request = new JsonObject();
    request.put("exchangeName", exchangeName);
    request.put("queueName", queueName);
    request.put("entities", entities);
    LOGGER.info(request);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            LOGGER.info(res);
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            /*
             * assertEquals(exchangeName, res.getString("exchange"));
             * assertEquals(queueName, res.getString("queue")); assertEquals(entities,
             * res.getJsonArray("entities"));
             */
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(209)
  @DisplayName(" management api /queue to delete a queue")
  public void testDeleteQueue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(ar -> {
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
  @Order(210)
  @DisplayName(" management api /queue to delete a queue when no queue exist")
  public void testDeleteQueue404(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_QUEUE_URL + "/" + queueName;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString("title"), "failure");
        assertEquals(res.getString("detail"), "Queue does not exist");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(211)
  @DisplayName(" management api /exchange to delete a exchange")
  public void testDeleteExchange(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/" + exchangeName;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        assertEquals(res.getString("exchange"), exchangeName);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(212)
  @DisplayName(" management api /exchange to delete a exchange when no exchange exist")
  public void testDeleteExchange404(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_EXCHANGE_URL + "/" + exchangeName;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString("title"), "failure");
        assertEquals(res.getString("detail"), "Exchange not found");
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failNow(ar.cause());
      }
    });
  }

  @Test
  @Order(213)
  @DisplayName(" management api /vhost to create a vhost")
  public void testCreateVhost(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL;
    JsonObject request = new JsonObject();
    request.put("vHost", vhost);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        handler -> {
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
  @Order(214)
  @DisplayName(" management api /vhost to create a vhost which already exist")
  public void testCreateVhost400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL;
    JsonObject request = new JsonObject();
    request.put("vHost", vhost);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(request,
        handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertEquals(res.getInteger("type"), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString("title"), "failure");
            assertEquals(res.getString("detail"), "vHost already exists");
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(215)
  @DisplayName(" management api /vhost to delete a vhost")
  public void testDeleteVhost(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL + "/" + vhost;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
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
  @Order(216)
  @DisplayName(" management api /vhost to delete a vhost when no vhost exist ")
  public void testDeleteVhost400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_VHOST_URL + "/" + vhost;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject res = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        assertEquals(res.getInteger("type"), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString("title"), "failure");
        assertEquals(res.getString("detail"), "No vhosts found");
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(217)
  @DisplayName("management api /adapter/register without token")
  public void testAdapterRegistrationWithoutToken(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put("id", adapterId);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(requestJson, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        assertTrue(result.containsKey("type"));
        assertTrue(result.containsKey("title"));
        assertTrue(result.containsKey("detail"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(218)
  @DisplayName(" management api /adapter/register to register a adapter")
  public void testRegisterAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put("id", adapterId);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).putHeader("token", fakeToken)
        .sendJsonObject(requestJson, handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(result.containsKey("id"));
            assertTrue(result.containsKey("vHost"));
            assertTrue(result.containsKey("username"));
            assertTrue(result.containsKey("apiKey"));
            adapterId = result.getString("id");
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(219)
  @DisplayName(" management api /adapter/register to register already existing adapter")
  public void testRegisterAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put("id", adapterId);
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).putHeader("token", fakeToken)
        .sendJsonObject(requestJson, handler -> {
          if (handler.succeeded()) {
            // JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            // TODO: As per sheet correct response is not returned from databroker
            // assertTrue(result.containsKey("status"));
            // assertTrue(result.containsKey("title"));
            // assertTrue(result.containsKey("details"));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(220)
  @DisplayName("management api /adapter to get adapter details")
  public void testGetAdapterDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.get(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        // JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(221)
  @DisplayName("management api /adapter/heartbeat to publish data without token")
  public void testPublishHeartBeat401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "heartbeat");
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        // assertTrue(result.containsKey("status"));
        // assertTrue(result.containsKey("title"));
        // assertTrue(result.containsKey("details"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(222)
  @DisplayName("management api /adapter/heartbeat to publish data")
  public void testPublishHeartBeat(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "heartbeat");
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(json,
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(223)
  @DisplayName("management api /adapter/downstreamissue to publish data without token")
  public void testPublishDownstreamissue401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "server issue");
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        // assertTrue(result.containsKey("status"));
        // assertTrue(result.containsKey("title"));
        // assertTrue(result.containsKey("detail"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(224)
  @DisplayName("management api /adapter/downstreamissue to publish data")
  public void testPublishDownstreamissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "server issue");
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).sendJsonObject(json,
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(225)
  @DisplayName("management api /adapter/dataissue to publish data without token")
  public void testPublishDataissue401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "data issue");
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(json, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        // assertTrue(result.containsKey("status"));
        // assertTrue(result.containsKey("title"));
        // assertTrue(result.containsKey("details"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(226)
  @DisplayName("management api /adapter/dataissue to publish data")
  public void testPublishDataissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue";
    JsonObject json = new JsonObject();
    json.put("id", adapterId);
    json.put("time", LocalDateTime.now().toString());
    json.put("status", "data issue");
    client.post(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(227)
  @DisplayName("management api /adapter to delete a adapter")
  public void testDeleteAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
        assertTrue(result.containsKey("id"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(228)
  @DisplayName("management api /adapter to delete already deleted adapter")
  public void testDeleteAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader("token", fakeToken).send(handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        // assertTrue(result.containsKey("status"));
        // assertTrue(result.containsKey("title"));
        // assertTrue(result.containsKey("details"));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

}

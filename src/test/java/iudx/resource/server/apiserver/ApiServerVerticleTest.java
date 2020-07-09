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
     * testContext.completeNow(); } }); <<<<<<< HEAD
     * 
     * ======= >>>>>>> 7917b4209f984d10cf740174f86959fd256b8273
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

  @Test
  @Order(1)
  @DisplayName("test /entities endpoint with invalid parameters")
  public void testEntitiesBadRequestParam(Vertx vertx, VertxTestContext testContext) {
    String apiURL = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiURL + "?id2=id1,id2").send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertTrue(res.containsKey(Constants.JSON_TYPE));
        assertTrue(res.containsKey(Constants.JSON_TITLE));
        assertTrue(res.containsKey(Constants.JSON_DETAIL));
        assertEquals(res.getInteger(Constants.JSON_TYPE), 400);
        testContext.completeNow();
      } else if (ar.failed()) {
        testContext.failed();
      }
    });
  }

  @Test
  @Order(2)
  @DisplayName("test /entities endpoint for a circle geometry")
  public void testEntities4CircleGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxDistance=1000")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoJsonLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, "[25.319768,82.987988]").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(3)
  @DisplayName("test /entities endpoint for polygon geometry")
  public void testEntities4PolygonGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoJsonLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES,
            "[[[82.9738998413086,25.330372970610558],[82.97201156616211,25.28428253090838],[83.02436828613281,25.285524253944203],[83.02007675170898,25.32866622999033],[82.9738998413086,25.330372970610558]]]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(4)
  @DisplayName("test /entities endpoint for linestring geometry")
  public void testEntities4LineStringGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "intersects")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "linestring")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoJsonLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES,
            "[[82.97527313232422,25.292043091311733],[82.99467086791992,25.30678678767568],[83.00085067749023,25.323545863751555]]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(5)
  @DisplayName("test /entities endpoint for response filter query")
  public void testResponseFilterQuery(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE, "latitude,longitude,resource-id")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(6)
  @DisplayName("test /entities endpoint for bbox geometry")
  public void testEntities4BoundingBoxGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoJsonLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES,
            "[[82.97698974609375,25.321994194865383],[83.00411224365234,25.291267057619464]]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(7)
  @DisplayName("test /entities for geo + responseFilter(attrs) ")
  public void testGeo_ResponseFilter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE, "latitude,longitude,resource-id")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoJsonLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES,
            "[[[82.9738998413086,25.330372970610558],[82.97201156616211,25.28428253090838],[83.02436828613281,25.285524253944203],[83.02007675170898,25.32866622999033],[82.9738998413086,25.330372970610558]]]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(8)
  @DisplayName("test /entities for empty id")
  public void testBadRequestForEntities(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).send(handler -> {
      if (handler.succeeded()) {
        assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @Order(9)
  @DisplayName("test /temporal/entities for before relation")
  public void testTemporalEntitiesBefore(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "before")
        .addQueryParam(Constants.NGSILDQUERY_TIME, "2020-06-01T14:20:01Z").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }

  @Test
  @Order(10)
  @DisplayName("test /temporal/entities for after relation")
  public void testTemporalEntitiesAfter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "after")
        .addQueryParam(Constants.NGSILDQUERY_TIME, "2020-06-01T14:20:01Z").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }

  @Test
  @Order(11)
  @DisplayName("test /temporal/entities for between relation")
  public void testTemporalEntitiesBetween(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl)
        .addQueryParam(Constants.NGSILDQUERY_ID,
            "rs.varanasi.iudx.org.in/varanasi-swm-vehicles/varanasi-swm-vehicles-live")
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "during")
        .addQueryParam(Constants.NGSILDQUERY_TIME, "2020-06-01T14:20:01Z")
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, "2020-06-03T14:40:01Z").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
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
    json.put(Constants.JSON_NAME, Constants.JSON_STREAMING_NAME);
    json.put(Constants.JSON_TYPE, Constants.JSON_STREAMING_TYPE);
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            subscriptionId = handler.result().bodyAsJsonObject().getString(Constants.JSON_SUBS_ID);
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
    json.put(Constants.JSON_NAME, Constants.JSON_STREAMING_NAME);
    json.put(Constants.JSON_TYPE, Constants.JSON_STREAMING_TYPE);
    json.put(Constants.JSON_ENTITIES, entities);
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
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
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
  @Order(104)
  @DisplayName("test /subscription endpoint to delete a subscription")
  public void testDeleteSubs(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
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
    request.put(Constants.JSON_EXCHANGE_NAME, exchangeName);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
            assertEquals(res.getString(Constants.JSON_EXCHANGE), exchangeName);
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
    request.put(Constants.JSON_EXCHANGE_NAME, exchangeName);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
            assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
            assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_EXCHANGE_EXIST);
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
    request.put(Constants.JSON_QUEUE_NAME, queueName);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), ar.result().statusCode());
            assertEquals(res.getString(Constants.JSON_QUEUE), queueName);
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
    request.put(Constants.JSON_QUEUE_NAME, queueName);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
            assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
            assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_QUEUE_EXIST);
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
    request.put(Constants.JSON_EXCHANGE_NAME, exchangeName);
    request.put(Constants.JSON_QUEUE_NAME, queueName);
    request.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertEquals(exchangeName, res.getString(Constants.JSON_EXCHANGE));
            assertEquals(queueName, res.getString(Constants.JSON_QUEUE));
            assertEquals(entities, res.getJsonArray(Constants.JSON_ENTITIES));
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
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
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
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
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
    request.put(Constants.JSON_EXCHANGE_NAME, exchangeName);
    request.put(Constants.JSON_QUEUE_NAME, queueName);
    request.put(Constants.JSON_ENTITIES, entities);
    LOGGER.info(request);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, handler -> {
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        assertEquals(res.getString(Constants.JSON_QUEUE), queueName);
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
        assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_QUEUE_NOT_EXIST);
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.Ok.getCode(), ar.result().statusCode());
        assertEquals(res.getString(Constants.JSON_EXCHANGE), exchangeName);
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.BadRequestData.getCode(), ar.result().statusCode());
        assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
        assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
        assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_EXCHANGE_NOT_FOUND);
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
    request.put(Constants.JSON_VHOST, vhost);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertEquals(res.getString(Constants.JSON_VHOST), vhost);
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
    request.put(Constants.JSON_VHOST, vhost);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(request, handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
            assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
            assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_VHOST_EXIST);
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            assertEquals(res.getString(Constants.JSON_VHOST), vhost);
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
            assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
            assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_NO_VHOST);
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
    requestJson.put(Constants.JSON_ID, adapterId);
    client.post(PORT, BASE_URL, apiUrl).sendJsonObject(requestJson, handler -> {
      if (handler.succeeded()) {
        JsonObject result = handler.result().bodyAsJsonObject();
        assertEquals(ResponseType.AuthenticationFailure.getCode(), handler.result().statusCode());
        assertTrue(result.containsKey(Constants.JSON_TYPE));
        assertTrue(result.containsKey(Constants.JSON_TITLE));
        assertTrue(result.containsKey(Constants.JSON_DETAIL));
        testContext.completeNow();
      } else if (handler.failed()) {
        testContext.failNow(handler.cause());
      }
    });
  }

  //@Test
  @Order(218)
  @DisplayName(" management api /adapter/register to register a adapter")
  public void testRegisterAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put(Constants.JSON_ID, adapterId);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .putHeader(Constants.HEADER_TOKEN, fakeToken).sendJsonObject(requestJson, handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(result.containsKey(Constants.JSON_ID));
            assertTrue(result.containsKey(Constants.JSON_VHOST));
            assertTrue(result.containsKey(Constants.JSON_USERNAME));
            assertTrue(result.containsKey(Constants.JSON_APIKEY));
            adapterId = result.getString(Constants.JSON_ID);
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  //@Test
  @Order(219)
  @DisplayName(" management api /adapter/register to register already existing adapter")
  public void testRegisterAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put(Constants.JSON_ID, adapterId);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .putHeader(Constants.HEADER_TOKEN, fakeToken).sendJsonObject(requestJson, handler -> {
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

  //@Test
  @Order(220)
  @DisplayName("management api /adapter to get adapter details")
  public void testGetAdapterDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
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
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_HEARTBEAT);
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

  //@Test
  @Order(222)
  @DisplayName("management api /adapter/heartbeat to publish data")
  public void testPublishHeartBeat(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_HEARTBEAT);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(json, handler -> {
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
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_SERVERISSUE);
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

  //@Test
  @Order(224)
  @DisplayName("management api /adapter/downstreamissue to publish data")
  public void testPublishDownstreamissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS,Constants.JSON_STATUS_SERVERISSUE);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .sendJsonObject(json, handler -> {
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
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_DATAISSUE);
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

  //@Test
  @Order(226)
  @DisplayName("management api /adapter/dataissue to publish data")
  public void testPublishDataissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_DATAISSUE);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  //@Test
  @Order(227)
  @DisplayName("management api /adapter to delete a adapter")
  public void testDeleteAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            assertTrue(result.containsKey(Constants.JSON_ID));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  //@Test
  @Order(228)
  @DisplayName("management api /adapter to delete already deleted adapter")
  public void testDeleteAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, fakeToken)
        .send(handler -> {
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

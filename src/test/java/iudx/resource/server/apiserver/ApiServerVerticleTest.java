package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDateTime;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
import iudx.resource.server.apiserver.subscription.SubsType;
import iudx.resource.server.apiserver.util.Constants;
import iudx.resource.server.configuration.Configuration;

/* TODO : Need to update End to End Adaptor testing */
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
/**
 *@deprecated  As of current release, API test cases moved to Integration pipeline}
 */
@Deprecated
@Disabled
public class ApiServerVerticleTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(ApiServerVerticleTest.class);
  private static final int PORT = 8443;
  private static final String BASE_URL = "127.0.0.1";
  private static String exchangeName;
  private static String queueName;
  private static String vhost;
  private static JsonArray entities;
  private static String subscriptionId;
  // TODO: remove value after callback creates works.
  private static String callbackSubscriptionId = "abc/xyz/123";
  private static String publicToken;
  private static String adapterId;
  private static String resourceGroup;
  private static String resourceServer;
  private static String streamingSubscriptionAliasName;
  private static String callbackSubscriptionAliasName;
  private static String testId;
  private static Configuration config;
  private static String circleCoords;
  private static String polygonCoords;
  private static String bboxCoords;
  private static String lineCoords;
  private static String time;
  private static String endTime;
  private static String qparamGreater;
  private static String qparamLess;
  private static String qparamGreaterEqual;
  private static String qparamLessEqual;
  private static String authToken;
  private static String invalidauthToken;
  private static String entityId;


  private static WebClient client;

  ApiServerVerticleTest() {}

  @BeforeAll
  public static void setup(Vertx vertx, io.vertx.reactivex.core.Vertx vertx2,
      VertxTestContext testContext) {
    WebClientOptions clientOptions =
        new WebClientOptions().setSsl(true).setVerifyHost(false).setTrustAll(true);
    client = WebClient.create(vertx, clientOptions);

    config = new Configuration();
    JsonObject apiConfig = config.configLoader(4, vertx2);
    exchangeName = UUID.randomUUID().toString().replaceAll("-", "");
    queueName = UUID.randomUUID().toString().replaceAll("-", "");
    vhost = UUID.randomUUID().toString().replaceAll("-", "");


    // get test params from config
    testId = apiConfig.getString("resourceID");
    entityId = "iudx.org/cb03444a83e7d71f9b0894b1ae650d6b/rs.test.iudx.org.in/aqm-bosch-climo";
    entities = new JsonArray().add(entityId + "/*");
    circleCoords = apiConfig.getString("circleCoords");
    polygonCoords = apiConfig.getString("polygonCoords");
    bboxCoords = apiConfig.getString("bboxCoords");
    lineCoords = apiConfig.getString("lineCoords");
    time = apiConfig.getString("temporalTime");
    endTime = apiConfig.getString("temporalEndTime");
    qparamGreater = apiConfig.getString("qparamGreaterThan");
    qparamLess = apiConfig.getString("qparamLessThan");
    qparamGreaterEqual = apiConfig.getString("qparamGreaterEquals");
    qparamLessEqual = apiConfig.getString("qparamLessEquals");
    authToken = apiConfig.getString("authToken");
    invalidauthToken = apiConfig.getString("invalidauthToken");


    publicToken = "public";
    adapterId = UUID.randomUUID().toString();
    resourceGroup = UUID.randomUUID().toString();
    resourceServer = UUID.randomUUID().toString();
    streamingSubscriptionAliasName = "alias-streaming-test";
    callbackSubscriptionAliasName = "alias-callback-test";
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("test /entities endpoint with invalid parameters")
  public void testEntitiesBadRequestParam(Vertx vertx, VertxTestContext testContext)
      throws InterruptedException {
    // Thread.sleep(50000);
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
  @DisplayName("/entities endpoint for a circle geometry")
  public void testEntities4CircleGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxDistance=1000")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, circleCoords).send(handler -> {
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
  @DisplayName("/entities endpoint for polygon geometry")
  public void testEntities4PolygonGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, polygonCoords).send(handler -> {
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
  @DisplayName("/entities endpoint for linestring geometry")
  public void testEntities4LineStringGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "intersects")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "linestring")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, lineCoords).send(handler -> {
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
  @DisplayName("/entities endpoint for response filter query")
  public void testResponseFilterQuery(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
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
  @DisplayName("/entities endpoint for bbox geometry")
  public void testEntities4BoundingBoxGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords).send(handler -> {
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
  @DisplayName("/entities for geo + responseFilter(attrs) ")
  public void testGeo_ResponseFilter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE, "latitude,longitude,resource-id")
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, polygonCoords).send(handler -> {
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
  @DisplayName("/entities for attribute query (property >)")
  public void testAttributeQueryGreaterThan(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, qparamGreater).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(9)
  @DisplayName("/entities for attribute query (property <)")
  public void testAttributeQueryLessThan(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, qparamLess).send(handler -> {
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
  @DisplayName("/entities for attribute query (property >=)")
  public void testAttributeQueryGreaterEquals(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, qparamGreaterEqual).send(handler -> {
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
  @DisplayName("/entities for attribute query (property <=)")
  public void testAttributeQueryLessEquals(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, qparamLessEqual).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(12)
  @DisplayName("/entities for empty id")
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
  @Order(13)
  @DisplayName("/temporal/entities for before relation")
  public void testTemporalEntitiesBefore(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "before")
        .addQueryParam(Constants.NGSILDQUERY_TIME, endTime).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }

  @Test
  @Order(14)
  @DisplayName("/temporal/entities for after relation")
  public void testTemporalEntitiesAfter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "after")
        .addQueryParam(Constants.NGSILDQUERY_TIME, time).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }

  @Test
  @Order(15)
  @DisplayName("/temporal/entities for between relation")
  public void testTemporalEntitiesBetween(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "during")
        .addQueryParam(Constants.NGSILDQUERY_TIME, time)
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, endTime).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }

  @Test
  @Order(16)
  @DisplayName("/temporal/entities complex query (geo + temporal + response filter)")
  public void testComplexQuery(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "before")
        .addQueryParam(Constants.NGSILDQUERY_TIME, endTime)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxDistance=1000")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, circleCoords)
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
  @Order(17)
  @DisplayName("/entities endpoint for a circle geometry count")
  public void testCountEntities4CircleGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxDistance=100")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, circleCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(18)
  @DisplayName("/entities endpoint for polygon geometry count")
  public void testCountEntities4PolygonGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT)
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, polygonCoords).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(19)
  @DisplayName("/entities endpoint for linestring geometry count")
  public void testCountEntities4LineStringGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "intersects")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "linestring")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, lineCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(20)
  @DisplayName("/entities endpoint for bbox geometry count")
  public void testCountEntities4BoundingBoxGeom(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }


  @Test
  @Order(21)
  @DisplayName("/entities endpoint for invalid id validation (more than 100 char as resource id)")
  public void testValidation4Id(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    String id = testId
        + "S43sdliE9W4Gtaq0vZvBvZ9OBAwvWcJxFHatpa3Qh5IhnEJfj6C4pJTejBAtYoDbOWuA8cjX2N9THEG7KzovWkDi"
        + "koJU2dnxvzndxkdPfIQdM0LoMKO3SHiNrktEb27m4qTft4WjthUmMHFUa9eeFtOpDUcqJ4x5pLWxBbIFGDTFe3w8g"
        + "UWNZZQ762OwyCbzJfixtYCorFp6Odfq7hPRg2N07nRrL8SxCqjtlS1ywftFJ3RMdkHrlNSTAQo881vkluBu8ggTUc"
        + "BG6tCGCoVldJ3CTSiTZuM265UYBrxIUdBFGhchb2jcCGomlsvb6q6Qo1HBzicLLbHQ23sdpQ6gqJH1l0uOsnd0ZSa"
        + "yCusFd0YrroGXuYxII5Dk84XVNIEgrd4SyF6FYoWHcKLlZN3snexLP0atKhmnInEwYh6hgAMuYmkaogsbMxzPJa5y"
        + "nPynKqv2D8ByegALqyx7kRvjma08uUGbh6Zu75FrI8mKl2kJcfgjR5cpk1gJqDECyNkF";
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, id)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(22)
  @DisplayName("/entities endpoint for invalid number of attrs attributes (more than 6)")
  public void testValidationInvalidAttrsCount(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE,
            "temprature,speed,location,observationSpace,abc,xyz,wxy")
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(23)
  @DisplayName("/entities endpoint for invalid attrs attributes (Empty)")
  public void testValidationEmptyAttrsCount(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE, " ")
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(24)
  @DisplayName("/entities endpoint for invalid attrs attributes (exceed max char limit)")
  public void testValidationAttrsCharLimit(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.NGSILDQUERY_ATTRIBUTE,
            "S43sdliE9W4Gtaq0vZvBvZ9OBAwvWcJxFHatpa3Qh5IhnEJfj6C4pJTejBAtYoDbOWuA8cjX2N9THEG7KzovWkDi\"\n"
                + "        + \"koJU2dnxvzndxkdPfIQdM0LoMKO3SHiNrktEb27m4qTft4WjthUmMHFUa9eeFtOpDUcqJ4x5pLWxBbIFGDTFe3w8g\"\n"
                + "        + \"UWNZZQ762OwyCbzJfixtYCorFp6Odfq7hPRg2N07nRrL8SxCqjtlS1ywftFJ3RMdkHrlNSTAQo881vkluBu8ggTUc\"\n")
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(25)
  @DisplayName("/entities endpoint for invalid georel attributes")
  public void testValidationInvalidGeoRel(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within123")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(26)
  @DisplayName("/entities endpoint for invalid georel attributes (Empty)")
  public void testValidationEmptyGeoRel(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, " ")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "bbox")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(27)
  @DisplayName("/entities endpoint for invalid geometry")
  public void testValidationInvalidGeometry(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "ellipse")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(28)
  @DisplayName("/entities endpoint for invalid geometry (Empty)")
  public void testValidationEmptyGeometry(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, " ")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(29)
  @DisplayName("/entities endpoint for invalid geoproperty")
  public void testValidationInvalidGeoProperty(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "geoLocation")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, bboxCoords)
        .addQueryParam(Constants.IUDXQUERY_OPTIONS, Constants.JSON_COUNT).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(30)
  @DisplayName("/entities endpoint for invalid coordinates")
  public void testValidationInvalidCoordinates(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxdistance=10")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, "[21.178,72.834,32.8978]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(31)
  @DisplayName("/entities endpoint for invalid coordinates (precision)")
  public void testValidationInvalidCoordinatesPrecision(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "near;maxdistance=10")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "point")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES, "[21.178,72.834328978]").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(31)
  @DisplayName("/entities endpoint for invalid coordinates (count > maxlimit)")
  public void testValidationInvalidCoordinatesCount(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_GEOREL, "within")
        .addQueryParam(Constants.NGSILDQUERY_GEOMETRY, "polygon")
        .addQueryParam(Constants.NGSILDQUERY_GEOPROPERTY, "location")
        .addQueryParam(Constants.NGSILDQUERY_COORDINATES,
            "[[[72.77463,21.206658],[72.778072,21.198336],"
            + "[72.767257,21.193855],[72.772064,21.177849],"
            + "[72.788372,21.186492],[72.804851,21.176728],"
            + "[72.817039,21.188733],[72.817382,21.198336],"
            + "[72.802619,21.208418],[72.799358,21.211779],"
            + "[72.790431,21.208098],[72.783565,21.209219],"
            + "[72.774639,21.206658]]]")
        .send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(32)
  @DisplayName("/entities for attribute query (invalid operator)")
  public void testValidationsAttributeQueryInvalidOperator(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, "attributeName</20.5").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(33)
  @DisplayName("/entities for attribute query (invalid Value)")
  public void testValidationsAttributeQueryInvalidValue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_ENTITIES_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_Q, "attributeName</abc").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }
  
  @Test
  @Order(34)
  @DisplayName("/temporal/entities for invalid timerel")
  public void testValidationInvalidTimeRel(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "almost")
        .addQueryParam(Constants.NGSILDQUERY_TIME, time)
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, endTime).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }
  
  @Test
  @Order(35)
  @DisplayName("/temporal/entities for invalid timerel (Empty)")
  public void testValidationEmptyTimeRel(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, " ")
        .addQueryParam(Constants.NGSILDQUERY_TIME, time)
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, endTime).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }
  
  @Test
  @Order(36)
  @DisplayName("/temporal/entities for invalid time (Empty)")
  public void testValidationEmptyTime(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "during")
        .addQueryParam(Constants.NGSILDQUERY_TIME, time)
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, endTime).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }
  
  @Test
  @Order(36)
  @DisplayName("/temporal/entities for invalid time interval(duration > 10)")
  public void testValidationTimeDuration(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_TEMPORAL_URL;
    client.get(PORT, BASE_URL, apiUrl).addQueryParam(Constants.NGSILDQUERY_ID, testId)
        .addQueryParam(Constants.NGSILDQUERY_TIMEREL, "during")
        .addQueryParam(Constants.NGSILDQUERY_TIME, "2020-09-01T14:20:00Z")
        .addQueryParam(Constants.NGSILDQUERY_ENDTIME, "2020-09-19T14:20:00Z").send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });

  }
  

  /** Subscription API test **/

  @Test
  @Order(100)
  @DisplayName("/subscription endpoint to create a subscription")
  public void testCreateStreamingSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_ID));
            subscriptionId = handler.result().bodyAsJsonObject().getString(Constants.JSON_ID);
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(101)
  @DisplayName("/subscription endpoint to create subscription without token")
  public void testCreateStreaming401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(401, response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(102)
  @DisplayName("/subscription endpoint to create subscription without type in body")
  public void testCreateStreaming400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(400, response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(103)
  @DisplayName("/subscription endpoint to get a subscription without token in header")
  public void testGetStreamingSubscription401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage()).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(104)
  @DisplayName("/subscription endpoint to get subscription without type in body")
  public void testGetStreamingSub400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    client.get(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, publicToken).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(105)
  @DisplayName("/subscription endpoint to append a subscription without type in body")
  public void testAppendStreamingSubsctiption400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(testId);
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.patch(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(106)
  @DisplayName("/subscription endpoint to append subscription without token")
  public void testAppendSubsctiption401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(testId);
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.patch(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(107)
  @DisplayName("/subscription endpoint to append a subscription ")
  public void testAppendStreamingSubsctiption(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(entityId + "/#123");
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.patch(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_ENTITIES));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(108)
  @DisplayName("/subscription endpoint to update a subscription without token")
  public void testUpdateStreamingSubscription401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(testId);
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.put(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(108)
  @DisplayName("/subscription endpoint to update a subscription without  type")
  public void testUpdateStreamingSubscription400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(testId);
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.put(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(108)
  @DisplayName("/subscription endpoint to update a subscription with a non matching name and ID")
  public void testUpdateStreamingSubscription400NonMatchingNameFields(Vertx vertx,
      VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, callbackSubscriptionAliasName);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(testId);
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.put(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(108)
  @DisplayName("/subscription endpoint to update a subscription")
  public void testUpdateStreamingSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, streamingSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    JsonArray appendEntities = new JsonArray();
    appendEntities.add(entityId + "/#12345");
    json.put(Constants.JSON_ENTITIES, appendEntities);
    client.put(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_ENTITIES));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(109)
  @DisplayName("/subscription endpoint to get a subscription")
  public void testGetSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    System.out.println("subs  ID :" + subscriptionId);
    client.get(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(110)
  @DisplayName("/subscription endpoint to delete a subscription without type")
  public void testDeleteSubs400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    System.out.println("subs  ID :" + subscriptionId);
    client.delete(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(111)
  @DisplayName("/subscription endpoint to delete a subscription without token")
  public void testDeleteSubs401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    System.out.println("subs  ID :" + subscriptionId);
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage()).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                response.getInteger(Constants.JSON_TYPE));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(112)
  @DisplayName("/subscription endpoint to delete a subscription")
  public void testDeleteSubs(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + subscriptionId;
    System.out.println("subs  ID :" + subscriptionId);
    client.delete(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.STREAMING.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).send(handler -> {
          if (handler.succeeded()) {
            assertEquals(ResponseType.Ok.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }


  @Test
  @Order(113)
  @DisplayName("/subscription endpoint to create a callback subscription without token")
  public void testCreateCallbackSubscription401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, callbackSubscriptionAliasName);
    json.put(Constants.JSON_URL, "http://abc.xyz/callback");
    json.put(Constants.JSON_METHOD, "POST");
    json.put(Constants.JSON_USERNAME, "username");
    json.put(Constants.JSON_PASSWORD, "password");
    json.put(Constants.JSON_TYPE, Constants.SUBSCRIPTION);
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage())
        .sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test :: No relevance now for this test case
  @Order(114)
  @DisplayName("/subscription endpoint to create a callback subscription without type")
  public void testCreateCallbackSubscription400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, callbackSubscriptionAliasName);
    json.put(Constants.JSON_URL, "http://abc.xyz/callback");
    json.put(Constants.JSON_METHOD, "POST");
    json.put(Constants.JSON_USERNAME, "username");
    json.put(Constants.JSON_PASSWORD, "password");
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // TODO : uncomment and test, once callback subscription completed in databroker verticle
  // @Test
  @Order(115)
  @DisplayName("/subscription endpoint to create a callback subscription")
  public void testCreateCallbackSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL;
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_NAME, callbackSubscriptionAliasName);
    json.put(Constants.JSON_TYPE, SubsType.CALLBACK);
    json.put(Constants.JSON_URL, "http://abc.xyz/callback");
    json.put(Constants.JSON_METHOD, "POST");
    json.put(Constants.JSON_USERNAME, "username");
    json.put(Constants.JSON_PASSWORD, "password");
    json.put(Constants.JSON_ENTITIES, entities);
    client.post(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).sendJsonObject(json, handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_SUBS_ID));
            callbackSubscriptionId = response.getString(Constants.JSON_SUBS_ID);
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Order(116)
  @DisplayName("/subscription endpoint to get a callback subscription without token")
  public void testGetCallbackSubscription401(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + callbackSubscriptionId;
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage()).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }


  // @Test :: No relevance now for this test case
  @Order(117)
  @DisplayName("/subscription endpoint to get a callback subscription without type")
  public void testGetCallbackSubscription400NoType(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + callbackSubscriptionId;
    client.get(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            assertTrue(response.containsKey(Constants.JSON_TYPE));
            assertTrue(response.containsKey(Constants.JSON_TITLE));
            assertTrue(response.containsKey(Constants.JSON_DETAIL));
            assertEquals(ResponseType.BadRequestData.getCode(), handler.result().statusCode());
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // TODO : uncomment and test, once callback subscription completed in databroker verticle
  // @Test
  @Order(118)
  @DisplayName("/subscription endpoint to get a callback subscription")
  public void testGetCallbackSubscription(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.NGSILD_SUBSCRIPTION_URL + "/" + callbackSubscriptionId;
    client.get(PORT, BASE_URL, apiUrl)
        .putHeader(Constants.HEADER_OPTIONS, SubsType.CALLBACK.getMessage())
        .putHeader(Constants.HEADER_TOKEN, authToken).send(handler -> {
          if (handler.succeeded()) {
            JsonObject response = handler.result().bodyAsJsonObject();
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            assertEquals(ResponseType.AlreadyExists.getCode(), ar.result().statusCode());
            // TODO : discussion about Already exist code
            // JsonObject res = ar.result().bodyAsJsonObject();
            /*
             * 
             * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
             * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
             * assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_EXCHANGE_EXIST);
             */
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .sendJsonObject(request, ar -> {
          if (ar.succeeded()) {
            // JsonObject res = ar.result().bodyAsJsonObject();
            assertEquals(ResponseType.AlreadyExists.getCode(), ar.result().statusCode());
            /*
             * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
             * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
             * assertEquals(res.getString(Constants.JSON_DETAIL),
             * Constants.MSG_FAILURE_QUEUE_EXIST);
             */
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .sendJsonObject(request, handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            LOGGER.info(res);
            assertEquals(ResponseType.Created.getCode(), handler.result().statusCode());
            /*
             * assertEquals(exchangeName, res.getString("exchange")); assertEquals(queueName,
             * res.getString("queue")); assertEquals(entities, res.getJsonArray("entities"));
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken).send(ar -> {
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.ResourceNotFound.getCode(), ar.result().statusCode());
        /*
         * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
         * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
         * assertEquals(res.getString(Constants.JSON_DETAIL),
         * Constants.MSG_FAILURE_QUEUE_NOT_EXIST);
         */
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken).send(ar -> {
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken).send(ar -> {
      if (ar.succeeded()) {
        JsonObject res = ar.result().bodyAsJsonObject();
        assertEquals(ResponseType.ResourceNotFound.getCode(), ar.result().statusCode());
        /*
         * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
         * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
         * assertEquals(res.getString(Constants.JSON_DETAIL),
         * Constants.MSG_FAILURE_EXCHANGE_NOT_FOUND);
         */
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .sendJsonObject(request, handler -> {
          if (handler.succeeded()) {
            // JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AlreadyExists.getCode(), handler.result().statusCode());
            /*
             * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NO_CONTENT);
             * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
             * assertEquals(res.getString(Constants.JSON_DETAIL),
             * Constants.MSG_FAILURE_VHOST_EXIST);
             */
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .send(handler -> {
          if (handler.succeeded()) {
            JsonObject res = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.ResourceNotFound.getCode(), handler.result().statusCode());
            /*
             * assertEquals(res.getInteger(Constants.JSON_TYPE), HttpStatus.SC_NOT_FOUND);
             * assertEquals(res.getString(Constants.JSON_TITLE), Constants.MSG_FAILURE);
             * assertEquals(res.getString(Constants.JSON_DETAIL), Constants.MSG_FAILURE_NO_VHOST);
             */
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
    requestJson.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    requestJson.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, invalidauthToken)
        .sendJsonObject(requestJson, handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result().bodyAsJsonObject();
            assertEquals(ResponseType.AuthenticationFailure.getCode(),
                handler.result().statusCode());
            assertTrue(result.containsKey(Constants.JSON_TYPE));
            assertTrue(result.containsKey(Constants.JSON_TITLE));
            assertTrue(result.containsKey(Constants.JSON_DETAIL));
            testContext.completeNow();
          } else if (handler.failed()) {
            testContext.failNow(handler.cause());
          }
        });
  }

  // @Test
  @Order(218)
  @DisplayName(" management api /adapter/register to register a adapter")
  public void testRegisterAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    requestJson.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .putHeader(Constants.HEADER_TOKEN, publicToken).sendJsonObject(requestJson, handler -> {
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

  // @Test
  @Order(219)
  @DisplayName(" management api /adapter/register to register already existing adapter")
  public void testRegisterAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/register";
    JsonObject requestJson = new JsonObject();
    requestJson.put(Constants.JSON_RESOURCE_GROUP, resourceGroup);
    requestJson.put(Constants.JSON_RESOURCE_SERVER, resourceServer);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
        .putHeader(Constants.HEADER_TOKEN, publicToken).sendJsonObject(requestJson, handler -> {
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

  // @Test
  @Order(220)
  @DisplayName("management api /adapter to get adapter details")
  public void testGetAdapterDetails(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.get(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

  // @Test
  @Order(222)
  @DisplayName("management api /adapter/heartbeat to publish data")
  public void testPublishHeartBeat(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/heartbeat";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_HEARTBEAT);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

  // @Test
  @Order(224)
  @DisplayName("management api /adapter/downstreamissue to publish data")
  public void testPublishDownstreamissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/downstreamissue";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_SERVERISSUE);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

  // @Test
  @Order(226)
  @DisplayName("management api /adapter/dataissue to publish data")
  public void testPublishDataissue(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/dataissue";
    JsonObject json = new JsonObject();
    json.put(Constants.JSON_ID, adapterId);
    json.put(Constants.JSON_TIME, LocalDateTime.now().toString());
    json.put(Constants.JSON_STATUS, Constants.JSON_STATUS_DATAISSUE);
    client.post(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

  // @Test
  @Order(227)
  @DisplayName("management api /adapter to delete a adapter")
  public void testDeleteAdapter(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

  // @Test
  @Order(228)
  @DisplayName("management api /adapter to delete already deleted adapter")
  public void testDeleteAdapter400(Vertx vertx, VertxTestContext testContext) {
    String apiUrl = Constants.IUDX_MANAGEMENT_ADAPTER_URL + "/" + adapterId;
    client.delete(PORT, BASE_URL, apiUrl).putHeader(Constants.HEADER_TOKEN, authToken)
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

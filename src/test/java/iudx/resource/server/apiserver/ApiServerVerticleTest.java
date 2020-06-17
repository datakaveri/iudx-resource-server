package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ApiServerVerticleTest {

  @BeforeEach
  public void setup(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new ApiServerVerticle(), handler -> {
      if (handler.succeeded()) {
        testContext.completeNow();
      }
    });

  }

  // @Test
  public void testHandleEntitiesQuery(Vertx vertx, VertxTestContext testContext) {
//    TestSuite.create("api test").beforeEach(context -> {
//      vertx.eventBus().consumer("iudx.rs.database.service").handler(message -> {
//        message.reply(new JsonObject());
//      });
//    }).test("api calls entities", context -> {
//      WebClient client = WebClient.create(vertx);
//      client.get(8443, "localhost", "/ngsi-ld/v1/entities/?id=id1,id2").as(BodyCodec.string())
//          .send(testContext.succeeding(response -> testContext.verify(() -> {
//            assertEquals(response.statusCode(), 200);
//            testContext.completeNow();
//          })));
//    }).run();
//    testContext.completeNow();
  }

  // @Test
  public void testHello(Vertx vertx, VertxTestContext testContext) {
    WebClient client = WebClient.create(vertx);
    client.get(8443, "localhost", "/ngsi-ld/v1/hello").as(BodyCodec.string())
        .send(testContext.succeeding(response -> testContext.verify(() -> {
          assertEquals(response.statusCode(), 200);
          testContext.completeNow();
        })));
  }

  @AfterEach
  public void teardown(Vertx vertx, VertxTestContext testContext) {
  }

}

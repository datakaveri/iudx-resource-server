package iudx.resource.server.apiserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.database.DatabaseService;

@ExtendWith(VertxExtension.class)
public class ApiServerVerticleTest {

	private Vertx vertx;
	DatabaseService databaseService = mock(DatabaseService.class);

	@BeforeEach
	public void setup(Vertx vertx, VertxTestContext testContext) {
		vertx.deployVerticle(new ApiServerVerticle(), testContext.succeeding(id -> testContext.completeNow()));
	}

	// @Test
	public void apiSearchTest(Vertx vertx, VertxTestContext testContext) {
		String url = "/ngsi-ld/v1/entities/?id=id1&geoproperty=location&georel=near;maxDistance==360&geometry=Point&coordinates=%5B8.684783577919006%2C49.406131991436396%5D";
		WebClient client = WebClient.create(vertx);
		JsonArray respBody = new JsonArray();
		AsyncResult<JsonArray> asyncResult = mock(AsyncResult.class);

		when(asyncResult.succeeded()).thenReturn(true);
		when(asyncResult.result()).thenReturn(respBody);

		doAnswer((Answer<AsyncResult<JsonArray>>) arguments -> {
			((Handler<AsyncResult<JsonArray>>) arguments.getArgument(0)).handle(asyncResult);
			return null;
		}).when(databaseService).searchQuery(any(), any());

		client.get(8443, "localhost", url).as(BodyCodec.string())
				.send(testContext.succeeding(response -> testContext.verify(() -> {
					JsonObject res = new JsonObject(response.body());
					assertEquals(response.statusCode(), ResponseType.Ok.getCode());
					assertTrue(res.containsKey("id"));
					testContext.completeNow();
				})));

	}

	@AfterEach
	public void teardown(Vertx vertx, VertxTestContext testContext) {
	}

}

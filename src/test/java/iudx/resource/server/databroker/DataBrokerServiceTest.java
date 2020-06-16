package iudx.resource.server.databroker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URLEncoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class DataBrokerServiceTest {

  DataBrokerService service;
  private WebClientOptions webConfig;
  private WebClient webClient;

  DataBrokerServiceTest() {
    Vertx vertx = Vertx.vertx();
    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost("68.183.80.248");
    webConfig.setDefaultPort(15672);
    webConfig.setKeepAliveTimeout(86400000);

    webClient = WebClient.create(vertx, webConfig);
  }

  @BeforeAll
  @DisplayName("Deploy a verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    vertx.deployVerticle(new DataBrokerVerticle(),
        testContext.succeeding(id -> testContext.completeNow()));
  }


  @Test
  @Disabled
  void successCreateExchange(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    System.out.println("INSIDE successTestEvent");
    request.put("exchangeName", "ankita_exchange11");
    String exchangeName = request.getString("exchangeName");
    String vhost = URLEncoder.encode("/");
    String url = "/api/exchanges/" + vhost + "/" + exchangeName;
    JsonObject obj = new JsonObject();
    obj.put("type", Constants.EXCHANGE_TYPE);
    obj.put("auto_delete", false);
    obj.put("durable", true);
    HttpRequest<Buffer> webRequest =
        webClient.put(url).basicAuthentication("IUDXDemoUser", "IUDXDemoUser@123");
    webRequest.sendJsonObject(obj, testContext.succeeding(response -> testContext.verify(() -> {
      System.out.println(response.statusCode());
      assertEquals(response.statusCode(), 201);
      testContext.completeNow();
    })));
  }

  @Test
  void successCreateQueue(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    System.out.println("INSIDE successTestEvent");
    request.put("queueName", "ankita_queue11");
    String queueName = request.getString("queueName");
    String vhost = URLEncoder.encode("/");
    String url = "/api/exchanges/" + vhost + "/" + queueName;
    String user = "IUDXDemoUser";
    String password = "IUDXDemoUser@123";
    url = "/api/queues/" + vhost + "/" + queueName;
    JsonObject configProp = new JsonObject();
    configProp.put("x-message-ttl", Constants.X_MESSAGE_TTL);
    configProp.put("x-max-length", Constants.X_MAXLENGTH);
    configProp.put("x-queue-mode", Constants.X_QUEQUE_MODE);
    HttpRequest<Buffer> webRequest = webClient.put(url).basicAuthentication(user, password);
    webRequest.sendJsonObject(configProp,
        testContext.succeeding(response -> testContext.verify(() -> {
          System.out.println(response.statusCode());
          assertEquals(response.statusCode(), 201);
          testContext.completeNow();
        })));
  }

}



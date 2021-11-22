package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.RestResponse;
import iudx.resource.server.database.postgres.PostgresService;
import iudx.resource.server.databroker.DataBrokerService;

public final class AdminRestApi {

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService RMQbrokerService;
  private final PostgresService pgService;

  AdminRestApi(Vertx vertx, DataBrokerService brokerService, PostgresService pgService) {
    this.vertx = vertx;
    this.RMQbrokerService = brokerService;
    this.pgService = pgService;
    this.router = Router.router(vertx);
  }

  public Router init() {
    router
        .post(Api.REVOKE_TOKEN.getPath())
        .handler(this::handleRevokeTokenRequest);

    return router;
  }


  private void handleRevokeTokenRequest(RoutingContext context) {
    HttpServerResponse response = context.response();

    JsonObject requestBody = context.getBodyAsJson();
    String sql = "INSERT INTO revoked_tokens(client_id,rs_url,token,expiry) VALUES($1,$2,$3,$4)";
    sql.replace("$1", requestBody.getString("client_id"));
    sql.replace("$2", requestBody.getString("rs_url"));
    sql.replace("$3", requestBody.getString("token"));
    sql.replace("$4", requestBody.getString("expiry"));

    pgService.executeQuery(sql, pgSuccessHandler -> {
      if (pgSuccessHandler.succeeded()) {
        RMQbrokerService.publishMessage(new JsonObject(), "rs-token-invalidation", "rs-token-invalidation",
            rmqSuccessHandler -> {
              if (rmqSuccessHandler.succeeded()) {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(200)
                    .end(new RestResponse.Builder()
                        .withType("200")
                        .withTitle("success")
                        .withMessage("passed")
                        .build().toJson().toString());
              } else {
                response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .setStatusCode(400)
                    .end(new RestResponse.Builder()
                        .withType("400")
                        .withTitle("failed")
                        .withMessage("failed")
                        .build().toJson().toString());
              }
            });
      } else {
        response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
            .setStatusCode(400)
            .end(new RestResponse.Builder()
                .withType("400")
                .withTitle("failed")
                .withMessage("failed")
                .build().toJson().toString());
      }
    });


  }

}

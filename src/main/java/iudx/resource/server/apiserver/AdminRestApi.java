package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.util.Constants.APPLICATION_JSON;
import static iudx.resource.server.apiserver.util.Constants.CONTENT_TYPE;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.RestResponse;
import iudx.resource.server.database.archives.DatabaseService;
import iudx.resource.server.databroker.DataBrokerService;

public final class AdminRestApi {

  private final Vertx vertx;
  private final Router router;
  private final DataBrokerService RMQbrokerService;
  private final DatabaseService DbService;

  AdminRestApi(Vertx vertx,DataBrokerService brokerService,DatabaseService dbService) {
    this.vertx = vertx;
    this.RMQbrokerService=brokerService;
    this.DbService=dbService;
    this.router = Router.router(vertx);
  }

  public Router init() {
    router
        .post(Api.REVOKE_TOKEN.getPath())
        .handler(this::handlerRevokeTokenRequest);

    return router;
  }


  private void handlerRevokeTokenRequest(RoutingContext context) {
    HttpServerResponse response = context.response();
    
    
   JsonObject requestBody=context.getBodyAsJson();
   RMQbrokerService.publishFromAdaptor(new JsonObject(), handler->{
     
   });

    response.putHeader(CONTENT_TYPE, APPLICATION_JSON)
        .setStatusCode(200)
        .end(new RestResponse.Builder()
            .withType("200")
            .withTitle("success")
            .withMessage("passed")
            .build().toJson().toString());
  }

}

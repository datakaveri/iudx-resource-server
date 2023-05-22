package iudx.resource.server.apiserver;

import static iudx.resource.server.apiserver.util.Constants.USER_ID;
import static iudx.resource.server.common.HttpStatusCode.UNAUTHORIZED;
import static iudx.resource.server.common.ResponseUrn.INVALID_TOKEN_URN;

import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.response.ResponseType;
import iudx.resource.server.common.HandleResponse;
import iudx.resource.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ManagementRestApi {

  private static final Logger LOGGER = LogManager.getLogger(ManagementRestApi.class);

  private final DataBrokerService rmqBrokerService;
  public HandleResponse handleResponseToReturn;

  ManagementRestApi(DataBrokerService brokerService) {
    this.rmqBrokerService = brokerService;
    handleResponseToReturn = new HandleResponse();
  }

  public void resetPassword(RoutingContext routingContext) {
    LOGGER.trace("Info: resetPassword method started");

    HttpServerResponse response = routingContext.response();
    JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
    JsonObject request = new JsonObject();
    request.put(USER_ID, authInfo.getString(USER_ID));

    rmqBrokerService.resetPassword(
        request,
        handler -> {
          if (handler.succeeded()) {
            handleResponseToReturn.handleSuccessResponse(
                response, ResponseType.Ok.getCode(), handler.result().toString());
          } else {
            handleResponseToReturn.handleResponse(response, UNAUTHORIZED, INVALID_TOKEN_URN);
          }
        });
  }
}

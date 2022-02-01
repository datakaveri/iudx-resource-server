package iudx.resource.server.apiserver;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.handlers.AuthHandler;
import iudx.resource.server.apiserver.handlers.FailureHandler;
import iudx.resource.server.apiserver.handlers.ValidationHandler;
import iudx.resource.server.apiserver.util.RequestType;
import iudx.resource.server.common.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AsyncRestApi {

	private static final Logger LOGGER = LogManager.getLogger(AsyncRestApi.class);

	private final Vertx vertx;
	private final Router router;

	AsyncRestApi(Vertx vertx) {
		this.vertx = vertx;
		this.router = Router.router(vertx);
	}

	public Router init() {

		FailureHandler validationsFailureHandler = new FailureHandler();
		ValidationHandler asyncSearchValidationHandler = new ValidationHandler(vertx, RequestType.ASYNC);

		router
				.get(Api.SEARCH.path)
				.handler(asyncSearchValidationHandler)
				.handler(AuthHandler.create(vertx))
				.handler(this::handleAsyncSearchRequest)
				.handler(validationsFailureHandler);

		router
				.get(Api.STATUS.path)
				.handler(asyncSearchValidationHandler)
				.handler(AuthHandler.create(vertx))
				.handler(this::handleAsyncStatusRequest)
				.handler(validationsFailureHandler);

		return router;
	}

	private void handleAsyncSearchRequest(RoutingContext routingContext) {
	}

	private void handleAsyncStatusRequest(RoutingContext routingContext) {
	}
}

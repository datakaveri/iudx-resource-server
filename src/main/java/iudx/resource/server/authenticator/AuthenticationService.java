package iudx.resource.server.authenticator;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Authentication Service.
 * <h1>Authentication Service</h1>
 * <p>
 * The Authentication Service in the IUDX Resource Server defines the operations
 * to be performed with the IUDX Authentication and Authorization server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface AuthenticationService {

  /**
   * The tokenInterospect method implements the authentication and authorization
   * module using IUDX APIs.
   * 
   * @param request            which is a JsonObject
   * @param authenticationInfo which is a JsonObject
   * @param handler            which is a request handler
   * @return AuthenticationService which is a service
   */

  @Fluent
  AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * 
   * @param vertx   which is the vertx instance
   * @param address which is the proxy address
   * @return AuthenticationServiceVertxEBProxy which is a service proxy
   */

  @GenIgnore
  static AuthenticationService createProxy(Vertx vertx, String address) {
    return new AuthenticationServiceVertxEBProxy(vertx, address);
  }
}

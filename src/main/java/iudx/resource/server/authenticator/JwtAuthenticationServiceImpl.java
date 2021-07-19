package iudx.resource.server.authenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;

public class JwtAuthenticationServiceImpl implements AuthenticationService {

  final JWTAuth jwtAuth;

  JwtAuthenticationServiceImpl(final JWTAuth jwtAuth) {
    this.jwtAuth = jwtAuth;
  }

  @Override
  public AuthenticationService tokenInterospect(JsonObject request, JsonObject authenticationInfo,
      Handler<AsyncResult<JsonObject>> handler) {

    isAuthenticJwt(authenticationInfo.getString("token"));

    return this;
  }


  private Future<Boolean> isAuthenticJwt(String jwtToken) {
    Promise<Boolean> promise = Promise.promise();
    
    
    
//    jwtAuth.authenticate(
//        new JsonObject()
//            .put("token", jwtToken)
//            .put("options", new JsonObject()
//                .put("ignoreExpiration", true)))
//        .onSuccess(user -> {
//          System.out.println("User: " + user.principal());
//          promise.complete();
//        }).onFailure(err -> {
//          System.out.println("failed : " + err);
//          promise.fail("failed");
//        });

    return promise.future();
  }

}

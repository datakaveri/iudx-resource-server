package iudx.resource.server.databroker.util;

import java.util.function.Function;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class ExchangeRollbackFunction<T,U> implements Function<T,U>{
  
  private  String exchange;
  private  String vhost;
  
  public ExchangeRollbackFunction(String exchange,String vhost) {
    // TODO Auto-generated constructor stub
    this.exchange=exchange;
    this.vhost=vhost;
  }

  @Override
  public U apply(T arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}

package iudx.resource.server.apiserver.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.function.Function;
import io.vertx.core.json.JsonArray;

public class Util {
  
  public static <T> List<T> toList(JsonArray arr) {
    if (arr == null) {
      return null;
    } else {
      return (List<T>) arr.getList();
    }
  }
  

  public static Function<String, URI> toUriFunction = (value) -> {
    URI uri = null;
    try {
      uri = new URI(value);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return uri;
  };

}

package iudx.resource.server.apiserver.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

public class Util {

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

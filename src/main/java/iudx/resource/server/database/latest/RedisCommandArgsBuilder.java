package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.DEFAULT_ATTRIBUTE;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCommandArgsBuilder {

  private static final Logger LOGGER = LogManager.getLogger(RedisCommandArgsBuilder.class);

  public RedisArgs getRedisCommandArgs(String id, boolean isGroup) {
    RedisArgs args = new RedisArgs();

    LOGGER.debug("In LatestSearch Redis");

    String key = id.replace("-", "_")
        .replaceAll("/", "_")
        .replaceAll("\\.", "_");

    args.setKey(key);
    // SHA1 generator
    String sha = DigestUtils.sha1Hex(id);
    LOGGER.debug("Generated SHA1: " + sha);

    StringBuilder pathParam = new StringBuilder();

    if (isGroup) {
      // itms type resource
      pathParam.append(".");
    } else {
      // aqm type resource
      pathParam.append(".")
          .append("_")
          .append(sha)
          .append(DEFAULT_ATTRIBUTE);

    }
    args.setPath(pathParam.toString());
    return args;
  }
}

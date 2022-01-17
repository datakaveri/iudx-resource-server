package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.DEFAULT_ATTRIBUTE;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCommandArgsBuilder {

  private static final Logger LOGGER = LogManager.getLogger(RedisCommandArgsBuilder.class);

  public RedisArgs getRedisCommandArgs(String id, boolean isUniqueAttribueExist) {
    RedisArgs args = new RedisArgs();

    LOGGER.debug("In LatestSearch Redis");

    String key = id.replace("-", "_")
        .replaceAll("/", "_")
        .replaceAll("\\.", "_");

    args.setKey(key);

    StringBuilder pathParam = new StringBuilder();

    if (isUniqueAttribueExist) {
      // itms type resource
      pathParam.append(".");
    } else {
      // aqm type resource
      StringBuilder shaId = new StringBuilder(id).append("/").append(DEFAULT_ATTRIBUTE);
      String sha = DigestUtils.sha1Hex(shaId.toString());
      pathParam.append(".")
          .append("_")
          .append(sha);

    }
    args.setPath(pathParam.toString());
    return args;
  }
}

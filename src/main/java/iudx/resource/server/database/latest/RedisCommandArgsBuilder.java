package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.DEFAULT_ATTRIBUTE;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCommandArgsBuilder {

  private static final Logger LOGGER = LogManager.getLogger(RedisCommandArgsBuilder.class);

  public RedisArgs getRedisCommandArgs(
      String id, boolean isUniqueAttribueExist, String tenantPrefix) {
    LOGGER.trace("In LatestSearch Redis");

    String idKey = id.replace("-", "_").replaceAll("/", "_").replaceAll("\\.", "_");
    /*
     * example: key =
     * iudx:iisc_ac_in_89a36273d77dac4cf38114fca1bbe64392547f86_rs_iudx_io_pune_env_flood_FWR055
     * where "iudx" redis namespace and key is the other part
     */
    RedisArgs args = new RedisArgs();
    if (!tenantPrefix.equals("none")) {
      String namespace = tenantPrefix.concat(":");
      idKey = namespace.concat(idKey);
    }
    final String key = idKey;

    args.setKey(key);

    StringBuilder pathParam = new StringBuilder();

    if (isUniqueAttribueExist) {
      // itms type resource
      pathParam.append(".");
    } else {
      // aqm type resource
      StringBuilder shaId = new StringBuilder(id).append("/").append(DEFAULT_ATTRIBUTE);
      String sha = DigestUtils.sha1Hex(shaId.toString());
      pathParam.append(".").append("_").append(sha);
    }
    args.setPath(pathParam.toString());
    return args;
  }
}

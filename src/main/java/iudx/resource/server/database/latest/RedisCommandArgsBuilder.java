package iudx.resource.server.database.latest;

import static iudx.resource.server.database.archives.Constants.DEFAULT_ATTRIBUTE;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCommandArgsBuilder {

  private static final Logger LOGGER = LogManager.getLogger(RedisCommandArgsBuilder.class);

  public RedisArgs getRedisCommandArgs(String id, boolean isUniqueAttribueExist,
      String tenantPrefix) {
    RedisArgs args = new RedisArgs();

    LOGGER.trace("In LatestSearch Redis");

    String idKey = id.replace("-", "_").replaceAll("/", "_").replaceAll("\\.", "_");
    /*
     * example: key = iudx:
     * suratmunicipal_org_6db486cb4f720e8585ba1f45a931c63c25dbbbda_rs_iudx_org_in_surat_itms_realtime_info_surat_itms_live_eta
     * where "iudx" redis namespace and key is the other part
     */
    if (!tenantPrefix.equals("none"))
      ;
    {
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

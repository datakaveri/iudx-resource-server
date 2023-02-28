package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.metering.util.Constants.LIMIT_QUERY;
import static iudx.resource.server.metering.util.Constants.OFFSET_QUERY;

public class LimitOffSet {
    private static final Logger LOGGER = LogManager.getLogger(LimitOffSet.class);
    JsonObject jsonObject = null;
    StringBuilder finalQuery = null;
    String limit = null;
    String offset = null;
    LimitOffSet(JsonObject jO, StringBuilder q) {
        this.jsonObject = jO;
        this.finalQuery = q;
    }

    public StringBuilder setLimitOffset() {
        limit = jsonObject.getString(LIMITPARAM);
        offset = jsonObject.getString(OFFSETPARAM);
        finalQuery.append(LIMIT_QUERY
                .replace("$7", limit));

        finalQuery.append(OFFSET_QUERY
                .replace("$8", offset));

        return finalQuery;
    }
}

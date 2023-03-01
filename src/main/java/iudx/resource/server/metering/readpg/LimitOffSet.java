package iudx.resource.server.metering.readpg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.metering.util.Constants.LIMIT_QUERY;
import static iudx.resource.server.metering.util.Constants.OFFSET_QUERY;

public class LimitOffSet {
    private static final Logger LOGGER = LogManager.getLogger(LimitOffSet.class);
    int limit, offset;
    StringBuilder finalQuery = null;

    LimitOffSet(int limit, int offset, StringBuilder q) {
        this.limit = limit;
        this.offset = offset;
        this.finalQuery = q;
    }

    public StringBuilder setLimitOffset() {

        finalQuery.append(LIMIT_QUERY
                .replace("$7", Integer.toString(limit)));

        finalQuery.append(OFFSET_QUERY
                .replace("$8", Integer.toString(offset)));

        return finalQuery;
    }
}

package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;

import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;

public class ReadQueryBuilder {
    MeteringReadBuilder meteringReadBuilder = null;

    public String getQuery(JsonObject jsonObject) {
        String query = null;
        String checkProvider = jsonObject.getString(PROVIDER_ID);
        if (checkProvider != null) {
            meteringReadBuilder = new ProviderBuilder(jsonObject);
            query = meteringReadBuilder.add();
        } else {
            meteringReadBuilder = new ConsumerBuilder(jsonObject);
            query = meteringReadBuilder.add();
        }
        int limit = Integer.parseInt(jsonObject.getString(LIMITPARAM));
        int offset = Integer.parseInt(jsonObject.getString(OFFSETPARAM));
        LimitOffSet limitOffSet = new LimitOffSet(limit, offset, new StringBuilder(query));
        query = String.valueOf(limitOffSet.setLimitOffset());
        return query;
    }
}

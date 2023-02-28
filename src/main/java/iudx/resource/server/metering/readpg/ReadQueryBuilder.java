package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;

public class ReadQueryBuilder {
    private static final Logger LOGGER = LogManager.getLogger(ReadQueryBuilder.class);
    MeteringReadBuilder meteringReadBuilder = null;
    public String getQuery(JsonObject jsonObject){
        String query = null;
        String checkProvider = jsonObject.getString(PROVIDER_ID);
        if(checkProvider!=null){
            meteringReadBuilder = new ProviderBuilder(jsonObject);
            query = meteringReadBuilder.add();
        }
        else {
            meteringReadBuilder = new ConsumerBuilder(jsonObject);
            query = meteringReadBuilder.add();
        }
        JsonObject limitOffset = new JsonObject().put(LIMITPARAM,jsonObject.getString(LIMITPARAM))
                .put(OFFSETPARAM,jsonObject.getString(OFFSETPARAM));
        LimitOffSet limitOffSet = new LimitOffSet(limitOffset, new StringBuilder(query));
        query = String.valueOf(limitOffSet.setLimitOffset());
        return query;
    }
}

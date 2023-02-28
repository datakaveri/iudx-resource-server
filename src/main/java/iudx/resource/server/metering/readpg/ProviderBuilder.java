package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;

import static iudx.resource.server.apiserver.util.Constants.LIMITPARAM;
import static iudx.resource.server.apiserver.util.Constants.OFFSETPARAM;
import static iudx.resource.server.metering.util.Constants.*;
import static iudx.resource.server.metering.util.Constants.TABLE_NAME;

public class ProviderBuilder implements MeteringReadBuilder {
    private static final Logger LOGGER = LogManager.getLogger(ProviderBuilder.class);
    JsonObject jsonObject;
    String providerQuery = PROVIDERID_TIME_INTERVAL_READ_QUERY;
    StringBuilder finalQuery = null;
    public ProviderBuilder(JsonObject object) {
        this.jsonObject = object;
    }

    @Override
    public String add() {
        String startTime = jsonObject.getString(START_TIME);
        String endTime = jsonObject.getString(END_TIME);
        String resourceId = jsonObject.getString(RESOURCE_ID);
        String userId = jsonObject.getString(USER_ID);
        String api = jsonObject.getString(API);
        String providerID = jsonObject.getString(PROVIDER_ID);
        String consumerID = jsonObject.getString(CONSUMER_ID);
        String databaseTableName = jsonObject.getString(TABLE_NAME);

        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long fromTime = getEpochTime(startZDT);
        long toTime = getEpochTime(endZDT);

        finalQuery = new StringBuilder(providerQuery
                .replace("$0",databaseTableName)
                .replace("$1",Long.toString(fromTime))
                .replace("$2",Long.toString(toTime))
                .replace("$3",providerID));

        if(resourceId!=null){
            finalQuery.append(RESOURCEID_QUERY
                    .replace("$5", resourceId));
        }

        if(api!=null){
            finalQuery.append(API_QUERY
                    .replace("$4", api));
        }

        if(consumerID!=null){
            finalQuery.append(USER_ID_QUERY
                    .replace("$6", userId));
        }

        finalQuery.append(ORDER_BY);

        LOGGER.debug("From Provider Builder = "+ finalQuery);
        return finalQuery.toString();
    }

    @Override
    public long getEpochTime(ZonedDateTime time) {
        return time.toInstant().toEpochMilli();
    }
}

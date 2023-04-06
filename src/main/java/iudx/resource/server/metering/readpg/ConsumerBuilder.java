package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;

import java.time.ZonedDateTime;

import static iudx.resource.server.metering.util.Constants.*;

public class ConsumerBuilder implements MeteringReadBuilder {
    JsonObject jsonObject;
    String consumerQuery = CONSUMERID_TIME_INTERVAL_READ_QUERY;
    StringBuilder finalQuery = null;

    public ConsumerBuilder(JsonObject object) {
        this.jsonObject = object;
    }

    @Override
    public String add() {
        String startTime = jsonObject.getString(START_TIME);
        String endTime = jsonObject.getString(END_TIME);
        String resourceId = jsonObject.getString(RESOURCE_ID);
        String userId = jsonObject.getString(USER_ID);
        String api = jsonObject.getString(API);
        String databaseTableName = jsonObject.getString(TABLE_NAME);

        finalQuery = new StringBuilder(consumerQuery
                .replace("$0", databaseTableName)
                .replace("$1", startTime)
                .replace("$2", endTime)
                .replace("$3", userId));

        if (resourceId != null) {
            finalQuery.append(RESOURCEID_QUERY
                    .replace("$5", resourceId));
        }

        if (api != null) {
            finalQuery.append(API_QUERY
                    .replace("$4", api));
        }

        finalQuery.append(ORDER_BY);

        return finalQuery.toString();
    }

    @Override
    public long getEpochTime(ZonedDateTime time) {
        return time.toInstant().toEpochMilli();
    }
}

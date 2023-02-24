package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;

import static iudx.resource.server.metering.util.Constants.*;

public class ConsumerBuilder implements ReadDecorator{
    private static final Logger LOGGER = LogManager.getLogger(ConsumerBuilder.class);
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
        String limit = jsonObject.getString("limit");
        String offset = jsonObject.getString("offset");

        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long fromTime = getEpochTime(startZDT);
        long toTime = getEpochTime(endZDT);

        finalQuery = new StringBuilder(consumerQuery
                .replace("$0",databaseTableName)
                .replace("$1",Long.toString(fromTime))
                .replace("$2",Long.toString(toTime))
                .replace("$3",userId));

        if(resourceId!=null){
            finalQuery = finalQuery.append(" and resourceid = '$5' "
                    .replace("$5", resourceId));
        }

        if(api!=null){
            finalQuery = finalQuery.append(" and api = '$4' "
                    .replace("$4", api));
        }

        finalQuery = finalQuery.append(ORDER_BY);

        if(limit!=null || offset!=null){
            LimitOffSet limitOffset = new LimitOffSet(jsonObject,finalQuery);
            finalQuery = limitOffset.setLimitOffset();
        }
        LOGGER.debug("From Consumer Builder = "+ finalQuery);
        return finalQuery.toString();
    }

    @Override
    public long getEpochTime(ZonedDateTime time) {
        return time.toInstant().toEpochMilli();
    }
}

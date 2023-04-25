package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;

import static iudx.resource.server.metering.util.Constants.*;

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

    finalQuery =
        new StringBuilder(
            providerQuery
                .replace("$0", databaseTableName)
                .replace("$1", startTime)
                .replace("$2", endTime)
                .replace("$3", providerID));

    if (resourceId != null) {
      finalQuery.append(RESOURCEID_QUERY.replace("$5", resourceId));
    }

    if (api != null) {
      finalQuery.append(API_QUERY.replace("$4", api));
    }

    if (consumerID != null) {
      finalQuery.append(USER_ID_QUERY.replace("$6", consumerID));
    }

    finalQuery.append(ORDER_BY);

    return finalQuery.toString();
  }

  @Override
  public long getEpochTime(ZonedDateTime time) {
    return time.toInstant().toEpochMilli();
  }
}

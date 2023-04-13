package iudx.resource.server.metering.readpg;

import static iudx.resource.server.metering.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;

public class ProviderBuilder implements MeteringReadBuilder {
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
    String api = jsonObject.getString(API);
    String providerId = jsonObject.getString(PROVIDER_ID);
    String databaseTableName = jsonObject.getString(TABLE_NAME);

    finalQuery =
        new StringBuilder(
            providerQuery
                .replace("$0", databaseTableName)
                .replace("$1", startTime)
                .replace("$2", endTime)
                .replace("$3", providerId));

    if (resourceId != null) {
      finalQuery.append(RESOURCEID_QUERY.replace("$5", resourceId));
    }

    if (api != null) {
      finalQuery.append(API_QUERY.replace("$4", api));
    }
    String consumerId = jsonObject.getString(CONSUMER_ID);
    if (consumerId != null) {
      finalQuery.append(USER_ID_QUERY.replace("$6", consumerId));
    }

    finalQuery.append(ORDER_BY);

    return finalQuery.toString();
  }

  @Override
  public long getEpochTime(ZonedDateTime time) {
    return time.toInstant().toEpochMilli();
  }
}

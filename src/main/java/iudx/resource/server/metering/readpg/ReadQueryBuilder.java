package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.resource.server.metering.util.Constants.PROVIDER_ID;

public class ReadQueryBuilder {
    private static final Logger LOGGER = LogManager.getLogger(ReadQueryBuilder.class);
    ReadDecorator readDecorator = null;
    public String getQuery(JsonObject jsonObject){
        String query = null;
        String checkProvider = jsonObject.getString(PROVIDER_ID);
        if(checkProvider!=null){
            readDecorator = new ProviderBuilder(jsonObject);
            query = readDecorator.add();
        }
        else {
            readDecorator = new ConsumerBuilder(jsonObject);
            query = readDecorator.add();
        }

        return query;
    }
}

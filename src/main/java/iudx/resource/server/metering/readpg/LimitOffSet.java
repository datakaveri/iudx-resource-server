package iudx.resource.server.metering.readpg;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LimitOffSet {
    private static final Logger LOGGER = LogManager.getLogger(LimitOffSet.class);
    JsonObject jsonObject = null;
    StringBuilder finalQuery= null;
    LimitOffSet(JsonObject jO, StringBuilder q){
        this.jsonObject = jO;
        this.finalQuery = q;
    }
    String limit = null;
    String offset =null;
    public StringBuilder setLimitOffset(){
        limit = jsonObject.getString("limit");
        offset = jsonObject.getString("offset");
        if(limit!=null){
            finalQuery = finalQuery.append(" limit '$7'"
                    .replace("$7", limit));
        }

        if(offset!=null){
            finalQuery = finalQuery.append(" offset '$8'"
                    .replace("$8", offset));
        }
        return finalQuery;
    }

}

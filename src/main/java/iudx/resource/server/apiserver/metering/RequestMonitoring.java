package iudx.resource.server.apiserver.metering;

import static iudx.resource.server.apiserver.util.Constants.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import iudx.resource.server.apiserver.metering.DBConnect;
public class RequestMonitoring {
	
	  private static final Logger LOGGER = LogManager.getLogger(RequestMonitoring.class);
	  static DBConnect connect=new DBConnect();
	  
	  public static void updateDB(RoutingContext routingContext) {
		
		  JsonObject authInfo = (JsonObject) routingContext.data().get("authInfo");
		  String api=routingContext.currentRoute().getPath();
		  String email=authInfo.getString(JSON_CONSUMER);
		  String split[]=routingContext.queryParams().toString().split("/");
		  String resource_ID=split[1];
		  long current_Time_Stamp = System.currentTimeMillis();
		  LOGGER.info("API : " +api);
		  LOGGER.info("email : " +email);
		  LOGGER.info("resource_id : " +resource_ID);
		  LOGGER.info("TimeStamp in epoch : " +current_Time_Stamp);
//		  String email,String resourceid,String Api,long timestamp
		  connect.addData(email,resource_ID, api, current_Time_Stamp);
		  LOGGER.info("Updated Successfully");
		  
		  
		  int nanoOfSecond = 0;
		  ZoneOffset offset = ZoneOffset.UTC;
		  LocalDateTime ldt = LocalDateTime.ofEpochSecond(current_Time_Stamp, nanoOfSecond, offset);
		  System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(ldt));
		  
	     }
	  public static void readDB() {
		  connect.getData();
	  }
	  
}

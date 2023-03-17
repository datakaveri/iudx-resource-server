package iudx.resource.server.metering.util;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import static iudx.resource.server.apiserver.util.Constants.ENDT;
import static iudx.resource.server.apiserver.util.Constants.STARTT;
import static iudx.resource.server.metering.util.Constants.*;

public class DateValidation {
    private static final Logger LOGGER = LogManager.getLogger(DateValidation.class);
    public JsonObject dateParamCheck(JsonObject request){

        //since + is treated as space in uri
        String startTime = request.getString(STARTT).trim().replaceAll("\\s", "+");
        String endTime = request.getString(ENDT).trim().replaceAll("\\s", "+");

        ZonedDateTime zdt;
        try {
            zdt = ZonedDateTime.parse(startTime);
            LOGGER.debug("Parsed time: " + zdt.toString());
            zdt = ZonedDateTime.parse(endTime);
            LOGGER.debug("Parsed time: " + zdt.toString());
        } catch (DateTimeParseException e) {
            LOGGER.error("Invalid Date exception: " + e.getMessage());
            return new JsonObject().put(ERROR, INVALID_DATE_TIME);
        }
        ZonedDateTime startZDT = ZonedDateTime.parse(startTime);
        ZonedDateTime endZDT = ZonedDateTime.parse(endTime);

        long zonedDateTimeDayDifference = zonedDateTimeDayDifference(startZDT, endZDT);
        long zonedDateTimeMinuteDifference = zonedDateTimeMinuteDifference(startZDT, endZDT);

        LOGGER.trace(
                "PERIOD between given time day :{} , minutes :{}",
                zonedDateTimeDayDifference,
                zonedDateTimeMinuteDifference);

        if (zonedDateTimeDayDifference < 0
                || zonedDateTimeMinuteDifference <= 0) {
            LOGGER.error(INVALID_DATE_DIFFERENCE);
            return new JsonObject().put(ERROR, INVALID_DATE_DIFFERENCE);
        }
        request.put(STARTT, startTime);
        request.put(ENDT, endTime);
        return request;

    }
    private long zonedDateTimeDayDifference(ZonedDateTime startTime, ZonedDateTime endTime) {
        return ChronoUnit.DAYS.between(startTime, endTime);
    }

    private long zonedDateTimeMinuteDifference(ZonedDateTime startTime, ZonedDateTime endTime) {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }
}

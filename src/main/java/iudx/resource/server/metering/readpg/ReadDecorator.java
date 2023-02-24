package iudx.resource.server.metering.readpg;

import java.time.ZonedDateTime;

public interface ReadDecorator {
    String add();
     long getEpochTime(ZonedDateTime time) ;
}

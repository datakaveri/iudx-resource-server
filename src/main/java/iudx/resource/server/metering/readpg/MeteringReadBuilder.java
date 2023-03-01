package iudx.resource.server.metering.readpg;

import java.time.ZonedDateTime;

public interface MeteringReadBuilder {
    String add();

    long getEpochTime(ZonedDateTime time);
}

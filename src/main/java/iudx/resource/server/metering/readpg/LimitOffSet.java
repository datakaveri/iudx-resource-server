package iudx.resource.server.metering.readpg;

import static iudx.resource.server.metering.util.Constants.*;

public class LimitOffSet {
  int limit;
  int offset;
  StringBuilder finalQuery = null;

  LimitOffSet(int limit, int offset, StringBuilder q) {
    this.limit = limit;
    this.offset = offset;
    this.finalQuery = q;
  }

  public StringBuilder setLimitOffset() {

    finalQuery.append(LIMIT_QUERY.replace("$7", Integer.toString(limit)));

    finalQuery.append(OFFSET_QUERY.replace("$8", Integer.toString(offset)));

    return finalQuery;
  }
}

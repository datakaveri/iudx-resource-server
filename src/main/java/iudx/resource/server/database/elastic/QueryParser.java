package iudx.resource.server.database.elastic;

import org.elasticsearch.index.query.BoolQueryBuilder;

public interface QueryParser {
  BoolQueryBuilder parse();
}

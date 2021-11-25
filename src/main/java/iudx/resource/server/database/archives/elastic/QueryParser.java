package iudx.resource.server.database.archives.elastic;

import org.elasticsearch.index.query.BoolQueryBuilder;

public interface QueryParser {
  BoolQueryBuilder parse();
}

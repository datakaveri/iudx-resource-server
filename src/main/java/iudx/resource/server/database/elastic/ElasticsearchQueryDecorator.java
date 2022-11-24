package iudx.resource.server.database.elastic;

import java.util.List;
import java.util.Map;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

public interface ElasticsearchQueryDecorator {
  Map<FilterType,List<Query>> add();
}

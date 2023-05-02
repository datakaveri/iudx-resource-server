package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public interface ConvertElasticResponse {
    void write(List<Hit<ObjectNode>> searchHits);
    void start(List<Hit<ObjectNode>> searchHits);
    void end();
}

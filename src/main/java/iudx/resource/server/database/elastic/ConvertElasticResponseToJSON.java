package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.List;

public class ConvertElasticResponseToJSON implements Convert {
 private File file;
    public ConvertElasticResponseToJSON(File file)
    {
        this.file = file;
    }
    @Override
    public void write(List<Hit<ObjectNode>> searchHits) {

    }
}

package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ConvertElasticResponseToJSON implements ConvertElasticResponse {
    private File file;
    private FileWriter filew;
    private static final Logger LOGGER = LogManager.getLogger(ConvertElasticResponseToJSON.class);

    /**
     * Writes ElasticSearch response batch response into a JSON File
     * @param file File to write JSON response
     */
    public ConvertElasticResponseToJSON(File file) {
        try {
            this.file = file;
            this.filew = new FileWriter(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void start(List<Hit<ObjectNode>> searchHits)
    {
        try {
            filew.write('[');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void end()
    {
        try {
            filew.write(']');
            filew.close();
//            long kb = file.length() / 1024;
//            LOGGER.debug("JSON File length in MB : {}", kb/ 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void write(List<Hit<ObjectNode>> searchHits) {
        try {
            boolean appendComma = false;
            for (Hit<ObjectNode> sh : searchHits) {
                if (appendComma) {
                    filew.write("," + sh.source().toString());
                } else {
                    filew.write(sh.source().toString());
                }
                appendComma = true;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

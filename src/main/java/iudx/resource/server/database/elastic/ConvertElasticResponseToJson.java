package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ConvertElasticResponseToJson extends AbstractConvertElasticSearchResponse {
  private final FileWriter fileWriter;
  //    private static final Logger LOGGER =
  // LogManager.getLogger(ConvertElasticResponseToJSON.class);

  /**
   * Writes ElasticSearch response batch response into a JSON File
   *
   * @param file File to write JSON response
   */
  public ConvertElasticResponseToJson(File file) {
    super(file);
    try {
      this.fileWriter = new FileWriter(file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void write(List<Hit<ObjectNode>> searchHits) {
    try {
      fileWriter.write('[');
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void append(List<Hit<ObjectNode>> searchHits, boolean isLastRecord) {
    try {
      fileWriter.write(']');
      fileWriter.close();
      //            long kb = file.length() / 1024;
      //            LOGGER.debug("JSON File length in MB : {}", kb/ 1024);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void append(List<Hit<ObjectNode>> searchHits) {
    try {
      boolean appendComma = false;
      for (Hit<ObjectNode> sh : searchHits) {
        if (appendComma) {
          fileWriter.write("," + sh.source().toString());
        } else {
          fileWriter.write(sh.source().toString());
        }
        appendComma = true;
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}

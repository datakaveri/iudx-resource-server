package iudx.resource.server.database.elastic;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConvertElasticResponseToCSV implements ConvertElasticResponse {
    private static final Logger LOGGER = LogManager.getLogger(ConvertElasticResponseToCSV.class);
    private File csvFile;


    /**
     * Converts JSON records from Elasticsearch batch response to CSV format and writes it into a CSV file
     * @param file File to write csv records
     */
    public ConvertElasticResponseToCSV(File file) {
        this.csvFile = file;
    }

    /**
     * Flattens each record from Elastic search response and appends it to the file
     * @param searchHits
     */
    public void flattenRecord(List<Hit<ObjectNode>> searchHits) {
        for (Hit hit : searchHits) {
            Map<String, Object> map = new JsonFlatten((JsonNode) hit.source()).flatten();
            Set<String> header = map.keySet();
            appendToCSVFile(map, header);
        }
    }

    /**
     * Produces set of headers from the first record or row of the data
     * @param searchHits Elastic search scroll response
     */
    public void getHeader(List<Hit<ObjectNode>> searchHits) {
        for (Hit hit : searchHits) {
            Map<String, Object> map = new JsonFlatten((JsonNode) hit.source()).flatten();
            Set<String> header = map.keySet();
            simpleFileWriter(header);
            break;
        }
    }

    /**
     * Appends the values from the records to the csv file according to the header
     * @param map Data to be appended
     * @param header Set of headers
     */
    private void appendToCSVFile(Map<String, Object> map, Set<String> header) {
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(csvFile, true);
            StringBuilder stringBuilder = new StringBuilder();

            for (String field : header) {
                Object cell = map.get(field);
                if (cell == null) {
                    stringBuilder.append("" + ",");
                } else {
                    stringBuilder.append(cell + ",");
                }
            }


            String row = stringBuilder.substring(0, stringBuilder.length() - 1);
            fileWriter.append(row).append("\n");

            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Writes the column names or the header for the csv file
     * @param header  Set of headers to be written
     */
    private void simpleFileWriter(Set<String> header) {
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(csvFile);
            StringBuilder stringBuilder = new StringBuilder();
            for (String obj : header) {
                stringBuilder.append(obj + ",");
            }
            String data = stringBuilder.substring(0, stringBuilder.length() - 1);
        try {
            fileWriter.write(data + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(List<Hit<ObjectNode>> searchHits) {
        this.flattenRecord(searchHits);
    }

    @Override
    public void start(List<Hit<ObjectNode>> searchHits) {
        this.getHeader(searchHits);
    }

    @Override
    public void end() {
    }
}

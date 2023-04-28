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

public class ConvertElasticResponseToCSV implements Convert{
    private static final Logger LOGGER = LogManager.getLogger(ConvertElasticResponseToCSV.class);
    private File csvFile;

    public ConvertElasticResponseToCSV(File file) {
        this.csvFile = file;
        LOGGER.debug("File path : {} ", file.getAbsoluteFile());
    }


    public void appendToFile(List<Hit<ObjectNode>> searchHits) {
        for (Hit hit : searchHits) {
            Map<String, Object> map = new JsonFlatten((JsonNode) hit.source()).flatten();
//          LOGGER.debug("Map : " + map);
//          LOGGER.debug("Field names | header : "+ map.keySet());
//         make this as the csv header

            Set<String> header = map.keySet();
            appendToCSVFile(map, header);
        }
    }

    public void getHeader(List<Hit<ObjectNode>> searchHits) {
        for (Hit hit : searchHits) {
            Map<String, Object> map = new JsonFlatten((JsonNode) hit.source()).flatten();
            Set<String> header = map.keySet();
            LOGGER.debug("Whats this csvFile hereee : {}", csvFile);
            simpleFileWriter(header, csvFile);
            break;
        }
    }

    private void appendToCSVFile(Map<String, Object> map, Set<String> header) {
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(csvFile, true);
            StringBuilder stringBuilder = new StringBuilder();

//      LOGGER.debug("map.entrySet() : " + map.entrySet());
            for (String field : header) {
                var cell = map.get(field);
                if (cell == null) {
                    stringBuilder.append("" + ",");
                } else {
                    stringBuilder.append(cell + ",");
                }
//        LOGGER.debug("map.get(field) : " + map.get(field));
            }


            String row = stringBuilder.substring(0, stringBuilder.length() - 1);
//      LOGGER.debug("ROW : " + row);
            fileWriter.append(row).append("\n");

            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    // for writing headers
    private void simpleFileWriter(Set<String> header, File file) {
        FileWriter fileWriter;
        LOGGER.debug("Whats the file path here ? : {}" , file);
        if(!file.exists())
        {
            createCSVFile(file);
        }
        try {
            fileWriter = new FileWriter(csvFile);
            StringBuilder stringBuilder = new StringBuilder();
            for (String obj : header) {
                stringBuilder.append(obj + ",");
            }
            String data = stringBuilder.substring(0, stringBuilder.length() - 1);
            fileWriter.write(data + "\n");

            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(List<Hit<ObjectNode>> searchHits) {
        this.getHeader(searchHits);
        this.appendToFile(searchHits);
    }

    private void createCSVFile(File file)
    {
        try {
            file.getParentFile().mkdirs();

            LOGGER.debug("whats file here : " + file.getPath());

            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package iudx.resource.server.database.elastic;

import java.io.File;

import static iudx.resource.server.apiserver.util.Constants.HEADER_CSV;
import static iudx.resource.server.apiserver.util.Constants.HEADER_PARQUET;

public class ConvertFactory {
    private ConvertElasticResponseToCSV responseToCSV;
    private ConvertElasticResponseToParquet responseToParquet;
    private String format;
    private File file;

    public ConvertFactory(String filePath, String searchId, String format) {
        this.format = format;
        file = new File(filePath + "/" + searchId + "-" + format + "." + format);
        responseToCSV = new ConvertElasticResponseToCSV(file);
        responseToParquet = new ConvertElasticResponseToParquet(file);
    }

    public Convert createInstance() {
        switch (format) {
            case HEADER_PARQUET: {
                return responseToParquet;
            }
            case HEADER_CSV: {
                return responseToCSV;
            }
            default: {
                throw new IllegalArgumentException("Parquet, CSV, JSON are allowed file formats");
            }
        }
    }


}

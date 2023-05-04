package iudx.resource.server.database.elastic;

import java.io.File;

import static iudx.resource.server.apiserver.util.Constants.HEADER_CSV;

public class ConvertElasticResponseFactory  {
    private AbstractConvertElasticSearchResponse responseToCSV;
//    private AbstractConvertElasticSearchResponse responseToParquet;
    private AbstractConvertElasticSearchResponse responseToJSON;
    private String format;

    public ConvertElasticResponseFactory(String format, File file) {
        this.format = format;
        responseToCSV = new ConvertElasticResponseToCSV(file);
//        responseToParquet = new ConvertElasticResponseToParquet(file);
        responseToJSON = new ConvertElasticResponseToJSON(file);
    }

    public ConvertElasticResponse createInstance() {
        switch (format) {
            case HEADER_CSV:
                return responseToCSV;
//            case HEADER_PARQUET:
//                return responseToParquet;
            default:
                return responseToJSON;
        }
    }

}

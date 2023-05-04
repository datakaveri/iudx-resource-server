package iudx.resource.server.database.elastic;

import static iudx.resource.server.apiserver.util.Constants.HEADER_CSV;

import java.io.File;

public class ConvertElasticResponseFactory {
  private AbstractConvertElasticSearchResponse responseToCsv;
  // private AbstractConvertElasticSearchResponse responseToParquet;
  private AbstractConvertElasticSearchResponse responseToJson;
  private String format;

  public ConvertElasticResponseFactory(String format, File file) {
    this.format = format;
    responseToCsv = new ConvertElasticResponseToCsv(file);
    // responseToParquet = new ConvertElasticResponseToParquet(file);
    responseToJson = new ConvertElasticResponseToJson(file);
  }

  public ConvertElasticResponse createInstance() {
    switch (format) {
      case HEADER_CSV:
        return responseToCsv;
        //            case HEADER_PARQUET:
        //                return responseToParquet;
      default:
        return responseToJson;
    }
  }
}

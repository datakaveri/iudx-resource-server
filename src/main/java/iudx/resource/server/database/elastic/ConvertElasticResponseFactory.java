package iudx.resource.server.database.elastic;

import static iudx.resource.server.apiserver.util.Constants.HEADER_CSV;

import java.io.File;

public class ConvertElasticResponseFactory {
  private AbstractReformatElasticSearchResponse responseToCsv;
  // private AbstractConvertElasticSearchResponse responseToParquet;
  private AbstractReformatElasticSearchResponse responseToJson;
  private String format;

  public ConvertElasticResponseFactory(String format, File file) {
    this.format = format;
    responseToCsv = new ReformatElasticResponseToCsv(file);
    // responseToParquet = new ConvertElasticResponseToParquet(file);
    responseToJson = new ReformatElasticResponseToJson(file);
  }

  public ReformatElasticResponse createInstance() {
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

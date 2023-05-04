package iudx.resource.server.database.elastic;


import java.io.File;

public abstract class AbstractConvertElasticSearchResponse implements ConvertElasticResponse{
    File file;

    public AbstractConvertElasticSearchResponse(File file) {
        this.file = file;
    }

}

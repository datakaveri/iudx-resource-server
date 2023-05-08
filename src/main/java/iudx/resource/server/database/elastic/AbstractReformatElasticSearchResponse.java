package iudx.resource.server.database.elastic;

import java.io.File;

public abstract class AbstractReformatElasticSearchResponse implements ReformatElasticResponse {
  File file;

  public AbstractReformatElasticSearchResponse(File file) {
    this.file = file;
  }
}

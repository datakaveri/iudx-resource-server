package iudx.resource.server.database.elastic;

import java.io.File;

public abstract class AbstractEsSearchResponseFormatter implements EsResponseFormatter {
  File file;

  public AbstractEsSearchResponseFormatter(File file) {
    this.file = file;
  }
}

package iudx.resource.server.database.async;

public interface ProgressListener {

  void updateProgress(double progress);

  void finish();
}

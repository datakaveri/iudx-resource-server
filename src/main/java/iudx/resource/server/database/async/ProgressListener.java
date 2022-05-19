package iudx.resource.server.database.async;

public interface ProgressListener {
  
  public void updateProgress(double progress);
  
  public void finish();

}

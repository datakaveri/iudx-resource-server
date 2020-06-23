package iudx.resource.server.apiserver.query;

import java.time.LocalDateTime;

public class TemporalRelation {

  private LocalDateTime endTime;
  private String temprel;
  private LocalDateTime time;

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public String getTemprel() {
    return temprel;
  }

  public void setTemprel(String temprel) {
    this.temprel = temprel;
  }

  public LocalDateTime getTime() {
    return time;
  }

  public void setTime(LocalDateTime time) {
    this.time = time;
  }

}

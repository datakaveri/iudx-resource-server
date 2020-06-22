package iudx.resource.server.apiserver.query;

public class GeoRelation {

  private Double maxDistance;
  private Double minDistance;
  private String relation;

  public Double getMaxDistance() {
    return maxDistance;
  }

  public void setMaxDistance(Double maxDistance) {
    this.maxDistance = maxDistance;
  }

  public Double getMinDistance() {
    return minDistance;
  }

  public void setMinDistance(Double minDistance) {
    this.minDistance = minDistance;
  }

  public String getRelation() {
    return relation;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }

}

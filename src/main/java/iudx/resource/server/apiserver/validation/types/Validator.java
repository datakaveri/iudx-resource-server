package iudx.resource.server.apiserver.validation.types;

public interface Validator {

  boolean isValid();

  int failureCode();

  String failureMessage();

}

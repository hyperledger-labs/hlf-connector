package hlf.java.rest.client.model;

public abstract class AbstractModelValidator<T extends ValidatedDataModel> {
  public abstract void validate(T dataModel);
}

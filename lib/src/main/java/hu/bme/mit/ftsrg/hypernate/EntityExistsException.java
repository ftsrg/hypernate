package hu.bme.mit.ftsrg.hypernate;

public class EntityExistsException extends DataAccessException {

  public EntityExistsException(final String key) {
    super("Entity with key '%s' already exists".formatted(key));
  }
}

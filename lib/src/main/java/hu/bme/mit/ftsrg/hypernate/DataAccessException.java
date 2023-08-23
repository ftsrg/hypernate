package hu.bme.mit.ftsrg.hypernate;

public abstract class DataAccessException extends Exception {

  public DataAccessException(final String message) {
    super(message);
  }

  public DataAccessException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

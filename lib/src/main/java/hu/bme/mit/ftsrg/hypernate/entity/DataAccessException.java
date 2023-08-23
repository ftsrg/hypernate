/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

public abstract class DataAccessException extends Exception {

  public DataAccessException(final String message) {
    super(message);
  }

  public DataAccessException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

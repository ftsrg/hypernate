/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

public class SerializationException extends Exception {

  public SerializationException(final String message) {
    super(message);
  }

  public SerializationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

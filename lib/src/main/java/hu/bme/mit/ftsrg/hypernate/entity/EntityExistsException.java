/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

/** Exception thrown when the {@link Entity} to create already exists. */
public class EntityExistsException extends DataAccessException {

  public EntityExistsException(final String key) {
    super("Entity with key '%s' already exists".formatted(key));
  }
}

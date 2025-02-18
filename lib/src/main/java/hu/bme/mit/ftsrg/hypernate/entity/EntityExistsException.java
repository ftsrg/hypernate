/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import lombok.experimental.StandardException;

/** Exception thrown when the {@link Entity} to create already exists. */
@StandardException
public class EntityExistsException extends DataAccessException {

  public EntityExistsException(final String key) {
    super("Entity with key '%s' already exists".formatted(key));
  }
}

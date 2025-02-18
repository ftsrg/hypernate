/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import lombok.experimental.StandardException;

/** Exception thrown when the queried {@link Entity} could not be found. */
@StandardException
public class EntityNotFoundException extends DataAccessException {

  public EntityNotFoundException(final String key) {
    super("Entity with key '%s' could not be found".formatted(key));
  }
}

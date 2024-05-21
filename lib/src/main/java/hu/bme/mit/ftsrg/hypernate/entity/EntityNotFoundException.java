/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

public class EntityNotFoundException extends DataAccessException {

  public EntityNotFoundException(final String key) {
    super(String.format("Entity with key '%s' could not be found", key));
  }
}

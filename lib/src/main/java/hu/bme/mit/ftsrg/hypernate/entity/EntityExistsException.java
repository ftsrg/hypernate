/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

public class EntityExistsException extends DataAccessException {

  public EntityExistsException(final String key) {
    super(String.format("Entity with key '%s' already exists", key));
  }
}

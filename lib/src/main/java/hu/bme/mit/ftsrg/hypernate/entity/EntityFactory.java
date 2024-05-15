/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import com.jcabi.aspects.Loggable;

/**
 * Minimal interface to allow entities to return factories that produce them.
 *
 * @param <Type> the entity's type
 */
@Loggable(Loggable.DEBUG)
public interface EntityFactory<Type extends Entity<Type>> {
  /**
   * Simply gives a new, empty instance of the entity of type <code>Type</code>.
   *
   * @return a new, empty entity instance
   */
  Type create();
}

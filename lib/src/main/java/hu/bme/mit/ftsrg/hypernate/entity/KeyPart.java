/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a {@link SerializableEntity} as a primary key part.
 *
 * <p>Add this annotation to all fields you wish to have as primary keys. {@link
 * SerializableEntityBase#getKeyParts()} returns an array constructed of these, with additional
 * padding.
 *
 * @see SerializableEntity
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface KeyPart {}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import hu.bme.mit.ftsrg.hypernate.exception.HypernateException;
import lombok.experimental.StandardException;

/**
 * Exception thrown when there was a problem during the serialization or deserialization of an
 * {@link Entity}.
 */
@StandardException
public class SerializationException extends HypernateException {}

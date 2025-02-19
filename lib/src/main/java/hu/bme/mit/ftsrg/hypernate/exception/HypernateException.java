/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.exception;

import lombok.experimental.StandardException;

/** Based class for all Hypernate-related exceptions. */
@StandardException
public abstract class HypernateException extends RuntimeException {}

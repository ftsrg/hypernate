/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

/** Event that should be fired after all transaction logic has been executed. */
@Value
@EqualsAndHashCode(callSuper = true)
public class TransactionEnd extends HypernateEvent {}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

/** Event that should be fired before any transaction logic is executed. */
@Value
@EqualsAndHashCode(callSuper = true)
public class TransactionBegin extends HypernateEvent {}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware.event;

/** React to (internal) chaincode events. */
public interface HypernateEventHandler {

  /**
   * Expected to be called when a {@link HypernateEvent} subtype has been fired.
   *
   * <p>See common events {@link TransactionBegin} and {@link TransactionEnd}.
   *
   * <p>By default, event handlers are no-op.
   *
   * @param event event object with optional payload
   */
  default void handleEventInternal(HypernateEvent event) {}
}

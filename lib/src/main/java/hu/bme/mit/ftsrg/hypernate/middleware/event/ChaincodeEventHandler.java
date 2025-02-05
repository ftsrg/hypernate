package hu.bme.mit.ftsrg.hypernate.middleware.event;

import org.hyperledger.fabric.protos.peer.ChaincodeEvent;

/** React to (internal) chaincode events. */
public interface ChaincodeEventHandler {

  /**
   * Expected to be called when a {@link HypernateEvent} subtype has been fired.
   *
   * <p>See common events {@link TransactionBegin} and {@link TransactionEnd}.
   *
   * <p>By default, event handlers are no-op.
   *
   * @param event event object with optional payload
   * @param <T> type of the event object
   */
  default <T extends HypernateEvent> void onEvent(T event) {}
}

package hu.bme.mit.ftsrg.hypernate.contract;

import hu.bme.mit.ftsrg.hypernate.context.HypernateContext;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionBegin;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionEnd;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.shim.ChaincodeStub;

/** Contract base class enriched with default before-/after-transaction event handling. */
public interface HypernateContract extends ContractInterface {

  @Override
  default Context createContext(ChaincodeStub stub) {
    return new HypernateContext(stub);
  }

  /**
   * Executed before any transaction logic runs.
   *
   * <p>When overriding this method, your method body should start with a call to {@code super} if
   * you want middlewares to receive the {@link TransactionBegin} event.
   *
   * @param ctx the Hypernate context
   */
  default void beforeTransaction(HypernateContext ctx) {
    ctx.fireEvent(new TransactionBegin());
  }

  /**
   * Executed after transaction logic has finished.
   *
   * <p>When overriding this method, your method body should start with a call to {@code super} if
   * you want middlewares to receive the {@link TransactionBegin} event.
   *
   * @param ctx the Hypernate context
   */
  default void afterTransaction(HypernateContext ctx) {
    ctx.fireEvent(new TransactionEnd());
  }
}

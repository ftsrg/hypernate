/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.contract;

import hu.bme.mit.ftsrg.hypernate.context.HypernateContext;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddlewareChain;
import hu.bme.mit.ftsrg.hypernate.middleware.Middleware;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionBegin;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionEnd;
import java.util.*;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.shim.ChaincodeStub;

/** Contract base class enriched with default before-/after-transaction event handling. */
public interface HypernateContract extends ContractInterface {

  @Override
  default Context createContext(ChaincodeStub fabricStub) {
    return new HypernateContext(initMiddlewares(fabricStub));
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

  /**
   * Initialize the middleware chain.
   *
   * <p>Normally, Hypernate processes the {@link Middleware} annotation on the contract class if it
   * exists.
   *
   * <p>You can override this behaviour with custom middleware initialization logic by overriding
   * this method.
   *
   * @param fabricStub the stub object provided by Fabric (should normally be the last in the chain)
   * @return the middleware chain
   */
  default ChaincodeStubMiddlewareChain initMiddlewares(final ChaincodeStub fabricStub) {
    Middleware middlewareAnnotation = getClass().getAnnotation(Middleware.class);
    if (middlewareAnnotation == null) {
      return ChaincodeStubMiddlewareChain.emptyChain(fabricStub);
    }

    Class<? extends ChaincodeStubMiddleware>[] middlewareClasses = middlewareAnnotation.value();
    ChaincodeStubMiddlewareChain.Builder builder = ChaincodeStubMiddlewareChain.builder(fabricStub);
    Arrays.stream(middlewareClasses).forEach(builder::add);

    return builder.build();
  }
}

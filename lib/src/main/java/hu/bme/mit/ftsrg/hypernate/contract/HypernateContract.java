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
    ChaincodeStubMiddlewareChain middlwareChain = initMiddlewares(fabricStub);
    HypernateContext ctx = new HypernateContext(middlwareChain);
    middlwareChain.forEach(ctx::subscribeToEvents);
    return ctx;
  }

  @Override
  default void beforeTransaction(Context ctx) {
    if (ctx instanceof HypernateContext hypCtx) {
      hypCtx.fireEvent(new TransactionBegin());
    } else {
      ContractInterface.super.beforeTransaction(ctx);
    }
  }

  @Override
  default void afterTransaction(Context ctx, Object _result) {
    if (ctx instanceof HypernateContext hypCtx) {
      hypCtx.fireEvent(new TransactionEnd());
    } else {
      ContractInterface.super.beforeTransaction(ctx);
    }
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

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.Registry;
import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddlewareChain;
import hu.bme.mit.ftsrg.hypernate.middleware.event.HypernateEvent;
import org.hyperledger.fabric.contract.Context;

/**
 * Context enriched with {@link Registry} and {@link ChaincodeStubMiddleware}s
 *
 * <p>The registry can be used to manage entities.
 */
public class HypernateContext extends Context {

  private final ChaincodeStubMiddlewareChain middlewareChain;

  public HypernateContext(final ChaincodeStubMiddlewareChain middlewareChain) {
    super(middlewareChain.getFirst());
    this.middlewareChain = middlewareChain;
    this.stub = middlewareChain.getFabricStub();
  }

  /**
   * Get the {@link Registry} object.
   *
   * <p>The {@link Registry} can be used to perform CRUD operations with/on {@link Entity}s.
   *
   * @return the registry
   */
  public Registry getRegistry() {
    return new Registry(getStub());
  }

  /**
   * Fire a {@link HypernateEvent}.
   *
   * <p>Notifies all middlewares in the chain (in the order in which they have been added).
   *
   * @param event the event to fire
   */
  public void fireEvent(final HypernateEvent event) {
    middlewareChain.forEach(mw -> mw.onEvent(event));
  }
}

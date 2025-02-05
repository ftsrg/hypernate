/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.Registry;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.event.HypernateEvent;
import java.util.LinkedList;
import java.util.List;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Context enriched with {@link Registry} and {@link ChaincodeStubMiddleware}s
 *
 * <p>The registry can be used to manage entities.
 */
public class HypernateContext extends Context {

  private final List<ChaincodeStubMiddleware> middlewares = new LinkedList<>();

  public HypernateContext(final ChaincodeStub stub) {
    super(stub);
  }

  public Registry getRegistry() {
    return new Registry(getStub());
  }

  public void installMiddleware(ChaincodeStubMiddleware middleware) {
    middlewares.add(middleware);
  }

  public void uninstallMiddleware(ChaincodeStubMiddleware middleware) {
    middlewares.remove(middleware);
  }

  public void raiseEvent(final HypernateEvent event) {
    middlewares.forEach(mw -> mw.onEvent(event));
  }
}

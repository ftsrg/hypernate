/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.Registry;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddlewareBase;
import java.util.LinkedList;
import java.util.List;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Context enriched with {@link Registry} and {@link ChaincodeStubMiddlewareBase}s
 *
 * <p>The registry can be used to manage entities.
 */
public class HypernateContext extends Context {

  private final List<ChaincodeStub> middlewares = new LinkedList<>();

  public HypernateContext(final ChaincodeStub stub) {
    super(stub);
  }

  public Registry getRegistry() {
    return new Registry(getStub());
  }

  public void installMiddleware(ChaincodeStub middleware) {
    middlewares.add(middleware);
  }

  public void uninstallMiddleware(ChaincodeStub middleware) {
    middlewares.remove(middleware);
  }
}

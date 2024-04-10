/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.middleware.LoggingStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.WriteBackCachedChaincodeStubMiddleware;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.hyperledger.fabric.shim.ChaincodeStub;

/** Context extended with all available stub middlewares and a registry. */
public class ContextWithMiddlewares extends ContextWithRegistry {

  private final Deque<ChaincodeStub> stubMiddlewares = new ArrayDeque<>();

  private final List<Runnable> finishHooks = new ArrayList<>();

  public ContextWithMiddlewares(final ChaincodeStub fabricStub) {
    super(fabricStub);

    /*
     * Stub chain:
     *   --> LOGGER --> WRITE BACK CACHE --> FABRIC STUB    ( --> ledger )
     */
    this.stubMiddlewares.push(fabricStub);
    final WriteBackCachedChaincodeStubMiddleware cachedMiddleware =
        new WriteBackCachedChaincodeStubMiddleware(this.stubMiddlewares.peek());
    this.stubMiddlewares.push(cachedMiddleware);
    this.stubMiddlewares.push(new LoggingStubMiddleware(this.stubMiddlewares.peek()));

    this.finishHooks.add(cachedMiddleware::dispose);
  }

  @Override
  public ChaincodeStub getStub() {
    return this.stubMiddlewares.peek();
  }

  /**
   * Finalize the execution of a transaction.
   *
   * <p>This method MUST be called at the end of every transaction to ensure correct operation.
   */
  public void commit() {
    for (final Runnable hook : this.finishHooks) hook.run();
  }
}

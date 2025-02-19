/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddlewareChain;
import hu.bme.mit.ftsrg.hypernate.middleware.event.HypernateEvent;
import hu.bme.mit.ftsrg.hypernate.registry.Registry;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.SubmissionPublisher;
import org.hyperledger.fabric.contract.Context;

/**
 * Context enriched with {@link Registry} and {@link ChaincodeStubMiddleware}s
 *
 * <p>The registry can be used to manage entities.
 */
public class HypernateContext extends Context {

  private final ChaincodeStubMiddlewareChain middlewareChain;

  private final SubmissionPublisher<HypernateEvent> eventPublisher = new SubmissionPublisher<>();

  private final Queue<HypernateEvent> eventQueue = new LinkedList<>();

  private Registry registry;

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
    if (registry == null) {
      registry = new Registry(middlewareChain.getFirst());
    }

    return registry;
  }

  /**
   * Fire a {@link HypernateEvent}.
   *
   * <p>Notifies all middlewares in the chain (in the order in which they have been added).
   *
   * @param event the event to fire
   */
  public void fireEvent(final HypernateEvent event) {
    eventPublisher.submit(event);
  }

  public void subscribeToEvents(final Subscriber<HypernateEvent> subscriber) {
    eventPublisher.subscribe(subscriber);
  }
}

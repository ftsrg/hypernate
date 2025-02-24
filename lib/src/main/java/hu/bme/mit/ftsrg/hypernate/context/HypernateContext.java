/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddleware;
import hu.bme.mit.ftsrg.hypernate.middleware.ChaincodeStubMiddlewareChain;
import hu.bme.mit.ftsrg.hypernate.middleware.event.HypernateEvent;
import hu.bme.mit.ftsrg.hypernate.registry.Registry;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import lombok.Getter;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Context enriched with {@link Registry} and {@link ChaincodeStubMiddleware}s
 *
 * <p>The registry can be used to manage entities.
 */
public class HypernateContext extends Context {

  @Getter private final ChaincodeStub fabricStub;

  private final ChaincodeStubMiddlewareChain middlewareChain;

  private final List<Subscriber<? super HypernateEvent>> subscribers = new LinkedList<>();

  private final Flow.Publisher<HypernateEvent> eventPublisher = subscribers::add;

  private final Queue<HypernateEvent> eventQueue = new LinkedList<>();

  @Getter private final Registry registry;

  public HypernateContext(final ChaincodeStubMiddlewareChain middlewareChain) {
    super(middlewareChain.getFirst());
    this.middlewareChain = middlewareChain;
    this.fabricStub = middlewareChain.getFabricStub();
    this.registry = new Registry(middlewareChain.getFirst());
  }

  /**
   * Fire a {@link HypernateEvent}.
   *
   * <p>Notifies all middlewares in the chain (in the order in which they have been added).
   *
   * @param event the event to fire
   */
  public void fireEvent(final HypernateEvent event) {
    subscribers.forEach(s -> s.onNext(event));
  }

  public void subscribeToEvents(final Subscriber<HypernateEvent> subscriber) {
    eventPublisher.subscribe(subscriber);
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.middleware.event.HypernateEvent;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionBegin;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionEnd;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import lombok.experimental.Delegate;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * {@link ChaincodeStub} middlewares to be chained.
 *
 * <p>Stub middleware classes provide the same interface as {@link ChaincodeStub}, but under the
 * hood they maintain a reference to another {@link ChaincodeStub} and delegate all calls to that.
 * You can override any method in this class to inject your custom behaviour, such as logging,
 * access control, caching, etc.
 */
@Loggable(Loggable.DEBUG)
public abstract class ChaincodeStubMiddleware implements ChaincodeStub, Subscriber<HypernateEvent> {

  /** The next {@link ChaincodeStub} in the chain. */
  @Delegate ChaincodeStub nextStub;

  @Override
  public void onSubscribe(Subscription subscription) {}

  @Override
  public void onNext(HypernateEvent event) {
    if (event instanceof TransactionBegin) {
      onTransactionBegin();
    } else if (event instanceof TransactionEnd) {
      onTransactionEnd();
    }

    onEvent(event);
  }

  @Override
  public void onError(Throwable throwable) {
    throwable.printStackTrace();
  }

  @Override
  public void onComplete() {}

  /**
   * Hypernate event listener.
   *
   * <p>You can override this method to react to any {@link HypernateEvent}s received. For example:
   *
   * <pre>{@code
   * @Override
   * protected void onEvent(final HypernateEvent event) {
   *   if (event instanceof MyCustomEvent) {
   *     System.out.println("Handling custom event");
   *   } else {
   *     System.out.println("Received non-handled event – ignoring it");
   *   }
   * }
   * }</pre>
   *
   * @param event event object
   */
  public void onEvent(final HypernateEvent event) {}

  /** Convenience method to for handling the {@link TransactionBegin} event. */
  protected void onTransactionBegin() {}

  /** Convenience method to for handling the {@link TransactionEnd} event. */
  protected void onTransactionEnd() {}
}

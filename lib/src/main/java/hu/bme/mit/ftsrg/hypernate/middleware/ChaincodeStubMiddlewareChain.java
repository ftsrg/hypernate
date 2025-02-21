/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hyperledger.fabric.shim.ChaincodeStub;

/** Encapsulating class for a chain of {@link ChaincodeStubMiddleware}s. */
@Getter
@RequiredArgsConstructor
public class ChaincodeStubMiddlewareChain {

  private final ChaincodeStub fabricStub;

  private final List<ChaincodeStubMiddleware> middlewares;

  /**
   * Get a builder object for a middleware chain.
   *
   * <p>Usage example:
   *
   * <pre>{@code
   * // Fabric's stub pass to eg ContractInterface#createContext
   * ChaincodeStub fabricStub;
   * var chain = ChaincodeStubMiddlewareChain.builder(fabricStub)
   *                                         .add(WriteBackCachedChaincodeStubMiddleware.class)
   *                                         .add(LoggingStubMiddleware.class)
   *                                         .build();
   * }</pre>
   *
   * @param fabricStub the stub object provided by Fabric
   * @return a builder object to construct the middleware chain conveniently
   */
  public static Builder builder(final ChaincodeStub fabricStub) {
    return new Builder(fabricStub);
  }

  /**
   * Get an empty middleware chain.
   *
   * @param fabricStub the stub object provided by Fabric
   * @return an empty middleware chain
   */
  public static ChaincodeStubMiddlewareChain emptyChain(final ChaincodeStub fabricStub) {
    return new ChaincodeStubMiddlewareChain(fabricStub, Collections.emptyList());
  }

  /**
   * Get the first {@link ChaincodeStubMiddleware} in the chain.
   *
   * <p>The <i>first</i> is the one handling stub methods <i>first</i>. After that follow the rest
   * of the chain, and, finally, the Fabric stub.
   *
   * @return the first middleware in the chain
   */
  public ChaincodeStubMiddleware getFirst() {
    return middlewares.get(0);
  }

  /**
   * Iterate over the list of {@link ChaincodeStubMiddleware}s in the chain.
   *
   * <p>Iteration order follows chaining order.
   *
   * @param consumer lambda to execute for each middleware in the chain.
   */
  public void forEach(final Consumer<ChaincodeStubMiddleware> consumer) {
    middlewares.forEach(consumer);
  }

  /** Builder for a chaincode stub middleware chain. */
  public static class Builder {

    private final ChaincodeStub fabricStub;

    private final Deque<ChaincodeStubMiddleware> middlewares = new LinkedList<>();

    Builder(ChaincodeStub fabricStub) {
      this.fabricStub = fabricStub;
    }

    /**
     * Add another {@link ChaincodeStubMiddleware} to the chain.
     *
     * <p>This method will automatically link this and the previously added middleware (or the
     * Fabric stub) together.
     *
     * @param middlewareClass the type of {@link ChaincodeStubMiddleware} to add -- will be
     *     instantiated using a no-arg constructor
     */
    public Builder add(Class<? extends ChaincodeStubMiddleware> middlewareClass) {
      Constructor<? extends ChaincodeStubMiddleware> constructor;
      try {
        constructor = middlewareClass.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Could not find no-arg constructor for " + middlewareClass, e);
      }

      ChaincodeStubMiddleware middlewareInstance;
      try {
        middlewareInstance = constructor.newInstance();
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new RuntimeException("Failed to instantiate " + middlewareClass, e);
      }

      middlewareInstance.nextStub = middlewares.isEmpty() ? fabricStub : middlewares.peek();
      middlewares.addFirst(middlewareInstance);

      return this;
    }

    /**
     * Build the middleware chain.
     *
     * @return the middleware chain with all the {@link #add(Class) add}ed {@link
     *     ChaincodeStubMiddleware}s.
     */
    public ChaincodeStubMiddlewareChain build() {
      return new ChaincodeStubMiddlewareChain(fabricStub, middlewares.stream().toList());
    }
  }
}

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

@Getter
@RequiredArgsConstructor
public class ChaincodeStubMiddlewareChain {

  private final ChaincodeStub fabricStub;

  private final List<ChaincodeStubMiddleware> middlewares;

  public static Builder builder(final ChaincodeStub fabricStub) {
    return new Builder(fabricStub);
  }

  public static ChaincodeStubMiddlewareChain emptyChain(final ChaincodeStub fabricStub) {
    return new ChaincodeStubMiddlewareChain(fabricStub, Collections.emptyList());
  }

  public ChaincodeStubMiddleware getFirst() {
    return middlewares.get(0);
  }

  public void forEach(final Consumer<ChaincodeStubMiddleware> consumer) {
    middlewares.forEach(consumer);
  }

  public static class Builder {

    private final ChaincodeStub fabricStub;

    private final Deque<ChaincodeStubMiddleware> middlewares = new LinkedList<>();

    Builder(ChaincodeStub fabricStub) {
      this.fabricStub = fabricStub;
    }

    public void add(Class<? extends ChaincodeStubMiddleware> middlewareClass) {
      Constructor<? extends ChaincodeStubMiddleware> constructor;
      try {
        constructor = middlewareClass.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        throw new RuntimeException("Could not no-arg constructor for " + middlewareClass, e);
      }

      ChaincodeStubMiddleware middlewareInstance;
      try {
        middlewareInstance = constructor.newInstance();
      } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
        throw new RuntimeException("Failed to instantiate " + middlewareClass, e);
      }

      middlewareInstance.nextStub = middlewares.isEmpty() ? fabricStub : middlewares.peek();
      middlewares.addFirst(middlewareInstance);
    }

    public ChaincodeStubMiddlewareChain build() {
      return new ChaincodeStubMiddlewareChain(fabricStub, middlewares.stream().toList());
    }
  }
}

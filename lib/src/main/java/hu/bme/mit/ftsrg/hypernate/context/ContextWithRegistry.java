/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.registry.Registry;
import hu.bme.mit.ftsrg.hypernate.registry.RegistryImpl;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Fabric's original context extended with a {@link Registry}.
 *
 * <p>The registry can be used to manage entities.
 */
public class ContextWithRegistry extends Context {

  private final Registry registry = new RegistryImpl();

  public ContextWithRegistry(final ChaincodeStub stub) {
    super(stub);
  }

  public Registry getRegistry() {
    return registry;
  }
}

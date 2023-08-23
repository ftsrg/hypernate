package hu.bme.mit.ftsrg.hypernate;

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

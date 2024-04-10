/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.context;

import hu.bme.mit.ftsrg.hypernate.Registry;
import lombok.Getter;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Fabric's original context extended with a {@link Registry}.
 *
 * <p>The registry can be used to manage entities.
 */
@Getter
public class ContextWithRegistry extends Context {

  private final Registry registry;

  public ContextWithRegistry(final ChaincodeStub stub) {
    super(stub);
    registry = new Registry(stub);
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import com.jcabi.aspects.Loggable;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Stub middleware that only sends {@link ChaincodeStub#putState(String, byte[])} calls once the
 * transaction is finished.
 *
 * @see ChaincodeStubMiddlewareBase
 */
@Loggable(Loggable.DEBUG)
public final class UpdateThrottledChaincodeStubMiddleware extends ChaincodeStubMiddlewareBase {

  public UpdateThrottledChaincodeStubMiddleware(ChaincodeStub nextLayer) {
    super(nextLayer);
  }

  /* TODO implement ? */
}

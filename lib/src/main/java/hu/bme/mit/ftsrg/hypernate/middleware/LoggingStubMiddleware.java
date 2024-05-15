/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.util.Arrays;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Stub middleware that simply logs all {@link ChaincodeStub#getState(String)}, {@link
 * ChaincodeStub#putState(String, byte[])}, and {@link ChaincodeStub#delState(String)} calls.
 *
 * @see ChaincodeStubMiddlewareBase
 */
public class LoggingStubMiddleware extends ChaincodeStubMiddlewareBase {

  private final Logger logger;

  public LoggingStubMiddleware(final ChaincodeStub next) {
    this(next, new Logger() {});
  }

  public LoggingStubMiddleware(final ChaincodeStub next, final Logger logger) {
    super(next);
    this.logger = logger;
  }

  @Override
  public byte[] getState(final String key) {
    logger.log("Getting state for key '{}'", key);
    final byte[] value = this.nextLayer.getState(key);
    logger.log("Got state for key '{}'; value = '{}'", key, Arrays.toString(value));
    return value;
  }

  @Override
  public void putState(final String key, final byte[] value) {
    logger.log("Setting state for key '{}' to have value '{}'", key, Arrays.toString(value));
    this.nextLayer.putState(key, value);
    logger.log("Done setting state for key '{}'", key);
  }

  @Override
  public void delState(final String key) {
    logger.log("Deleting state for key '{}'", key);
    this.nextLayer.delState(key);
    logger.log("Done deleting state for key '{}'", key);
  }
}

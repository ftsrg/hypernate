/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.util.Arrays;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Stub middleware that simply logs all {@link ChaincodeStub#getState(String)}, {@link
 * ChaincodeStub#putState(String, byte[])}, and {@link ChaincodeStub#delState(String)} calls.
 *
 * @see ChaincodeStubMiddlewareBase
 */
public class LoggingStubMiddleware extends ChaincodeStubMiddlewareBase {

  private static final Logger logger = LoggerFactory.getLogger(LoggingStubMiddleware.class);
  private final Level logLevel;

  public LoggingStubMiddleware(final ChaincodeStub next) {
    this(next, Level.INFO);
  }

  public LoggingStubMiddleware(final ChaincodeStub next, final Level logLevel) {
    super(next);
    this.logLevel = logLevel;
  }

  @Override
  public byte[] getState(final String key) {
    logger.atLevel(this.logLevel).log("Getting state for key '{}'", key);
    final byte[] value = this.nextLayer.getState(key);
    logger
        .atLevel(this.logLevel)
        .log("Got state for key '{}'; value = '{}'", key, Arrays.toString(value));
    return value;
  }

  @Override
  public void putState(final String key, final byte[] value) {
    logger
        .atLevel(this.logLevel)
        .log("Setting state for key '{}' to have value '{}'", key, Arrays.toString(value));
    this.nextLayer.putState(key, value);
    logger.atLevel(this.logLevel).log("Done setting state for key '{}'", key);
  }

  @Override
  public void delState(final String key) {
    logger.atLevel(this.logLevel).log("Deleting state for key '{}'", key);
    this.nextLayer.delState(key);
    logger.atLevel(this.logLevel).log("Done deleting state for key '{}'", key);
  }
}

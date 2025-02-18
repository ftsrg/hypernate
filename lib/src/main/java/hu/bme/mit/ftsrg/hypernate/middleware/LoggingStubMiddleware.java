/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.util.Arrays;
import org.hyperledger.fabric.shim.ChaincodeStub;

/**
 * Stub middleware that simply logs all {@link ChaincodeStub#getState(String)}, {@link
 * ChaincodeStub#putState(String, byte[])}, and {@link ChaincodeStub#delState(String)} calls.
 *
 * @see ChaincodeStubMiddleware
 */
public class LoggingStubMiddleware extends ChaincodeStubMiddleware {

  /* TODO: SLF4J should be used */
  private final Logger logger;

  public LoggingStubMiddleware() {
    this(new Logger() {});
  }

  public LoggingStubMiddleware(final Logger logger) {
    this.logger = logger;
  }

  /**
   * Get the raw state at {@code key} but log a message before and after doing so.
   *
   * @param key the queried key
   * @return the raw state at {@code key}
   */
  @Override
  public byte[] getState(final String key) {
    logger.log("Getting state for key '{}'", key);
    final byte[] value = this.nextStub.getState(key);
    logger.log("Got state for key '{}'; value = '{}'", key, Arrays.toString(value));
    return value;
  }

  /**
   * Write raw state passed in {@code value} at {@code key} but log a message before and after doing
   * so.
   *
   * @param key where to write {@code value}
   * @param value what to write at {@code key}
   */
  @Override
  public void putState(final String key, final byte[] value) {
    logger.log("Setting state for key '{}' to have value '{}'", key, Arrays.toString(value));
    this.nextStub.putState(key, value);
    logger.log("Done setting state for key '{}'", key);
  }

  /**
   * Delete the value at {@code key} but log a message before and after doing so.
   *
   * @param key the key whose value should be deleted
   */
  @Override
  public void delState(final String key) {
    logger.log("Deleting state for key '{}'", key);
    this.nextStub.delState(key);
    logger.log("Done deleting state for key '{}'", key);
  }
}

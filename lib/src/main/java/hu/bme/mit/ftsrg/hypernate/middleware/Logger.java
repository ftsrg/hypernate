/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.io.PrintStream;

/** Abstract logging interface used by {@link LoggingStubMiddleware}. */
public interface Logger {

  /**
   * Log a message to the standard output.
   *
   * <p>A newline is automatically appended.
   *
   * @param format format string
   * @param args arguments; will be passed to {@link PrintStream#printf(String, Object...)
   *     System.out.printf} together with {@code format}.
   */
  default void log(String format, Object... args) {
    System.out.printf(format + "%n", args);
  }
}

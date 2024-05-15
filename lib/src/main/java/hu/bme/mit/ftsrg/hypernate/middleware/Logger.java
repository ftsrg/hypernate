/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

public interface Logger {
  default void log(String format, Object... args) {
    System.out.printf(format + "%n", args);
  }
}

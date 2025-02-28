/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

public class MissingPrimaryKeysException extends RuntimeException {
  public MissingPrimaryKeysException(String message) {
    super(message);
  }
}

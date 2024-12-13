/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.mappers;

import java.util.function.Function;

public class IntegerZeroPadder implements Function<Integer, String> {
  private static final String FORMAT_STRING =
      "%0" + String.valueOf(Integer.MAX_VALUE).length() + "d";

  @Override
  public String apply(Integer integer) {
    return String.format(IntegerZeroPadder.FORMAT_STRING, integer);
  }
}

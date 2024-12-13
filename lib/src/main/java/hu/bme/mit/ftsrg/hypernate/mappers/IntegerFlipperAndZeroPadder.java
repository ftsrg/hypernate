/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.mappers;

import java.util.function.Function;

public class IntegerFlipperAndZeroPadder implements Function<Integer, String> {
  private static final String FORMAT_STRING =
      "%0" + String.valueOf(Integer.MAX_VALUE).length() + "d";

  @Override
  public String apply(Integer integer) {
    return String.format(IntegerFlipperAndZeroPadder.FORMAT_STRING, Integer.MAX_VALUE - integer);
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.mappers;

import java.util.function.Function;

public class LongZeroPadder implements Function<Long, String> {
  private static final String FORMAT_STRING = "%0" + String.valueOf(Long.MAX_VALUE).length() + "d";

  @Override
  public String apply(Long number) {
    return String.format(LongZeroPadder.FORMAT_STRING, number);
  }
}

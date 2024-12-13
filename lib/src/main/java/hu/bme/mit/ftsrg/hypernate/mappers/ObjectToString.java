/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.mappers;

import java.util.function.Function;

public class ObjectToString implements Function<Object, String> {
  @Override
  public String apply(Object object) {
    return object.toString();
  }
}

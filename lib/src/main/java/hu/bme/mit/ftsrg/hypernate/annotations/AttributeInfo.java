/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.annotations;

import hu.bme.mit.ftsrg.hypernate.mappers.ObjectToString;
import java.util.function.Function;

public @interface AttributeInfo {
  String name();

  Class<? extends Function<?, String>> mapper() default ObjectToString.class;
}

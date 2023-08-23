/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.util;

import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import java.util.ArrayList;
import java.util.List;
import org.hyperledger.fabric.contract.Context;
import org.slf4j.Logger;

/**
 * Ugly utility class to log method class without AOP.
 *
 * <p>The sane way to do this would be using Aspect-Oriented Programming (AOP) with AspectJ.
 * Unfortunately, I failed to get that working with OpenJML and gradle. So we have this.
 */
public final class MethodLogger {

  private final Logger logger;
  private final String className;

  public MethodLogger(final Logger logger, final String className) {
    this.logger = logger;
    this.className = className;
  }

  public String generateParamsString(final String... params) {
    return String.join(",", params);
  }

  public String generateParamsString(final int... params) {
    final List<String> strings = new ArrayList<>();
    for (final int i : params) strings.add(String.valueOf(i));
    return generateParamsString(strings.toArray(new String[0]));
  }

  public String generateParamsString(final Context ctx, final String... params) {
    return "%s,%s".formatted(ctx.toString(), generateParamsString(params));
  }

  public String generateParamsString(final Context ctx, final int... params) {
    return "%s,%s".formatted(ctx.toString(), generateParamsString(params));
  }

  public <Type extends Entity<Type>> String generateParamsString(final Type obj) {
    return obj.toString();
  }

  public void logStart(final String methodName, final String paramsString) {
    logger.debug("START:{}#{}({})", this.className, methodName, paramsString);
  }

  public void logEnd(
      final String methodName, final String paramsString, final String returnString) {
    logger.debug("END:{}#{}({})->{}", this.className, methodName, paramsString, returnString);
  }
}

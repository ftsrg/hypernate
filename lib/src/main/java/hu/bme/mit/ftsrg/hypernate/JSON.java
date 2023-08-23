/* SPDX-License-Identifier: Apache-2.0 */

package hu.bme.mit.ftsrg.hypernate;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;

/** A convenience facade for a concrete JSON-serializer. */
@UtilityClass
public final class JSON {

  private static final Gson gson = new Gson();

  /**
   * Serialize an object to a JSON string.
   *
   * @param obj The object to serialize
   * @return The JSON-serialization of <code>obj</code>
   */
  public static String serialize(final Object obj) {
    return gson.toJson(obj);
  }

  /**
   * Deserialize a JSON string into an object.
   *
   * @param json The JSON string to deserialize
   * @param clazz The type of the object to interpret the JSON as
   * @return The resulting object
   */
  public static <T> T deserialize(final String json, final Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }
}

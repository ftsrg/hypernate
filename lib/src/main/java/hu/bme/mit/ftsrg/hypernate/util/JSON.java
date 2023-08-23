/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import java.io.IOException;
import lombok.experimental.UtilityClass;

/** A convenience facade for a concrete JSON-serializer. */
@UtilityClass
public final class JSON {

  private static final ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
  }

  /**
   * Serialize an object to a JSON string.
   *
   * @param obj The object to serialize
   * @return The JSON-serialization of <code>obj</code>
   */
  public static String serialize(final Object obj) throws SerializationException {
    try {
      return mapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Failed to serialize to JSON", e);
    }
  }

  /**
   * Deserialize a JSON string into an object.
   *
   * @param json The JSON string to deserialize
   * @param clazz The type of the object to interpret the JSON as
   * @return The resulting object
   */
  public static <T> T deserialize(final String json, final Class<T> clazz)
      throws SerializationException {
    try {
      return mapper.readValue(json, clazz);
    } catch (IOException e) {
      throw new SerializationException("Failed to deserialize from JSON", e);
    }
  }
}

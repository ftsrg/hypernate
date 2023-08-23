/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.EqualsAndHashCode;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic database entity that can be serialized to JSON and to byte arrays.
 *
 * <p>The default method implementations provide some reflection-based implementations, so that you
 * do not have to implement all the boilerplate yourself in the actual entity classes.
 *
 * @param <Type> the type of the entity (required because of {@link Entity#getFactory()})
 */
@EqualsAndHashCode
@DataType
public interface Entity<Type extends Entity<Type>> {

  static final Logger logger = LoggerFactory.getLogger(Entity.class);

  static final int padLength = Integer.toString(Integer.MAX_VALUE).length();

  /**
   * Get the type of this entity; essentially a table name.
   *
   * <p>The default is an all-caps string, such as <code>CUSTOMER</code>.
   *
   * @return the arbitrary identifier of this entity type
   * @see Entity#getType()
   */
  default String getType() {
    final String type = this.getClass().getName().toUpperCase();
    logger.debug("Returning type name: {}", type);
    return type;
  }

  /**
   * Get the composite key for this entity.
   *
   * <p>The composite key is an array of strings comprising the primary key fields of the entity (ie
   * those annotated with {@link KeyPart}).
   *
   * <p>The default implementation takes all fields annotated with {@link KeyPart} in the entity
   * class, pads them to {@link Entity#padLength} and creates an array from those.
   *
   * <p><b>WARNING:</b> the order of keys might matter; this default implementation has not been
   * tested.
   *
   * @return the composite key of this entity
   * @see Entity#getKeyParts()
   */
  default String[] getKeyParts() {
    // Stream-based implementation replaced with code below to accommodate OpenJML...
    final List<String> keyParts = new ArrayList<>();
    for (final Field field : this.getClass().getDeclaredFields()) {
      logger.debug("Checking if field {} is a key part...", field.getName());
      field.setAccessible(true);
      if (field.isAnnotationPresent(KeyPart.class)) {
        logger.debug("Field {} seems to be a key part; getting its value", field.getName());
        try {
          keyParts.add(pad(field.getInt(this)));
        } catch (IllegalAccessException e) {
          logger.error("Failed to get the value of a key part", e);
          throw new RuntimeException(e);
        }
      }
    }

    logger.debug("Returning key parts for entity: {}", keyParts);
    return keyParts.toArray(new String[0]);
  }

  /**
   * Serialize this entity into a byte array.
   *
   * <p>The default implementation is the encoded version of {@link Entity#toJson()}.
   *
   * @return this entity serialized into a byte array
   * @throws SerializationException if the object cannot be serialized into a JSON string
   * @see Entity#fromBuffer(byte[])
   * @see Entity#toBuffer()
   */
  default byte[] toBuffer() throws SerializationException {
    final String json = this.toJson();
    final byte[] buffer = json.getBytes(StandardCharsets.UTF_8);
    logger.debug("Returning buffer (size={}) from JSON: {}", buffer.length, json);
    return buffer;
  }

  /**
   * Deserialize this entity from a byte array.
   *
   * <p>The default implementation expects an encoded JSON string.
   *
   * @param buffer the buffer to parse
   * @throws SerializationException if the JSON decoded from the byte array cannot be deserialized
   * @see Entity#toBuffer()
   * @see Entity#fromBuffer(byte[])
   */
  default void fromBuffer(byte[] buffer) throws SerializationException {
    final String json = new String(buffer, StandardCharsets.UTF_8);
    logger.debug("Parsing entity from JSON: {}", json);
    this.fromJson(json);
  }

  /**
   * Serialize this entity into a JSON string.
   *
   * @return this entity serialized into a JSON string
   * @throws SerializationException if the object cannot be serialized into a JSON string
   * @see Entity#fromJson(String)
   * @see Entity#toJson()
   */
  default String toJson() throws SerializationException {
    final String json = JSON.serialize(this);
    logger.debug("Returning JSON string from entity: {}", json);
    return json;
  }

  /**
   * Deserialize this entity from a JSON string.
   *
   * <p>The reflection-based default implementation sets all fields with matching names from the
   * JSON. Fields that are not found in the JSON remain unset. Conversely, extraneous keys in the
   * JSON are ignored.
   *
   * @param json the JSON string to parse
   * @throws SerializationException if the object cannot be deserialized from the JSON string
   * @see Entity#toJson()
   * @see Entity#fromJson(String)
   */
  default void fromJson(String json) throws SerializationException {
    logger.debug("Deserializing from JSON string: {}...", json);
    final Object obj = JSON.deserialize(json, this.getClass());
    final Field[] ourFields = this.getClass().getDeclaredFields();
    /*
     * Try to get values for our known fields from the deserialized
     * object.  This process is forgiving: if one of our fields does not
     * exist inside the deserialized object, we leave it as is; if the
     * deserialized object contains fields we do not recognize, we
     * silently ignore them.
     */
    for (final Field ourField : ourFields) {
      logger.debug("Attempting to set field {}", ourField.getName());
      ourField.setAccessible(true);
      try {
        final Field theirField = obj.getClass().getDeclaredField(ourField.getName());
        theirField.setAccessible(true);
        try {
          ourField.set(this, theirField.get(obj));
        } catch (IllegalArgumentException | IllegalAccessException e) {
          logger.error("Got exception while trying to access/set a field", e);
          e.printStackTrace();
        }
      } catch (NoSuchFieldException e) {
        logger.error("Got exception while trying to access/set a field", e);
        e.printStackTrace();
      }
    }
  }

  /**
   * Get a factory for this entity type.
   *
   * <p><b>WARNING:</b> the rather lousy default reflection-based implementation that has not been
   * tested
   *
   * @return A factory that can be used to create empty instances of this entity
   * @see Entity#getFactory()
   */
  default EntityFactory<Type> getFactory() {
    // FIXME can we eliminate this unchecked cast in a reasonable way?
    @SuppressWarnings("unchecked")
    final Class<Type> ourClass = (Class<Type>) this.getClass();
    // Lambda-based implementation replaced with code below to accommodate OpenJML...
    return new EntityFactory<>() {
      @Override
      public Type create() {
        logger.debug("Was asked to create a new entity instance of {}...", ourClass.getName());
        try {
          return ourClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
            | IllegalAccessException
            | InvocationTargetException
            | NoSuchMethodException e) {
          logger.error("Failed to create new entity instance using factory", e);
          throw new RuntimeException(e);
        }
      }
    };
  }

  /**
   * Converts the number to text and pads it to a fix length.
   *
   * @param num The number to pad.
   * @return The padded number text.
   */
  private static String pad(final int num) {
    return String.format("%0" + padLength + "d", num);
  }
}

/* SPDX-License-Identifier: Apache-2.0 */

package hu.bme.mit.ftsrg.hypernate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.EqualsAndHashCode;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link SerializableEntity} entities.
 *
 * <p>This class provides some reflection-based implementations of the {@link SerializableEntity}
 * methods, so that you do not have to implement all the boilerplate yourself in the final entity
 * classes.
 *
 * @param <Type> the type of the entity
 */
@EqualsAndHashCode
@DataType
public abstract class SerializableEntityBase<Type extends SerializableEntity<Type>>
    implements SerializableEntity<Type> {

  private static final Logger logger = LoggerFactory.getLogger(SerializableEntityBase.class);

  /**
   * Returns the entity type name, which is its class's name in all-uppercase.
   *
   * @return The name of the class of this entity, in all-uppercase
   * @see SerializableEntity#getType()
   */
  @Override
  public String getType() {
    final String type = this.getClass().getName().toUpperCase();
    logger.debug("Returning type name: {}", type);
    return type;
  }

  /**
   * Serializes this entity to a JSON and then to a byte array.
   *
   * @return This entity, serialized into a JSON string and then converted to a UTF-8 byte array.
   * @see SerializableEntity#toBuffer()
   */
  @Override
  public byte[] toBuffer() {
    final String json = this.toJson();
    final byte[] buffer = json.getBytes(StandardCharsets.UTF_8);
    logger.debug("Returning buffer (size={}) from JSON: {}", buffer.length, json);
    return buffer;
  }

  /**
   * Deserializes this entity from a byte array.
   *
   * @param buffer the buffer to parse
   * @see SerializableEntityBase#toBuffer()
   */
  @Override
  public void fromBuffer(final byte[] buffer) {
    final String json = new String(buffer, StandardCharsets.UTF_8);
    logger.debug("Parsing entity from JSON: {}", json);
    this.fromJson(json);
  }

  /**
   * Serializes this entity into a JSON.
   *
   * @return this entity as a JSON string
   * @see SerializableEntity#toJson()
   */
  @Override
  public String toJson() {
    final String json = JSON.serialize(this);
    logger.debug("Returning JSON string from entity: {}", json);
    return json;
  }

  /**
   * Deserializes this entity from a JSON.
   *
   * <p>This reflection-based implementation sets all fields with matching names from the JSON.
   * Fields that are not found in the JSON remain unset. Conversely, extraneous keys in the JSON are
   * ignored.
   *
   * @param json the JSON string to parse
   * @see SerializableEntityBase#toJson()
   */
  @Override
  public void fromJson(final String json) {
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
   * Get the composite key for this entity.
   *
   * <p>This reference implementation takes all fields annotated with {@link KeyPart} in the entity
   * class, pads them to {@link SerializableEntityBase#padLength} and creates an array from those.
   *
   * <p><b>WARNING:</b> the order of keys might matter; this implementation has not been tested.
   *
   * @return the composite key of this entity
   */
  @Override
  public String[] getKeyParts() {
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
   * Get a factory that can instantiate this entity.
   *
   * <p><b>WARNING:</b> this is a rather lousy reflection-based implementation that has not been
   * tested
   *
   * @return a {@link EntityFactory} for this entity type
   */
  @Override
  public EntityFactory<Type> getFactory() {
    final Class<? extends SerializableEntityBase> ourClass = this.getClass();
    // Lambda-based implementation replaced with code below to accommodate OpenJML...
    return new EntityFactory<>() {
      @Override
      public Type create() {
        logger.debug("Was asked to create a new entity instance of {}...", ourClass.getName());
        try {
          return (Type) ourClass.getDeclaredConstructor().newInstance();
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

  private static final int padLength = Integer.toString(Integer.MAX_VALUE).length();

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

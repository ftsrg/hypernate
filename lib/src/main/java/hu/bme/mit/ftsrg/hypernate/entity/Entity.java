/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.annotations.AttributeInfo;
import hu.bme.mit.ftsrg.hypernate.annotations.PrimaryKey;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic database entity that can be serialized to JSON and to byte arrays.
 *
 * <p>The default method implementations provide some reflection-based implementations, so that you
 * do not have to implement all the boilerplate yourself in the actual entity classes.
 */
@DataType
@Loggable(Loggable.DEBUG)
public interface Entity {

  Logger logger = LoggerFactory.getLogger(Entity.class);

  int padLength = Integer.toString(Integer.MAX_VALUE).length();

  /**
   * Converts the number to text and pads it to a fix length.
   *
   * @param num The number to pad.
   * @return The padded number text.
   */
  private static String pad(final int num) {
    return String.format("%0" + padLength + "d", num);
  }

  private static String applyAttrMapper(final AttributeInfo attributeInfo, final Object key) {
    Class<? extends Function<Object, String>> mapperClass = attributeInfo.mapper();
    Constructor<? extends Function<Object, String>> mapperCtor;
    try {
      mapperCtor = mapperClass.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      logger.error("Could not find no-arg constructor for mapper {}", mapperClass.getName());
      throw new RuntimeException(e);
    }

    Function<Object, String> mapper;
    try {
      mapper = mapperCtor.newInstance();
    } catch (InstantiationException e) {
      logger.error("Failed to instantiate mapper {}", mapperClass.getName());
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      logger.error("Could not access constructor for mapper {}", mapperClass.getName());
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      logger.error(
          "An exception was thrown by the constructor of mapper {}", mapperClass.getName());
      throw new RuntimeException(e);
    }
    logger.trace(
        "Successfully instantiated mapper of type {} for primary key attribute {}",
        mapperClass.getName(),
        attributeInfo.name());

    return mapper.apply(key);
  }

  /**
   * Get the type of this entity; essentially a table name.
   *
   * <p>The default is an all-caps string, such as <code>CUSTOMER</code>.
   *
   * @return the arbitrary identifier of this entity type
   * @see Entity#getType()
   */
  @JsonIgnore
  default String getType() {
    return this.getClass().getName().toUpperCase();
  }

  /**
   * Get the composite key for this entity.
   *
   * <p>The composite key is an array of strings comprising the primary key fields of the entity (ie
   * those defined in {@link PrimaryKey}.
   *
   * @return the composite key of this entity
   */
  @JsonIgnore
  default String[] getPrimaryKeys() {
    return Arrays.stream(this.getClass().getAnnotation(PrimaryKey.class).value())
        .map(
            attrInfo -> {
              logger.debug("Processing primary key attribute {}", attrInfo.name());
              final Object value = getFieldValueForAttr(attrInfo);
              final String mappedKey = applyAttrMapper(attrInfo, value);
              logger.debug(
                  "Result of primary key mapping for attribute {} is {}",
                  attrInfo.name(),
                  mappedKey);
              return mappedKey;
            })
        .toArray(String[]::new);
  }

  @JsonIgnore
  default String[] getPartialKey(Object... parts) {
    final AttributeInfo[] attrInfos = this.getClass().getAnnotation(PrimaryKey.class).value();
    return IntStream.range(0, Math.min(attrInfos.length, parts.length))
        .mapToObj(i -> applyAttrMapper(attrInfos[i], parts[i]))
        .toArray(String[]::new);
  }

  private Object getFieldValueForAttr(final AttributeInfo attrInfo) {
    final Field field;
    try {
      field = getClass().getField(attrInfo.name());
    } catch (NoSuchFieldException e) {
      logger.error("Could not find field {} in class {}", attrInfo.name(), getClass().getName());
      throw new RuntimeException(e);
    }
    logger.trace("Found field for primary key attribute {}", attrInfo.name());

    final Object value;
    try {
      value = field.get(this);
    } catch (IllegalAccessException e) {
      logger.error("Could not access field {} in class {}", field.getName(), getClass().getName());
      throw new RuntimeException(e);
    }

    return value;
  }

  default CompositeKey getCompositeKey(ChaincodeStub stub) {
    return stub.createCompositeKey(getType(), getPrimaryKeys());
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
    return toJson().getBytes(StandardCharsets.UTF_8);
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
    return JSON.serialize(this);
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
        }
      } catch (NoSuchFieldException e) {
        logger.error("Got exception while trying to access/set a field", e);
      }
    }
  }
}

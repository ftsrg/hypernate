/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.annotations.AttributeInfo;
import hu.bme.mit.ftsrg.hypernate.annotations.PrimaryKey;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import hu.bme.mit.ftsrg.hypernate.util.JSON;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Loggable(Loggable.DEBUG)
public class Registry {

  private static final Logger logger = LoggerFactory.getLogger(Registry.class);

  private final ChaincodeStub stub;

  public Registry(final ChaincodeStub stub) {
    this.stub = stub;
  }

  public <T> void mustCreate(final T entity) throws EntityExistsException, SerializationException {
    assertNotExists(entity);

    final String key = getCompositeKey(entity);
    final byte[] buffer = EntityUtil.toBuffer(entity);
    putState(key, buffer);
  }

  public <T> void tryCreate(final T entity) {
    try {
      mustCreate(entity);
    } catch (SerializationException | EntityExistsException e) {
      logger.warn(
          "{} exception ({}) while trying to create {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T> void mustUpdate(final T entity)
      throws EntityNotFoundException, SerializationException {
    assertExists(entity);

    final String key = getCompositeKey(entity);
    final byte[] buffer = EntityUtil.toBuffer(entity);
    putState(key, buffer);
  }

  public <T> void tryUpdate(final T entity) {
    try {
      mustUpdate(entity);
    } catch (SerializationException | EntityNotFoundException e) {
      logger.warn(
          "{} exception ({}) while trying to update {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T> void mustDelete(final T entity) throws EntityNotFoundException {
    assertExists(entity);

    final String key = getCompositeKey(entity);
    stub.delState(key);
  }

  public <T> void tryDelete(final T entity) {
    try {
      mustDelete(entity);
    } catch (EntityNotFoundException e) {
      logger.warn(
          "{} exception ({}) while trying to delete {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T> T mustRead(Class<T> clazz, Object... keys)
      throws EntityNotFoundException, SerializationException {
    if (keys.length != EntityUtil.getPrimaryKeyCount(clazz)) {
      throw new IllegalArgumentException(
          "Partial key array does not match number of primary keys for " + clazz.getName());
    }

    final String key =
        stub.createCompositeKey(EntityUtil.getType(clazz), EntityUtil.getPartialKey(clazz))
            .toString();
    final byte[] data = stub.getState(key);

    if (data == null || data.length == 0) {
      throw new EntityNotFoundException(key);
    }

    return tryDeserializeEntity(clazz, data);
  }

  public <T> T tryRead(Class<T> clazz, Object... keys) {
    try {
      return mustRead(clazz, keys);
    } catch (SerializationException | EntityNotFoundException e) {
      logger.warn(
          "{} exception ({}) while trying to read {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          clazz.getSimpleName());
      return null;
    }
  }

  public <T> List<T> readAll(final Class<T> clazz) {
    final String key = stub.createCompositeKey(EntityUtil.getType(clazz)).toString();
    Iterator<KeyValue> iterator = stub.getStateByPartialCompositeKey(key).iterator();
    Iterable<KeyValue> iterable = () -> iterator;
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(
            kv -> {
              final byte[] value = kv.getValue();
              logger.debug(
                  "Found value at partial key {}: {} -> {}",
                  key,
                  kv.getKey(),
                  Arrays.toString(value));
              return tryDeserializeEntity(clazz, value);
            })
        .collect(Collectors.toList());
  }

  private static <T> T tryDeserializeEntity(Class<T> clazz, byte[] value) {
    final T entity;

    try {
      entity = EntityUtil.fromBuffer(value, clazz);
    } catch (SerializationException e) {
      logger.error("Failed to deserialize entity from data: {}", value);
      throw new RuntimeException(e);
    }
    logger.debug("Deserialized entity from data: {}", entity);

    return entity;
  }

  private void putState(String key, byte[] buf) {
    stub.putState(key, buf);
  }

  @Loggable(Loggable.DEBUG)
  private boolean keyExists(final String key) {
    final byte[] valueOnLedger = stub.getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  @Loggable(Loggable.DEBUG)
  private <T> boolean exists(final T ent) {
    return keyExists(getCompositeKey(ent));
  }

  @Loggable(Loggable.DEBUG)
  private <T> void assertNotExists(final T ent) throws EntityExistsException {
    if (exists(ent)) {
      throw new EntityExistsException(getCompositeKey(ent));
    }
  }

  @Loggable(Loggable.DEBUG)
  private <T> void assertExists(final T ent) throws EntityNotFoundException {
    if (!exists(ent)) {
      throw new EntityNotFoundException(getCompositeKey(ent));
    }
  }

  private <T> String getCompositeKey(final T ent) {
    return stub.createCompositeKey(EntityUtil.getType(ent), EntityUtil.getPrimaryKeys(ent))
        .toString();
  }

  @UtilityClass
  private class EntityUtil {

    Logger logger = LoggerFactory.getLogger(EntityUtil.class);

    <T> String getType(final T entity) {
      return getType(entity.getClass());
    }

    <T> String getType(final Class<T> clazz) {
      return clazz.getName().toUpperCase();
    }

    <T> int getPrimaryKeyCount(final Class<T> clazz) {
      return clazz.getAnnotation(PrimaryKey.class) != null
          ? clazz.getAnnotation(PrimaryKey.class).value().length
          : 0;
    }

    <T> String[] getPrimaryKeys(final T entity) {
      return Arrays.stream(entity.getClass().getAnnotation(PrimaryKey.class).value())
          .map(
              attrInfo -> {
                logger.debug("Processing primary key attribute {}", attrInfo.name());
                final Object value = getFieldValueForAttr(entity, attrInfo);
                final String mappedKey = applyAttrMapper(attrInfo, value);
                logger.debug(
                    "Result of primary key mapping for attribute {} is {}",
                    attrInfo.name(),
                    mappedKey);
                return mappedKey;
              })
          .toArray(String[]::new);
    }

    <T> String[] getPartialKey(final T entity, final Object... parts) {
      return getPartialKey(entity.getClass(), parts);
    }

    <T> String[] getPartialKey(final Class<T> clazz, final Object... parts) {
      final AttributeInfo[] attrInfos = clazz.getAnnotation(PrimaryKey.class).value();
      return IntStream.range(0, Math.min(attrInfos.length, parts.length))
          .mapToObj(i -> applyAttrMapper(attrInfos[i], parts[i]))
          .toArray(String[]::new);
    }

    <T> byte[] toBuffer(final T entity) throws SerializationException {
      return toJson(entity).getBytes(StandardCharsets.UTF_8);
    }

    <T> T fromBuffer(final byte[] buffer, final Class<T> clazz) throws SerializationException {
      final String json = new String(buffer, StandardCharsets.UTF_8);
      logger.debug("Parsing entity from JSON: {}", json);
      return JSON.deserialize(json, clazz);
    }

    <T> String toJson(final T entity) throws SerializationException {
      return JSON.serialize(entity);
    }

    private String applyAttrMapper(final AttributeInfo attrInfo, final Object key) {
      Class<? extends Function<Object, String>> mapperClass = attrInfo.mapper();
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
          attrInfo.name());

      return mapper.apply(key);
    }

    private <T> Object getFieldValueForAttr(final T ent, final AttributeInfo attrInfo) {
      final Field field;
      try {
        field = ent.getClass().getDeclaredField(attrInfo.name());
      } catch (NoSuchFieldException e) {
        logger.error(
            "Could not find field {} in class {}", attrInfo.name(), ent.getClass().getName());
        throw new RuntimeException(e);
      }
      field.setAccessible(true);
      logger.trace("Found field for primary key attribute {}", attrInfo.name());

      final Object value;
      try {
        value = field.get(ent);
      } catch (IllegalAccessException e) {
        logger.error(
            "Could not access field {} in class {}", field.getName(), ent.getClass().getName());
        throw new RuntimeException(e);
      }

      return value;
    }
  }
}

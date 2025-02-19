/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Loggable(Loggable.DEBUG)
public class Registry {

  private static final Logger classLogger = LoggerFactory.getLogger(Registry.class);
  private static final Logger stubCallLogger =
      LoggerFactory.getLogger(Registry.class.getName() + ":StubCalls");
  private final ChaincodeStub stub;

  public Registry(final ChaincodeStub stub) {
    this.stub = stub;
  }

  private static <T extends Entity> T instantiateEntity(Class<T> clazz) {
    Constructor<T> ctor;
    try {
      ctor = clazz.getDeclaredConstructor();
    } catch (NoSuchMethodException e) {
      classLogger.error("Could not find no-arg constructor for entity {}", clazz.getSimpleName());
      throw new RuntimeException(e);
    }

    T entity = null;
    try {
      entity = ctor.newInstance();
    } catch (InstantiationException e) {
      classLogger.error("Failed to instantiate entity of type {}", clazz.getSimpleName());
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      classLogger.error(
          "Access denied while instantiating entity of type {}", clazz.getSimpleName());
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      classLogger.error(
          "Exception occurred while instantiating entity of type {}", clazz.getSimpleName());
      throw new RuntimeException(e);
    }

    return entity;
  }

  private static <T extends Entity> T tryDeserializeEntity(Class<T> clazz, byte[] value) {
    final T entity = instantiateEntity(clazz);

    try {
      entity.fromBuffer(value);
    } catch (SerializationException e) {
      classLogger.error("Failed to deserialize entity from data: {}", value);
      throw new RuntimeException(e);
    }
    classLogger.debug("Deserialized entity from data: {}", entity);

    return entity;
  }

  public <T extends Entity> void mustCreate(final T entity)
      throws EntityExistsException, SerializationException {
    assertNotExists(entity);

    final String key = entity.getCompositeKey(stub).toString();
    final byte[] buffer = entity.toBuffer();
    putState(key, buffer);
  }

  private void putState(String key, byte[] buffer) {
    stubCallLogger.debug(
        "Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);
  }

  public <T extends Entity> void tryCreate(final T entity) {
    try {
      mustCreate(entity);
    } catch (SerializationException | EntityExistsException e) {
      classLogger.warn(
          "{} exception ({}) while trying to create {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T extends Entity> void mustUpdate(final T entity)
      throws EntityNotFoundException, SerializationException {
    assertExists(entity);

    final String key = entity.getCompositeKey(stub).toString();
    final byte[] buffer = entity.toBuffer();
    putState(key, buffer);
  }

  public <T extends Entity> void tryUpdate(final T entity) {
    try {
      mustUpdate(entity);
    } catch (SerializationException | EntityNotFoundException e) {
      classLogger.warn(
          "{} exception ({}) while trying to update {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T extends Entity> void mustDelete(final T entity) throws EntityNotFoundException {
    assertExists(entity);

    final String key = entity.getCompositeKey(stub).toString();
    stubCallLogger.debug("Calling stub#delState with key={}", key);
    stub.delState(key);
  }

  public <T extends Entity> void tryDelete(final T entity) {
    try {
      mustDelete(entity);
    } catch (EntityNotFoundException e) {
      classLogger.warn(
          "{} exception ({}) while trying to delete {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          entity.getClass().getSimpleName());
    }
  }

  public <T extends Entity> T mustRead(Class<T> clazz, Object... keys)
      throws EntityNotFoundException, SerializationException {
    T template = instantiateEntity(clazz);
    final String key =
        stub.createCompositeKey(template.getType(), template.getPartialKey(keys)).toString();
    stubCallLogger.debug("Calling stub#getState with key={}", key);
    final byte[] data = stub.getState(key);
    stubCallLogger.debug("Got data from stub#getState for key={}: {}", key, Arrays.toString(data));

    if (data == null || data.length == 0) {
      throw new EntityNotFoundException(key);
    }

    return tryDeserializeEntity(clazz, data);
  }

  public <T extends Entity> T tryRead(Class<T> clazz, Object... keys) {
    try {
      return mustRead(clazz, keys);
    } catch (SerializationException | EntityNotFoundException e) {
      classLogger.warn(
          "{} exception ({}) while trying to read {} entity",
          e.getClass(),
          e.getLocalizedMessage(),
          clazz.getSimpleName());
      return null;
    }
  }

  public <T extends Entity> List<T> readAll(final Class<T> clazz) {
    T template = instantiateEntity(clazz);
    final String key = stub.createCompositeKey(template.getType()).toString();
    stubCallLogger.debug("Calling stub#getStateByPartialCompositeKey with partial key={}", key);
    Iterator<KeyValue> iterator = stub.getStateByPartialCompositeKey(key).iterator();
    Iterable<KeyValue> iterable = () -> iterator;
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(
            kv -> {
              final byte[] value = kv.getValue();
              classLogger.debug(
                  "Found value at partial key {}: {} -> {}",
                  key,
                  kv.getKey(),
                  Arrays.toString(value));
              return tryDeserializeEntity(clazz, value);
            })
        .collect(Collectors.toList());
  }

  @Loggable(Loggable.DEBUG)
  private boolean keyExists(final String key) {
    final byte[] valueOnLedger = stub.getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> boolean exists(final T entity) {
    return keyExists(entity.getCompositeKey(stub).toString());
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> void assertNotExists(final T entity) throws EntityExistsException {
    if (exists(entity)) {
      throw new EntityExistsException(entity.getCompositeKey(stub).toString());
    }
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> void assertExists(final T entity) throws EntityNotFoundException {
    if (!exists(entity)) {
      throw new EntityNotFoundException(entity.getCompositeKey(stub).toString());
    }
  }
}

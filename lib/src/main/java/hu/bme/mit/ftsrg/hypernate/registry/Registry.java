/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import java.util.*;
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

  public <T extends Entity> void create(final T entity)
      throws EntityExistsException, SerializationException {
    assertNotExists(entity);

    final String key = key(entity);
    final byte[] buffer = entity.toBuffer();
    stubCallLogger.debug(
        "Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);
  }

  public <T extends Entity> void update(final T entity)
      throws EntityNotFoundException, SerializationException {
    assertExists(entity);

    final String key = key(entity);
    final byte[] buffer = entity.toBuffer();
    stubCallLogger.debug(
        "Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);
  }

  public <T extends Entity> void delete(final T entity) throws EntityNotFoundException {
    assertExists(entity);

    final String key = key(entity);
    stubCallLogger.debug("Calling stub#delState with key={}", key);
    stub.delState(key);
  }

  public <T extends Entity> T read(final T target)
      throws EntityNotFoundException, SerializationException {
    final String key = key(target);
    stubCallLogger.debug("Calling stub#getState with key={}", key);
    final byte[] data = stub.getState(key);
    stubCallLogger.debug("Got data from stub#getState for key={}: {}", key, Arrays.toString(data));

    if (data == null || data.length == 0) {
      throw new EntityNotFoundException(key);
    }

    target.fromBuffer(data);
    classLogger.debug("Deserialized entity from data: {}", target);

    return target;
  }

  public <T extends Entity> List<T> readAll(final T template) throws SerializationException {
    final List<T> entities = new ArrayList<>();
    final String compositeKey = stub.createCompositeKey(template.getType()).toString();
    stubCallLogger.debug(
        "Calling stub#getStateByPartialCompositeKey with partial key={}", compositeKey);
    for (KeyValue keyValue : stub.getStateByPartialCompositeKey(compositeKey)) {
      final byte[] value = keyValue.getValue();
      classLogger.debug("Found value at partial key={}: {}", compositeKey, Arrays.toString(value));
      final T entity = (T) template.create();
      entity.fromBuffer(value);
      classLogger.debug("Deserialized entity from data: {}", entity);
      entities.add(entity);
    }
    classLogger.debug(
        "Found {} entities in total for partial key={}", entities.size(), compositeKey);

    return entities;
  }

  public <T extends Entity> SelectionBuilder<T> select(final T template)
      throws SerializationException {
    return new SelectionBuilder<>(readAll(template));
  }

  @Loggable(Loggable.DEBUG)
  private boolean keyExists(final String key) {
    final byte[] valueOnLedger = stub.getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> boolean exists(final T obj) {
    return keyExists(key(obj));
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> void assertNotExists(final T obj) throws EntityExistsException {
    if (exists(obj)) {
      throw new EntityExistsException(key(obj));
    }
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> void assertExists(final T obj) throws EntityNotFoundException {
    if (!exists(obj)) {
      throw new EntityNotFoundException(key(obj));
    }
  }

  @Loggable(Loggable.DEBUG)
  private <T extends Entity> String key(final T obj) {
    return stub.createCompositeKey(obj.getType(), obj.getKeyParts()).toString();
  }

  public interface Matcher<T extends Entity> {

    boolean match(T entity);
  }

  @Loggable(Loggable.DEBUG)
  public static final class SelectionBuilder<T extends Entity> {

    private final Logger logger = LoggerFactory.getLogger(SelectionBuilder.class);

    private List<T> selection;

    SelectionBuilder(final List<T> entities) {
      selection = entities;
    }

    public SelectionBuilder<T> matching(final Matcher<T> matcher) {
      final List<T> newSelection = new ArrayList<>();
      for (T entity : selection) {
        logger.debug("Testing matcher on entity: {}", entity);
        if (matcher.match(entity)) {
          logger.debug("Entity matches");
          newSelection.add(entity);
        } else {
          logger.debug("Entity does not match");
        }
      }
      logger.debug("Matched {} entities in total with matcher", newSelection.size());
      selection = newSelection;
      return this;
    }

    public SelectionBuilder<T> sortedBy(final Comparator<T> comparator) {
      selection.sort(comparator);
      logger.debug("Sorted entities");
      return this;
    }

    public SelectionBuilder<T> descending() {
      Collections.reverse(selection);
      logger.debug("Reversed entity order");
      return this;
    }

    public List<T> get() {
      return selection;
    }

    public T getFirst() {
      if (selection.isEmpty()) {
        logger.debug("No entites in this selection; returning null");
        return null;
      }

      final T first = selection.get(0);
      logger.debug("Returning the first entity in selection: {}", first);
      return first;
    }
  }
}

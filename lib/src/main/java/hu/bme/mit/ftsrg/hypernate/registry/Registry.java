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

  public <Type extends Entity> void create(final Type entity)
      throws EntityExistsException, SerializationException {
    assertNotExists(entity);

    final String key = getKey(entity);
    final byte[] buffer = entity.toBuffer();
    stubCallLogger.debug(
        "Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);
  }

  public <Type extends Entity> void update(final Type entity)
      throws EntityNotFoundException, SerializationException {
    assertExists(entity);

    final String key = getKey(entity);
    final byte[] buffer = entity.toBuffer();
    stubCallLogger.debug(
        "Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);
  }

  public <Type extends Entity> void delete(final Type entity) throws EntityNotFoundException {
    assertExists(entity);

    final String key = getKey(entity);
    stubCallLogger.debug("Calling stub#delState with key={}", key);
    stub.delState(key);
  }

  public <Type extends Entity> Type read(final Type target)
      throws EntityNotFoundException, SerializationException {
    final String key = getKey(target);
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

  public <Type extends Entity> List<Type> readAll(final Type template)
      throws SerializationException {
    final List<Type> entities = new ArrayList<>();
    final String compositeKey = stub.createCompositeKey(template.getType()).toString();
    stubCallLogger.debug(
        "Calling stub#getStateByPartialCompositeKey with partial key={}", compositeKey);
    for (KeyValue keyValue : stub.getStateByPartialCompositeKey(compositeKey)) {
      final byte[] value = keyValue.getValue();
      classLogger.debug("Found value at partial key={}: {}", compositeKey, Arrays.toString(value));
      final Type entity = (Type) template.create();
      entity.fromBuffer(value);
      classLogger.debug("Deserialized entity from data: {}", entity);
      entities.add(entity);
    }
    classLogger.debug(
        "Found {} entities in total for partial key={}", entities.size(), compositeKey);

    return entities;
  }

  public <Type extends Entity> SelectionBuilder<Type> select(final Type template)
      throws SerializationException {
    return new SelectionBuilder<>(readAll(template));
  }

  @Loggable(Loggable.DEBUG)
  private boolean keyExists(final String key) {
    final byte[] valueOnLedger = stub.getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  @Loggable(Loggable.DEBUG)
  private <Type extends Entity> boolean exists(final Type obj) {
    return keyExists(getKey(obj));
  }

  @Loggable(Loggable.DEBUG)
  private <Type extends Entity> void assertNotExists(final Type obj) throws EntityExistsException {
    if (exists(obj)) {
      throw new EntityExistsException(getKey(obj));
    }
  }

  @Loggable(Loggable.DEBUG)
  private <Type extends Entity> void assertExists(final Type obj) throws EntityNotFoundException {
    if (!exists(obj)) {
      throw new EntityNotFoundException(getKey(obj));
    }
  }

  @Loggable(Loggable.DEBUG)
  private <Type extends Entity> String getKey(final Type obj) {
    return stub.createCompositeKey(obj.getType(), obj.getKeyParts()).toString();
  }

  public interface Matcher<Type extends Entity> {

    boolean match(Type entity);
  }

  @Loggable(Loggable.DEBUG)
  public static final class SelectionBuilder<Type extends Entity> {

    private final Logger logger = LoggerFactory.getLogger(SelectionBuilder.class);

    private List<Type> selection;

    SelectionBuilder(final List<Type> entities) {
      selection = entities;
    }

    public SelectionBuilder<Type> matching(final Matcher<Type> matcher) {
      final List<Type> newSelection = new ArrayList<>();
      for (Type entity : selection) {
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

    public SelectionBuilder<Type> sortedBy(final Comparator<Type> comparator) {
      selection.sort(comparator);
      logger.debug("Sorted entities");
      return this;
    }

    public SelectionBuilder<Type> descending() {
      Collections.reverse(selection);
      logger.debug("Reversed entity order");
      return this;
    }

    public List<Type> get() {
      return selection;
    }

    public Type getFirst() {
      if (selection.isEmpty()) {
        logger.debug("No entites in this selection; returning null");
        return null;
      }

      final Type first = selection.get(0);
      logger.debug("Returning the first entity in selection: {}", first);
      return first;
    }
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityFactory;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import hu.bme.mit.ftsrg.hypernate.util.MethodLogger;
import java.util.*;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Loggable(Loggable.DEBUG) // FIXME how to configure AspectJ with OpenJML and Gradle?
public class Registry {

  private static final Logger logger = LoggerFactory.getLogger(Registry.class);
  private static final MethodLogger methodLogger = new MethodLogger(logger, "Registry");
  private final ChaincodeStub stub;

  public Registry(final ChaincodeStub stub) {
    this.stub = stub;
  }

  public <Type extends Entity<Type>> void create(final Type entity)
      throws EntityExistsException, SerializationException {
    final String paramsString = methodLogger.generateParamsString(entity);
    methodLogger.logStart("create", paramsString);

    assertNotExists(entity);

    final String key = getKey(entity);
    final byte[] buffer = entity.toBuffer();
    logger.debug("Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);

    methodLogger.logEnd("create", paramsString, "<void>");
  }

  public <Type extends Entity<Type>> void update(final Type entity)
      throws EntityNotFoundException, SerializationException {
    final String paramsString = methodLogger.generateParamsString(entity);
    methodLogger.logStart("update", paramsString);

    assertExists(entity);

    final String key = getKey(entity);
    final byte[] buffer = entity.toBuffer();
    logger.debug("Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    stub.putState(key, buffer);

    methodLogger.logEnd("update", paramsString, "<void>");
  }

  public <Type extends Entity<Type>> void delete(final Type entity) throws EntityNotFoundException {
    final String paramsString = methodLogger.generateParamsString(entity);
    methodLogger.logStart("delete", paramsString);

    assertExists(entity);

    final String key = getKey(entity);
    logger.debug("Calling stub#delState with key={}", key);
    stub.delState(key);

    methodLogger.logEnd("delete", paramsString, "<void>");
  }

  public <Type extends Entity<Type>> Type read(final Type target)
      throws EntityNotFoundException, SerializationException {
    final String paramsString = methodLogger.generateParamsString(target);
    methodLogger.logStart("read", paramsString);

    final String key = getKey(target);
    logger.debug("Calling stub#getState with key={}", key);
    final byte[] data = stub.getState(key);
    logger.debug("Got data from stub#getState for key={}: {}", key, Arrays.toString(data));

    if (data == null || data.length == 0) {
      throw new EntityNotFoundException(key);
    }

    target.fromBuffer(data);
    logger.debug("Deserialized entity from data: {}", target);

    methodLogger.logEnd("read", paramsString, target.toString());
    return target;
  }

  public <Type extends Entity<Type>> List<Type> readAll(final Type template)
      throws SerializationException {
    final String paramsString = methodLogger.generateParamsString(template);
    methodLogger.logStart("readAll", paramsString);

    final List<Type> entities = new ArrayList<>();
    final String compositeKey = stub.createCompositeKey(template.getType()).toString();
    logger.debug("Calling stub#getStateByPartialCompositeKey with partial key={}", compositeKey);
    for (KeyValue keyValue : stub.getStateByPartialCompositeKey(compositeKey)) {
      final byte[] value = keyValue.getValue();
      logger.debug("Found value at partial key={}: {}", compositeKey, Arrays.toString(value));
      final EntityFactory<Type> factory = template.getFactory();
      final Type entity = factory.create();
      entity.fromBuffer(value);
      logger.debug("Deserialized entity from data: {}", entity);
      entities.add(entity);
    }
    logger.debug("Found {} entities in total for partial key={}", entities.size(), compositeKey);

    methodLogger.logEnd("readAll", paramsString, entities.toString());
    return entities;
  }

  public <Type extends Entity<Type>> SelectionBuilder<Type> select(final Type template)
      throws SerializationException {
    final String paramsString = methodLogger.generateParamsString(template);
    methodLogger.logStart("select", paramsString);

    final SelectionBuilder<Type> builder = new SelectionBuilder<>(readAll(template));
    methodLogger.logEnd("select", paramsString, builder.toString());
    return builder;
  }

  private boolean keyExists(final String key) {
    final byte[] valueOnLedger = stub.getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  private <Type extends Entity<Type>> boolean exists(final Type obj) {
    return keyExists(getKey(obj));
  }

  private <Type extends Entity<Type>> void assertNotExists(final Type obj)
      throws EntityExistsException {
    if (exists(obj)) {
      throw new EntityExistsException(getKey(obj));
    }
  }

  private <Type extends Entity<Type>> void assertExists(final Type obj)
      throws EntityNotFoundException {
    if (!exists(obj)) {
      throw new EntityNotFoundException(getKey(obj));
    }
  }

  private <Type extends Entity<Type>> String getKey(final Type obj) {
    final String paramsString = methodLogger.generateParamsString(obj);
    methodLogger.logStart("getKey", paramsString);

    final CompositeKey compositeKey = stub.createCompositeKey(obj.getType(), obj.getKeyParts());

    methodLogger.logEnd("getKey", paramsString, compositeKey.toString());
    return compositeKey.toString();
  }

  public interface Matcher<Type extends Entity<Type>> {

    boolean match(Type entity);
  }

  public static final class SelectionBuilder<Type extends Entity<Type>> {

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

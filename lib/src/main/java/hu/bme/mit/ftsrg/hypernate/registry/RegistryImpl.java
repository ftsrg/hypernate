/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry;

import com.jcabi.aspects.Loggable;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityFactory;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializableEntity;
import hu.bme.mit.ftsrg.hypernate.util.MethodLogger;
import java.util.*;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Loggable(Loggable.DEBUG) // FIXME how to configure AspectJ with OpenJML and Gradle?
public class RegistryImpl implements Registry {

  private static final Logger logger = LoggerFactory.getLogger(RegistryImpl.class);

  private static final MethodLogger methodLogger = new MethodLogger(logger, "RegistryImpl");

  public RegistryImpl() {}

  private <Type extends SerializableEntity<Type>> String getKey(final Context ctx, final Type obj) {
    final String paramsString = methodLogger.generateParamsString(ctx, obj);
    methodLogger.logStart("getKey", paramsString);

    final CompositeKey compositeKey =
        ctx.getStub().createCompositeKey(obj.getType(), obj.getKeyParts());

    methodLogger.logEnd("getKey", paramsString, compositeKey.toString());
    return compositeKey.toString();
  }

  @Override
  public <Type extends SerializableEntity<Type>> void create(final Context ctx, final Type entity)
      throws EntityExistsException {
    final String paramsString = methodLogger.generateParamsString(ctx, entity);
    methodLogger.logStart("create", paramsString);

    assertNotExists(ctx, entity);

    final String key = getKey(ctx, entity);
    final byte[] buffer = entity.toBuffer();
    logger.debug("Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    ctx.getStub().putState(key, buffer);

    methodLogger.logEnd("create", paramsString, "<void>");
  }

  @Override
  public <Type extends SerializableEntity<Type>> void update(final Context ctx, final Type entity)
      throws EntityNotFoundException {
    final String paramsString = methodLogger.generateParamsString(ctx, entity);
    methodLogger.logStart("update", paramsString);

    assertExists(ctx, entity);

    final String key = getKey(ctx, entity);
    final byte[] buffer = entity.toBuffer();
    logger.debug("Calling stub#putState with key={} and value={}", key, Arrays.toString(buffer));
    ctx.getStub().putState(key, buffer);

    methodLogger.logEnd("update", paramsString, "<void>");
  }

  @Override
  public <Type extends SerializableEntity<Type>> void delete(final Context ctx, final Type entity)
      throws EntityNotFoundException {
    final String paramsString = methodLogger.generateParamsString(ctx, entity);
    methodLogger.logStart("delete", paramsString);

    assertExists(ctx, entity);

    final String key = getKey(ctx, entity);
    logger.debug("Calling stub#delState with key={}", key);
    ctx.getStub().delState(key);

    methodLogger.logEnd("delete", paramsString, "<void>");
  }

  @Override
  public <Type extends SerializableEntity<Type>> Type read(final Context ctx, final Type target)
      throws EntityNotFoundException {
    final String paramsString = methodLogger.generateParamsString(ctx, target);
    methodLogger.logStart("read", paramsString);

    final String key = getKey(ctx, target);
    logger.debug("Calling stub#getState with key={}", key);
    final byte[] data = ctx.getStub().getState(key);
    logger.debug("Got data from stub#getState for key={}: {}", key, Arrays.toString(data));

    if (data == null || data.length == 0) throw new EntityNotFoundException(key);

    target.fromBuffer(data);
    logger.debug("Deserialized entity from data; JSON representation={}", target.toJson());

    methodLogger.logEnd("read", paramsString, target.toJson());
    return target;
  }

  @Override
  public <Type extends SerializableEntity<Type>> List<Type> readAll(
      final Context ctx, final Type template) {
    final String paramsString = methodLogger.generateParamsString(ctx, template);
    methodLogger.logStart("readAll", paramsString);

    final List<Type> entities = new ArrayList<>();
    final String compositeKey = ctx.getStub().createCompositeKey(template.getType()).toString();
    logger.debug("Calling stub#getStateByPartialCompositeKey with partial key={}", compositeKey);
    for (KeyValue keyValue : ctx.getStub().getStateByPartialCompositeKey(compositeKey)) {
      final byte[] value = keyValue.getValue();
      logger.debug("Found value at partial key={}: {}", compositeKey, Arrays.toString(value));
      final EntityFactory<Type> factory = template.getFactory();
      final Type entity = factory.create();
      entity.fromBuffer(value);
      logger.debug("Deserialized entity from data; JSON representation={}", entity.toJson());
      entities.add(entity);
    }
    logger.debug("Found {} entities in total for partial key={}", entities.size(), compositeKey);

    methodLogger.logEnd("readAll", paramsString, entities.toString());
    return entities;
  }

  @Override
  public <Type extends SerializableEntity<Type>> SelectionBuilder<Type> select(
      final Context ctx, final Type template) {
    final String paramsString = methodLogger.generateParamsString(ctx, template);
    methodLogger.logStart("select", paramsString);

    final SelectionBuilder<Type> builder = new SelectionBuilderImpl<>(this.readAll(ctx, template));
    methodLogger.logEnd("select", paramsString, builder.toString());
    return builder;
  }

  private boolean keyExists(final Context ctx, final String key) {
    final byte[] valueOnLedger = ctx.getStub().getState(key);
    return valueOnLedger != null && valueOnLedger.length > 0;
  }

  public <Type extends SerializableEntity<Type>> boolean exists(final Context ctx, final Type obj) {
    return keyExists(ctx, getKey(ctx, obj));
  }

  public <Type extends SerializableEntity<Type>> void assertNotExists(
      final Context ctx, final Type obj) throws EntityExistsException {
    if (exists(ctx, obj)) throw new EntityExistsException(getKey(ctx, obj));
  }

  public <Type extends SerializableEntity<Type>> void assertExists(
      final Context ctx, final Type obj) throws EntityNotFoundException {
    if (!exists(ctx, obj)) throw new EntityNotFoundException(getKey(ctx, obj));
  }

  private static final class SelectionBuilderImpl<Type extends SerializableEntity<Type>>
      implements SelectionBuilder<Type> {

    private List<Type> selection;

    SelectionBuilderImpl(final List<Type> entities) {
      this.selection = entities;
    }

    @Override
    public SelectionBuilder<Type> matching(final Matcher<Type> matcher) {
      final List<Type> newSelection = new ArrayList<>();
      for (Type entity : this.selection) {
        logger.debug("Testing matcher on entity (JSON): {}", entity.toJson());
        if (matcher.match(entity)) {
          logger.debug("Entity matches");
          newSelection.add(entity);
        } else logger.debug("Entity does not match");
      }
      logger.debug("Matched {} entities in total with matcher", newSelection.size());
      this.selection = newSelection;
      return this;
    }

    @Override
    public SelectionBuilder<Type> sortedBy(final Comparator<Type> comparator) {
      this.selection.sort(comparator);
      logger.debug("Sorted entities");
      return this;
    }

    @Override
    public SelectionBuilder<Type> descending() {
      Collections.reverse(this.selection);
      logger.debug("Reversed entity order");
      return this;
    }

    @Override
    public List<Type> get() {
      return this.selection;
    }

    @Override
    public Type getFirst() {
      if (this.selection.isEmpty()) {
        logger.debug("No entites in this selection; returning null");
        return null;
      }

      final Type first = this.selection.get(0);
      logger.debug("Returning the first entity in selection (JSON): {}", first.toJson());
      return first;
    }
  }
}

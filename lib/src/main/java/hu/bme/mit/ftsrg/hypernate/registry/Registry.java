/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.registry;

import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializableEntity;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import java.util.Comparator;
import java.util.List;
import org.hyperledger.fabric.contract.Context;

public interface Registry {

  <Type extends SerializableEntity<Type>> void create(Context ctx, Type entity)
      throws EntityExistsException, SerializationException;

  <Type extends SerializableEntity<Type>> void update(Context ctx, Type entity)
      throws EntityNotFoundException, SerializationException;

  <Type extends SerializableEntity<Type>> void delete(Context ctx, Type entity)
      throws EntityNotFoundException;

  <Type extends SerializableEntity<Type>> Type read(Context ctx, Type target)
      throws EntityNotFoundException, SerializationException;

  <Type extends SerializableEntity<Type>> List<Type> readAll(Context ctx, Type template)
      throws SerializationException;

  <Type extends SerializableEntity<Type>> SelectionBuilder<Type> select(Context ctx, Type template)
      throws SerializationException;

  interface SelectionBuilder<Type extends SerializableEntity<Type>> {
    SelectionBuilder<Type> matching(Matcher<Type> matcher);

    SelectionBuilder<Type> sortedBy(Comparator<Type> comparator);

    SelectionBuilder<Type> descending();

    List<Type> get();

    Type getFirst();
  }

  interface Matcher<Type extends SerializableEntity<Type>> {
    boolean match(Type entity);
  }

  enum Order {
    ASCENDING,
    DESCENDING
  }
}

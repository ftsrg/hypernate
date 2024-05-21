/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.EntityNotFoundException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class RegistryTest {

  private static final String ENTITY_TYPE = "TEST_ENTITY";
  private static final String[] ENTITY_KEY_PARTS = {"P1", "P2"};
  private static final byte[] ENTITY_BUFFER = {1, 2, 3};
  private static final CompositeKey ENTITY_COMPOSITE_KEY =
      new CompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS);
  private static final String ENTITY_COMPOSITE_KEY_STR = ENTITY_COMPOSITE_KEY.toString();

  @Mock private ChaincodeStub stub;

  @Mock private TestEntity entity;

  private Registry registry;

  @BeforeEach
  void setup() {
    registry = new Registry(stub);

    given(entity.getType()).willReturn(ENTITY_TYPE);
  }

  private static class TestEntity implements Entity {}

  @Nested
  class given_empty_ledger {

    @Test
    void when_create_then_call_putState() throws SerializationException, EntityExistsException {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(entity.toBuffer()).willReturn(ENTITY_BUFFER);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);

      registry.create(entity);

      then(stub).should().putState(ENTITY_COMPOSITE_KEY_STR, ENTITY_BUFFER);
    }

    @Test
    void when_update_then_throw_not_found() {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);

      assertThrows(EntityNotFoundException.class, () -> registry.update(entity));
    }

    @Test
    void when_delete_then_throw_not_found() {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);

      assertThrows(EntityNotFoundException.class, () -> registry.delete(entity));
    }

    @Test
    void when_read_then_throw_not_found() {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);

      assertThrows(EntityNotFoundException.class, () -> registry.read(entity));
    }

    @Test
    void when_readAll_then_return_empty_list() throws SerializationException {
      given(stub.createCompositeKey(ENTITY_TYPE)).willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getStateByPartialCompositeKey(anyString()))
          .willReturn(
              new QueryResultsIterator<KeyValue>() {
                @Override
                public void close() {}

                @Override
                public @Nonnull Iterator<KeyValue> iterator() {
                  return new Iterator<KeyValue>() {
                    @Override
                    public boolean hasNext() {
                      return false;
                    }

                    @Override
                    public KeyValue next() {
                      throw new UnsupportedOperationException();
                    }
                  };
                }
              });

      List<TestEntity> results = registry.readAll(entity);

      then(stub).should().getStateByPartialCompositeKey(anyString());
      assertTrue(results.isEmpty());
    }
  }

  @Nested
  class given_existing_entity {

    @Test
    void when_create_then_throw_exists() {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getState(ENTITY_COMPOSITE_KEY_STR)).willReturn(ENTITY_BUFFER);

      assertThrows(EntityExistsException.class, () -> registry.create(entity));
    }

    @Test
    void when_update_then_call_putState() throws SerializationException, EntityNotFoundException {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(entity.toBuffer()).willReturn(ENTITY_BUFFER);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getState(ENTITY_COMPOSITE_KEY_STR)).willReturn(ENTITY_BUFFER);

      registry.update(entity);

      then(stub).should().putState(ENTITY_COMPOSITE_KEY_STR, ENTITY_BUFFER);
    }

    @Test
    void when_delete_then_call_delState() throws EntityNotFoundException {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getState(ENTITY_COMPOSITE_KEY_STR)).willReturn(ENTITY_BUFFER);

      registry.delete(entity);

      then(stub).should().delState(ENTITY_COMPOSITE_KEY_STR);
    }

    @Test
    void when_read_then_call_getState_and_return_entity()
        throws SerializationException, EntityNotFoundException {
      given(entity.getKeyParts()).willReturn(ENTITY_KEY_PARTS);
      given(stub.createCompositeKey(ENTITY_TYPE, ENTITY_KEY_PARTS))
          .willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getState(ENTITY_COMPOSITE_KEY_STR)).willReturn(ENTITY_BUFFER);

      TestEntity result = registry.read(entity);

      then(stub).should().getState(ENTITY_COMPOSITE_KEY_STR);
      assertEquals(result, entity);
    }

    @Test
    void when_readAll_then_return_one_long_list() throws SerializationException {
      given(entity.getType()).willReturn(ENTITY_TYPE);
      given(entity.create()).willReturn(entity);
      given(stub.createCompositeKey(ENTITY_TYPE)).willReturn(ENTITY_COMPOSITE_KEY);
      given(stub.getStateByPartialCompositeKey(anyString()))
          .willReturn(
              new QueryResultsIterator<KeyValue>() {
                private boolean done = false;

                @Override
                public void close() {}

                @Override
                public @Nonnull Iterator<KeyValue> iterator() {
                  return new Iterator<KeyValue>() {
                    @Override
                    public boolean hasNext() {
                      return !done;
                    }

                    @Override
                    public KeyValue next() {
                      if (done) {
                        throw new UnsupportedOperationException();
                      }

                      done = true;

                      return new KeyValue() {
                        @Override
                        public String getKey() {
                          return ENTITY_COMPOSITE_KEY_STR;
                        }

                        @Override
                        public byte[] getValue() {
                          return ENTITY_BUFFER;
                        }

                        @Override
                        public String getStringValue() {
                          return Arrays.toString(ENTITY_BUFFER);
                        }
                      };
                    }
                  };
                }
              });

      List<TestEntity> results = registry.readAll(entity);

      then(stub).should().getStateByPartialCompositeKey(anyString());
      assertEquals(1, results.size());
    }
  }
}

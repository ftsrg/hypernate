/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import hu.bme.mit.ftsrg.hypernate.entity.BasicEntity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class IntegrationTest {

  @Mock private ChaincodeStub stub;
  private Registry registry;
  private TestEntity entity;

  @Data
  @EqualsAndHashCode(callSuper = true)
  private static final class TestEntity extends BasicEntity {
    final int id;
  }

  @BeforeEach
  void setup() {
    registry = new Registry(stub);
    entity = new TestEntity(10);
  }

  @Test
  void given_entity_when_create_then_success()
      throws SerializationException, EntityExistsException {
    CompositeKey key = new CompositeKey(entity.getType(), entity.getKeyParts());

    given(stub.createCompositeKey(entity.getType(), entity.getKeyParts())).willReturn(key);

    registry.create(entity);

    then(stub).should().putState(key.toString(), entity.toBuffer());
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import hu.bme.mit.ftsrg.hypernate.entity.Entity;
import hu.bme.mit.ftsrg.hypernate.entity.EntityExistsException;
import hu.bme.mit.ftsrg.hypernate.entity.SerializationException;
import lombok.Data;
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

  @BeforeEach
  void setup() {
    registry = new Registry(stub);
    entity = new TestEntity(10);
  }

  @Test
  void given_entity_when_create_then_success()
      throws SerializationException, EntityExistsException {
    var key = new CompositeKey(entity.getType(), entity.getKeyParts());

    given(stub.createCompositeKey(entity.getType(), entity.getKeyParts())).willReturn(key);

    registry.create(entity);

    then(stub).should().putState(key.toString(), entity.toBuffer());
  }

  @Data
  private static final class TestEntity implements Entity {
    final int id;
  }
}

/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import com.jcabi.aspects.Loggable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import hu.bme.mit.ftsrg.hypernate.middleware.event.ChaincodeEventHandler;
import lombok.experimental.Delegate;
import org.hyperledger.fabric.protos.peer.ChaincodeEvent;
import org.hyperledger.fabric.protos.peer.SignedProposal;
import org.hyperledger.fabric.shim.Chaincode.Response;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.CompositeKey;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.QueryResultsIteratorWithMetadata;

/**
 * {@link ChaincodeStub} middlewares to be chained.
 *
 * <p>Stub middleware classes provide the same interface as {@link ChaincodeStub}, but under the
 * hood they maintain a reference to another {@link ChaincodeStub} and delegate all calls to that.
 * You can override any method in this class to inject your custom behaviour, such as logging,
 * access control, caching, etc.
 */
@Loggable(Loggable.DEBUG)
public abstract class ChaincodeStubMiddleware implements ChaincodeStub, ChaincodeEventHandler {

  public ChaincodeStubMiddleware(final ChaincodeStub nextLayer) {
    this.nextLayer = nextLayer;
  }

  @Delegate(types = ChaincodeStub.class)
  protected final ChaincodeStub nextLayer;
}

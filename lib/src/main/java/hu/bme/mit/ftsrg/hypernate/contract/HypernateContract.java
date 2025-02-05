package hu.bme.mit.ftsrg.hypernate.contract;

import hu.bme.mit.ftsrg.hypernate.context.HypernateContext;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionBegin;
import hu.bme.mit.ftsrg.hypernate.middleware.event.TransactionEnd;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.shim.ChaincodeStub;

public interface HypernateContract extends ContractInterface {

  @Override
  default Context createContext(ChaincodeStub stub) {
    return new HypernateContext(stub);
  }

  default void beforeTransaction(HypernateContext ctx) {
    ctx.fireEvent(new TransactionBegin());
  }

  default void afterTransaction(HypernateContext ctx) {
    ctx.fireEvent(new TransactionEnd());
  }
}

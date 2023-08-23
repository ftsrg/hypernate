/* SPDX-License-Identifier: Apache-2.0 */
package hu.bme.mit.ftsrg.hypernate.middleware;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * Base class for {@link ChaincodeStub} middlewares.
 *
 * <p>This class provides the same interface as {@link ChaincodeStub}, but under the hood it
 * maintains a reference to another {@link ChaincodeStub} and delegates all calls to that. You can
 * override any method in this class to inject your custom behaviour, such as logging, access
 * control, caching, etc.
 */
public abstract class ChaincodeStubMiddlewareBase implements ChaincodeStub {

  public ChaincodeStubMiddlewareBase(final ChaincodeStub nextLayer) {
    this.nextLayer = nextLayer;
  }

  protected final ChaincodeStub nextLayer;

  @Override
  public List<byte[]> getArgs() {
    return this.nextLayer.getArgs();
  }

  @Override
  public List<String> getStringArgs() {
    return this.nextLayer.getStringArgs();
  }

  @Override
  public String getFunction() {
    return this.nextLayer.getFunction();
  }

  @Override
  public List<String> getParameters() {
    return this.nextLayer.getParameters();
  }

  @Override
  public String getTxId() {
    return this.nextLayer.getTxId();
  }

  @Override
  public String getChannelId() {
    return this.nextLayer.getChannelId();
  }

  @Override
  public Response invokeChaincode(
      final String chaincodeName, final List<byte[]> args, final String channel) {
    return this.nextLayer.invokeChaincode(chaincodeName, args, channel);
  }

  @Override
  public byte[] getState(final String key) {
    return this.nextLayer.getState(key);
  }

  @Override
  public byte[] getStateValidationParameter(final String key) {
    return this.nextLayer.getStateValidationParameter(key);
  }

  @Override
  public void putState(final String key, final byte[] value) {
    this.nextLayer.putState(key, value);
  }

  @Override
  public void setStateValidationParameter(final String key, final byte[] value) {
    this.nextLayer.setStateValidationParameter(key, value);
  }

  @Override
  public void delState(final String key) {
    this.nextLayer.delState(key);
  }

  @Override
  public QueryResultsIterator<KeyValue> getStateByRange(
      final String startKey, final String endKey) {
    return this.nextLayer.getStateByRange(startKey, endKey);
  }

  @Override
  public QueryResultsIteratorWithMetadata<KeyValue> getStateByRangeWithPagination(
      final String startKey, final String endKey, final int pageSize, final String bookmark) {
    return this.nextLayer.getStateByRangeWithPagination(startKey, endKey, pageSize, bookmark);
  }

  @Override
  public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(final String compositeKey) {
    return this.nextLayer.getStateByPartialCompositeKey(compositeKey);
  }

  @Override
  public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(
      final String objectType, final String... attributes) {
    return this.nextLayer.getStateByPartialCompositeKey(objectType, attributes);
  }

  @Override
  public QueryResultsIterator<KeyValue> getStateByPartialCompositeKey(
      final CompositeKey compositeKey) {
    return this.nextLayer.getStateByPartialCompositeKey(compositeKey);
  }

  @Override
  public QueryResultsIteratorWithMetadata<KeyValue> getStateByPartialCompositeKeyWithPagination(
      final CompositeKey compositeKey, final int pageSize, final String bookmark) {
    return this.nextLayer.getStateByPartialCompositeKeyWithPagination(
        compositeKey, pageSize, bookmark);
  }

  @Override
  public CompositeKey createCompositeKey(final String objectType, final String... attributes) {
    return this.nextLayer.createCompositeKey(objectType, attributes);
  }

  @Override
  public CompositeKey splitCompositeKey(final String compositeKey) {
    return this.nextLayer.splitCompositeKey(compositeKey);
  }

  @Override
  public QueryResultsIterator<KeyValue> getQueryResult(final String query) {
    return this.nextLayer.getQueryResult(query);
  }

  @Override
  public QueryResultsIteratorWithMetadata<KeyValue> getQueryResultWithPagination(
      final String query, final int pageSize, final String bookmark) {
    return this.nextLayer.getQueryResultWithPagination(query, pageSize, bookmark);
  }

  @Override
  public QueryResultsIterator<KeyModification> getHistoryForKey(final String key) {
    return this.nextLayer.getHistoryForKey(key);
  }

  @Override
  public byte[] getPrivateData(final String collection, final String key) {
    return this.nextLayer.getPrivateData(collection, key);
  }

  @Override
  public byte[] getPrivateDataHash(final String collection, final String key) {
    return this.nextLayer.getPrivateDataHash(collection, key);
  }

  @Override
  public byte[] getPrivateDataValidationParameter(final String collection, final String key) {
    return this.nextLayer.getPrivateDataValidationParameter(collection, key);
  }

  @Override
  public void putPrivateData(final String collection, final String key, final byte[] value) {
    this.nextLayer.putPrivateData(collection, key, value);
  }

  @Override
  public void setPrivateDataValidationParameter(
      final String collection, final String key, final byte[] value) {
    this.nextLayer.setPrivateDataValidationParameter(collection, key, value);
  }

  @Override
  public void delPrivateData(final String collection, final String key) {
    this.nextLayer.delPrivateData(collection, key);
  }

  @Override
  public void purgePrivateData(final String collection, final String key) {
    this.nextLayer.purgePrivateData(collection, key);
  }

  @Override
  public QueryResultsIterator<KeyValue> getPrivateDataByRange(
      final String collection, final String startKey, final String endKey) {
    return this.nextLayer.getPrivateDataByRange(collection, startKey, endKey);
  }

  @Override
  public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(
      final String collection, final String compositeKey) {
    return this.nextLayer.getPrivateDataByPartialCompositeKey(collection, compositeKey);
  }

  @Override
  public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(
      final String collection, final CompositeKey compositeKey) {
    return this.nextLayer.getPrivateDataByPartialCompositeKey(collection, compositeKey);
  }

  @Override
  public QueryResultsIterator<KeyValue> getPrivateDataByPartialCompositeKey(
      final String collection, final String objectType, final String... attributes) {
    return this.nextLayer.getPrivateDataByPartialCompositeKey(collection, objectType, attributes);
  }

  @Override
  public QueryResultsIterator<KeyValue> getPrivateDataQueryResult(
      final String collection, final String query) {
    return this.nextLayer.getPrivateDataQueryResult(collection, query);
  }

  @Override
  public void setEvent(final String name, final byte[] payload) {
    this.nextLayer.setEvent(name, payload);
  }

  @Override
  public ChaincodeEvent getEvent() {
    return this.nextLayer.getEvent();
  }

  @Override
  public SignedProposal getSignedProposal() {
    return this.nextLayer.getSignedProposal();
  }

  @Override
  public Instant getTxTimestamp() {
    return this.nextLayer.getTxTimestamp();
  }

  @Override
  public byte[] getCreator() {
    return this.nextLayer.getCreator();
  }

  @Override
  public Map<String, byte[]> getTransient() {
    return this.nextLayer.getTransient();
  }

  @Override
  public byte[] getBinding() {
    return this.nextLayer.getBinding();
  }

  @Override
  public String getMspId() {
    return this.nextLayer.getMspId();
  }
}

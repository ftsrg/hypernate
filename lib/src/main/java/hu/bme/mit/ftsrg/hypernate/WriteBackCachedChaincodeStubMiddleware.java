/* SPDX-License-Identifier: Apache-2.0 */

package hu.bme.mit.ftsrg.hypernate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub middleware that caches reads in a local state.
 *
 * @see ChaincodeStubMiddlewareBase
 */
public final class WriteBackCachedChaincodeStubMiddleware extends ChaincodeStubMiddlewareBase {

  private static final Logger logger =
      LoggerFactory.getLogger(WriteBackCachedChaincodeStubMiddleware.class);

  private final Map<String, CachedItem> cache = new HashMap<>();

  WriteBackCachedChaincodeStubMiddleware(final ChaincodeStub next) {
    super(next);
  }

  @Override
  public byte[] getState(final String key) {
    CachedItem cached = cache.get(key);

    // New read, add to cache
    if (cached == null) {
      logger.debug("Cache miss for key={} while reading; getting from next layer & caching", key);
      final byte[] value = this.nextLayer.getState(key);
      cached = new CachedItem(key, value);
      cache.put(key, cached);
    }

    // Already marked for deletion
    if (cached.isToDelete()) {
      logger.debug("Value at key={} marked for deletion; returning null", key);
      return null;
    }

    logger.debug("Returning value of cached item at key={}", key);
    return cached.getValue();
  }

  @Override
  public void putState(final String key, final byte[] value) {
    CachedItem cached = cache.get(key);

    // Blind write!
    if (cached == null) {
      logger.debug(
          "Cache miss for key={} while writing; creating new cache entry with null value", key);
      cached = new CachedItem(key, null); // Initial value set later
      cache.put(key, cached);
    }

    if (cached.isToDelete()) {
      logger.error("Entry at key={} already deleted; cannot update", key);
      throw new RuntimeException("Ledger entry " + key + " is already marked for deletion");
    }

    logger.debug(
        "Setting value for cache item with key={} to a {}-long byte array", key, value.length);
    cached.setValue(value); // Sets the dirty flag if needed
  }

  @Override
  public void delState(final String key) {
    CachedItem cached = cache.get(key);

    // Blind delete!
    if (cached == null) {
      logger.debug(
          "Cache miss for key={} while deleting; creating new cache entry with null value", key);
      cached = new CachedItem(key, null);
      cache.put(key, cached);
    }

    logger.debug("Deleting value from cache with key={}", key);
    cached.delete();
  }

  public void dispose() {
    for (final Map.Entry<String, CachedItem> entry : cache.entrySet()) {
      final CachedItem item = entry.getValue();

      if (item == null || !item.isDirty() || item.getValue() == null) continue;

      if (item.isToDelete()) this.nextLayer.delState(item.getKey());
      else this.nextLayer.putState(item.getKey(), item.getValue());
    }
  }

  private static final class CachedItem {

    private final String key;
    private byte[] value;
    private boolean toDelete = false;
    private boolean dirty = false;

    CachedItem(final String key, final byte[] value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return this.key;
    }

    public byte[] getValue() {
      return this.value;
    }

    public void setValue(final byte[] value) {
      if (Arrays.equals(this.value, value)) return;

      this.value = value;
      this.dirty = true;
    }

    public boolean isDirty() {
      return this.dirty;
    }

    public boolean isToDelete() {
      return this.toDelete;
    }

    public void delete() {
      if (!this.toDelete) {
        this.toDelete = true;
        this.dirty = true;
      }
    }

    public boolean hasValue() {
      return this.value != null;
    }
  }
}

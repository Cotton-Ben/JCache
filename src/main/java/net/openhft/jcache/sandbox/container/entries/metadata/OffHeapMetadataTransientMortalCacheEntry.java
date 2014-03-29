package net.openhft.jcache.sandbox.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;
import org.infinispan.offheap.container.entries.OffHeapAbstractInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheValue;
import org.infinispan.offheap.marshall.core.OffHeapIds;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import static java.lang.Math.min;

/**
 *
 * @author Manik Surtani
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataTransientMortalCacheEntry extends OffHeapAbstractInternalCacheEntry implements MetadataAware {

   Object value;
   Metadata metadata;
   long created;
   long lastUsed;

   public OffHeapMetadataTransientMortalCacheEntry(Object key, Object value, Metadata metadata, long now) {
      this(key, value, metadata, now, now);
   }

   public OffHeapMetadataTransientMortalCacheEntry(Object key, Object value, Metadata metadata, long lastUsed, long created) {
      super(key);
      this.value = value;
      this.metadata = metadata;
      this.lastUsed = lastUsed;
      this.created = created;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public long getLifespan() {
      return metadata.lifespan();
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public long getCreated() {
      return created;
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredTransientMortal(
              metadata.maxIdle(), lastUsed, metadata.lifespan(), created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public final long getExpiryTime() {
      long lifespan = metadata.lifespan();
      long lset = lifespan > -1 ? created + lifespan : -1;
      long maxIdle = metadata.maxIdle();
      long muet = maxIdle > -1 ? lastUsed + maxIdle : -1;
      if (lset == -1) return muet;
      if (muet == -1) return lset;
      return min(lset, muet);
   }

   @Override
   public InternalCacheValue toInternalCacheValue() {
      return new OffHeapMetadataTransientMortalCacheValue(value, metadata, created, lastUsed);
   }

   @Override
   public long getLastUsed() {
      return lastUsed;
   }

   @Override
   public final void touch() {
      lastUsed = System.currentTimeMillis();
   }

   @Override
   public final void touch(long currentTimeMillis) {
      lastUsed = currentTimeMillis;
   }

   @Override
   public final void reincarnate() {
      reincarnate(System.currentTimeMillis());
   }

   @Override
   public void reincarnate(long now) {
      created = now;
   }

   @Override
   public long getMaxIdle() {
      return metadata.maxIdle();
   }

   @Override
   public Object setValue(Object value) {
      return this.value = value;
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   public static class Externalizer extends AbstractExternalizer<org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry ice) throws IOException {
         output.writeObject(ice.key);
         output.writeObject(ice.value);
         output.writeObject(ice.metadata);
         UnsignedNumeric.writeUnsignedLong(output, ice.created);
         UnsignedNumeric.writeUnsignedLong(output, ice.lastUsed);
      }

      @Override
      public org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         long lastUsed = UnsignedNumeric.readUnsignedLong(input);
         return new org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry(k, v, metadata, lastUsed, created);
      }

      @Override
      public Integer getId() {
         return OffHeapIds.METADATA_TRANSIENT_MORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry>>asSet(org.infinispan.offheap.container.entries.metadata.OffHeapMetadataTransientMortalCacheEntry.class);
      }
   }
}

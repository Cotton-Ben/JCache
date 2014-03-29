package net.openhft.jcache.sandbox.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.container.entries.InternalCacheValue;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.commons.util.concurrent.OffHeapUtil;
import org.infinispan.offheap.container.entries.OffHeapAbstractInternalCacheEntry;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
  *
 * @author Galder Zamarreño
 * @since 5.3
 *
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataMortalCacheEntry
                                    extends OffHeapAbstractInternalCacheEntry
                                    implements MetadataAware {

   protected Object value;
   protected Metadata metadata;
   protected long created;

   public OffHeapMetadataMortalCacheEntry(
                                        Object key,
                                        Object value,
                                        Metadata metadata,
                                        long created) {
      super(key);
      this.value = value;
      this.metadata = metadata;
      this.created = created;
   }

   @Override
   public Object getValue() {
      return value;
   }

   @Override
   public Object setValue(Object value) {
      return this.value = value;
   }

   @Override
   public final boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredMortal(metadata.lifespan(), created, now);
   }

   @Override
   public final boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   @Override
   public final long getCreated() {
      return created;
   }

   @Override
   public final long getLastUsed() {
      return -1;
   }

   @Override
   public final long getLifespan() {
      return metadata.lifespan();
   }

   @Override
   public final long getMaxIdle() {
      return -1;
   }

   @Override
   public final long getExpiryTime() {
      long lifespan = metadata.lifespan();
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public final void touch() {
      // no-op
   }

   @Override
   public final void touch(long currentTimeMillis) {
      // no-op
   }

   @Override
   public final void reincarnate() {
      reincarnate(System.currentTimeMillis());
   }

   @Override
   public void reincarnate(long now) {
      this.created = now;
   }


   @Override
   public InternalCacheValue toInternalCacheValue() {
      return new OffHeapMetadataMortalCacheValue(value, metadata, created);
   }


   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   public static class Externalizer extends AbstractExternalizer<org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry> {
      @Override
      public void writeObject(
                        ObjectOutput output,
                        org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry ice
                                                            ) throws IOException {
         output.writeObject(ice.key);
         output.writeObject(ice.value);
         output.writeObject(ice.metadata);
         UnsignedNumeric.writeUnsignedLong(output, ice.created);
      }

      @Override
      public org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry readObject(
                                                ObjectInput input
                                                        ) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         return new org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry(k, v, metadata, created);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_MORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry>> getTypeClasses() {
         return OffHeapUtil.<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry>>asSet(
                 org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry.class
         );
      }
   }
}

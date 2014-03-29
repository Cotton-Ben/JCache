package net.openhft.jcache.sandbox.container.entries.metadata;

import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import org.infinispan.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.offheap.container.entries.OffHeapExpiryHelper;
import org.infinispan.offheap.container.entries.OffHeapImmortalCacheValue;
import org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A mortal cache value, to correspond with
 * {@link org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheEntry}
 *
 * @author Galder Zamarreño
 * @since 5.1
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataMortalCacheValue
                                            extends OffHeapImmortalCacheValue
                                            implements MetadataAware {

   Metadata metadata;
   long created;

   public OffHeapMetadataMortalCacheValue(Object value, Metadata metadata, long created) {
      super(value);
      this.metadata = metadata;
      this.created = created;
   }

   @Override
   public InternalCacheEntry toInternalCacheEntry(Object key) {
      return new OffHeapMetadataMortalCacheEntry(key, value, metadata, created);
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }


    @Override
   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   @Override
   public final long getCreated() {
      return created;
   }

   @Override
   public final long getLifespan() {
      return metadata.lifespan();
   }

   @Override
   public boolean isExpired(long now) {
      return OffHeapExpiryHelper.isExpiredMortal(metadata.lifespan(), created, now);
   }

   @Override
   public boolean isExpired() {
      return isExpired(System.currentTimeMillis());
   }

   @Override
   public long getExpiryTime() {
      long lifespan = metadata.lifespan();
      return lifespan > -1 ? created + lifespan : -1;
   }

   @Override
   public final boolean canExpire() {
      return true;
   }

   public static class OffHeapExternalizer extends OffHeapAbstractExternalizer<org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output, org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue mcv) throws IOException {
         output.writeObject(mcv.value);
         output.writeObject(mcv.metadata);
         UnsignedNumeric.writeUnsignedLong(output, mcv.created);
      }

      @Override
      public org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         long created = UnsignedNumeric.readUnsignedLong(input);
         return new org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue(v, metadata, created);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_MORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue>>asSet(org.infinispan.offheap.container.entries.metadata.OffHeapMetadataMortalCacheValue.class);
      }
   }

}

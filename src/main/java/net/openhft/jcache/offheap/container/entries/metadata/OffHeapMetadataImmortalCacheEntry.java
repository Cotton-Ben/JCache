package net.openhft.jcache.offheap.container.entries.metadata;

import net.openhft.jcache.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheEntry;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import net.openhft.jcache.offheap.container.entries.OffHeapImmortalCacheEntry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 *
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
public class OffHeapMetadataImmortalCacheEntry extends OffHeapImmortalCacheEntry implements MetadataAware {

   protected Metadata _metadata;

   public OffHeapMetadataImmortalCacheEntry(Object key, Object value, Metadata metadata) {
      super(key, value);
      this._metadata = metadata;
   }

   @Override
   public Metadata getMetadata() {
      return _metadata;
   }

   @Override
   public void setMetadata( Metadata metadata) {
      this._metadata = metadata;
   }

    /*
   @Override
   public InternalCacheValue toInternalCacheValue() {
      return new MetadataImmortalCacheEntry(getKey(), getValue(), getMetadata());
   }
   */

   @Override
   public String toString() {
      return String.format("MetadataImmortalCacheEntry{key=%s, value=%s, metadata=%s}",
            key,
            value,
            _metadata);
   }

   public static class Externalizer extends OffHeapAbstractExternalizer<MetadataImmortalCacheEntry> {
      @Override
      public void writeObject(ObjectOutput output, MetadataImmortalCacheEntry ice) throws IOException {
         output.writeObject(ice.getKey());
         output.writeObject(ice.getValue());
         output.writeObject(ice.getMetadata());
      }

      @Override
      public MetadataImmortalCacheEntry readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object k = input.readObject();
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         return new MetadataImmortalCacheEntry(k, v, metadata);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_IMMORTAL_ENTRY;
      }

      @Override
      public Set<Class<? extends MetadataImmortalCacheEntry>> getTypeClasses() {
         return Util.<Class<? extends MetadataImmortalCacheEntry>>asSet(MetadataImmortalCacheEntry.class);
      }
   }
}

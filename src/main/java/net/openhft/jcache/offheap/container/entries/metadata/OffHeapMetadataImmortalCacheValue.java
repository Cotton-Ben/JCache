package net.openhft.jcache.offheap.container.entries.metadata;

import net.openhft.jcache.offheap.commons.marshall.OffHeapAbstractExternalizer;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.commons.util.Util;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataAware;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheEntry;
import org.infinispan.container.entries.metadata.MetadataImmortalCacheValue;
import org.infinispan.marshall.core.Ids;
import org.infinispan.metadata.Metadata;
import net.openhft.jcache.offheap.container.entries.OffHeapImmortalCacheValue;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

/**
 * A form of
 * {@link net.openhft.jcache.offheap.container.entries.OffHeapImmortalCacheValue} that
 * is {@link org.infinispan.container.entries.metadata.MetadataAware}
 *
 * @author Galder Zamarreño
 * @since 5.3
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 *
 */
public class OffHeapMetadataImmortalCacheValue extends OffHeapImmortalCacheValue implements MetadataAware {

   Metadata metadata;

   public OffHeapMetadataImmortalCacheValue(Object value, Metadata metadata) {
      super(value);
      this.metadata = metadata;
   }

   @Override
   public InternalCacheEntry toInternalCacheEntry(Object key) {
      return new MetadataImmortalCacheEntry(key, value, metadata);
   }

   @Override
   public Metadata getMetadata() {
      return metadata;
   }

   @Override
   public void setMetadata(Metadata _metadata) {
      this.metadata = _metadata;
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " {" +
            "value=" + value +
            ", metadata=" + metadata +
            '}';
   }

   public static class Externalizer extends OffHeapAbstractExternalizer<ImmortalCacheValue> {
      @Override
      public void writeObject(ObjectOutput output,ImmortalCacheValue icv) throws IOException {
         output.writeObject(icv.value);
         output.writeObject(icv.getMetadata());
      }

      @Override
      public ImmortalCacheValue readObject(ObjectInput input) throws IOException, ClassNotFoundException {
         Object v = input.readObject();
         Metadata metadata = (Metadata) input.readObject();
         return new ImmortalCacheValue(v);
      }

      @Override
      public Integer getId() {
         return Ids.METADATA_IMMORTAL_VALUE;
      }

      @Override
      public Set<Class<? extends ImmortalCacheValue>> getTypeClasses() {
         return Util.<Class<? extends ImmortalCacheValue>>asSet(MetadataImmortalCacheValue.class);
      }
   }

}

package net.openhft.jcache.container.versioning;

import net.jcip.annotations.Immutable;
import org.infinispan.commons.marshall.AbstractExternalizer;
import org.infinispan.container.versioning.EntryVersion;
import org.infinispan.container.versioning.IncrementableEntryVersion;
import org.infinispan.container.versioning.InequalVersionComparisonResult;
import org.infinispan.marshall.core.Ids;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

/**
 * A simple versioning scheme that is cluster-aware
 *
 * @author Manik Surtani
 * @since 5.1
 */
@Immutable
public class OffHeapSimpleClusteredVersion implements IncrementableEntryVersion {

   /**
    * The cache topology id in which it was first created.
    */
   private final int topologyId;

   final long version;

   public OffHeapSimpleClusteredVersion(int topologyId, long version) {
      this.version = version;
      this.topologyId = topologyId;
   }

   @Override
   public InequalVersionComparisonResult compareTo(EntryVersion other) {
      if (other instanceof org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion) {
         org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion otherVersion = (org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion) other;

         if (topologyId > otherVersion.topologyId)
            return InequalVersionComparisonResult.AFTER;
         if (topologyId < otherVersion.topologyId)
            return InequalVersionComparisonResult.BEFORE;

         if (version > otherVersion.version)
            return InequalVersionComparisonResult.AFTER;
         if (version < otherVersion.version)
            return InequalVersionComparisonResult.BEFORE;

         return InequalVersionComparisonResult.EQUAL;
      } else {
         throw new IllegalArgumentException("I only know how to deal with SimpleClusteredVersions, not " + other.getClass().getName());
      }
   }

   @Override
   public String toString() {
      return "SimpleClusteredVersion{" +
            "topologyId=" + topologyId +
            ", version=" + version +
            '}';
   }

   public static class Externalizer extends AbstractExternalizer<org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion> {

      @Override
      public void writeObject(ObjectOutput output, org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion ch) throws IOException {
         output.writeInt(ch.topologyId);
         output.writeLong(ch.version);
      }

      @Override
      @SuppressWarnings("unchecked")
      public org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion readObject(ObjectInput unmarshaller) throws IOException, ClassNotFoundException {
         int topologyId = unmarshaller.readInt();
         long version = unmarshaller.readLong();
         return new org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion(topologyId, version);
      }

      @Override
      public Integer getId() {
         return Ids.SIMPLE_CLUSTERED_VERSION;
      }

      @Override
      public Set<Class<? extends org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion>> getTypeClasses() {
         return Collections.<Class<? extends org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion>>singleton(org.infinispan.offheap.container.versioning.OffHeapSimpleClusteredVersion.class);
      }
   }
}

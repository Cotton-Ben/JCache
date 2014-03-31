package net.openhft.jcache.offheap.container;


import net.openhft.collections.SharedHashMap;
import net.openhft.collections.SharedHashMapBuilder;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;
//import net.openhft.jcache.commons.util.concurrent.ParallelIterableMap;
import org.infinispan.container.DataContainer;
import org.infinispan.container.InternalEntryFactory;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.eviction.ActivationManager;
import org.infinispan.eviction.EvictionManager;
import org.infinispan.eviction.PassivationManager;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.metadata.Metadata;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.spi.AdvancedCacheLoader;
import org.infinispan.util.CoreImmutables;
import org.infinispan.util.TimeService;
import org.infinispan.util.concurrent.BoundedConcurrentHashMap.EvictionListener;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.util.*;

/**
 * OffHeap OffHeapDefaultDataContainer is both eviction and non-eviction based data container.
 *
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author peter.lawrey@higherfrequencytrading.com
 */
@ThreadSafe
public class OffHeapDefaultDataContainer<K,V> implements DataContainer {

   private static final Log log = LogFactory.getLog(OffHeapDefaultDataContainer.class);
   private static final boolean trace = log.isTraceEnabled();

   protected SharedHashMap<Object, InternalCacheEntry> entries;
   protected InternalEntryFactory entryFactory;
   protected DefaultEvictionListener evictionListener;
   private EvictionManager evictionManager;
   private PassivationManager passivator;
   private ActivationManager activator;
   private PersistenceManager pm;
   private TimeService timeService;

   public OffHeapDefaultDataContainer(
           Class<K> keyType,
           Class<V> valueType,
           String openHFT_OffHeap_Operand,
           int entrysSize,
           int segmentsSize) {
       try {
           long t = System.currentTimeMillis();
           SharedHashMap<Object, InternalCacheEntry> _entries =
                   (SharedHashMap<Object, InternalCacheEntry>)  new SharedHashMapBuilder()
                           .generatedValueType(Boolean.TRUE)
                           .entrySize(entrysSize)
                           .minSegments(segmentsSize)
                           .create(
                                   new File("/dev/shm/" + openHFT_OffHeap_Operand + ".@t=" + t),
                                   keyType,
                                   valueType
                           );
           this.entries = _entries;
           System.out.println("OpenHFT /dev/shm/"+openHFT_OffHeap_Operand+".@t="+t+"  entries=["+
                   (
                   (entries!=null) ? entries.toString() : "NULL"
                    ) +
                   "]");
       } catch (Exception e) {
           e.printStackTrace();
       }

        //entries = CollectionFactory.makeConcurrentParallelMap(128, concurrencyLevel);
        evictionListener = null;
      }





    public OffHeapDefaultDataContainer(int concurrencyLevel) {
    }

   @Inject
   public void initialize(
                                EvictionManager evictionManager,
                                PassivationManager passivator,
                                InternalEntryFactory entryFactory,
                                ActivationManager activator,
                                PersistenceManager clm,
                                TimeService timeService
                            ) {
        System.out.println("RedHat Infinispan join point:  initialize ....");
        this.evictionManager = evictionManager;
        this.passivator = passivator;
        this.entryFactory = entryFactory;
        this.activator = activator;
        this.pm = clm;
        this.timeService = timeService;
   }


//   public static OffHeapDataContainer boundedDataContainer(int concurrencyLevel, int maxEntries,
//            EvictionStrategy strategy, EvictionThreadPolicy policy,
//            Equivalence keyEquivalence, Equivalence valueEquivalence) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel, maxEntries, strategy,
//            policy, keyEquivalence, valueEquivalence);
//   }
//
//   public static OffHeapDataContainer unBoundedDataContainer(int concurrencyLevel,
//         Equivalence keyEquivalence, Equivalence valueEquivalence) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel, keyEquivalence, valueEquivalence);
//   }

//   public static OffHeapDataContainer unBoundedDataContainer(int concurrencyLevel) {
//      return new OffHeapDefaultDataContainer(concurrencyLevel);
//   }

   @Override
   public InternalCacheEntry peek(Object key) {
      return entries.get(key);
   }



    @Override
   public InternalCacheEntry get(Object k) {
      InternalCacheEntry e = peek(k);
      if (e != null && e.canExpire()) {
         long currentTimeMillis = timeService.wallClockTime();
         if (e.isExpired(currentTimeMillis)) {
            entries.remove(k);
            e = null;
         } else {
            e.touch(currentTimeMillis);
         }
      }
      return e;
   }

    @Override
    public void put(Object k, Object v, Metadata metadata) {
        System.out.println("OHDDC.put(k="+k+",v="+v+",metaData="+metadata+");");
        System.out.println("RedHat ICE OHDDC.entries=["+entries+"]");
        InternalCacheEntry e = this.entries.get(k);
        System.out.println("RedHat InternalCacheEntry entries.get(k)=[" +
                (e != null ? e.toString():"NULL") +
                "]");
        if (e != null) {
            e.setValue(v);
            InternalCacheEntry original = e;
            e = entryFactory.update(e, metadata);
            // we have the same instance. So we need to reincarnate, if mortal.
            if (isMortalEntry(e) && original == e) {
                e.reincarnate(timeService.wallClockTime());
            }
        } else {
            System.out.println("RedHat EntryFactory eF.create(k,v,metadata); eF=[" +
                    (entryFactory != null ? entryFactory.toString():"NULL") +
                    "]");
            // this is a brand-new entry
            e = this.entryFactory.create(k, v,  metadata);
        }

        if (trace)
            log.tracef("Store %s in container", e);

        entries.put(k, e);
    }


    private boolean isMortalEntry(InternalCacheEntry e) {
      return e.getLifespan() > 0;
   }

   @Override
   public boolean containsKey(Object k) {
      InternalCacheEntry ice = peek(k);
      if (ice != null && ice.canExpire() && ice.isExpired(timeService.wallClockTime())) {
         entries.remove(k);
         ice = null;
      }
      return ice != null;
   }

   @Override
   public InternalCacheEntry remove(Object k) {
      InternalCacheEntry e = entries.remove(k);
      return e == null || (e.canExpire() && e.isExpired(timeService.wallClockTime())) ? null : e;
   }

   @Override
   public int size() {
      return entries.size();
   }

   @Override
   public void clear() {
      entries.clear();
   }

   @Override
   public Set<Object> keySet() {
      return Collections.unmodifiableSet(entries.keySet());
   }

   @Override
   public Collection<Object> values() {
      return new Values();
   }

   @Override
   public Set<InternalCacheEntry> entrySet() {
      return new EntrySet();
   }

   @Override
   public void purgeExpired() {
      long currentTimeMillis = timeService.wallClockTime();
      for (Iterator<InternalCacheEntry> purgeCandidates = entries.values().iterator(); purgeCandidates.hasNext();) {
         InternalCacheEntry e = purgeCandidates.next();
         if (e.isExpired(currentTimeMillis)) {
            purgeCandidates.remove();
         }
      }
   }



   @Override
   public Iterator iterator() {

      return new EntryIterator( entries.values().iterator() );
   }

   private final class DefaultEvictionListener implements EvictionListener<Object, InternalCacheEntry> {

      @Override
      public void onEntryEviction(Map<Object, InternalCacheEntry> evicted) {
         evictionManager.onEntryEviction(evicted);
      }

      @Override
      public void onEntryChosenForEviction(InternalCacheEntry entry) {
         passivator.passivate(entry);
      }

      @Override
      public void onEntryActivated(Object key) {
         activator.activate(key);
      }

      @Override
      public void onEntryRemoved(Object key) {
         if (pm != null)
            pm.deleteFromAllStores(key, false);
      }
   }

   private static class ImmutableEntryIterator extends EntryIterator {
      ImmutableEntryIterator(Iterator<InternalCacheEntry> it){
         super(it);
      }

      @Override
      public InternalCacheEntry next() {
         return CoreImmutables.immutableInternalCacheEntry(super.next());
   }
   }

   public static class EntryIterator implements Iterator<InternalCacheEntry> {

      private final Iterator<InternalCacheEntry> it;

      EntryIterator(Iterator<InternalCacheEntry> it){this.it=it;}

      @Override
      public InternalCacheEntry next() {
         return it.next();
      }

      @Override
      public boolean hasNext() {
         return it.hasNext();
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   /**
    * Minimal implementation needed for unmodifiable Set
    *
    */
   private class EntrySet extends AbstractSet<InternalCacheEntry> {

      @Override
      public boolean contains(Object o) {
         if (!(o instanceof Map.Entry)) {
            return false;
         }

         @SuppressWarnings("rawtypes")
         Map.Entry e = (Map.Entry) o;
         InternalCacheEntry ice = entries.get(e.getKey());
         if (ice == null) {
            return false;
         }
         return ice.getValue().equals(e.getValue());
      }

      @Override
      public Iterator<InternalCacheEntry> iterator() {
         return new ImmutableEntryIterator(entries.values().iterator());
      }

      @Override
      public int size() {
         return entries.size();
      }

      @Override
      public String toString() {
         return entries.toString();
      }
   }

   /**
    * Minimal implementation needed for unmodifiable Collection
    *
    */
   private class Values extends AbstractCollection<Object> {
      @Override
      public Iterator<Object> iterator() {
         return new ValueIterator(entries.values().iterator());
      }

      @Override
      public int size() {
         return entries.size();
      }
   }

   private static class ValueIterator implements Iterator<Object> {
      Iterator<InternalCacheEntry> currentIterator;

      private ValueIterator(Iterator<InternalCacheEntry> it) {
         currentIterator = it;
      }

      @Override
      public boolean hasNext() {
         return currentIterator.hasNext();
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Object next() {
         return currentIterator.next().getValue();
      }
   }



 //   @Override
//   public <K> void executeTask(
//                                 final AdvancedCacheLoader.KeyFilter<K> filter,
//                                 final IterableMap.KeyValueAction<Object, InternalCacheEntry> action
//                                ) throws InterruptedException  {
//      if (filter == null)
//         throw new IllegalArgumentException("No filter specified");
//      if (action == null)
//         throw new IllegalArgumentException("No action specified");
//
//      ParallelIterableMap<Object, InternalCacheEntry> map =
//              (ParallelIterableMap<Object, InternalCacheEntry>) entries;
//
//       map.forEach(512, new ParallelIterableMap.KeyValueAction<Object, InternalCacheEntry>() {
//
//           @Override
//           public void apply(Object o, InternalCacheEntry internalCacheEntry) {
//
//           }
//
//
////         @Override
////         public void apply(Object key, OffHeapInternalCacheEntry value) {
////            if (filter.shouldLoadKey((K)key)) {
////               action.apply((K)key, value);
////            }
////         }
//      });
//      //TODO figure out the way how to do interruption better (during iteration)
//      if(Thread.currentThread().isInterrupted()){
//         throw new InterruptedException();
//      }
//   }

//
//    public void initialize(Object o, Object o1, OffHeapInternalEntryFactoryImpl internalEntryFactory, Object o2, Object o3, TimeService timeService) {
//        System.out.println("initialize");
//    }
}

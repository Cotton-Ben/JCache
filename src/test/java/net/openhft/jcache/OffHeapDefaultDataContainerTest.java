package net.openhft.jcache;


import net.openhft.collections.SharedHashMap;
import net.openhft.collections.SharedHashMapBuilder;
import net.openhft.lang.model.DataValueClasses;
import org.infinispan.container.DataContainer;
import org.infinispan.container.entries.InternalCacheEntry;
import net.openhft.jcache.offheap.container.OffHeapDefaultDataContainer;
import net.openhft.jcache.offheap.container.OffHeapInternalEntryFactoryImpl;
import net.openhft.jcache.offheap.container.entries.*;
import net.openhft.jcache.offheap.metadata.OffHeapEmbeddedMetadata;
import net.openhft.jcache.offheap.util.OffHeapCoreImmutables;
import org.infinispan.test.AbstractInfinispanTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;


/**
 * @author ben.cotton@jpmorgan.com
 * @author dmitry.gordeev@jpmorgan.com
 * @author xiaoming.wang@jpmorgan.com
 *
 * modeled from RedHat's original SimpleDataContainerTest.java
 */


@Test(groups = "unit", testName = "jcache.OffHeapDataContainerTest")
public class OffHeapDefaultDataContainerTest extends AbstractInfinispanTest {
	DataContainer jcacheDataContainer;
    SharedHashMap<String, BondVOInterface> shm;


	@BeforeMethod
	public void setUp() throws InterruptedException, IOException {

		Thread.sleep(2000);
		System.out.println("ISPN7 JCACHE DataContainer view of OpenHFT SHM is being created");
		this.jcacheDataContainer = createJcacheContainer();
        this.shm = new SharedHashMapBuilder()
                .generatedValueType(true)
                .entrySize(512)
                .create(
                        new File("/dev/shm/openHFT_SHM"),
                        String.class,
                        BondVOInterface.class
                );
		Thread.sleep(2000);
		System.out.println("ISPN7 JCACHE DataContainer created jcacheDataContainer=["+jcacheDataContainer.toString()+"]");
		Thread.sleep(2000);
	}

	@AfterMethod
	public void tearDown() {
		this.jcacheDataContainer = null;
        this.shm = null;
	}



	protected DataContainer createJcacheContainer() {
		DataContainer ohjcacheDataContainer =new OffHeapDefaultDataContainer<String, BondVOInterface>(
				String.class,
				BondVOInterface.class,
				"net.openhft.jcache.OffHeapDefaultDataContainerTest",
				512,
				256
		);
		OffHeapInternalEntryFactoryImpl internalEntryFactory = new OffHeapInternalEntryFactoryImpl();
		internalEntryFactory.injectTimeService(TIME_SERVICE);
		((OffHeapDefaultDataContainer)ohjcacheDataContainer).initialize(null, null, internalEntryFactory, null, null, TIME_SERVICE);
		return ohjcacheDataContainer;
	}


	public void testOpenHFTasOffHeapJcacheOperandProvider() throws InterruptedException {
		//TODO: build a join to OpenHFT MetaData - this comes in OpenHFT 3.0d

        BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%


		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.maxIdle(100,TimeUnit.MINUTES)
						.build()
		);
		Thread.sleep(2000);
		System.out.println("ISPN7 JCACHE put() the BondVOInterface (\"369604103\") into DataContainer bondV=["+bondV+"]");

		Thread.sleep(2000);
		System.out.println("Using ISPN7 JCACHE to get(\"369604103\") BondVOInterface <--  DataContainer (bondV=["+bondV+"])");

		Thread.sleep(2000);
		InternalCacheEntry bondEntry = this.jcacheDataContainer.get("369604103");

		Thread.sleep(2000);
		System.out.println("ISPN7 JCACHE got the (\"369604103\") BondVOInterface from  DataContainer (entry.getSymbol()=["+
				((BondVOInterface) bondEntry).getSymbol() +
				"])");

		Thread.sleep(2000);
		assert bondEntry.getClass().equals(transienttype());
		assert bondEntry.getLastUsed() <= System.currentTimeMillis();
		long entryLastUsed = bondEntry.getLastUsed();
		Thread.sleep(2000);
		bondEntry = this.jcacheDataContainer.get("369604103");
		assert bondEntry.getLastUsed() > entryLastUsed;
		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.maxIdle(0, TimeUnit.MINUTES)
						.build()
		);
		this.jcacheDataContainer.purgeExpired();

		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.lifespan(100, TimeUnit.MINUTES)
						.build()
		);
		Thread.sleep(2000);
		assert this.jcacheDataContainer.size() == 1;

		bondEntry= jcacheDataContainer.get("369604103");
		assert bondEntry != null : "Entry should not be null!";
		assert bondEntry.getClass().equals(mortaltype()) : "Expected "+mortaltype()+", was " + bondEntry.getClass().getSimpleName();
		assert bondEntry.getCreated() <= System.currentTimeMillis();

		this.jcacheDataContainer.put("369604103", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(0, TimeUnit.MINUTES).build());
		Thread.sleep(10);
		assert this.jcacheDataContainer.get("k") == null;
		assert this.jcacheDataContainer.size() == 0;

		this.jcacheDataContainer.put("369604103", "v", new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(0, TimeUnit.MINUTES).build());
		Thread.sleep(100);
		assert this.jcacheDataContainer.size() == 1;
		this.jcacheDataContainer.purgeExpired();
		assert this.jcacheDataContainer.size() == 0;

//        //now some straight-up JCACHE bridge crossing from OpenHFT
//        ConfigurationBuilder jCacheConfig  = new ConfigurationBuilder();
//        jCacheConfig.dataContainer().dataContainer( this.jcacheDataContainer);
	}


	public void testResetOfCreationTime() throws Exception {
		long now = System.currentTimeMillis();
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%
		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.lifespan(1000, TimeUnit.SECONDS)
						.build())
		;
		long created1 = this.jcacheDataContainer.get("k").getCreated();
		assert created1 >= now;
		Thread.sleep(100);
		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.lifespan(1000, TimeUnit.SECONDS)
						.build()
		);
		long created2 = this.jcacheDataContainer.get("k").getCreated();
		assert created2 > created1 : "Expected " + created2 + " to be greater than " + created1;
	}


	public void testUpdatingLastUsed() throws Exception {
		long idle = 600000;
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%
		this.jcacheDataContainer.put(
                "369604103",
				bondV,
				new OffHeapEmbeddedMetadata.OffHeapBuilder()
						.build()
		);
		InternalCacheEntry ice = this.jcacheDataContainer.get("369604103");
		assert ice.getClass().equals(immortaltype());
		assert ice.toInternalCacheValue().getExpiryTime() == -1;
		assert ice.getMaxIdle() == -1;
		assert ice.getLifespan() == -1;
		this.jcacheDataContainer.put("369604103", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(idle, TimeUnit.MILLISECONDS).build());
		long oldTime = System.currentTimeMillis();
		Thread.sleep(100); // for time calc granularity
		ice =this.jcacheDataContainer.get("369604103");
		assert ice.getClass().equals(transienttype());
		assert ice.toInternalCacheValue().getExpiryTime() > -1;
		assert ice.getLastUsed() > oldTime;
		Thread.sleep(100); // for time calc granularity
		assert ice.getLastUsed() < System.currentTimeMillis();
		assert ice.getMaxIdle() == idle;
		assert ice.getLifespan() == -1;

		oldTime = System.currentTimeMillis();
		Thread.sleep(100); // for time calc granularity
		assert this.jcacheDataContainer.get("369604103") != null;

		// check that the last used stamp has been updated on a get
		assert ice.getLastUsed() > oldTime;
		Thread.sleep(100); // for time calc granularity
		assert ice.getLastUsed() < System.currentTimeMillis();
	}

	protected Class<? extends InternalCacheEntry> mortaltype() {
		return OffHeapMortalCacheEntry.class;
	}

	protected Class<? extends InternalCacheEntry> immortaltype() {
		return OffHeapImmortalCacheEntry.class;
	}

	protected Class<? extends InternalCacheEntry> transienttype() {
		return OffHeapTransientCacheEntry.class;
	}

	protected Class<? extends InternalCacheEntry> transientmortaltype() {
		return OffHeapTransientMortalCacheEntry.class;
	}


	public void testExpirableToImmortalAndBack() {
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%

		String value = "v";
		this.jcacheDataContainer.put("369604103", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
		assertContainerEntry(this.mortaltype(), value);

		value = "v2";
		this.jcacheDataContainer.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
		assertContainerEntry(this.immortaltype(), value);

		value = "v3";
		this.jcacheDataContainer.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
		assertContainerEntry(this.transienttype(), value);

		value = "v4";
		this.jcacheDataContainer.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder()
				.lifespan(100, TimeUnit.MINUTES).maxIdle(100, TimeUnit.MINUTES).build());
		assertContainerEntry(this.transientmortaltype(), value);

		value = "v41";
		this.jcacheDataContainer.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder()
				.lifespan(100, TimeUnit.MINUTES).maxIdle(100, TimeUnit.MINUTES).build());
		assertContainerEntry(this.transientmortaltype(), value);

		value = "v5";
		this.jcacheDataContainer.put("k", value, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
		assertContainerEntry(this.mortaltype(), value);
	}


	private void assertContainerEntry(
			Class<? extends InternalCacheEntry> type,
			String expectedValue
	) {
		assert this.jcacheDataContainer.containsKey("369604103");
		InternalCacheEntry entry = this.jcacheDataContainer.get("369604103");
		assertEquals(type, entry.getClass());
		assertEquals(expectedValue, entry.getValue());
	}



	public void testKeySet() {

		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%

		this.jcacheDataContainer.put(
                "369603001",
				bondV,
				new OffHeapEmbeddedMetadata.OffHeapBuilder()
						.lifespan(100, TimeUnit.MINUTES)
						.build()
		);
		this.jcacheDataContainer.put("369603002", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
		this.jcacheDataContainer.put(
                "369603002",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.maxIdle(100, TimeUnit.MINUTES)
						.build()
		);
		this.jcacheDataContainer.put(
                "369603003",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.maxIdle(100, TimeUnit.MINUTES)
						.lifespan(100, TimeUnit.MINUTES)
						.build()
		);

		Set<String> expected = new HashSet<String>();
		expected.add("369603001");
		expected.add("369603002");
		expected.add("369603003");
		expected.add("369603004");

		for (Object o : this.jcacheDataContainer.keySet()) {
			assert expected.remove(o);
		}

		assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
	}


	public void testContainerIteration() {

		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%


		System.out.println("bondV.getSymbol=["+bondV.getSymbol()+"]");
		jcacheDataContainer.put(
				"CUSIP1234",
				bondV,
				new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build()
		);
		jcacheDataContainer.put(
				"CUSIP4321",
				bondV,
				new OffHeapEmbeddedMetadata.OffHeapBuilder().build()
		);
		jcacheDataContainer.put(
				"1234CUSIP",
				bondV,
				new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build()
		);
		jcacheDataContainer.put(
				"4321CUSIP",
				bondV,
				new OffHeapEmbeddedMetadata
						.OffHeapBuilder()
						.maxIdle(100, TimeUnit.MINUTES)
						.lifespan(100, TimeUnit.MINUTES)
						.build());

		Set<String> expected = new HashSet<String>();
		expected.add("369603001");
		expected.add("369603002");
		expected.add("369603003");
		expected.add("369603004");

		for (InternalCacheEntry ice : jcacheDataContainer) {
			assert expected.remove(ice.getKey());
		}

		assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
	}


	public void testKeys() {
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%

		jcacheDataContainer.put("369603001", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603002", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
		jcacheDataContainer.put("369603003", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603004", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder()
				.maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

		Set<String> expected = new HashSet<String>();
		expected.add("369603001");
		expected.add("369603002");
		expected.add("369603003");
		expected.add("369603004");

		for (Object o : jcacheDataContainer.keySet()) assert expected.remove(o);

		assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
	}


	public void testValues() {
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%


		jcacheDataContainer.put("369603001", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603002", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
		jcacheDataContainer.put("369603003", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603004", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder()
				.maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

		Set<String> expected = new HashSet<String>();
        expected.add("369603001");
        expected.add("369603002");
        expected.add("369603003");
        expected.add("369603004");

		for (Object o : jcacheDataContainer.values()) assert expected.remove(o);

		assert expected.isEmpty() : "Did not see keys " + expected + " in iterator!";
	}


	public void testEntrySet() {
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%

		jcacheDataContainer.put("369603001", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().lifespan(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603002", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());
		jcacheDataContainer.put("369603003", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().maxIdle(100, TimeUnit.MINUTES).build());
		jcacheDataContainer.put("369603004", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder()
				.maxIdle(100, TimeUnit.MINUTES).lifespan(100, TimeUnit.MINUTES).build());

		Set<InternalCacheEntry> expected = new HashSet<InternalCacheEntry>();
		expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(jcacheDataContainer.get("369603001")));
		expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(jcacheDataContainer.get("369603002")));
		expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(jcacheDataContainer.get("369603003")));
		expected.add(OffHeapCoreImmutables.immutableInternalCacheEntry(jcacheDataContainer.get("369603004")));

		Set<Map.Entry<Object,Object>> actual = new HashSet<Map.Entry<Object, Object>>();
		for (Map.Entry<Object, Object> o : jcacheDataContainer.entrySet()) actual.add(o);

		assert actual.equals(expected) : "Expected to see keys " + expected + " but only saw " + actual;
	}


	public void testGetDuringKeySetLoop() {
		BondVOInterface bondV = DataValueClasses.newDirectReference(BondVOInterface.class);
        this.shm.acquireUsing("369604103", bondV);
		bondV.setSymbol("IBM_HY_30_YR_5.5");
		bondV.setIssueDate(20140315); //beware the ides of March
		bondV.setMaturityDate(20440315); //30 years
		bondV.setCoupon(0.055); //5.5%

		for (int i = 0; i < 10; i++) jcacheDataContainer.put(i+"", bondV, new OffHeapEmbeddedMetadata.OffHeapBuilder().build());

		int i = 0;
		for (Object key : jcacheDataContainer.keySet()) {
			jcacheDataContainer.peek(key); // calling get in this situations will result on corruption the iteration.
			i++;
		}

		assert i == 10 : "Expected the loop to run 10 times, only ran " + i;
	}
}

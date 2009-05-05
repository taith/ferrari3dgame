package com.dennisbijlsma.ferrari3d.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dennisbijlsma.ferrari3d.util.LapTime;

/**
 * Tests the classes {@code Session} and {@code LapTime}. This is essentially a
 * unit test, but I don't want to add JUnit as a dependency just for this class
 * so I'm using a poor man's custom version instead.
 */

public class TestSession {

	public static void main(String[] args) {
		TestSession test = new TestSession();
		test.testLapTime();
		test.testRunningLapTime();
		test.testCompareLapTimes();
		test.testSortLapTimes();
		System.out.println("OK");
	}
	
	public void testLapTime() {
		
		LapTime time1 = new LapTime();
		assertEquals(LapTime.TIME_NOT_SET, time1.getTime());
		assertEquals(LapTime.SECTOR_NOT_SET, time1.getSectorTime(0));
		
		LapTime time2 = new LapTime(60000);
		assertEquals(60000, time2.getTime());
		assertEquals(LapTime.SECTOR_NOT_SET, time2.getSectorTime(0));
		
		LapTime time3 = new LapTime(10000, 20000, 30000);
		assertEquals(60000, time3.getTime());
		assertEquals(10000, time3.getSectorTime(0));
		assertEquals(20000, time3.getSectorTime(1));
		assertEquals(30000, time3.getSectorTime(2));
	}
	
	public void testRunningLapTime() {
		LapTime time = new LapTime();
		time.setSectorTime(0, 10000, true);
		assertEquals(10000, time.getSectorTime(0));
		assertEquals(10000, time.getTime());
		time.setSectorTime(0, 15000, true);
		assertEquals(15000, time.getSectorTime(0));
		assertEquals(15000, time.getTime());
		time.setSectorTime(1, 20000, true);
		assertEquals(20000, time.getSectorTime(1));
		assertEquals(35000, time.getTime());
	}
	
	public void testCompareLapTimes() {
		LapTime time1 = new LapTime(1000);
		LapTime time2 = new LapTime(2000);
		assertEquals(-1, time1.compareTo(time2));
		assertEquals(1, time2.compareTo(time1));
		assertEquals(0, time1.compareTo(time1));
	}
	
	public void testSortLapTimes() {
		LapTime time1 = new LapTime(1000);
		LapTime time2 = new LapTime(2000);
		LapTime time3 = new LapTime(3000);
		List<LapTime> times = Arrays.asList(time1, time2, time3);
		Collections.sort(times);
		assertEquals(time1, times.get(0));
		assertEquals(time2, times.get(1));
		assertEquals(time3, times.get(2));
	}
	
	private void assertEquals(Object a, Object b) {
		if (!a.equals(b)) {
			throw new AssertionError();
		}
	}
	
	private void assertEquals(int a, int b) {
		if (a != b) {
			throw new AssertionError();
		}
	}
	
	private void assertTrue(boolean b) {
		if (!b) {
			throw new AssertionError();
		}
	}
}
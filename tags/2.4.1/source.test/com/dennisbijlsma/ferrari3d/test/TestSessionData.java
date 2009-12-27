//-----------------------------------------------------------------------------
// Ferrari3D
// TestSessionData
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.TrackRecord;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the various data classes (Session, Contestant, LapTime, 
 * TrackRecord).
 */
public class TestSessionData {

	@Test
	public void testSessionLocking() {
		
		Session session = new Session(Session.SessionMode.TIME, 3);
		assertFalse(session.isStarted());
		assertFalse(session.isFinished());
		
		session.setStarted();
		assertTrue(session.isStarted());
		assertFalse(session.isFinished());
		
		session.setFinished();
		assertTrue(session.isFinished());
	}
	
	@Test
	public void testLapTime() {
		
		Laptime time1 = new Laptime();
		assertEquals(Laptime.TIME_NOT_SET, time1.getTime());
		assertEquals(Laptime.SECTOR_NOT_SET, time1.getSectorTime(0));
		
		Laptime time2 = new Laptime(60000);
		assertEquals(60000, time2.getTime());
		assertEquals(Laptime.SECTOR_NOT_SET, time2.getSectorTime(0));
		
		Laptime time3 = new Laptime(10000, 20000, 30000);
		assertEquals(60000, time3.getTime());
		assertEquals(10000, time3.getSectorTime(0));
		assertEquals(20000, time3.getSectorTime(1));
		assertEquals(30000, time3.getSectorTime(2));
	}
	
	@Test
	public void testRunningLapTime() {
		
		Laptime time = new Laptime();
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
	
	@Test
	public void testCompareLapTimes() {
		Laptime time1 = new Laptime(1000);
		Laptime time2 = new Laptime(2000);
		assertEquals(-1, time1.compareTo(time2));
		assertEquals(1, time2.compareTo(time1));
		assertEquals(0, time1.compareTo(time1));
	}
	
	@Test
	public void testSortLapTimes() {
		Laptime time1 = new Laptime(1000);
		Laptime time2 = new Laptime(2000);
		Laptime time3 = new Laptime(3000);
		List<Laptime> times = Arrays.asList(time1, time2, time3);
		Collections.sort(times);
		assertEquals(time1, times.get(0));
		assertEquals(time2, times.get(1));
		assertEquals(time3, times.get(2));
	}
	
	@Test
	public void testTrackRecord() {
		TrackRecord record1 = new TrackRecord("driver", "car", "circuit", 
				new Laptime(100, 100, 100), "1.0.0", "01-01-2009");
		TrackRecord record2 = new TrackRecord("driver", "car", "circuit", 
				new Laptime(200, 200, 200), "1.0.0", "01-01-2009");
		assertEquals(-1, record1.compareTo(record2));
	}
}

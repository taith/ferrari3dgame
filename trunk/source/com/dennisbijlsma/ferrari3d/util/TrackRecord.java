//-----------------------------------------------------------------------------
// Ferrari3D
// TrackRecord
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import nl.colorize.util.Version;

/**
 * Record of a laptime. Along with the laptime itself this class also stores
 * information about when and how the time was set. 
 */

public final class TrackRecord implements Comparable<TrackRecord> {
	
	private String driverName;
	private String carName;
	private String circuitName;
	private Laptime laptime;
	private Version version;
	private String date;
	
	public TrackRecord(String driver, String car, String circuit, Laptime laptime,
			Version version, String date) {
		this.driverName = driver;
		this.carName = car;
		this.circuitName = circuit;
		this.laptime = laptime;
		this.version = version;
		this.date = date;
	}
	
	public TrackRecord(String driver, String car, String circuit, Laptime laptime,
			String version, String date) {
		this(driver, car, circuit, laptime, new Version(version), date);
	}
	
	public String getDriverName() { 
		return driverName; 
	}
	
	public String getCarName() { 
		return carName; 
	}
	
	public String getCircuitName() { 
		return circuitName; 
	}
	
	public Laptime getTime() { 
		return laptime; 
	}
	
	public Version getVersion() { 
		return version; 
	}
	
	public String getDate() { 
		return date; 
	}
	
	public int compareTo(TrackRecord other) {
		return laptime.compareTo(other.getTime());
	}
}

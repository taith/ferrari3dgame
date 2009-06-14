//--------------------------------------------------------------------------------
// Ferrari3D
// TrackRecord
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

/**
 * Record of a laptime. Along with the laptime itself this class also stores
 * information about when and how the time was set. Instances of this class are
 * immutable.
 */

public final class TrackRecord implements Comparable<TrackRecord> {
	
	private String driverName;
	private String carName;
	private String circuitName;
	private LapTime laptime;
	private String version;
	private String date;
	
	/**
	 * Creates a new track record with the specified fields.
	 */
	
	public TrackRecord(String driver, String car, String circuit, LapTime laptime,
			String version, String date) {

		this.driverName = driver;
		this.carName = car;
		this.circuitName = circuit;
		this.laptime = laptime;
		this.version = version;
		this.date = date;
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
	
	public LapTime getTime() { 
		return laptime; 
	}
	
	public String getVersion() { 
		return version; 
	}
	
	public String getDate() { 
		return date; 
	}
	
	/**
	 * Compares a track record with another based on their lap time.
	 */
	
	public int compareTo(TrackRecord other) {
		return laptime.compareTo(other.getTime());
	}
	
	/**
	 * Returns a string representation of this track record.
	 */
	
	@Override
	public String toString() {
		return "TrackRecord (driver=" + driverName + ")"; 
	}
}
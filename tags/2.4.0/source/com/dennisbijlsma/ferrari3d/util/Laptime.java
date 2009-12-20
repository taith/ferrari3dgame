//-----------------------------------------------------------------------------
// Ferrari3D
// Laptime
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

/**
 * Stores information about a lap that has been driven. Each lap consists of three
 * sectors, the combined times form the lap time. Note that in some cases the
 * sector times may not be available and only the total time can be obtained. Lap
 * times are comparable, so that they can easily be sorted with the fastest time
 * on top.
 */

public final class Laptime implements Comparable<Laptime> {
	
	private int time;
	private int[] sectors;
	
	public static final int TIME_NOT_SET = 300000;
	public static final int SECTOR_NOT_SET = 0;
	
	/**
	 * Creates a new lap time with a total time of TIME_NOT_SET. The sector times
	 * will be not set.
	 */
	
	public Laptime() {
		time = TIME_NOT_SET;
		sectors = new int[3];
	}
	
	/**
	 * Creates a new lap time with the specified total time. The sector times will
	 * be not set.
	 */
	
	public Laptime(int time) {
		this();
		setTime(time);
	}
	
	/**
	 * Creates a new lap time with the specified sector times. The total time
	 * will be equal to the sum of the sector times.
	 */
	
	public Laptime(int sector1, int sector2, int sector3) {
		this();
		setSectorTime(0, sector1, false);
		setSectorTime(1, sector2, false);
		setSectorTime(2, sector3, false);
		setTime(sector1 + sector2 + sector3);
	}
	
	/**
	 * Sets the total time of this lap as a number of milliseconds.
	 */
	
	public void setTime(int time) {
		this.time = time;
	}
	
	/**
	 * Returns the total time of this lap as a number of milliseconds. If the 
	 * total time is not set TIME_NOT_SET is returned.
	 */
	
	public int getTime() {
		return time;
	}
	
	/**
	 * Sets the sector time for the specified sector.
	 * @param recalculate Sets the total time to the sum of the sector times.
	 * @throws ArrayIndexOutOfBoundsException if the sector is not 0 - 2.
	 */
	
	public void setSectorTime(int sector, int sectorTime, boolean recalculate) {
		sectors[sector] = sectorTime;
		if (recalculate) {
			time = sectors[0] + sectors[1] + sectors[2];
		}
	}
	
	/**
	 * Returns the sector time for the specified sector. If the sector time is
	 * not set SECTOR_NOT_SET is returned.
	 * @throws IndexOutOfBoundsException if the sector is not 0 - 2.
	 */
	
	public int getSectorTime(int sector) {
		return sectors[sector];
	}
	
	/**
	 * Returns the combined sector times up to and including the specified sector.
	 */
	
	public int getIntermediateTime(int intermediate) {
		
		int intermediateTime = 0;
		for (int i = 0; i <= intermediate; i++) {
			intermediateTime += sectors[i];
		}
		
		return intermediateTime;
	}
	
	/**
	 * Compares this lap time with another one base on their total time. This 
	 * method returns -1 if this lap is faster than the other lap, 1 if it is
	 * slower, or 0 if the two lap times are equal.
	 */

	public int compareTo(Laptime other) {
		if (time < other.time) { return -1; }
		if (time > other.time) { return 1; }
		return 0;
	}
	
	/**
	 * Returns a string representation of this lap time.
	 */
	
	@Override
	public String toString() {
		return "LapTime(time=" + time + ")";
	}
}

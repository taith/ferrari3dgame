//--------------------------------------------------------------------------------
// Ferrari3D
// Laptime
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

/**
 * Stores a laptime. The lap time contains of three separate sectors, which when
 * the times are combined form the total time. All times are stored as integers
 * with millisecond precision.
 */

public class Laptime {

	private int[] sectors;
	private boolean complete;
	
	public static final int SECTOR_1=0;
	public static final int SECTOR_2=1;
	public static final int SECTOR_3=2;
	public static final int TIME_NOT_SET=300000;
	
	/**
	 * Creates a new <code>Laptime</code> object. Initially all sectors will be
	 * set to the default value.
	 */
	
	public Laptime() {
		
		sectors=new int[3];
		complete=false;
		
		for (int i=0; i<sectors.length; i++) {
			sectors[i]=TIME_NOT_SET;
		}
	}
	
	/**
	 * Creates a new <code>Laptime</code> object. All sector times will be set to
	 * the corresponding values.
	 */
	
	public Laptime(int sector1,int sector2,int sector3) {
	
		this();
		
		setSector(SECTOR_1,sector1,false);
		setSector(SECTOR_2,sector2,false);
		setSector(SECTOR_3,sector3,false);		
		setComplete(true);
	}
	
	/**
	 * Creates a new <code>Laptime</code> object. Only the total time of the lap
	 * will be set. Use this constructor only when the separate sector times are
	 * unknown.
	 */
	
	public Laptime(int laptime) {
	
		this(0,0,laptime);
	}
	
	/**
	 * Sets the specified sector time.
	 * @param sector One of the constant fields.
	 * @param value The time in milliseconds.
	 * @param increment When true, adds the value to the already existing.
	 */
	
	public void setSector(int sector,int value,boolean increment) {
		
		if ((!increment) || (sectors[sector]==TIME_NOT_SET)) {
			sectors[sector]=value;
		} else {	
			sectors[sector]+=value;
		}
	}
	
	/**
	 * Returns the sector time with the specified index. The index should be one
	 * of the <code>SECTOR</code> fields.
	 */
	
	public int getSector(int sector) {

		return sectors[sector];
	}
	
	/**
	 * Sets the laptime by setting all three sector times at once.
	 */
	
	public void setLaptime(int sector1,int sector2,int sector3) {
		
		sectors[0]=sector1;
		sectors[1]=sector2;
		sectors[2]=sector3;
	}
	
	/**
	 * Sets the laptime by setting only the total time. Use this method only when
	 * the separate sector times are unknown.
	 */
	
	public void setLaptime(int value) {
	
		setLaptime(0,0,value);
	}
	
	/**
	 * Returns the total laptime in millisecond precision. Note that this method
	 * will return a default value unless the lap was marked as completed.
	 */
	
	public int getLaptime() {
	
		if (getComplete()) {
			return sectors[0]+sectors[1]+sectors[2];
		} else {
			return TIME_NOT_SET;
		}
	}
	
	/**
	 * Returns the 'current' time for this lap. This is done by adding all sector
	 * times together. This method is the same as <code>getLaptime()</code>, but
	 * it can also be used when the lap is not yet completed.
	 */
	
	public int getCurrentTime() {
	
		int t=0;
		for (int i=0; i<sectors.length; i++) {
			if (sectors[i]!=TIME_NOT_SET) {
				t+=sectors[i];
			}
		}
		
		return t;
	}
	
	/**
	 * Returns the total laptime up to the specified intermediate.
	 */
	
	public int getIntermediateTime(int intermediate) {
		
		int intTime=0;
		
		for (int i=0; i<=intermediate; i++) {
			if (sectors[i]!=TIME_NOT_SET) {
				intTime+=sectors[i];
			}
		}
		
		return intTime;
	}
	
	/**
	 * Returns the 'current' intermediate. This means that the intermediate which 
	 * does not have a time yet will be returned.
	 * @return One of the <code>SECTOR</code> fields.
	 */
	
	public int getCurrentIntermediate() {
	
		if (sectors[SECTOR_3]!=TIME_NOT_SET) { return -1; }
		if (sectors[SECTOR_2]!=TIME_NOT_SET) { return SECTOR_3; }
		if (sectors[SECTOR_1]!=TIME_NOT_SET) { return SECTOR_2; }
		return SECTOR_1;
	}
	
	/**
	 * Sets if this lap is completed. When completed, the method <code>getLaptime()
	 * </code> can be used.
	 */
	
	public void setComplete(boolean c) {
	
		complete=c;
	}
	
	/**
	 * Returns if this lap is completed. When completed, the method <code>
	 * getLaptime()</code> can be used.
	 */
	
	public boolean getComplete() {
	
		return complete;
	}
}
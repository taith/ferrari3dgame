//--------------------------------------------------------------------------------
// Ferrari3D
// Laptime
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;



public class Laptime {

	private int[] sectors;
	private boolean complete;
	
	public static final int SECTOR_1=0;
	public static final int SECTOR_2=1;
	public static final int SECTOR_3=2;
	public static final int TIME_NOT_SET=300000;
	
	
	
	public Laptime() {
		
		sectors=new int[3];
		complete=false;
		
		for (int i=0; i<sectors.length; i++) {
			sectors[i]=TIME_NOT_SET;
		}
	}
	
	
	
	public Laptime(int sector1,int sector2,int sector3) {
	
		this();
		
		setSector(SECTOR_1,sector1,false);
		setSector(SECTOR_2,sector2,false);
		setSector(SECTOR_3,sector3,false);		
		setComplete(true);
	}
	
	
	
	public Laptime(int laptime) {
	
		this(0,0,laptime);
	}
	
	
	
	public void setSector(int sector,int value,boolean increment) {
		
		if ((!increment) || (sectors[sector]==TIME_NOT_SET)) {
			sectors[sector]=value;
		} else {	
			sectors[sector]+=value;
		}
	}
	
	
	
	public int getSector(int sector) {

		return sectors[sector];
	}
	
	
	
	public void setLaptime(int sector1,int sector2,int sector3) {
		
		sectors[0]=sector1;
		sectors[1]=sector2;
		sectors[2]=sector3;
	}
	
	
	
	public void setLaptime(int value) {
	
		setLaptime(0,0,value);
	}
	
	
	
	public int getLaptime() {
	
		if (getComplete()) {
			return sectors[0]+sectors[1]+sectors[2];
		} else {
			return TIME_NOT_SET;
		}
	}
	
	
	
	public int getCurrentTime() {
	
		int t=0;
		for (int i=0; i<sectors.length; i++) {
			if (sectors[i]!=TIME_NOT_SET) {
				t+=sectors[i];
			}
		}
		
		return t;
	}
	
	
	
	public int getIntermediateTime(int intermediate) {
		
		int intTime=0;
		
		for (int i=0; i<=intermediate; i++) {
			if (sectors[i]!=TIME_NOT_SET) {
				intTime+=sectors[i];
			}
		}
		
		return intTime;
	}
	
	
	
	public int getCurrentIntermediate() {
	
		if (sectors[SECTOR_3]!=TIME_NOT_SET) { return -1; }
		if (sectors[SECTOR_2]!=TIME_NOT_SET) { return SECTOR_3; }
		if (sectors[SECTOR_1]!=TIME_NOT_SET) { return SECTOR_2; }
		return SECTOR_1;
	}
	
	
	
	public void setComplete(boolean c) {
	
		complete=c;
	}
	
	
	
	public boolean getComplete() {
	
		return complete;
	}
	
	
	
	public boolean isSet() {
	
		return getLaptime()!=TIME_NOT_SET;
	}
}
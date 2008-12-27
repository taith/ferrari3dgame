//--------------------------------------------------------------------------------
// Ferrari3D
// Session
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.HashSet;
import java.util.Set;

/**
 * The <code>Session</code> stores information about all contestants for a game.
 * It can be used to look up contestants, or to add or remove them from the 
 * current game. This class also contains methods for calculating the current
 * race positions.
 */

public class Session {
	
	private SessionMode mode;
	private boolean started;
	private boolean finished;
	
	private Set<Contestant> contestants;
	
	public static enum SessionMode { 
		TIME,
		RACE 
	}

	/**
	 * Creates a new session with the specified mode. Initially no contestants
	 * will be added to the session.
	 * @param mode One of the <code>SessionMode</code> fields.
	 */
	
	public Session(SessionMode mode) {
		
		this.mode=mode;
		
		started=false;
		finished=false;
		
		contestants=new HashSet<Contestant>();
	}
	
	/**
	 * Returns which mode was set for the session during construction. This value
	 * is one of the <code>SessionMode</code> fields.
	 */
	
	public SessionMode getMode() {
		
		return mode;
	}
	
	/**
	 * Sets that this session has started. This method can only be called once,
	 * after that the started field becomes immutable.
	 */
	
	public void setStarted() {
	
		started=true;
		finished=false;
	}
	
	/**
	 * Returns if the session has been started. This method will initially return
	 * false.
	 */
	
	public boolean isStarted() {
	
		return started;
	}
	
	/**
	 * Sets that this session has finished. This method can only be called once,
	 * after that the finished fields becomes immutable.
	 */
	
	public void setFinished() {
		
		started=true;
		finished=true;
	}
	
	/**
	 * Returns if the session has been finished. This method will initially return
	 * false.
	 */
	
	public boolean isFinished() {
	
		return finished;
	}
	
	/**
	 * Adds the specified <code>Contestant</code> to the session. When it was 
	 * already added earlier this method does nothing.
	 */
	
	public void addContestant(Contestant contestant) {
		
		contestants.add(contestant);
	}
	
	/**
	 * Returns the set of all added contestants. The order may not reflect the 
	 * order in which the contestants were added.
	 */
	
	public Set<Contestant> getContestantsSet() {
	
		return contestants;
	}
		
	/**
	 * Returns the number of contestants in this session. This value will be the
	 * same as the length of the result of <code>getContestants()</code>.
	 */
	
	public int getNumContestants() {
	
		return contestants.size();
	}
	
	/**
	 * Returns the race position for the specified contestant <code>c</code>. The
	 * actual workings of this method depend on the session mode. The returned 
	 * position will be somewhere between 1 and the number of contestants. It is
	 * impossible that two contestants will return the same position.
	 */
	
	public int getRacePosition(Contestant c) {
		
		int pos=1;
				
		for (Contestant i : contestants) {
			if (c==i) {
				continue;
			}
			
			if (mode==SessionMode.TIME) {
				int cBestTime=c.getBestLaptime().getLaptime();
				int iBestTime=i.getBestLaptime().getLaptime();	
				
				if (cBestTime>iBestTime) {
					pos++;
				} else {
					if ((cBestTime==iBestTime) && (c.getID().hashCode()<i.getID().hashCode())) {
						pos++;
					}
				}
			}	
			
			if (mode==SessionMode.RACE) {
				int cDistance=c.getLap()*10000+c.getIntermediate()*1000+c.getPoint();
				int iDistance=i.getLap()*10000+i.getIntermediate()*1000+i.getPoint();
				
				if (cDistance<iDistance) {
					pos++;
				} else {
					if ((cDistance==iDistance) && (c.getID().hashCode()<i.getID().hashCode())) {
						pos++;
					}
				}
			}
		}
		
		return pos;
	}
	
	/**
	 * Returns the contestant for the specified race position. This method is the
	 * inverse of <code>getRacePosition(Contestant)</code>. When no contestant 
	 * exists on the specified position this method returns <code>null</code>.
	 */
	
	public Contestant getRacePositionID(int pos) {
		
		if ((pos<1) || (pos>getNumContestants())) {
			return null;
		}
			
		for (Contestant i : contestants) {
			if (getRacePosition(i)==pos) {
				return i;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the contestant that currently holds the fastest lap. The lap itself
	 * can then be obtained using <code>Contestant.getFastestLaptime()</code>.
	 */
	
	public Contestant getFastestLap() {
		
		Contestant fastest=null;
		for (Contestant i : contestants) {
			if ((fastest==null) || (i.getBestLaptime().getLaptime()<fastest.getBestLaptime().getLaptime())) {
				fastest=i;
			}
		}
		
		return fastest;
	}
}
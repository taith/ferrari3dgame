//--------------------------------------------------------------------------------
// Ferrari3D
// Session
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.*;

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
	
	private HashMap<String,Contestant> contestants;
	private Player player;
	
	public static enum SessionMode { TIME,RACE }

	/**
	 * Creates a new session with the specified mode. Initially no contestants
	 * will be added to the session.
	 * @param mode One of the <code>SessionMode</code> fields.
	 */
	
	public Session(SessionMode mode) {
		
		this.mode=mode;
		
		started=false;
		finished=false;
		
		contestants=new HashMap<String,Contestant>();
		player=null;
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
		
		if (contestants.containsValue(contestant)) {
			return;
		}
		
		contestants.put(contestant.getID(),contestant);
		
		if (contestant instanceof Player) {
			player=(Player) contestant;
		}
	}
		
	/**
	 * Returns the contestant with the specified ID. When no contestant exists
	 * with this ID this method will return <code>null</code>.
	 */
	
	public Contestant getContestant(String id) {
		
		return contestants.get(id);
	}
	
	/**
	 * Returns all contestants in this session as an array. The order of elements
	 * in the array will be random.
	 */
	
	public Contestant[] getContestants() {
	
		return contestants.values().toArray(new Contestant[0]);
	}
		
	/**
	 * Returns the number of contestants in this session. This value will be the
	 * same as the length of the array of <code>getContestants()</code>.
	 */
	
	public int getNumContestants() {
	
		return contestants.size();
	}
	
	/**
	 * Returns the human player for this session. When a human player as not (yet)
	 * been added this method will return <code>null</code>.
	 */
	
	public Player getPlayer() {
	
		return player;
	}
	
	/**
	 * Returns the race position for the specified contestant <code>c</code>. The
	 * actual workings of this method depend on the session mode. The returned 
	 * position will be somewhere between 1 and the number of contestants. It is
	 * impossible that two contestants will return the same position.
	 */
	
	public int getRacePosition(Contestant c) {
		
		int pos=1;
				
		for (Contestant i : getContestants()) {
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
			
		for (Contestant i : getContestants()) {
			if (getRacePosition(i)==pos) {
				return i;
			}
		}
		
		return null;
	}
}
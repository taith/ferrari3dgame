//-----------------------------------------------------------------------------
// Ferrari3D
// Session
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code Session} stores a set of contestants that participate against each
 * other. The rules of which contestant is considered the winner are dependant on
 * the session mode. In time mode the contestant with the fastest lap time wins,
 * in race mode the contestant who first completes a number of laps wins.
 */
public class Session {
	
	private SessionMode mode;
	private int laps;
	private boolean started;
	private boolean finished;
	private Set<Contestant> contestants;
	
	public enum SessionMode { 
		TIME,
		RACE 
	}

	/**
	 * Creates a new session with the specified mode. Initially no contestants
	 * will be added to the session.
	 */
	public Session(SessionMode mode, int laps) {
		this.mode = mode;
		this.laps = laps;
		started = false;
		finished = false;
		contestants = new HashSet<Contestant>();
	}
	
	public SessionMode getMode() {
		return mode;
	}
	
	@Deprecated
	public void setLaps(int laps) {
		this.laps = laps;
	}
	
	public int getLaps() {
		return laps;
	}
	
	public void setStarted() {
		started = true;
		finished = false;
		// Make the set of contestants immutable, it is too expensive to make a
		// defensive copy every time someone wishes to iterate all contestants.
		contestants = Collections.unmodifiableSet(contestants);
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public void setFinished() {
		started = true;
		finished = true;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Adds the specified contestant to the session. Once added, there is no way
	 * to remove the contestant from the session.
	 * @throws IllegalStateException if the session was already started.
	 */
	public void addContestant(Contestant contestant) {
		
		if (started) {
			throw new IllegalStateException("Session already started");
		}
		
		if (!contestants.contains(contestant)) {
			contestants.add(contestant);
		}
	}
	
	public Set<Contestant> getContestants() {
		return contestants;
	}
	
	public int getNumContestants() {
		return contestants.size();
	}
	
	/**
	 * Returns a {@code Comparator} for comparing contestants. The returned
	 * object depends on the session mode.
	 */
	private Comparator<Contestant> getContestantComparator() {
		switch (mode) {
			case TIME : return new BestLapComparator();
			case RACE : return new RaceComparator();
			default : throw new AssertionError();
		}
	}
	
	/**
	 * Returns a list of all contestants, sorted by race position. How the 
	 * contestants are compared depends on the session mode.
	 * <p>
	 * Note that the list indexes are between 0 and getNumContestants() - 1, in
	 * order to get the 'proper' position the list index must be incremented with 1.
	 */
	protected List<Contestant> getRacePositions() {
		List<Contestant> positions = new ArrayList<Contestant>();
		positions.addAll(contestants);
		Collections.sort(positions, getContestantComparator());
		return positions;
	}
	
	/**
	 * Returns the race position of the specified contestant. How the contestants
	 * in the session are compared depends on the session mode.
	 * @return The race position, between 1 and the number of contestants.
	 * @throws IllegalArgumentException if the contestant is not in this session.
	 */
	public int getRacePosition(Contestant c) {
		
		List<Contestant> positions = getRacePositions();
		for (int i = 0; i < positions.size(); i++) {
			if (positions.get(i) == c) {
				// Race position is list index plus one
				return i + 1;
			}
		}
		
		throw new IllegalArgumentException("Contestant is not in session: " + c);
	}
	
	/**
	 * Returns the contestant at the specified race position.
	 * @return The contestant at the specified position, or {@code null} when none.
	 */
	public Contestant getContestantAtRacePosition(int pos) {	
		if ((pos < 1) || (pos > getNumContestants())) {
			return null;
		}
		// List index is race position minus one
		List<Contestant> positions = getRacePositions();
		return positions.get(pos - 1);
	}
	
	/**
	 * Returns the contestant that holds the fastest lap in the session. The time
	 * itself can be obtained with {@code getFastestLap().getBestLaptime()}.
	 */
	public Contestant getFastestLap() {
		Contestant fastest = getContestantAtRacePosition(1);
		for (Contestant i : contestants) {
			if (i.getFastestLaptime().compareTo(fastest.getFastestLaptime()) == -1) {
				fastest = i;
			}
		}
		return fastest;
	}
	
	/**
	 * Comparator that compares contestants based on their fastest lap time. If 
	 * the fastest laps are equal the name of the contestants are compared.
	 */
	private static class BestLapComparator implements Comparator<Contestant> {

		public int compare(Contestant a, Contestant b) {
			int compared = a.getFastestLaptime().compareTo(b.getFastestLaptime());
			return (compared != 0) ? compared : a.getName().compareTo(b.getName());
		}
	}
	
	/**
	 * Comparator that compares contestants based on their lap, intermediate and 
	 * circuit point. If all are equal the names of the contestants are compared.
	 */
	private static class RaceComparator implements Comparator<Contestant> {
		
		public int compare(Contestant a, Contestant b) {
			int distanceA = getDistance(a);
			int distanceB = getDistance(b);
			if (distanceA > distanceB) { return -1; }
			if (distanceA < distanceB) { return 1; }
			return a.getName().compareTo(b.getName());
		}
		
		private int getDistance(Contestant c) {
			return c.getLap() * 10000 + c.getIntermediate() * 1000 + c.getPoint();
		}
	}
}

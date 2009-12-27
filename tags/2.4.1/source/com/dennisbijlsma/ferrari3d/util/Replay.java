//-----------------------------------------------------------------------------
// Ferrari3D
// Replay
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

/**
 * Records positional data for a car over a period of time. An instance of this
 * class should be created for every contestant in the session. 
 */
public class Replay {

	private float time;
	private int pointer;
	private int used;
	private float[][] positions;
	
	private static final float FRAME_RATE = 25f;
	private static final float FRAME_TIME = 1f / FRAME_RATE;
	private static final float MAX_TIME = 20f;
	private static final int MAX_FRAMES = 20 * 25;
	
	/**
	 * Creates a new replay.
	 */
	public Replay() {
		time = 0f;
		pointer = 0;
		used = 0;
		positions = new float[MAX_FRAMES][5];
	}
	
	/**
	 * Puts the specified position data in this replay. This method can be called
	 * as often as desired, but the data may only be stored if enough time has 
	 * elapsed since the last put.
	 * @param dt Delta time since the last frame, in seconds.
	 * @return True if the data was stored, false otherwise.
	 */
	public boolean put(float dt, float x, float y, float z, float direction, float orientation) {
		
		time += dt;
		if (time < FRAME_TIME) {
			return false;
		}
		
		positions[pointer][0] = x;
		positions[pointer][1] = y;
		positions[pointer][2] = z;
		positions[pointer][3] = direction;
		positions[pointer][4] = orientation;
		
		used = Math.max(used, pointer); 
		pointer = (pointer + 1) % MAX_FRAMES;
		time = 0f;
		return true;
	}
	
	/**
	 * Returns positional data for the specified time in the past. Multiple 
	 * invocations of this method may return the same data if the times are too
	 * close together.
	 * @param t Time in the past, in seconds. For example -3.5 or -1.7.
	 * @return An array with x, y, z, direction and orientation.
	 * @throws IllegalArgumentException if the time is too far in the past.
	 */
	public float[] get(float t) {
	
		if ((t < -MAX_TIME) || (t >= 0)) {
			throw new IllegalArgumentException("Invalid time value: " + t);
		}
		
		int tOffset = Math.round(t * FRAME_RATE);
		int index = pointer + tOffset;
		if (index < 0) {
			index += MAX_FRAMES;
		}
		if (index > used) {
			index = used;
		}

		return positions[index];
	}
	
	public float getReplayStartTime() {
		if (used >= MAX_FRAMES - 1) {
			return -MAX_TIME;
		} else {
			return -used / FRAME_RATE;
		}
	}
	
	public float getReplayEndTime() {
		return 0f;
	}
}

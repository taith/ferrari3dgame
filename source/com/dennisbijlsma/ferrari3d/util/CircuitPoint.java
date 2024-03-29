//-----------------------------------------------------------------------------
// Ferrari3D
// CircuitPoint
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

/**
 * Stores information about a point on the circuit. This point is used to 
 * determine position within the lap, but also by the AI to find its way and to 
 * know where to overtake.
 */
public class CircuitPoint {

	public float pointX;
	public float pointY;
	public float altX;
	public float altY;
	private float speed;
	private boolean intermediate;
	
	/**
	 * Creates a new {@code CircuitPoint} with the specified values. This 
	 * constructor should be used when loading, not setting all the different
	 * values independantly.
	 */
	public CircuitPoint(float pointX, float pointY, float altX, float altY,
			float speed, boolean intermediate) {
		this.pointX = pointX;
		this.pointY = pointY;
		this.altX = altX;
		this.altY = altY;
		this.speed = speed;
		this.intermediate = intermediate;
	}
	
	/**
	 * Creates a new {@code CircuitPoint} with default values.
	 */
	public CircuitPoint() {
		this(0f, 0f, 0f, 0f, 0f, false);
	}
	
	public void setIntermediate(boolean intermediate) {
		this.intermediate = intermediate;
	}
	
	public boolean isIntermediate() {
		return intermediate;
	}
	
	/**
	 * Returns true if this point has alternative coordinates. These coordinates
	 * indicate a second driving line that can be used for overtaking. When this
	 * method returns false, the altX and altY for this point should be ignored.
	 */
	public boolean isAltPoint() {
		return ((Math.abs(altX) > 0.1f) && (Math.abs(altY) > 0.1f));
	}
	
	/**
	 * Returns true if this point has AI speed attached. When no AI speed is 
	 * available for this point, future points should be referenced in order to
	 * know the correct speed. 
	 */
	public boolean isSuggestedSpeed() {
		return (Math.abs(speed) > 0.1f);
	}
	
	public void setSuggestedSpeed(float speed) {
		this.speed = speed;
	}
	
	public float getSuggestedSpeed() {
		return speed;
	}
}

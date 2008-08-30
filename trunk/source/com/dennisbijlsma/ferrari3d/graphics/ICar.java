//--------------------------------------------------------------------------------
// Ferrari3D
// ICar
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.*;

/**
 * Interface for cars. This mainly exists for testing purposes, to link non-3D
 * graphics to the <code>Contestant</code> class. Normally the default 
 * implementation of <code>Car</code> should be used.
 */

public interface ICar {
	
	/**
	 * Returns the transformation matrix for this car. This can be used to change
	 * the position and rotation of the car.
	 */
	
	public Matrix3D getTransform();
	
	/**
	 * Returns true if the car is currently driving on the track. 
	 */
	
	public boolean isOnTrack();
	
	/**
	 * Returns true when the car is currently positioned on the floor.
	 */
	
	public boolean isOnFloor();
	
	/**
	 * Returns if an object exists between the car's current position and the
	 * specified new position.
	 */
	
	public boolean isCollidingObject(float newX,float newY,float newZ,float newRot);
	
	/**
	 * Returns if the car will collide with the specified other car. The collision
	 * is checked for between the car's current position and the specified new one.
	 */
	
	public boolean isCollidingCar(float newX,float newY,float newZ,float newRot,ICar otherCar);
	
	/**
	 * Returns the car's name.
	 */
	
	public String getCarName();

	/**
	 * Returns the car's meta data with the specified key. When no such meta data
	 * is available, this method will return <code>null</code>.
	 */
	
	public String getInfo(String key);
}
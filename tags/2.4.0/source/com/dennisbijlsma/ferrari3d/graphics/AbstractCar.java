//-----------------------------------------------------------------------------
// Ferrari3D
// AbstractCar
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.util.Map;

import com.dennisbijlsma.core3d.ImmutableVector3D;

/**
 * Defines the non-visual aspects of a car. This includes methods for obtaining
 * information about the car, and checks how the car interacts with the circuit
 * and other cars.
 * <p>
 * This class exists because of the strong dependency of {@code Contestant} on
 * having a car. Without this class it would be impossible to have a non-visual
 * way of testing. 
 */

public abstract class AbstractCar {
	
	private String name;
	private Map<String,String> info;
	private ImmutableVector3D[] cameras;
	
	/**
	 * Default constructor.
	 */
	
	public AbstractCar() {
		
	}
	
	public void setCarName(String name) {
		this.name = name;
	}
	
	public String getCarName() {
		return name;
	}
	
	public void setInfo(Map<String,String> info) {
		this.info = info;
	}
	
	public String getInfo(String key) {
		return info.get(key);
	}
	
	public void setCameras(ImmutableVector3D[] cameras) {
		this.cameras = cameras;
	}
	
	public ImmutableVector3D getCamera(int index) {
		return cameras[index];
	}

	public boolean isOnTrack() {
		return true;
	}
	
	public boolean isOnFloor() {
		return true;
	}
	
	public boolean isCollidingObject(float newX, float newY, float newZ) {
		return false;
	}
	
	public boolean isCollidingCar(float newX, float newY, float newZ, AbstractCar otherCar) {
		return false;
	}
	
	public abstract void setPosition(float x, float z);
	
	public abstract void setOrientation(float angle);
	
	public abstract void doGearChange(boolean up);
	
	public abstract void doNextLap();
}

//-----------------------------------------------------------------------------
// Ferrari3D
// CarPhysics
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;

/**
 * Controls the physics for a car. The {@link #updatePhysics(float)} method should
 * be called every frame in order to produce accurate results.
 * <p>
 * Although this class contains setters for nearly all internal fields, these are
 * not meant to be called as a part of normal control flow. Instead, they should
 * be used for external influences on the car, such as setting the initial position
 * or setting values that arrived from a network message.
 */
public class CarPhysics {
	
	// Controls
	private float accelerator;
	private float brakes;
	private float steering;
	private int gearChange;
	
	// Movement
	private Vector3D position;
	private float speed;
	private boolean onTrack;
	
	// Rotation
	private float direction;
	private float orientation;
	private float angularSpeed;
	
	// Gearbox
	private int gear;
	private int rpm;
	
	// Car constants
	protected float CAR_ENGINE_BHP = 850f;
	protected float CAR_MASS = 600f;
	protected float CAR_TRACTION = 10f;
	protected float CAR_BRAKES = 1200f;
	protected float CAR_DRAG = 0.7f;
	protected float CAR_ROLL = 28f;
	protected float CAR_STEERING = 0.1f;
	protected float CAR_STIFFNESS = 10f;
	protected float CAR_RESISTANCE = 0.00065f;
	protected float CAR_GRIP = 2f; //TODO
	protected int CAR_GEARS = 7;
	protected int CAR_MIN_RPM = 15000;
	protected int CAR_MAX_RPM = 19000;
	protected int CAR_NEUTRAL_RPM = 4500;
	protected int CAR_GEAR_RATIO = 10;
	protected int CAR_GEAR_START = 25;
	
	// Other constants
	private static final float SCALE = 0.03f;
	private static final float LOW_SPEED = 5f;
	private static final float EPSILON = 0.001f;
	private static final float EPSILON_ANGLE = 0.0005f;
	private static final float REVERSE_SPEED = 5f;
	private static final float BRAKE_STEER_RESISTANCE = 0.5f;
	private static final float STEER_DROPOFF = 0.2f;
	private static final float OFF_TRACK_ROLL = 5f;
	private static final float OFF_TRACK_STEERING = 0.7f;
	
	/**
	 * Creates a new {@code CarPhysics}.
	 */
	public CarPhysics() {
	
		accelerator = 0f;
		brakes = 0;
		steering = 0f;
		gearChange = 0;
		
		position = new Vector3D();
		speed = 0f;
		
		direction = 0f;
		orientation = 0f;
		angularSpeed = 0;
		
		gear = 0;
		rpm = 0;
	}

	/**
	 * Updates the car physics for the current frame. This method uses the 
	 * currently set state of the accelerator, the brakes and the steering.
	 * @param dt Delta time since the previous frame, in seconds.
	 */
	public void updatePhysics(float dt) {
		
		if (gear == -1) { 
			steering = -steering; 
		}
		
		// Movement
		
		float fTraction = CAR_ENGINE_BHP * CAR_TRACTION * accelerator;
		float fBrakes = -CAR_BRAKES * CAR_TRACTION * brakes;
		float fDrag = -CAR_DRAG * speed * Math.abs(speed);
		float fRoll = -CAR_ROLL * speed;
		
		if (speed < EPSILON) { fBrakes = 0f; }
		if (!onTrack) { fRoll *= OFF_TRACK_ROLL; }
		
		float fLongitudinal = fTraction + fBrakes + fDrag + fRoll;
		
		// Lateral physics
		
		float fSteering = CAR_STEERING * steering;
		float fStiffness = -angularSpeed * CAR_STIFFNESS;
		float fResistance = -Math.signum(angularSpeed) * Math.abs(speed) * CAR_RESISTANCE;
		
		if (Math.abs(speed) < EPSILON) { fSteering = 0f; }
		if (fBrakes != 0f) { fSteering *= BRAKE_STEER_RESISTANCE; }
		if (!onTrack) { fSteering *= OFF_TRACK_STEERING; }
		
		float fLateral = fSteering + fStiffness + fResistance;

		// Gear & RPM

		if ((gearChange > 0) && (gear < CAR_GEARS)) { gear++; }
		if ((gearChange < 0) && (gear > -1)) { gear--; }
		
		float gearEntrySpeed = CAR_GEAR_START + CAR_GEAR_RATIO * (gear - 1);
		float nextEntrySpeed = CAR_GEAR_START + CAR_GEAR_RATIO * gear;
		float gearFactor = (speed - gearEntrySpeed) / (nextEntrySpeed - gearEntrySpeed);
		
		rpm = Math.round(CAR_MIN_RPM + gearFactor * (CAR_MAX_RPM - CAR_MIN_RPM));

		if (rpm < CAR_NEUTRAL_RPM) { rpm = CAR_NEUTRAL_RPM; }
		if (rpm > CAR_MAX_RPM) { rpm = CAR_MAX_RPM; }
		if ((gear == 0) && (accelerator > 0f)) { gear = 1; }
		if ((gear == -1) && (speed > LOW_SPEED)) { gear = 0; }
		if ((gear == 0) || (gear == -1)) { rpm = CAR_NEUTRAL_RPM; }
		
		// Sum physics
		
		speed += (fLongitudinal / CAR_MASS) * dt;
		speed *= 1f - (Math.abs(angularSpeed) * STEER_DROPOFF);
		angularSpeed += fLateral * dt;
		
		// Checks		

		if (speed < EPSILON) { speed = 0f; }
		if ((gear == -1) && (accelerator > 0f)) { speed = -REVERSE_SPEED; }
		if (Math.abs(speed) < EPSILON) { angularSpeed = 0f; }
		if (Math.abs(angularSpeed) < EPSILON_ANGLE) { angularSpeed = 0f; }
		
		// Update position and rotation
		
		position.setX((float) (position.getX() - Math.sin(direction) * speed * SCALE));
		position.setY(0f);
		position.setZ((float) (position.getZ() - Math.cos(direction) * speed * SCALE));
		
		orientation -= angularSpeed;
		direction += (orientation - direction) / CAR_GRIP;
	}
	
	/**
	 * Sets the state of the accelerator. The supplied value should be between
	 * 0 and 1.
	 */
	public void setAccelerator(float value) {
		assertControl(value, 0f, 1f);
		accelerator = value;
	}
	
	public float getAccelerator() {
		return accelerator;
	}
	
	/**
	 * Sets the state of the brakes. The supplied value should be between 0 and 1.
	 */
	public void setBrakes(float value) {
		assertControl(value, 0f, 1f);
		brakes = value;
	}
	
	public float getBrakes() {
		return brakes;
	}
	
	/**
	 * Sets the state of the car's steering. The supplied value should be between
	 * -1 and 1, where 0 indicates no steering.
	 */
	public void setSteering(float value) {
		assertControl(steering, -1f, 1f);
		steering = value;
	}
	
	public float getSteering() {
		return steering;
	}
	
	/**
	 * Sets the gear change value. A value of 0 indicates no change, a value of 1
	 * is a positive gear change, and a value of -1 is a negative gear change.
	 */
	public void setGearChange(int value) {
		assertControl(value, -1f, 1f);
		gearChange = value;
	}
	
	public int getGearChange() {
		return gearChange;
	}
	
	/**
	 * Asserts that the specified value is between {@code min} and {@code max}.
	 * @throws IllegalArgumentException if the value is outside of the range.
	 */
	private void assertControl(float value, float min, float max) {
		if ((value < min) || (value > max)) {
			throw new IllegalArgumentException("Value out of range: " + value);
		}
	}
	
	public void setPosition(ImmutableVector3D value) {
		position.setVector(value);
	}
	
	public ImmutableVector3D getPosition() {
		return position;
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public void setOnTrack(boolean onTrack) {
		this.onTrack = onTrack;
	}
	
	public boolean isOnTrack() {
		return onTrack;
	}
	
	public void setDirection(float direction) {
		this.direction = direction;
		this.orientation = direction;
	}
	
	public float getDirection() {
		return direction;
	}
	
	public void setOrientation(float orientation) {
		this.orientation = orientation;
		this.direction = orientation;
	}
	
	public float getOrientation() {
		return orientation;
	}
	
	public void setAngularSpeed(float angularSpeed) {
		this.angularSpeed = angularSpeed;
	}
	
	public float getAngularSpeed() {
		return angularSpeed;
	}
	
	public int getGear() {
		return gear;
	}
	
	public int getRPM() {
		return rpm;
	}
	
	/**
	 * Called to indicate that the car has been in a collision with a static 
	 * object on the circuit.
	 */
	public void collideObject() {
		speed = 0f;
		angularSpeed = 0f;
	}
	
	/**
	 * Called to indicate that the car has been in a collision with another car.
	 * @param other The car with which the collision was.
	 */
	public void collideCar(CarPhysics other) {
		
		float v1 = speed;
		float v2 = other.getSpeed();
		float m1 = CAR_MASS;
		float m2 = other.CAR_MASS;

		if (Math.abs(orientation - other.getOrientation()) > 1.57f) {
			v2 = -v2;
		}

		speed = (v1 * (m1 - m2) + 2 * m2 * v2) / (m1 + m2);
		angularSpeed = 0f;
	}
}

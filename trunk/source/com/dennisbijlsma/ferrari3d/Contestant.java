//--------------------------------------------------------------------------------
// Ferrari3D
// Contestant
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.LapTime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * A contestant is an enrant in a {@code Session}. This class combines the 
 * calculation of the car's physics, which is done by {@code CarPhysics}, and
 * the appearance of the car as a 3D model, which is done by {@code Car}.<p>
 * Like the {@code CarPhysics} class, this class contains a number of setters
 * that are not supposed to be a part of normal flow, but instead shoud be
 * used for external influences on this contestant. 
 */

public abstract class Contestant {
	
	private String name;
	private Session session;
	private CarPhysics physics;
	private Car car;
	private Circuit circuit;

	// Lap data
	private int lap;
	private int intermediate;
	private int point;
	private List<LapTime> laptimes;
	
	protected static final float INTERMEDIATE_DISTANCE = 30f;
	protected static final float WARNING_TIME = 3f;
	protected static final float WARNING_SPEED = 40f;
	protected static final float PENALTY_TIME = 10f;
	
	/**
	 * Creates a new contestant that is an entrant in the specified session.
	 * @param name The name with which the contestant can be identified.
	 * @param session The session in which this contestant is registered.
	 * @param physics Used to calculate physics for this contestant.
	 * @param car Used to update the appearance of this contestant.
	 * @param circuit Used to update the appearance of this contestant.
	 */
	
	public Contestant(String name, Session session, CarPhysics physics, Car car, Circuit circuit) {
		
		this.name = name;
		this.session = session;
		this.physics = physics;
		this.car = car;
		this.circuit = circuit;
		
		lap = 1;
		intermediate = 0;
		point = 0;
		
		laptimes = new ArrayList<LapTime>();
		laptimes.add(new LapTime());
		laptimes.add(new LapTime());
		
		// Initialize 3D model
		
		initCarInfo();
		car.setSoundEnabled((this instanceof Player) && Settings.getInstance().sound);
	}
	
	/**
	 * Initializes the car-specific constants in the {@code CarPhysics} class,
	 * based on the loaded car data.
	 */
	
	private void initCarInfo() {
		
		if (car.getInfo("engineBHP") == null) {
			Settings.getInstance().getLogger().warning("Car info not loaded ");
			return;
		}
		
		physics.CAR_ENGINE_BHP = Float.parseFloat(car.getInfo("engineBHP"));
		physics.CAR_MASS = Float.parseFloat(car.getInfo("mass"));
		physics.CAR_TRACTION = Float.parseFloat(car.getInfo("traction"));
		physics.CAR_BRAKES = Float.parseFloat(car.getInfo("brakes"));
		physics.CAR_DRAG = Float.parseFloat(car.getInfo("drag"));
		physics.CAR_ROLL = Float.parseFloat(car.getInfo("roll"));
		physics.CAR_STEERING = Float.parseFloat(car.getInfo("steering"));
		physics.CAR_STIFFNESS = Float.parseFloat(car.getInfo("stiffness"));
		physics.CAR_RESISTANCE = Float.parseFloat(car.getInfo("resistance"));
		physics.CAR_GEARS = Integer.parseInt(car.getInfo("gears"));
		physics.CAR_MIN_RPM = Integer.parseInt(car.getInfo("minRPM"));
		physics.CAR_MAX_RPM = Integer.parseInt(car.getInfo("maxRPM"));
		physics.CAR_NEUTRAL_RPM = Integer.parseInt(car.getInfo("neutralRPM"));
		physics.CAR_GEAR_RATIO = Integer.parseInt(car.getInfo("gearRatio"));
		physics.CAR_GEAR_START = Integer.parseInt(car.getInfo("gearStart"));
	}
	
	/**
	 * Updates this contestant for the current frame. This method will call the
	 * sub-stages of updating, which include {@link #updateControls(float)}, 
	 * {@link #updatePhysics(float)} and {@link #updateLapData(float)}.
	 * @param dt Delta time since the previous frame, in seconds.
	 */
	
	public void update(float dt) {
		
		updateControls(dt);
		updatePhysics(dt);
		updateLapData(dt);
		
		// Update car
		
		car.getTransform().getPosition().x = physics.getPosition().getX();
		car.getTransform().getPosition().z = physics.getPosition().getZ();
		car.getTransform().getRotation().y = physics.getOrientation();
		
		boolean accelerating = (physics.getAccelerator() > 0f);
		boolean braking = (physics.getBrakes() > 0f);
		int steering = (int) Math.signum(physics.getSteering());
		float speed = physics.getSpeed();
		int gear = physics.getGear();
		int rpm = physics.getRPM();
		
		car.updatePhysics(dt, speed);
		car.updateWheels(dt, speed, steering);
		car.updateRearLight(braking);
		car.updateSmoke(dt, speed, accelerating, braking, steering);
		car.updateEffects(dt, speed, braking, steering);
		car.updateSound(rpm, physics.CAR_MIN_RPM, physics.CAR_MAX_RPM, gear, 
				accelerating, (speed < 1f));
	}
	
	/**
	 * Updates the control settings for this contestant. This method is called
	 * as a part of the update process, and should set the correct state in
	 * the {@code CarPhysics} object obtained with {@link #getCarPhysics()}.
	 */
	
	protected abstract void updateControls(float dt);
	
	/**
	 * Updates the car's physics. This method will first calculate the next
	 * position of the car using the {@code CarPhysics} class. Then it will check
	 * if no obstacles (objects on the circuit or other cars) are in the way for
	 * that position. If there are, the position is reverted.
	 * @param dt Delta time since the previous frame, in seconds.
	 */
	
	protected void updatePhysics(float dt) {
	
		ImmutableVector3D previousPosition = new Vector3D(physics.getPosition());
		physics.setOnTrack(car.isOnTrack());
		physics.updatePhysics(dt);
		ImmutableVector3D newPosition = physics.getPosition();

		boolean collideObject = isCollidingObject(newPosition);
		if (collideObject) {
			physics.collideObject();
		}

		Contestant collideCar = isCollidingCar(newPosition);
		if (collideCar != null) {
			physics.collideCar(collideCar.getCarPhysics());
		}
		
		if ((collideObject) || (collideCar != null)) {
			// Revert to previous position
			physics.setPosition(previousPosition);
		}
	}
	
	/**
	 * Updates the lap data for this contestant. This method is called during
	 * the update process and determines the intermediate, lap and laptimes for
	 * the car depending on its position.
	 * @param dt Delta time since the previous frame, in seconds.
	 */
	
	protected void updateLapData(float dt) {
				
		// Lap & intermediate
		
		ImmutableVector3D position = physics.getPosition();
		CircuitPoint p = circuit.getIntermediate(intermediate);
		
		if (Utils.getDistance(position, p) < INTERMEDIATE_DISTANCE) {
			intermediate++;
			if (intermediate == 3) {
				getCurrentLaptime().setCompleted(true);
				lap++;
				intermediate = 0;
				laptimes.add(new LapTime());
				car.doNextLap();
			}
		}
		
		// Point
		
		for (int i=0; i<circuit.getNumPoints(); i++) {
			//TODO get the point in a more efficient way
			CircuitPoint p1 = circuit.getPoint(i);
			CircuitPoint p2 = circuit.getPoint(point);
			if (Utils.getDistance(position, p1) < Utils.getDistance(position, p2)) {
				setPoint(i);
			}
		}
		
		// Laptime
		
		LapTime currentLap = laptimes.get(lap);
		int sectorTime = currentLap.getSectorTime(intermediate);
		currentLap.setSectorTime(intermediate, sectorTime + Math.round(dt*1000f), true);
	}
	
	/**
	 * Returns if the specified vector collides with a static object on the
	 * circuit.
	 */
	
	private boolean isCollidingObject(ImmutableVector3D v) {
		return car.isCollidingObject(v.getX(), v.getY(), v.getZ());
	}
	
	/**
	 * Returns if the specified vector collides with another contestant. If so,
	 * that contestant is returned. If no collision with other cars occurs this
	 * method returns {@code null}.
	 */
	
	private Contestant isCollidingCar(ImmutableVector3D v) {
		
		for (Contestant i : session.getContestants()) {
			if (i == this) {
				continue;
			}
				
			if (car.isCollidingCar(v.getX(), v.getY(), v.getZ(), i.getCar())) {
				return i;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the name of this contestant.
	 */
	
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the session in which this contestant is registered.
	 */
	
	public Session getSession() {
		return session;
	}
	
	/**
	 * Returns the object used by this class to calculate the car's physics. This
	 * method should only be used by subclasses. 
	 */
	
	protected CarPhysics getCarPhysics() {
		return physics;
	}
	
	/**
	 * Returns the car attached to this contestant. 
	 */
	
	public Car getCar() {
		return car;
	}
	
	/**
	 * Returns the circuit attached to this contestant.
	 */
	
	public Circuit getCircuit() {
		return circuit;
	}
	
	public void setPosition(ImmutableVector3D position) {
		physics.setPosition(position);
	}
	
	public ImmutableVector3D getPosition() {
		return physics.getPosition();
	}
	
	public void setOrientation(float orientation) {
		physics.setOrientation(orientation);
	}
	
	public float getOrientation() {
		return physics.getOrientation();
	}
	
	public void setDirection(float direction) {
		physics.setDirection(direction);
	}
	
	public float getDirection() {
		return physics.getDirection();
	}
	
	public void setSpeed(float speed) {
		physics.setSpeed(speed);
	}
	
	public float getSpeed() {
		return physics.getSpeed();
	}
	
	public void setAngularSpeed(float angularSpeed) {
		physics.setAngularSpeed(angularSpeed);
	}
	
	public float getAngularSpeed() {
		return physics.getAngularSpeed();
	}
	
	public int getGear() {
		return physics.getGear();
	}
	
	public int getRPM() {
		return physics.getRPM();
	}
	
	public void setLap(int lap) {
		this.lap = lap;
	}
	
	public int getLap() {
		return lap;
	}
	
	public void setIntermediate(int intermediate) {
		this.intermediate = intermediate;
	}
	
	public int getIntermediate() {
		return intermediate;
	}
	
	public void setPoint(int point) {
	
		this.point = point;
		
		if (Settings.getInstance().debug) {
			CircuitPoint p = circuit.getPoint(point);
			if (p.isSuggestedSpeed()) {
				Settings.getInstance().getLogger().info(name + " -> point: " + point + 
						", speed: " + Math.round(physics.getSpeed()) + " (" + p.speed + ")");
			}
		}
	}
	
	public int getPoint() {
		return point;
	}
	
	/**
	 * Sets the value for the laptime at the specified index.
	 * @param index The lap number for which the time should be set.
	 * @param value The lap time in milliseconds.
	 */
	
	public void setLaptime(int index, int value) {
		
		while (getLaptime(index) == null) {
			LapTime newLap = new LapTime();
			newLap.setCompleted(true);
			laptimes.add(newLap);
		}
		
		getLaptime(index).setTime(value);
	}
	
	/**
	 * Returns the lap time for the lap with the specified index.
	 */
	
	public LapTime getLaptime(int index) {
		return laptimes.get(index);
	}
		
	/**
	 * Returns the lap time for the current lap.
	 */
	
	public LapTime getCurrentLaptime() {
		return getLaptime(lap);
	}
	
	/**
	 * Returns the lap time for the previous lap.
	 */
	
	public LapTime getPreviousLaptime() {
		return getLaptime(lap - 1);
	}
	
	/**
	 * Returns the best set lap time. When multiple laps have an identical time,
	 * the one which was first set is returned.
	 */
	
	public LapTime getBestLaptime() {
		
		//TODO don't loop all laps, store this value when it is set
		
		LapTime best = getLaptime(0);
		for (LapTime i : laptimes) {
			if ((i.isCompleted()) && (i.getTime() < best.getTime())) {
				best=i;
			}
		}
		
		return best;
	}
	
	/**
	 * Returns a string representation of this contestant.
	 */
	
	@Override
	public String toString() {
		return "Contestant (name=" + name + ")";
	}
}
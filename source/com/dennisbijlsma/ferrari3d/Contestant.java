//-----------------------------------------------------------------------------
// Ferrari3D
// Contestant
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.ferrari3d.graphics.AbstractCar;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * A contestant is an entrant in a {@code Session}. This class contains code for
 * combining the car physics, which are calculated by the {@code CarPhysics} 
 * class, with the position of the car on the track and in the session. In order
 * to update the contestant a {@code Car} and {@code Circuit} have to be set.
 */
public abstract class Contestant {
	
	private String name;
	private Session session;
	private CarPhysics physics;
	private AbstractCar car;
	private CircuitData circuitData;

	private int lap;
	private int intermediate;
	private int point;
	private List<Laptime> laptimes;
	
	protected static final float INTERMEDIATE_DISTANCE = 30f;
	protected static final float WARNING_TIME = 3f;
	protected static final float WARNING_SPEED = 40f;
	protected static final float PENALTY_TIME = 10f;
	
	/**
	 * Creates a new contestant that is an entrant in the specified session.
	 * The contestant cannot be updated until a car and circuit have been set
	 * using {@link #setCar(Car)} and {@link #setCircuit(Circuit)}.
	 * @param name The name with which the contestant can be identified.
	 * @param session The session in which this contestant is registered.
	 * @param physics Used to calculate physics for this contestant.
	 */
	public Contestant(String name, Session session) {
		
		this.name = name;
		this.session = session;
		this.physics = new CarPhysics();
		
		lap = 1;
		intermediate = 0;
		point = 0;
		
		laptimes = new ArrayList<Laptime>();
		laptimes.add(new Laptime());
		laptimes.add(new Laptime());
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
	public final void update(float dt) {
		updateControls(dt);
		updatePhysics(dt);
		updateLapData(dt);
		updateCarGraphics(dt);
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
		
		if (collideObject || (collideCar != null)) {
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
		CircuitPoint p = circuitData.getIntermediate(intermediate);
		
		if (Utils.getDistance(position, p) < INTERMEDIATE_DISTANCE) {
			intermediate++;
			if (intermediate == 3) {
				lap++;
				intermediate = 0;
				laptimes.add(new Laptime());
				car.doNextLap();
			}
		}
		
		// Point
		
		for (int i = 0; i < circuitData.getNumPoints(); i++) {
			//TODO get the point in a more efficient way
			CircuitPoint p1 = circuitData.getPoint(i);
			CircuitPoint p2 = circuitData.getPoint(point);
			if (Utils.getDistance(position, p1) < Utils.getDistance(position, p2)) {
				setPoint(i);
			}
		}
		
		// Laptime
		
		Laptime currentLap = laptimes.get(lap);
		int sectorTime = currentLap.getSectorTime(intermediate);
		currentLap.setSectorTime(intermediate, sectorTime + Math.round(dt * 1000f), true);
	}
	
	/**
	 * Updates the graphical state of the car used by this contestant. This
	 * method is called as the last step of the update cycle.
	 * @param dt Delta time since the previous frame, in seconds.
	 */
	protected void updateCarGraphics(float dt) {
		
		car.setPosition(physics.getPosition().getX(), physics.getPosition().getZ());
		car.setOrientation(physics.getOrientation());
		
		//TODO remove dependency 
		if (car instanceof Car) {
			Car _car = (Car) car;
			boolean accelerating = (physics.getAccelerator() > 0f);
			boolean braking = (physics.getBrakes() > 0f);
			int steering = (int) Math.signum(physics.getSteering());
			float speed = physics.getSpeed();
			int gear = physics.getGear();
			int rpm = physics.getRPM();
		
			_car.updatePhysics(dt, speed);
			_car.updateWheels(dt, speed, steering);
			_car.updateRearLight(braking);
			_car.updateSmoke(dt, speed, accelerating, braking, steering);
			_car.updateEffects(dt, speed, braking, steering);
			_car.updateSound(rpm, physics.CAR_MIN_RPM, physics.CAR_MAX_RPM, gear, 
					accelerating, (speed < 1f));
		}
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
	
	public final String getName() {
		return name;
	}
	
	public final Session getSession() {
		return session;
	}
	
	/**
	 * Returns the object used by this class to calculate the car's physics. This
	 * method should only be used by subclasses. 
	 */
	protected final CarPhysics getCarPhysics() {
		return physics;
	}
	
	public void setCar(AbstractCar car) {
		this.car = car;
		initCarInfo();
		//TODO remove dependency
		if (car instanceof Car) {
			((Car) car).setSoundEnabled((this instanceof Player) && Settings.getInstance().sound);
		}
	}
	
	public AbstractCar getCar() {
		return car;
	}
	
	public String getCarName() {
		return car.getCarName();
	}
	
	public void setCircuitData(CircuitData circuitData) {
		this.circuitData = circuitData;
	}
	
	public CircuitData getCircuitData() {
		return circuitData;
	}
	
	public String getCircuitName() {
		return circuitData.getCircuitName();
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
			CircuitPoint p = circuitData.getPoint(point);
			if (p.isSuggestedSpeed()) {
				Settings.getInstance().getLogger().info(String.format(
						"%s -> point: %d, speed: %.1f (%.1f)", 
						name, point, physics.getSpeed(), p.getSuggestedSpeed()));
			}
		}
	}
	
	public int getPoint() {
		return point;
	}
	
	public Laptime getLaptime(int index) {
		return laptimes.get(index);
	}
	
	public Laptime getCurrentLaptime() {
		return getLaptime(lap);
	}
	
	public Laptime getLastLaptime() {
		return getLaptime(lap - 1);
	}
	
	/**
	 * Returns the fastest set lap time set by this contestant. When multiple 
	 * laps have an identical time the one which was first set is returned.
	 */
	public Laptime getFastestLaptime() {
		
		//TODO don't loop all laps, store this value when it is set
		
		Laptime best = laptimes.get(0);
		
		// The current lap is not set, so it's not included in this calculation
		for (int i = 1; i < lap; i++) {
			if (laptimes.get(i).getTime() < best.getTime()) {
				best = laptimes.get(i);
			}
		}
		
		return best;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Contestant) {
			return ((Contestant) o).name.equals(name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public String toString() {
		return "Contestant(name=" + name + ")";
	}
}

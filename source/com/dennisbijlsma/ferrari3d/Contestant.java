//--------------------------------------------------------------------------------
// Ferrari3D
// Contestant
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.GameEntity;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * The <code>Contestant</code> controls a player, AI, or drone participating in a
 * session. Each contestant is responsible for updating its own state, both game
 * logic and graphics. The contestant is bound to a <code>Car</code> object, which
 * acts as its visual representation. A contestant should be registered with the
 * <code>Session</code>, which keeps track of all contestants that are currently
 * participating.
 */

public abstract class Contestant implements GameEntity,Comparable<Contestant> {
	
	private String id;
	private Session session;

	private Vector3D position;
	private float orientation;
	private float direction;
	private float speed;
	private float angular;
	private int gear;
	private int rpm;
	private float grip;
	private int lap;
	private int intermediate;
	private int point;
	private List<Laptime> laptimes;
	
	private int controlAccelerator;
	private int controlBrakes;
	private int controlSteering;
	private int controlGears;
	private Car car;
	private Circuit circuit;
	
	protected float CAR_ENGINE_BHP=850;
	protected float CAR_MASS=600f;
	protected float CAR_TRACTION=10f;
	protected float CAR_BRAKES=1200f;
	protected float CAR_DRAG=0.7f;
	protected float CAR_ROLL=28f;
	protected float CAR_STEERING=0.1f;
	protected float CAR_STIFFNESS=10f;
	protected float CAR_RESISTANCE=0.00065f;
	protected int CAR_GEARS=7;
	protected int CAR_MIN_RPM=15000;
	protected int CAR_MAX_RPM=19000;
	protected int CAR_NEUTRAL_RPM=4500;
	protected int CAR_GEAR_RATIO=10;
	protected int CAR_GEAR_START=25;
	
	private static final float SCALE=0.03f;
	private static final float LOW_SPEED=5f;
	private static final float EPSILON=0.001f;
	private static final float EPSILON_ANGLE=0.0005f;
	private static final float INTERMEDIATE_DISTANCE=30f;
	private static final float REVERSE_SPEED=5f;
	private static final float BRAKE_STEER_RESISTANCE=0.5f;
	private static final float STEER_DROPOFF=0.2f;
	private static final float OFF_TRACK_ROLL=5f;
	private static final float OFF_TRACK_STEERING=0.7f;
	
	public static enum Controls { 
		ACCELERATOR,
		BRAKES,
		STEERING,
		GEARS 
	}
	
	/**
	 * Creates a new <code>Contestant</code>. Initially it is not bound to any
	 * session, including the one from the parameter in this method. This needs
	 * to be done manually.
	 * @param id The unique ID describing the name of this class.
	 * @param session The <code>Session</code> that will contain this class.
	 * @param car The visual representation of the car.
	 * @param circuit The visual representation of the circuit.
	 */
	
	public Contestant(String id,Session session,Car car,Circuit circuit) {
		
		this.id=id;
		this.session=session;
		this.car=car;
		this.circuit=circuit;

		position=new Vector3D();
		orientation=0f;
		direction=0f;
		speed=0f;
		angular=0f;
		grip=1f;
		lap=1;
		intermediate=Laptime.SECTOR_1;
		point=0;
		laptimes=new ArrayList<Laptime>();
		laptimes.add(new Laptime());
		laptimes.add(new Laptime());		
		
		controlAccelerator=0;
		controlBrakes=0;
		controlSteering=0;
		controlGears=0;
		
		initCarInfo();
		car.setSoundEnabled(isPlayer() && Settings.getInstance().sound);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void update(float dt) {
		
		// Update data

		updateControls(dt);
		updatePhysics(dt);
		updateLapData(dt);
		
		// Update car position
		
		car.getTransform().getPosition().x=position.x;
		car.getTransform().getPosition().z=position.z;
		car.getTransform().getRotation().y=orientation;
		
		// Update car graphics
		
		boolean accelerating=(getControl(Controls.ACCELERATOR)==10);
		boolean braking=(getControl(Controls.BRAKES)==10);
		int steering=getControl(Controls.STEERING);
		
		car.updatePhysics(dt,speed);
		car.updateWheels(dt,speed,steering);
		car.updateRearLight(braking);
		car.updateSmoke(dt,speed,accelerating,braking,steering);
		car.updateEffects(dt,speed,braking,steering);
		car.updateSound(rpm,CAR_MIN_RPM,CAR_MAX_RPM,gear,accelerating,(speed<1f));
	}
	
	/**
	 * Updates the controls for this <code>Contestant</code>. This method is 
	 * called during <code>update(float)</code>, which is executed every frame
	 * in the game loop. Controls can come from different sources, such as the
	 * keyboard or being generated by AI.
	 * @param dt The delta time for this frame.  
	 */
	
	protected abstract void updateControls(float dt);
	
	/**
	 * Updates the physics for this <code>Contestant</code>. This method is 
	 * called during <code>update(float)</code>, which is executed every frame
	 * in the game loop. This method will update the position of the car, as well
	 * as setting information such as the current speed.
	 * @param dt The delta time for this frame.  
	 */
	
	protected void updatePhysics(float dt) {
		
		// Controls
		
		float accelerator=0.1f*getControl(Controls.ACCELERATOR);
		float brakes=0.1f*getControl(Controls.BRAKES);
		float steering=0.1f*getControl(Controls.STEERING);
		int gearChange=getControl(Controls.GEARS);
		
		boolean isOnTrack=car.isOnTrack();
		boolean isOnFloor=car.isOnFloor();
		
		if (gear==-1) { steering=-steering; }
		
		// Longitudinal physics
		
		float fTraction=CAR_ENGINE_BHP*CAR_TRACTION*accelerator;
		float fBrakes=-CAR_BRAKES*CAR_TRACTION*brakes;
		float fDrag=-CAR_DRAG*speed*Math.abs(speed);
		float fRoll=-CAR_ROLL*speed;
		
		if (speed<EPSILON) { fBrakes=0f; }
		if (!isOnTrack) { fRoll*=OFF_TRACK_ROLL; }
		if (!isOnFloor) { fTraction=0f; }
		
		float fLongitudinal=fTraction+fBrakes+fDrag+fRoll;
		
		// Lateral physics
		
		float fSteering=CAR_STEERING*steering;
		float fStiffness=-angular*CAR_STIFFNESS;
		float fResistance=-Math.signum(angular)*Math.abs(speed)*CAR_RESISTANCE;
		
		if (Math.abs(speed)<EPSILON) { fSteering=0f; }
		if ((fBrakes!=0f) && (!isAI())) { fSteering*=BRAKE_STEER_RESISTANCE; }
		if (!isOnTrack) { fSteering*=OFF_TRACK_STEERING; }
		
		float fLateral=fSteering+fStiffness+fResistance;

		// Gear & RPM

		if ((gearChange>0) && (gear<CAR_GEARS)) { 
			gear++;
			car.doGearChange(true);
		}
		
		if ((gearChange<0) && (gear>-1)) { 
			gear--; 
			car.doGearChange(false);
		}
		
		float gearEntrySpeed=CAR_GEAR_START+CAR_GEAR_RATIO*(gear-1);
		float nextEntrySpeed=CAR_GEAR_START+CAR_GEAR_RATIO*gear;
		float gearFactor=(speed-gearEntrySpeed)/(nextEntrySpeed-gearEntrySpeed);
		
		rpm=Math.round(CAR_MIN_RPM+gearFactor*(CAR_MAX_RPM-CAR_MIN_RPM));

		if (rpm<CAR_NEUTRAL_RPM) { rpm=CAR_NEUTRAL_RPM; }
		if (rpm>CAR_MAX_RPM) { rpm=CAR_MAX_RPM; }
		if ((gear==0) && (accelerator>0f)) { gear=1; }
		if ((gear==-1) && (speed>LOW_SPEED)) { gear=0; }
		if ((gear==0) || (gear==-1)) { rpm=CAR_NEUTRAL_RPM; }
		
		// Sum physics
		
		speed+=(fLongitudinal/CAR_MASS)*dt;
		speed*=1f-(Math.abs(angular)*STEER_DROPOFF);
		angular+=fLateral*dt;
		
		// Checks		

		if (speed<EPSILON) { speed=0f; }
		if ((gear==-1) && (accelerator>0f)) { speed=-REVERSE_SPEED; }
		if (Math.abs(speed)<EPSILON) { angular=0f; }
		if (Math.abs(angular)<EPSILON_ANGLE) { angular=0f; }
		
		// Update position
		
		float newX=(float) (position.x-Math.sin(direction)*speed*SCALE);
		float newY=0f;
		float newZ=(float) (position.z-Math.cos(direction)*speed*SCALE);
		float newRot=orientation-angular;
		
		if (isCollidingObject(newX,newY,newZ)) {
			speed=0f;
			angular=0f;
		} else {
			Contestant ccar=isCollidingCar(newX,newY,newZ);
			
			if (ccar!=null) {
				float v1=speed;
				float v2=ccar.getSpeed(false);
				float m1=CAR_MASS;
				float m2=ccar.CAR_MASS;
		
				if (Math.abs(orientation-ccar.getOrientation())>1.57f) {
					v2=-v2;
				}
		
				speed=(v1*(m1-m2)+2*m2*v2)/(m1+m2);
			} else {
				position.x=newX;
				position.y=newY;
				position.z=newZ;
				orientation=newRot;
				direction=newRot;
			}
		}

	}
	
	/**
	 * Updates lap data for this <code>Contestant</code>. This method is called 
	 * during {@see #update(float)}, which is executed every frame in the game 
	 * loop. Lap data involves the current point and intermediate, and is used 
	 * to determine the progress within the session.
	 * @param dt The delta time for this frame.  
	 */
	
	protected void updateLapData(float dt) {
				
		// Lap & intermediate
		
		CircuitPoint p=circuit.getIntermediate(intermediate);
		
		if (Utils.getDistance(position.x,position.z,p.pointX,p.pointY)<INTERMEDIATE_DISTANCE) {
			intermediate++;
			if (intermediate==3) {
				getCurrentLaptime().setComplete(true);
				lap++;
				intermediate=Laptime.SECTOR_1;
				laptimes.add(new Laptime());
				car.doNextLap();
			}
		}
		
		// Point
		
		for (int i=0; i<circuit.getNumPoints(); i++) {
			//TODO get the point in a more efficient way
			CircuitPoint p1=circuit.getPoint(i);
			CircuitPoint p2=circuit.getPoint(point);
			if (Utils.getDistance(position.x,position.z,p1.pointX,p1.pointY)<
					Utils.getDistance(position.x,position.z,p2.pointX,p2.pointY)) {
				setPoint(i);
			}
		}
		
		// Laptime
		
		Laptime currentLap=laptimes.get(lap);
		currentLap.setSector(intermediate,Math.round(dt*1000f),true);
	}
	
	/**
	 * Returns true if the car is colliding with an object. This method is for
	 * collision avoidance, so it uses the potential new position instead of the
	 * current one.
	 */
	
	private boolean isCollidingObject(float newX,float newY,float newZ) {
	
		return car.isCollidingObject(newX,newY,newZ);
	}
	
	/**
	 * Returns if the car is colliding with another car. This method is for
	 * collision avoidance, so it uses the potential new position instead of the
	 * current one. When a potential collision is detected, that object is returned.
	 */
	
	private Contestant isCollidingCar(float newX,float newY,float newZ) {
		
		for (Contestant i : session.getContestants()) {
			if (i==this) {
				continue;
			}
				
			if (car.isCollidingCar(newX,newY,newZ,i.getCar())) {
				return i;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the unique ID for this <code>Contestant</code>. This ID can be
	 * used by the session to identify it.
	 */
	
	public String getID() {
		
		return id;
	}
	
	/**
	 * Returns the <code>Session</code> object which keeps track of all different
	 * <code>Contestant</code>s. 
	 */
	
	public Session getSession() {
	
		return session;
	}
	
	/**
	 * Sets the position of this contestant. 
	 */
	
	public void setPosition(Vector3D p) {
	
		position.setVector(p);
	}
	
	/**
	 * Returns the position of this contestant.
	 */
	
	public Vector3D getPosition() {
		
		return position;
	}
	
	/**
	 * Sets the orientation of this contestant. This is the rotation around the
	 * Y-axis in which the car is heading.
	 */
	
	public void setOrientation(float orientation) {
		
		this.orientation=orientation;
	}
	
	/**
	 * Returns the orientation of this contestant. This is the rotation around
	 * the Y-axis in which the car is heading.
	 */
	
	public float getOrientation() {
	
		return orientation;
	}
	
	/**
	 * Sets the direction of this contestant. This is the rotation around the 
	 * Y-axis in which the car is moving.
	 */
	
	public void setDirection(float direction) {
		
		this.direction=direction;
	}
	
	/**
	 * Returns the direction of this contestant. This is the rotation around the 
	 * Y-axis in which the car is moving.
	 */
	
	public float getDirection() {
	
		return direction;
	}
	
	/**
	 * Sets the speed of the car. This method should only be called by subclasses.
	 * The parameter should be the speed value in metres per second. 
	 */
	
	protected void setSpeed(float speed) {
		
		this.speed=speed;
	}
	
	/**
	 * Returns the current speed of the car. This value is in metres per second
	 * by default, but can optionally be returned in kilometers per hour. 
	 */
	
	public float getSpeed(boolean kmh) {
		
		return kmh ? speed*3.6f : speed;
	}
	
	/**
	 * Sets the angular speed of the car. This method should only be called by 
	 * subclasses. The parameter should be in radians per second. 
	 */
	
	protected void setAngular(float angular) {
		
		this.angular=angular;
	}
	
	/**
	 * Returns the current angular speed of the car. This value is in radians
	 * per second.
	 */
	
	public float getAngular() {
	
		return angular;
	}
	
	/**
	 * Returns the gear that the car is in. Positive values indicate the gear
	 * number, typically between 1 and 6 or 7. A value of 0 stands for neutral
	 * gear, while a value of -1 stands for reverse.
	 */
	
	public int getGear() {
		
		return gear;
	}
	
	/**
	 * Returns the RPM for the car. This value is typically between 0 and 19000
	 * depending on the engine type of the car.
	 */
	
	public int getRPM() {
	
		return rpm;
	}
	
	/**
	 * Returns the current grip level. The returned number will be between 0 and
	 * 1, where 1 indicates full grip.
	 */
	
	public float getGrip() {
		
		return grip;
	}
	
	/**
	 * Sets the current lap. This method should only be called by subclasses who
	 * override any behavior.
	 */
	
	protected void setLap(int lap) {
	
		this.lap=lap;
	}
	
	/**
	 * Returns the current lap. The initial lap is 1, even when the car has not
	 * crossed the finish line for the first time.
	 */
	
	public int getLap() {
		
		return lap;
	}
	
	/**
	 * Sets the current intermediate. This method should only be called by 
	 * subclasses who override any behavior.
	 */
	
	protected void setIntermediate(int intermediate) {
	
		this.intermediate=intermediate;
	}
	
	/**
	 * Returns the current intermediate. Look at the class <code>Laptime</code>
	 * for possible values of this field.
	 */
	
	public int getIntermediate() {
		
		return intermediate;
	}
	
	/**
	 * Sets the current circuit point. This method should only be called by 
	 * subclasses who override any behavior.
	 */
	
	protected void setPoint(int point) {
	
		this.point=point;
		
		if (Settings.getInstance().debug) {
			CircuitPoint p=circuit.getPoint(point);
			if (p.isSuggestedSpeed()) {
				Settings.getInstance().getLogger().info(id+" -> point: "+point+", speed: "+
						Math.round(speed)+" ("+p.speed+")");
			}
		}
	}
	
	/**
	 * Returns the current circuit point. This value indicates the closest point,
	 * so multiple invocations of this method may return different results.
	 */
	
	public int getPoint() {
		
		return point;
	}
	
	/**
	 * Sets the lap time in milliseconds for the lap with the specified index.
	 * This method should only be called by subclasses.
	 */
	
	protected void setLaptime(int index,int value) {
		
		while (getLaptime(index)==null) {
			Laptime newLap=new Laptime();
			newLap.setComplete(true);
			laptimes.add(newLap);
		}
		
		getLaptime(index).setLaptime(value);
	}
	
	/**
	 * Returns the lap time for the lap with the specified index. This index
	 * should be between 0 and the current lap.
	 */
	
	public Laptime getLaptime(int index) {
		
		return laptimes.get(index);
	}
		
	/**
	 * Returns the lap time for the current lap. As this lap has not been 
	 * completed yet the actual time value will change.
	 */
	
	public Laptime getCurrentLaptime() {
	
		return getLaptime(lap);
	}
	
	/**
	 * Returns the lap time for the previous lap. When the car is currently in
	 * the first lap, the returned object will be empty.
	 */
	
	public Laptime getPreviousLaptime() {
	
		return getLaptime(lap-1);
	}
	
	/**
	 * Returns the best set lap time. When multiple laps have an identical time,
	 * the one which was first set (the one with the lowest index) is returned.
	 */
	
	public Laptime getBestLaptime() {
		
		//TODO don't loop all laps, store this value when it is set
		
		Laptime best=getLaptime(0);
		
		for (Laptime i : laptimes) {
			if ((i.getComplete()) && (i.getLaptime()<best.getLaptime())) {
				best=i;
			}
		}
		
		return best;
	}
	
	/**
	 * Sets the control with the specified key. The set value should be between
	 * 0 and 10 for linear controls, or -10 and 10 for two-way controls.
	 */
	
	public void setControl(Controls key,int value) {
	
		switch (key) {
			case ACCELERATOR : controlAccelerator=value; break;
			case BRAKES : controlBrakes=value; break;
			case STEERING : controlSteering=value; break;
			case GEARS : controlGears=value; break;
			default : break;
		}
	}
	
	/**
	 * Returns the value for the control with the specified key. The returned 
	 * value will be between 0 and 10 for linear controls, or between -10 and 
	 * 10 for two-way controls.
	 */
	
	public int getControl(Controls key) {
		
		switch (key) {
			case ACCELERATOR : return controlAccelerator;
			case BRAKES : return controlBrakes;
			case STEERING : return controlSteering;
			case GEARS : return controlGears;
			default : return 0;
		}
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
	
	/**
	 * Sets the car info. This involves parsing all fields in the <code>Car</code>
	 * object.
	 */
	
	private void initCarInfo() {
		
		if (car.getInfo("engineBHP")==null) {
			Settings.getInstance().getLogger().warning("Car info not loaded ");
			return;
		}
		
		CAR_ENGINE_BHP=Float.parseFloat(car.getInfo("engineBHP"));
		CAR_MASS=Float.parseFloat(car.getInfo("mass"));
		CAR_TRACTION=Float.parseFloat(car.getInfo("traction"));
		CAR_BRAKES=Float.parseFloat(car.getInfo("brakes"));
		CAR_DRAG=Float.parseFloat(car.getInfo("drag"));
		CAR_ROLL=Float.parseFloat(car.getInfo("roll"));
		CAR_STEERING=Float.parseFloat(car.getInfo("steering"));
		CAR_STIFFNESS=Float.parseFloat(car.getInfo("stiffness"));
		CAR_RESISTANCE=Float.parseFloat(car.getInfo("resistance"));
		CAR_GEARS=Integer.parseInt(car.getInfo("gears"));
		CAR_MIN_RPM=Integer.parseInt(car.getInfo("minRPM"));
		CAR_MAX_RPM=Integer.parseInt(car.getInfo("maxRPM"));
		CAR_NEUTRAL_RPM=Integer.parseInt(car.getInfo("neutralRPM"));
		CAR_GEAR_RATIO=Integer.parseInt(car.getInfo("gearRatio"));
		CAR_GEAR_START=Integer.parseInt(car.getInfo("gearStart"));
	}
	
	/**
	 * Returns true if this <code>Contestant</code> is a player.
	 * @deprecated Players should not have unfair advantages.
	 */
	
	@Deprecated
	private boolean isPlayer() {
	
		return (this instanceof Player);
	}
	
	/**
	 * Returns true if this <code>Contestant</code> is an AI.
	 * @deprecated AIs should not have unfair advantages.
	 */
	
	@Deprecated
	private boolean isAI() {
	
		return (this instanceof AI);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public int compareTo(Contestant other) {
	
		switch (session.getMode()) {
			case TIME :
				int bestlap=getBestLaptime().getLaptime();
				int otherBestlap=other.getBestLaptime().getLaptime();
				if (bestlap>otherBestlap) { return 1; }
				if (bestlap<otherBestlap) { return -1; }
				return 0;
			case RACE :
				int distance=getLap()*10000+getIntermediate()*1000+getPoint();
				int otherDistance=other.getLap()*10000+other.getIntermediate()*1000+other.getPoint();
				if (distance>otherDistance) { return 1; }
				if (distance<otherDistance) { return -1; }
				return 0;
			default :
				throw new IllegalStateException();
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public String toString() {
		
		return "[Contestant: "+id+"]";
	}
}
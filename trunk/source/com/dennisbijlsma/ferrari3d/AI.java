//--------------------------------------------------------------------------------
// Ferrari3D
// AI
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * A contestant that is controlled by artificial intelligence. The AI will be 
 * assigned a unique personality during construction, allowing for different AI
 * personalities. Both driving skill and aggression are taken into account when
 * the AI makes decisions wheter to overtake or defend.
 */

public class AI extends Contestant {
	
	private float skill;
	private float aggression;	
	private Vector3D point;
	private RaceLine line;
	private Behavior behavior;
	private float behaviorTime;
	
	private Vector3D tempVector1;
	private Vector3D tempVector2;
		
	private static final int LOOK_AHEAD_POINTS = 3;
	private static final float BRAKE_RELATIVE_DISTANCE = 0.2f;
	private static final float LIFT_DISTANCE = 10f;
	private static final float STEERING_EPSILON = 0.05f;
	private static final float MIN_SPEED = 8f; 
	private static final float LINE_WIDTH = 2f;
	private static final float OVERTAKING_LINE_LENGTH = 50f;
	private static final float OVERTAKING_TIME = 2f;
	private static final float BLOCKING_LINE_LENGTH = 30f;
	private static final float BLOCKING_TIME = 3f;
	private static final float EVADE_DISTANCE = 5f;
	private static final float EVADE_TIME = 2f;
	private static final float BASE_CHANCE = 0.01f;
	
	public enum RaceLine { 
		IDEAL,
		ALTERNATIVE 
	}
	
	public enum Behavior { 
		DRIVING,
		OVERTAKING,
		BLOCKING,
		EVADING
	}

	/**
	 * Creates a new AI contestant. The personality of the AI will be generated. A
	 * number of predefined personalities exist, and these are used to create an 
	 * infinite number of possible values.
	 */
	
	public AI(String id, Session session, CarPhysics physics, Car car, Circuit circuit) {
		
		super(id, session, physics, car, circuit);
	
		point = new Vector3D();
		line = RaceLine.IDEAL;
		behavior = Behavior.DRIVING;
		behaviorTime = 0f;
		
		tempVector1 = new Vector3D();
		tempVector2 = new Vector3D();
		
		// Generate skill level
		
		switch (Settings.getInstance().aiLevel) {
			case Settings.AI_EASY : skill = 0.4f; break;
			case Settings.AI_NORMAL : skill = 0.6f; break;
			case Settings.AI_HARD : skill = 0.8f; break;
			default : throw new AssertionError();
		}
		
		skill += random(0.2f);
		skill = Math.round(skill * 10f) / 10f;
		
		// Generate aggression
		
		aggression = 0.2f;
		aggression += random(0.8f);
		aggression = Math.round(aggression * 10f) / 10f;
		
		if (Settings.getInstance().debug) {
			Settings.getInstance().getLogger().info(getName() + " -> skill: " + 
					skill + ", aggression: " + aggression);
		}
	}
	
	/**
	 * Updates the controls for this AI contestant.
	 */
	
	@Override
	protected void updateControls(float dt) {
		
		// Set all controls to default values

		CarPhysics physics = getCarPhysics();
		physics.setAccelerator(0f);
		physics.setBrakes(0f);
		physics.setSteering(0f);
		physics.setGearChange(0);
		
		// Calculate the new controls
	
		updateAI(dt);
	}
	
	/**
	 * Updates the AI behavior for this frame. Both the driving behavior and the
	 * overtaking and/or defending behavior will be updated. When no other 
	 * contestants are nearby, the AI will follow the ideal line that consists of
	 * circuit points, and use the attached data to accelerate or brake. When 
	 * other cars are nearby, the AI will either attempt to overtake the car in
	 * front, or defend its position. This behavior depends on the personality of
	 * the AI.
	 * @param dt Delta time for this frame.
	 */
	
	@SuppressWarnings("deprecation")
	protected void updateAI(float dt) {
		
		CarPhysics physics = getCarPhysics();
		
		int targetIndex = getTargetPoint();
		int focusIndex = getFocusPoint();		
		CircuitPoint target = getPointAt(targetIndex);
		CircuitPoint focus = getPointAt(focusIndex);
		
		if ((line == RaceLine.IDEAL) || (!target.isAltPoint())) {			
			point.setVector(target.pointX, 0f, target.pointY);			
		} else {
			point.setVector(target.altX, 0f, target.altY);
		}
			
		// Set steering
		
		tempVector1.setVector(getPosition());
		tempVector2.setVector(point);
		tempVector2.relativeTo(tempVector1, tempVector2, getOrientation());
						
		if (tempVector2.z < -STEERING_EPSILON) { physics.setSteering(-1f); }
		if (tempVector2.z > STEERING_EPSILON) { physics.setSteering(1f); }
		
		// Set accelerator and brakes
		
		float deltaSpeed = getSpeed() - focus.speed;
		float distance = getPointDistance(getPointAt(getPoint()), focus);
		
		if (deltaSpeed > distance * BRAKE_RELATIVE_DISTANCE) {
			physics.setBrakes(1f);
		} else {
			if (distance < LIFT_DISTANCE) {
				physics.setAccelerator(skill);
			} else {
				physics.setAccelerator(1f);
			}
		}
		
		// Behavior
		
		for (Contestant i : getSession().getContestants()) {
			if ((i == this) || (behaviorTime > 0f)) {
				continue;
			}
			
			tempVector1.setVector(getPosition());
			tempVector2.setVector(i.getPosition());
			tempVector2.relativeTo(tempVector1, tempVector2, getOrientation());
			
			boolean overtakingChance = chance(BASE_CHANCE * (1f - aggression));
			boolean blockingChance = chance(BASE_CHANCE);
			
			if ((isLineFront(tempVector2)) && (overtakingChance)) {
				setBehavior(Behavior.OVERTAKING, OVERTAKING_TIME);
				setRaceLine((line == RaceLine.IDEAL) ? RaceLine.ALTERNATIVE : RaceLine.IDEAL);
			} else {
				if ((isFront(tempVector2)) && (overtakingChance)) {
					setBehavior(Behavior.OVERTAKING,OVERTAKING_TIME);
				}
			}
			
			if ((isLineBack(tempVector2)) && (blockingChance)) {
				setBehavior(Behavior.BLOCKING, BLOCKING_TIME);
			} else {
				if ((isBack(tempVector2)) && (blockingChance)) {
					setBehavior(Behavior.BLOCKING, BLOCKING_TIME);
					setRaceLine((line == RaceLine.IDEAL) ? RaceLine.ALTERNATIVE : RaceLine.IDEAL);
				}
			}
			
			if (isEvadeDistance(tempVector2)) {
				setBehavior(Behavior.EVADING, EVADE_TIME);
			}
		}
		
		if (behaviorTime > 0f) {
			behaviorTime -= dt;
		} else {
			setBehavior(Behavior.DRIVING, 0f);
			setRaceLine(RaceLine.IDEAL);
		}
		
		switch (behavior) {
			case DRIVING : break;
			case OVERTAKING : break;
			case BLOCKING : break;
			case EVADING : physics.setAccelerator(0f); break;
			default : throw new AssertionError();
		}
		
		// Checks
		
		if ((getSpeed() < MIN_SPEED) && (physics.getAccelerator() == 0f)) {
			physics.setAccelerator(1f);
			physics.setBrakes(0f);
		}
		
		if (!getCar().isOnTrack()) {
			physics.setAccelerator(getSpeed() > MIN_SPEED ? 0f : 1f);
			physics.setBrakes(getSpeed() > MIN_SPEED ? 1f : 0f);
		}
		
		if (!getSession().isStarted() || getSession().isFinished()) {
			physics.setAccelerator(0f);
			physics.setBrakes(0f);
		}
		
		// Gear controls
		
		if ((getGear() < physics.CAR_GEARS) && (getRPM() >= physics.CAR_MAX_RPM)) { 
			physics.setGearChange(1);
		}
		
		if ((getGear() > 1) && (getRPM() <= physics.CAR_MIN_RPM)) { 
			physics.setGearChange(-1);
		}
	}
	
	/**
	 * Sets the new behavior for this AI car. The behavior will be active for 
	 * the specified amount of time. 
	 */
	
	private void setBehavior(Behavior newBehavior, float time) {
		
		if ((Settings.getInstance().debug) && (behavior != newBehavior)) {
			Settings.getInstance().getLogger().info(getName() + " -> behavior: " + newBehavior);
		}
		
		behavior = newBehavior;
		behaviorTime = time;
	}
	
	/**
	 * Sets the current racing line for this AI car.
	 */
	
	private void setRaceLine(RaceLine newLine) {
		
		if ((Settings.getInstance().debug) && (line != newLine)) {
			Settings.getInstance().getLogger().info(getName() + " -> race line: " + newLine);
		}
		
		line = newLine;
	}
	
	/**
	 * Returns the circuit point at the specified index. This method is the 
	 * equivalent of calling <code>getCircuit().getPoint(int)</code>.
	 */
	
	private CircuitPoint getPointAt(int index) {
		return getCircuit().getPoint(index);
	}
	
	/**
	 * Returns the distance between two circuit points. The distance returned is
	 * an absolute number.
	 */
	
	private float getPointDistance(CircuitPoint point1, CircuitPoint point2) {
		
		float deltaX = Math.abs(point2.pointX - point1.pointX);
		float deltaY = Math.abs(point2.pointY - point1.pointY);
		
		return (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
	}
	
	/**
	 * Returns the index of the point to where this AI should steer. This is not 
	 * the closest point, but one slightly further ahead.
	 */
	
	private int getTargetPoint() {
		return getPoint() + LOOK_AHEAD_POINTS;
	}
	
	/**
	 * Returns the index of the closest point which contains AI data. This will 
	 * be used by this AI to know if it should accelerate, brake or lift. 
	 */
	
	private int getFocusPoint() {
		
		for (int i=getPoint(); i<getCircuit().getNumPoints(); i++) {
			if (getCircuit().getPoint(i).isSuggestedSpeed()) {
				return i;
			}
		}
	
		return 0;
	}
	
	/**
	 * Returns if the specified vector is on the same line as this AI, in front 
	 * of it. The line is measured both in thickness and in length.
	 */
	
	private boolean isLineFront(Vector3D p) {
		
		if (p.x>0f) {
			return false;
		} else {		
			return ((p.x > -OVERTAKING_LINE_LENGTH) && (Math.abs(p.z) < LINE_WIDTH));
		}
	}
	
	/**
	 * Returns if the specified vector is in front of this AI.
	 */
	
	private boolean isFront(Vector3D p) {
		return ((p.x < 0f) && (p.x > -OVERTAKING_LINE_LENGTH));
	}
	
	/**
	 * Returns if the specified vector is on the same line as this AI, behind it.
	 * The line is measured both in thickness and in length.
	 */
	
	private boolean isLineBack(Vector3D p) {
		
		if (p.x<0f) {
			return false;
		} else {
			return ((p.x < BLOCKING_LINE_LENGTH) && (Math.abs(p.z) < LINE_WIDTH));
		}
	}
	
	/**
	 * Returns if the specified vector is behind this AI.
	 */
	
	private boolean isBack(Vector3D p) {
		return ((p.x > 0f) && (p.x < BLOCKING_LINE_LENGTH));
	}
	
	/**
	 * Returns if the specified point is within the minimum distance. When this
	 * is the case the AI car will attempt to avoid a collision at all cost.
	 */
	
	private boolean isEvadeDistance(Vector3D p) {
		return ((p.x < 0f) && (p.x > -EVADE_DISTANCE) && (Math.abs(p.z) < LINE_WIDTH));
	}
		
	/**
	 * Convenience method that returns a number between 0 and max.
	 */
	
	private float random(float max) {
		return (float) Math.random() * max;
	}
	
	/**
	 * Convenience method that returns if a random chance succeeds. A parameter
	 * of 1 will always pass and a parameter of 0 will always fail.
	 */
	
	private boolean chance(float factor) {
		return (random(1f) > 1f - factor);
	}
}
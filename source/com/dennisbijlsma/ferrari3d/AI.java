//--------------------------------------------------------------------------------
// Ferrari3D
// AI
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.data.*;

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
	private AILine line;
	private AIBehavior behavior;
	private float behaviorTime;
	
	private Vector3D tempVector1;
	private Vector3D tempVector2;
	
	private static enum AILine { IDEAL,OVERTAKING }
	private static enum AIBehavior { DRIVING,OVERTAKING,BLOCKING }
		
	private static final int LOOK_AHEAD_POINTS=3;
	private static final float BRAKE_RELATIVE_DISTANCE=0.05f;
	private static final float LIFT_DISTANCE=100f;
	private static final float STEERING_EPSILON=0.05f;
	private static final float MIN_SPEED=8f;
	private static final float OVERTAKING_LINE_WIDTH=2f;
	private static final float OVERTAKING_LINE_LENGTH=50f;
	private static final float OVERTAKING_TIME=3f;
	private static final float BLOCKING_TIME=5f;
	private static final float MINIMUM_DISTANCE=15f;

	/**
	 * Creates a new AI contestant. The personality of the AI will be generated. A
	 * number of predefined personalities exist, and these are used to create an 
	 * infinite number of possible values.
	 */
	
	public AI(String id,Session session,Car car,Circuit circuit) {
		
		super(id,session,car,circuit);
	
		point=new Vector3D();
		line=AILine.IDEAL;
		behavior=AIBehavior.DRIVING;
		behaviorTime=0f;
		
		tempVector1=new Vector3D();
		tempVector2=new Vector3D();
		
		// Generate skill level
		
		switch (Settings.getInstance().aiLevel) {
			case Settings.AI_EASY : skill=0.4f; break;
			case Settings.AI_NORMAL : skill=0.6f; break;
			case Settings.AI_HARD : skill=0.8f; break;
			default : break;
		}
		
		skill+=random(0.2f);
		skill=Math.round(skill*10f)/10f;
		
		// Generate aggression
		
		aggression=0.2f;
		aggression+=random(0.8f);
		aggression=Math.round(aggression*10f)/10f;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	protected void updateControls(float dt) {
		
		setControl(Controls.ACCELERATOR,0);
		setControl(Controls.BRAKES,0);
		setControl(Controls.STEERING,0);
		setControl(Controls.GEARS,0);
	
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
	
	protected void updateAI(float dt) {
		
		int targetIndex=getTargetPoint();
		int focusIndex=getFocusPoint();		
		CircuitPoint target=getPointAt(targetIndex);
		CircuitPoint focus=getPointAt(focusIndex);
		
		if ((line==AILine.IDEAL) || (!target.isAltPoint())) {			
			point.setVector(target.pointX,0f,target.pointY);			
		} else {
			point.setVector(target.altX,0f,target.altY);
		}
			
		// Set steering
		
		tempVector1.setVector(getPosition());
		tempVector2.setVector(point);
		tempVector2.relativeTo(tempVector1,tempVector2,getOrientation(),'y');
						
		if (tempVector2.z<-STEERING_EPSILON) { setControl(Controls.STEERING,-10); }
		if (tempVector2.z>STEERING_EPSILON) { setControl(Controls.STEERING,10); }
		
		// Set accelerator and brakes
		
		float deltaSpeed=getSpeed(false)-focus.speed;
		float distance=getPointDistance(getPointAt(getPoint()),focus);
		
		if (deltaSpeed>distance*BRAKE_RELATIVE_DISTANCE) {
			setControl(Controls.BRAKES,10);
		} else {
			if (distance<LIFT_DISTANCE) {
				setControl(Controls.ACCELERATOR,(int) (skill*4f));
			} else {
				setControl(Controls.ACCELERATOR,10);
			}
		}
		
		// Update behavior
		
		AIBehavior newBehavior=AIBehavior.DRIVING;
		
		for (Contestant i : getSession().getContestants()) {
			if (i==this) {
				continue;
			}
		
			tempVector1.setVector(getPosition());
			tempVector2.setVector(i.getPosition());
			tempVector2.relativeTo(tempVector1,tempVector2,getOrientation(),'y');
			
			if ((behaviorTime>0f) && (isLineFront(tempVector2))) { 
				newBehavior=AIBehavior.OVERTAKING; 
			}
			
			if ((behaviorTime>0f) && (isLineBack(tempVector2))) { 
				newBehavior=AIBehavior.BLOCKING; 
			}
			
			if (isMinimumDistance(tempVector2)) {
				if (line==AILine.IDEAL) {
					line=AILine.OVERTAKING;
				} else {
					setControl(Controls.ACCELERATOR,0);
				}
			}
		}
		
		if (behaviorTime<=0f) {
			behavior=newBehavior;
			
			switch (behavior) {
				case DRIVING : behaviorTime=0f; line=AILine.IDEAL; break;
				case OVERTAKING : behaviorTime=getOvertakingBehaviorTime(); line=getOvertakingLine(); break;
				case BLOCKING : behaviorTime=getBlockingBehaviorTime(); line=getBlockingLine(); break;
				default : break;
			}
		} else {
			behaviorTime-=dt;
		}
		
		// Checks
		
		if ((getSpeed(false)<MIN_SPEED) && (getControl(Controls.ACCELERATOR)==0)) {
			setControl(Controls.ACCELERATOR,10);
			setControl(Controls.BRAKES,0);
		}
		
		if (!getCar().isOnTrack()) {
			setControl(Controls.ACCELERATOR,getSpeed(false)>MIN_SPEED ? 0 : 10);
			setControl(Controls.BRAKES,getSpeed(false)>MIN_SPEED ? 10 : 0);
		}
		
		if ((!getSession().isStarted()) || (getSession().isFinished())) {
			setControl(Controls.ACCELERATOR,0);
			setControl(Controls.BRAKES,0);
		}
		
		// Gear controls
		
		if ((getGear()<CAR_GEARS) && (getRPM()>=CAR_MAX_RPM)) { setControl(Controls.GEARS,1); }
		if ((getGear()>1) && (getRPM()<=CAR_MIN_RPM)) { setControl(Controls.GEARS,-1); }
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
	
	private float getPointDistance(CircuitPoint point1,CircuitPoint point2) {
		
		float deltaX=Math.abs(point2.pointX-point1.pointX);
		float deltaY=Math.abs(point2.pointY-point1.pointY);
		
		return (float) Math.sqrt(deltaX*deltaX+deltaY*deltaY);
	}
	
	/**
	 * Returns the index of the point to where this AI should steer. This is not 
	 * the closest point, but one slightly further ahead.
	 */
	
	private int getTargetPoint() {
	
		return getPoint()+LOOK_AHEAD_POINTS;
	}
	
	/**
	 * Returns the index of the closest point which contains AI data. This will 
	 * be used by this AI to know if it should accelerate, brake or lift. 
	 */
	
	private int getFocusPoint() {
		
		for (int i=getPoint(); i<getCircuit().getPoints().length; i++) {
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
			return ((p.x>-getLineLength()) && (Math.abs(p.z)<OVERTAKING_LINE_WIDTH));
		}
	}
	
	/**
	 * Returns if the specified vector is on the same line as this AI, behind it.
	 * The line is measured both in thickness and in length.
	 */
	
	private boolean isLineBack(Vector3D p) {
		
		if (p.x<0f) {
			return false;
		} else {
			return ((p.x<getLineLength()) && (Math.abs(p.z)<OVERTAKING_LINE_WIDTH));
		}
	}
	
	/**
	 * Returns if the specified point is within the minimum distance. When this
	 * is the case the AI car will attempt to avoid a collision at all cost.
	 */
	
	private boolean isMinimumDistance(Vector3D p) {
	
		return ((p.x<0f) && (p.x>-MINIMUM_DISTANCE) && (Math.abs(p.z)<OVERTAKING_LINE_WIDTH));
	}
	
	/**
	 * Returns the line length for this AI. Braver AIs will have a shorter length,
	 * while more cowardly AIs will have a longer length.
	 */
	
	private float getLineLength() {
		
		return (1.5f-aggression)*OVERTAKING_LINE_LENGTH;
	}
	
	/**
	 * Returns the overtaking behavior time for this AI. The time will be slightly 
	 * randomized depending on the personality of the AI.
	 */
	
	private float getOvertakingBehaviorTime() {
	
		return (2.5f-(skill+aggression))*OVERTAKING_TIME;
	}
	
	/**
	 * Returns the blocking behavior time for this AI. The time will be slightly 
	 * randomized depending on the personality of the AI.
	 */
	
	private float getBlockingBehaviorTime() {
	
		return (2.5f-(skill+aggression))*BLOCKING_TIME;
	}
	
	/**
	 * Returns the line on which the AI will attempt to overtake. Braver AIs will
	 * attempt to overtake around the outside more often. 
	 */
	
	private AILine getOvertakingLine() {
	
		return (line==AILine.IDEAL) ? AILine.OVERTAKING : AILine.IDEAL;
	}
	
	/**
	 * Returns the line on which the AI will attempt to block overtaking attempts.
	 * More skillfull AIs will attempt to block the inside line.
	 */
	
	private AILine getBlockingLine() {
	
		if (chance(0.5f)) {
			return (line==AILine.IDEAL) ? AILine.OVERTAKING : AILine.IDEAL;
		} else {
			return line;
		}
	}
		
	/**
	 * Returns a random numer between 0 and <code>max</code>. The returned number 
	 * will be in float precision. 
	 */
	
	private float random(float max) {
		
		return (float) Math.random()*max;
	}
	
	/**
	 * Returns a random number between 0 and 1. Calling this method is the 
	 * equivalent of calling <code>random(1f)</code>.
	 */
	
	private float random() {
	
		return random(1f);
	}
	
	/**
	 * Returns a random decision based on the specified chance. A chance of 1
	 * will always return true, one of 0 will always return false.
	 */
	
	private boolean chance(float factor) {
	
		return (random()>1f-factor);
	}
}
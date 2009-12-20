//-----------------------------------------------------------------------------
// Ferrari3D
// Player
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * A human-controlled {@code Contestant}. The contestant is controller via
 * keyboard input. The exact controls are dependant on the settings.
 * <p>
 * This class also contains some extra checks to counter cheating and corner
 * cutting.
 */

public class Player extends Contestant {
	
	private Controller controller;
	private int[] controlset;
	private boolean autoGears;
	private boolean autoReverse;
	
	private float warningTime;
	private float penaltyTime;
	
	/**
	 * Creates a new {@code Player} that will use the specified controller.
	 * @param controller The object that will provide input for this player.
	 * @param controlset The control keys, see {@link Settings#getControlSet()}.
	 */
	
	public Player(String name, Session session, Controller controller, int[] controlset) {
		
		super(name, session);

		this.controller = controller;
		this.controlset = controlset;
		this.autoGears = Settings.getInstance().autoGears;
		this.autoReverse = Settings.getInstance().autoReverse;
		
		warningTime = 0f;
		penaltyTime = 0f;
	}
	
	/**
	 * Sets the controls for this contestant based on keyboard input during the
	 * last frame.
	 */
	
	@Override
	protected void updateControls(float dt) {
		
		// Set all controls to default values
	
		CarPhysics physics = getCarPhysics();
		physics.setAccelerator(0f);
		physics.setBrakes(0f);
		physics.setSteering(0f);
		physics.setGearChange(0);
		
		// Driving controls
						
		if (controller.isKeyPressed(controlset[0])) { physics.setAccelerator(1f); }
		if (controller.isKeyPressed(controlset[1])) { physics.setBrakes(1f); }
		if (controller.isKeyPressed(controlset[2])) { physics.setSteering(-1f); }
		if (controller.isKeyPressed(controlset[3])) { physics.setSteering(1f); }
		
		// Gear controls
		
		if (autoGears) {
			if ((getGear() < physics.CAR_GEARS) && (getRPM() >= physics.CAR_MAX_RPM)) { 
				physics.setGearChange(1); 
			}
			if ((getGear() > 1) && (getRPM() <= physics.CAR_MIN_RPM)) { 
				physics.setGearChange(-1); 
			}
		} else {
			if (controller.isKeyReleased(controlset[4])) { physics.setGearChange(1); }
			if (controller.isKeyReleased(controlset[5])) { physics.setGearChange(-1); }			
		}
		
		// Reverse controls
		
		if ((autoReverse) && (getSpeed() <= 0f)) {
			if ((physics.getAccelerator() > 0f) && (getGear() < 1)) {
				physics.setGearChange(1);
			}
			
			if (physics.getBrakes() > 0f) {
				physics.setGearChange(-1);
				if (getGear() == -1) { 
					physics.setAccelerator(1f);
				}
			}
		}
		
		// Checks
		
		if (!getSession().isStarted() || getSession().isFinished()) {
			physics.setAccelerator(0f);
			physics.setBrakes(0f);
		}
		
		// Penalty
		
		if ((!getCar().isOnTrack()) && (getSpeed() > WARNING_SPEED)) {
			warningTime += dt;
			if (warningTime >= WARNING_TIME) {
				penaltyTime = PENALTY_TIME;
				warningTime = 0f;
			}
		} else {
			if (warningTime > 0f) {
				warningTime -= dt;
			}
		}
		
		if (penaltyTime > 0f) {
			penaltyTime -= dt;
			physics.setAccelerator(0f);
		}
	}
	
	/**
	 * Returns if this contestant is currently having a penalty. During this time
	 * the accelerator's effect will be reduced.
	 */
	
	public boolean isPenalty() {
		return (penaltyTime > 0f);
	}
}

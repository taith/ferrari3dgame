//--------------------------------------------------------------------------------
// Ferrari3D
// Player
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.game.Controller;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * A human-controlled <code>Contestant</code>. The contestant is controller via
 * keyboard input. The exact controls are dependant on the settings.<br><br>
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
	
	private static final float WARNING_TIME=3f;
	private static final float WARNING_SPEED=40f;
	private static final float PENALTY_TIME=10f;
	
	/**
	 * Creates a new <code>Player</code>. The player will be controlled using
	 * the specified keys.
	 */
	
	public Player(String id,Session session,Car car,Circuit circuit,Controller controller,int[] controlset) {
		
		super(id,session,car,circuit);

		this.controller=controller;
		this.controlset=controlset;
		this.autoGears=Settings.getInstance().autoGears;
		this.autoReverse=Settings.getInstance().autoReverse;
		
		warningTime=0f;
		penaltyTime=0f;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void updateControls(float dt) {
	
		setControl(Controls.ACCELERATOR,0);
		setControl(Controls.BRAKES,0);
		setControl(Controls.STEERING,0);
		setControl(Controls.GEARS,0);
		
		// Driving controls
						
		if (controller.isKeyPressed(controlset[0])) { setControl(Controls.ACCELERATOR,10); }
		if (controller.isKeyPressed(controlset[1])) { setControl(Controls.BRAKES,10); }
		if (controller.isKeyPressed(controlset[2])) { setControl(Controls.STEERING,-10); }
		if (controller.isKeyPressed(controlset[3])) { setControl(Controls.STEERING,10); }
		
		// Gear controls
		
		if (autoGears) {
			if ((getGear()<CAR_GEARS) && (getRPM()>=CAR_MAX_RPM)) { setControl(Controls.GEARS,1); }
			if ((getGear()>1) && (getRPM()<=CAR_MIN_RPM)) { setControl(Controls.GEARS,-1); }
		} else {
			if (controller.isKeyReleased(controlset[4])) { setControl(Controls.GEARS,1); }
			if (controller.isKeyReleased(controlset[5])) { setControl(Controls.GEARS,-1); }			
		}
		
		// Reverse controls
		
		if ((autoReverse) && (getSpeed(false)<=0f)) {
			if ((getControl(Controls.ACCELERATOR)!=0) && (getGear()<1)) {
				setControl(Controls.GEARS,1);
			}
			
			if (getControl(Controls.BRAKES)!=0) {
				setControl(Controls.GEARS,-1);
				if (getGear()==-1) { 
					setControl(Controls.ACCELERATOR,10);
				}
			}
		}
		
		// Checks
		
		if ((!getSession().isStarted()) || (getSession().isFinished())) {
			setControl(Controls.ACCELERATOR,0);
			setControl(Controls.BRAKES,0);
		}
		
		// Penalty
		
		if ((!getCar().isOnTrack()) && (getSpeed(false)>WARNING_SPEED)) {
			warningTime+=dt;
			if (warningTime>=WARNING_TIME) {
				penaltyTime=PENALTY_TIME;
				warningTime=0f;
			}
		} else {
			if (warningTime>0f) {
				warningTime-=dt;
			}
		}
		
		if (penaltyTime>0f) {
			penaltyTime-=dt;
			setControl(Controls.ACCELERATOR,0);
		}
	}
	
	/**
	 * Returns if this contestant is currently having a penalty. During this time
	 * the accelerator's effect will be reduced.
	 */
	
	public boolean isPenalty() {
		
		return (penaltyTime>0f);
	}
}
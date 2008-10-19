//--------------------------------------------------------------------------------
// Ferrari3D
// Player
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.game.InputController;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * A human-controlled <code>Contestant</code>. Controls data is received from
 * input devices such as the keyboard. Some settings might be automated depending
 * on the game configuration.
 */

public class Player extends Contestant {
	
	private InputController controller;
	private String[] controlset;
	private boolean autoGears;
	private boolean autoReverse;
	
	/**
	 * Creates a new <code>Contestant</code>. A number of control options needs to
	 * be specified, which will determine the control style.
	 * @param controller The input device that controls this player.
	 * @param controlset The control set, which can be changed from the menu.
	 * @param autoGears If true, gear switching will be done automatically.
	 * @param autoReverse If ture, selecting reverse will be done automatically.
	 */
	
	public Player(String id,Session session,Car car,Circuit circuit,InputController controller) {
		
		super(id,session,car,circuit);

		this.controller=controller;
		this.controlset=Settings.getInstance().getControlSet();
		this.autoGears=Settings.getInstance().autoGears;
		this.autoReverse=Settings.getInstance().autoReverse;
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
	}
}
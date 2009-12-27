//-----------------------------------------------------------------------------
// Ferrari3D
// Drone
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.messaging.Message;

/**
 * A placeholder {@code Contestant} which represents a multiplayer peer. This
 * class listens to update messages coming from the other computer, and updates 
 * all data accordingly. When no new messages are available an attempt is done 
 * to interpolate the position based on past data. 
 */
public class Drone extends Contestant implements MessageListener {
	
	private boolean receivedThisFrame;
	
	/**
	 * Creates a new {@code Drone}.
	 */
	public Drone(String name, Session session) {
		super(name, session);
		receivedThisFrame = false;
	}
	
	/**
	 * Empty implementation, as this class receives its input from multiplayer
	 * messages.
	 */
	@Override
	protected void updateControls(float dt) {
		
	}
	
	/**
	 * If a message was received this frame, this method does nothing. Otherwise
	 * and attempt is done to interpolate the position based on data received in
	 * the past.
	 */
	@Override
	protected void updatePhysics(float dt) {
		if (receivedThisFrame) {
			receivedThisFrame = false;
			return;
		}
		super.updatePhysics(dt);
	}
	
	/**
	 * Empty implementation, as this class receives its input from multiplayer
	 * messages.
	 */
	@Override
	protected void updateLapData(float dt) {
		
	}
	
	/**
	 * Invoked when a message is received from the server. This class will only
	 * listen for update messages.
	 */
	public void messageReceived(Message message) {
		
		if (!message.getType().equals(Multiplayer.MESSAGE_UPDATE) || 
				!getName().equals(message.getParameter("id"))) {
			return;
		}
		
		setPosition(parseVector(message.getParameter("position")));
		setOrientation(Float.parseFloat(message.getParameter("orientation")));
		setDirection(Float.parseFloat(message.getParameter("direction")));
		setLap(Integer.parseInt(message.getParameter("lap")));
		setIntermediate(Integer.parseInt(message.getParameter("intermediate")));
		setPoint(Integer.parseInt(message.getParameter("point")));
		parseLaptime(getLap(), message.getParameter("laptime"));
		setSpeed(Float.parseFloat(message.getParameter("speed")));
		setAngularSpeed(Float.parseFloat(message.getParameter("angularSpeed")));
		
		receivedThisFrame = true;
	}
	
	private Vector3D parseVector(String message) {
		String[] parts = message.split("x");
		return new Vector3D(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]),
				Float.parseFloat(parts[2]));
	}
	
	private void parseLaptime(int index, String message) {
		if (getLaptime(index) != null) {
			int time = Integer.parseInt(message);
			getLaptime(index).setTime(time);
		}
	}
}

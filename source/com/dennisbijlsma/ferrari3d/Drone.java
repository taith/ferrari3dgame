//--------------------------------------------------------------------------------
// Ferrari3D
// Drone
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.GameEventListener;
import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.xmlserver.Message;

/**
 * A placeholder <code>Contestant</code> which represents a multiplayer peer. This
 * class listens to update messages coming from the other computer, and updates 
 * all data accordingly. When no new messages are available an attempt is done to
 * interpolate data.
 */

public class Drone extends Contestant implements GameEventListener {
	
	/**
	 * Creates a new {@code Drone}.
	 */
	
	public Drone(String id, Session session, CarPhysics physics, Car car, Circuit circuit) {
		super(id, session, physics, car, circuit);
	}
	
	/**
	 * Empty implementation, as this class receives its input from multiplayer
	 * messages.
	 */

	@Override
	protected void updateControls(float dt) { }
	
	/**
	 * Empty implementation, as this class receives its input from multiplayer
	 * messages.
	 */

	@Override
	protected void updatePhysics(float dt) { }
	
	/**
	 * Empty implementation, as this class receives its input from multiplayer
	 * messages.
	 */
	
	@Override
	protected void updateLapData(float dt) { }
	
	/** {@inheritDoc} */
	
	public void onGameEvent(String type, Object source) {
		
		Message message = (Message) source;
		String messageType = message.getType();
		String id = message.getParameter("id");
		
		if ((getName().equals(id)) && (messageType.equals(Multiplayer.MESSAGE_UPDATE))) {
			setPosition(parseVector(message.getParameter("position")));
			setOrientation(Float.parseFloat(message.getParameter("orientation")));
			setDirection(Float.parseFloat(message.getParameter("direction")));
			setLap(Integer.parseInt(message.getParameter("lap")));
			setIntermediate(Integer.parseInt(message.getParameter("intermediate")));
			setPoint(Integer.parseInt(message.getParameter("point")));
			parseLaptime(getLap(),message.getParameter("laptime"));
			setSpeed(Float.parseFloat(message.getParameter("speed")));
			setAngularSpeed(Float.parseFloat(message.getParameter("angularSpeed")));
		}
	}
	
	/**
	 * Parses a <code>Vector3D</code> from a string.
	 */
	
	private Vector3D parseVector(String message) {
		String[] temp = message.split(",");
		Vector3D v = new Vector3D();
		v.x = Float.parseFloat(temp[0]);
		v.y = Float.parseFloat(temp[1]);
		v.z = Float.parseFloat(temp[2]);
		return v;
	}
	
	/**
	 * Parses a <code>Laptime</code> from a string.
	 */
	
	private void parseLaptime(int index,String message) {
		if (getLaptime(index) != null) {
			getLaptime(index).setTime(Integer.parseInt(message));
		}
	}
}
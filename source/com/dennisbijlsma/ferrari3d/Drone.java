//--------------------------------------------------------------------------------
// Ferrari3D
// Drone
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.Map;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.GameEvent;
import com.dennisbijlsma.core3d.game.GameEventListener;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.xmlserver.Message;

/**
 * A placeholder <code>Contestant</code> which represents a multiplayer peer. This
 * class listens to update messages coming from the other computer, and updates 
 * all data accordingly. When no new messages are available an attempt is done to
 * interpolate data.
 */

public class Drone extends Contestant implements GameEventListener {
	
	/**
	 * Creates a new <code>Drone</code>. The drone will register itself with the
	 * multiplayer handler, and will listen for any new connections. 
	 */
	
	public Drone(String id,Session session,Car car,Circuit circuit) {
		
		super(id,session,car,circuit);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void updateControls(float dt) {
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void updatePhysics(float dt) {
		
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void updateLapData(float dt) {
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public void onGameEvent(GameEvent event) {
		
		String type=((Message) event.getSource()).getType();
		Map<String,String> parameters=((Message) event.getSource()).getAllParameters();
		String id=parameters.get("id");
		
		if ((getID().equals(id)) && (type.equals(Multiplayer.MESSAGE_UPDATE))) {
			parseVector3D(getPosition(),parameters.get("position"));
			setOrientation(Float.parseFloat(parameters.get("orientation")));
			setDirection(Float.parseFloat(parameters.get("direction")));
			setLap(Integer.parseInt(parameters.get("lap")));
			setIntermediate(Integer.parseInt(parameters.get("intermediate")));
			setPoint(Integer.parseInt(parameters.get("point")));
			parseLaptime(getLap(),parameters.get("laptime"));
			setSpeed(Float.parseFloat(parameters.get("speed")));
			setAngular(Float.parseFloat(parameters.get("angular")));
		}
		
		System.out.println(getAngular());
	}
	
	/**
	 * Parses a <code>Vector3D</code> from a string.
	 */
	
	private void parseVector3D(Vector3D p,String message) {
	
		String[] temp=message.split(",");
		p.x=Float.parseFloat(temp[0]);
		p.y=Float.parseFloat(temp[1]);
		p.z=Float.parseFloat(temp[2]);
	}
	
	/**
	 * Parses a <code>Laptime</code> from a string.
	 */
	
	private void parseLaptime(int index,String message) {
			
		if (getLaptime(index)!=null) {
			getLaptime(index).setLaptime(Integer.parseInt(message));
		} else {
			Settings.getInstance().getLogger().warning(getID()+": lap data update for nonexisting lap: "+index);
		}
	}
	
	/**
	 * Interpolates the position and rotation of the car, bases on data received
	 * from previous update messages. When the network speed it too slow, this 
	 * method will be called too often, and the car movement will appear odd.
	 */
	
	private void interpolate(float dt) {
	
		super.updatePhysics(dt);
	}
}
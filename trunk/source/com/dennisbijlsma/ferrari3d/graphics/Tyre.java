//--------------------------------------------------------------------------------
// Ferrari3D
// Tyre
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.Transform3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;

/**
 * Controls the visual representation of a car tyre. This class is controlled by 
 * the <code>Car</code> class, which controls all four tyres of a car. The position
 * of the tyre is represented by the <code>CarPosition</code> enum. 
 */

public class Tyre {

	private Model node;
	private TyrePosition position;
	
	private SceneGraphNode circuitNode;
	private Vector3D pickVector;
	
	private static final float PICK_HEIGHT = 1f;
	private static final Vector3D PICK_DIRECTION_FLOOR = new Vector3D(0f, -1f, 0f);
	
	public enum TyrePosition { 
		LEFT_FRONT,
		RIGHT_FRONT,
		LEFT_REAR,
		RIGHT_REAR 
	}
	
	/**
	 * Creates a new <code>Tyre</code> from the specified scene graph node.
	 * @param node The visual represenattion of the tyre.
	 * @param position The position of this tyre on the car.
	 * @param circuitNode The node to perform picking operations on.
	 */
	
	public Tyre(Model node, TyrePosition position, SceneGraphNode circuitNode) {
		
		if ((node == null) || (position == null)) {
			throw new IllegalArgumentException("Invalid tyre node");
		}
		
		this.node = node;
		this.position = position;
		this.circuitNode = circuitNode;
		pickVector = new Vector3D();
	}
	
	/**
	 * Returns the distance between this tyre and the floor. If the floor cannot
	 * be detected this method will return null.
	 */
	
	public float pickCircuit() {

		Transform3D worldTransform = node.getWorldTransform();
		Vector3D worldPosition = worldTransform.getPosition();
		pickVector.setVector(worldPosition.x, PICK_HEIGHT, worldPosition.z);		
		return circuitNode.pickDistance(pickVector, PICK_DIRECTION_FLOOR, true);
	}
	
	/**
	 * Returns the scene graph node that represents the tyre.
	 */
	
	public Model getNode() {
		return node;
	}
	
	/**
	 * Returns the position of this tyre.
	 */
	
	public TyrePosition getTyrePosition() {
		return position;
	}
}
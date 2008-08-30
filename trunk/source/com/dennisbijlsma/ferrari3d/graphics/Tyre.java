//--------------------------------------------------------------------------------
// Ferrari3D
// Tyre
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.*;

/**
 * Controls the visual representation of a car tyre. This class is controlled by 
 * the <code>Car</code> class, which controls all four tyres of a car. The position
 * of the tyre is represented by the <code>CarPosition</code> enum. 
 */

public class Tyre {

	private Model node;
	private TyrePosition position;
	
	private Picker picker;
	private Vector3D pickVector;
	
	public static enum TyrePosition { LEFT_FRONT,RIGHT_FRONT,LEFT_REAR,RIGHT_REAR }
	
	private static final float PICK_HEIGHT=1f;
	private static final Vector3D PICK_DIRECTION_FLOOR=new Vector3D(0f,-1f,0f);
	
	/**
	 * Creates a new <code>Tyre</code> from the specified scene graph node.
	 * @param node The visual represenattion of the tyre.
	 * @param position The position of this tyre on the car.
	 * @param picker Needed to perform picking operations from the tyre.
	 */
	
	public Tyre(Model node,TyrePosition position,Picker picker) {
		
		if ((node==null) || (position==null)) {
			throw new IllegalArgumentException("Invalid tyre node");
		}
		
		this.node=node;
		this.position=position;
		this.picker=picker;
		
		pickVector=new Vector3D();
	}
	
	/**
	 * Returns the distance between this tyre and the floor.
	 */
	
	public float pickCircuit() {

		Matrix3D worldTransform=node.getWorldTransform();
		Vector3D worldPosition=worldTransform.getPosition();
		pickVector.setVector(worldPosition.x,PICK_HEIGHT,worldPosition.z);
		
		return picker.pickDistance(pickVector,PICK_DIRECTION_FLOOR);
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
	
	/**
	 * Returns true when this tyre is at the front of the car. This is a 
	 * convenience method for checking the tyre position manually.
	 */
	
	public boolean isFrontTyre() {
	
		return ((position==TyrePosition.LEFT_FRONT) || (position==TyrePosition.RIGHT_FRONT));
	}
}
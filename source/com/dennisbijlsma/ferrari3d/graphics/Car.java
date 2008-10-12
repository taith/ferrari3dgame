//--------------------------------------------------------------------------------
// Ferrari3D
// Car
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.*;
import java.util.*;

import com.dennisbijlsma.core3d.data.*;
import com.dennisbijlsma.core3d.scene.*;
import com.dennisbijlsma.ferrari3d.util.*;

/**
 * The <code>Car</code> class is the visual representation of a player car. It 
 * controls movement of the scene graph node, as well as checking how it interacts
 * with other cars and the circuit itself.
 */

public class Car extends Model {
	
	private SceneGraphNode geometry;
	private Tyre tyreLF;
	private Tyre tyreRF;
	private Tyre tyreLR;
	private Tyre tyreRR;
	private Model steeringWheel;
	private Model rearLight;
	private Model helmet;
	private Model shadow;
		
	private String name;
	private HashMap<String,String> info;
	private Vector3D[] cameras;
	
	private SceneGraphNode circuitNode;
	private Vector3D pickVector;
	private Vector3D pickVectorDir;
	private boolean isOnTrack;
	private boolean isOnFloor;
	private float gravity;
	
	private boolean isRearLight;
	private float smokeTimer;
	
	public static final int CAMERA_COCKPIT=1;
	public static final int CAMERA_T_CAM=2;
	public static final int CAMERA_FOLLOW=3;
	public static final int CAMERA_CHASE=4;
	public static final int CAMERA_HELICOPTER=5;
	
	private static final float PICK_HEIGHT=1f;
	private static final Vector3D PICK_DIRECTION_FLOOR=new Vector3D(0f,-1f,0f);
	private static final float REFERENCE_PLANE=1f;
	private static final float WHEEL_BASE=1.5f;
	private static final float COLLISION_DISTANCE=1.8f;
	private static final float COLLISION_HEIGHT=0.3f;
	private static final float EPSILON=0.03f;
	
	private static final float WHEEL_LONGITUDINAL=8f;
	private static final float WHEEL_LATERAL=0.3f;
	private static final Color REAR_LIGHT_ON=new Color(100,0,0);
	private static final Color REAR_LIGHT_OFF=new Color(150,150,150);
	private static final Image STEERING_WHEEL_TEXTURE=Utils.loadImage("data/graphics/steeringwheel.png");
	private static final Vector3D SHADOW_SIZE=new Vector3D(3.4f,1.8f,0f);
	private static final Image SHADOW_TEXTURE=Utils.loadImage("data/graphics/shadow.png");
	private static final float SHADOW_OFFSET=0.01f;
	private static final Image SMOKE_TEXTURE=Utils.loadImage("data/graphics/smoke.png");
	private static final float SMOKE_COOLDOWN_TIME=3f;
	private static final float SMOKE_ACCELERATE_SPEED=15f;
	private static final float SMOKE_LOCK_SPEED=50f;
	private static final float BRAKING_CAMBER_POS=0.02f;
	private static final float BRAKING_CAMBER_ROT=-0.01f;
	private static final float BRAKING_CAMBER_SPEED=50f;
	private static final float WOBBLE_FACTOR=0.002f;
	private static final float STEERING_WHEEL_OFFSET=0.5f;
	
	/**
	 * Creates a new <code>Car</code> with the specified geometry. Not all meta
	 * fields of the object will be set when using the constructor.
	 * @param geometry The geometry node of the car.
	 * @param lod The reduced geometry (LOD) node.
	 * @param circuitNode The node in the scene to perform picking operations on.
	 */
	
	public Car(SceneGraphNode geometry,LODNode lod,SceneGraphNode circuitNode) {
		
		super("Car");
		
		this.geometry=geometry;
		
		addChild(lod);
		
		this.circuitNode=circuitNode;
		circuitNode.setPickMode(SceneGraphNode.PickMode.GEOMETRY);
		pickVector=new Vector3D();
		pickVectorDir=new Vector3D();
		isOnTrack=true;
		isOnFloor=true;
		gravity=0f;
		
		// Create geometry
		
		tyreLF=new Tyre((Model) geometry.getChild("wheelLF"),Tyre.TyrePosition.LEFT_FRONT,circuitNode);
		tyreRF=new Tyre((Model) geometry.getChild("wheelRF"),Tyre.TyrePosition.RIGHT_FRONT,circuitNode);
		tyreLR=new Tyre((Model) geometry.getChild("wheelLR"),Tyre.TyrePosition.LEFT_REAR,circuitNode);
		tyreRR=new Tyre((Model) geometry.getChild("wheelRR"),Tyre.TyrePosition.RIGHT_REAR,circuitNode);
		steeringWheel=(Model) geometry.getChild("steeringWheel");
		rearLight=(Model) geometry.getChild("rearLight");
		helmet=(Model) geometry.getChild("helmet");
		
		steeringWheel.applyTexture(STEERING_WHEEL_TEXTURE);
		
		shadow=new Primitive("shadow",Primitive.ShapeType.QUAD,SHADOW_SIZE);
		shadow.getTransform().getRotation().y=1.57f;
		shadow.applyTexture(SHADOW_TEXTURE);
		shadow.applyBlend();
		addChild(shadow);
		
		// Initialize
		
		isRearLight=true;
		smokeTimer=0f;
	}
	
	/**
	 * Picks for collisions between this car and the circuit's floor. The
	 * returned value will be the distance between this graphic and the floor, or
	 * 0 when no collision was found.
	 */
	
	private float pickCircuit() {
	
		Vector3D position=getTransform().getPosition();
		pickVector.setVector(position.x,PICK_HEIGHT,position.z);
		
		return circuitNode.pickDistance(pickVector,PICK_DIRECTION_FLOOR);
	}
	
	/**
	 * Picks for collisions between this car and other objects. The picking 
	 * will be performed between the car's current location in the specified
	 * direction. The returned value is the distance to the collision.
	 */
	
	private float pickObject(Vector3D direction) {
		
		return circuitNode.pickDistance(pickVector,pickVectorDir);
	}
	
	/**
	 * Updates the cars physics for this frame. This involves multiple pick
	 * operations, to check the distance between the car and the ground, to
	 * update gravity, and to check for collisions against any objects. After
	 * this method has been called the results are accesible via <code>isOnTrack()
	 * </code>, <code>isOnFloor()</code>, and <code>isCollidingObject()</code>.
	 * @param dt Delta time for this frame.
	 * @param speed The current speed of the car, in metres per second.
	 * @param fullDetail When false, perform only basic operations.
	 */
	
	public void updatePhysics(float dt,float speed) {
		
		Vector3D position=getTransform().getPosition();
		Vector3D geometryPosition=geometry.getTransform().getPosition();
		Vector3D geometryRotation=geometry.getTransform().getRotation();
		
		// Picking
		
		float pickCar=pickCircuit();
		float pickLF=tyreLF.pickCircuit();
		float pickRF=tyreRF.pickCircuit();
		float pickLR=tyreLR.pickCircuit();
		float pickRR=tyreRR.pickCircuit();	
		
		// Terrain following
		
		isOnTrack=(pickCar<REFERENCE_PLANE+EPSILON);
		
		float referenceY=PICK_HEIGHT-(pickLF+pickRF+pickLR+pickRR)/4f;
		
		float frontY=(pickLF+pickRF)/2f;
		float rearY=(pickLR+pickRR)/2f;
		float longitudinal=(float) Math.tan((rearY-frontY)/WHEEL_BASE);
		
		float leftY=(pickLF+pickLR)/2f;
		float rightY=(pickLR+pickRR)/2f;
		float lateral=(float) Math.tan((leftY-rightY)/(WHEEL_BASE/2f));
		
		geometryRotation.x=longitudinal;
		geometryRotation.z=lateral;
		shadow.getTransform().getPosition().y=referenceY-position.y+SHADOW_OFFSET;
		
		// Gravity
		
		isOnFloor=(Math.abs(position.y-referenceY)<EPSILON);
		position.y=referenceY;
	}
	
	/**
	 * Updates the postions for all wheels. This involves both longitudinal
	 * movement (the turning of the wheels) as well as lateral movement (because
	 * the wheels may move depending on steering movement).
	 * @param dt Delta time for this frame.
	 * @param speed The current speed (in m/s).
	 * @param steering The signum of the current steering movement.
	 */
	
	public void updateWheels(float dt,float speed,int steering) {
		
		// Longitudinal
		
		float wheelSpeedLong=(speed*dt)*WHEEL_LONGITUDINAL;
		tyreLF.getNode().getTransform().getRotation().x+=wheelSpeedLong;
		tyreRF.getNode().getTransform().getRotation().x+=wheelSpeedLong;
		tyreLR.getNode().getTransform().getRotation().x+=wheelSpeedLong;
		tyreRR.getNode().getTransform().getRotation().x+=wheelSpeedLong;
		
		// Lateral
		
		float wheelLateral=-Math.signum(steering)*WHEEL_LATERAL;
		tyreLF.getNode().getTransform().getRotation().y=wheelLateral;
		tyreRF.getNode().getTransform().getRotation().y=wheelLateral;
		helmet.getTransform().getRotation().y=wheelLateral;
	}
	
	/**
	 * Updates the state of the rear light. There is a number of possible
	 * situations when the rear light should be on, the most common is when the
	 * car is braking.
	 * @param enabled True if the light should be on, false when off.
	 */
	
	public void updateRearLight(boolean enabled) {
		
		if (isRearLight!=enabled) {
			isRearLight=enabled;
			rearLight.applyColor(isRearLight ? REAR_LIGHT_ON : REAR_LIGHT_OFF);
		}
	}
	
	/**
	 * Updates the state of the smoke coming off the car's tyres.
	 * @param dt Delta time for this frame.
	 * @param speed The speed of the car in metres per second.
	 * @param accelerating True when the car is accelerating.
	 * @param braking True when the car is braking.
	 * @param wheel Negative values for the left wheel, positive for the right.
	 */
	
	public void updateSmoke(float dt,float speed,boolean accelerating,boolean braking,int wheel) {
		
		boolean lockedAccel=((speed<SMOKE_ACCELERATE_SPEED) && (speed>0f) && (accelerating));
		boolean lockedBraking=((speed>SMOKE_LOCK_SPEED) && (braking));
		boolean isSmoke=(lockedAccel || lockedBraking);
		
		// Update timer
		
		if (isSmoke) {
			smokeTimer=0f;
		} else {
			smokeTimer+=dt;
			if (smokeTimer>SMOKE_COOLDOWN_TIME) {
				isSmoke=true;
				smokeTimer=0f;
			}
		}
		
		// Update smoke appearance
		
		//TODO
	}
	
	/**
	 * Updates miscellaneous other effects. This method is optional, and could be
	 * used only for certain cars to optimize performance. Currently the following
	 * effects are used:<br><br>
	 * <ul><li>Braking camber</li><li>Off-track wobbling</li><li>Steering wheel
	 * movement</li></ul>
	 * @param dt Delta time for this frame.
	 * @param speed The current speed of the car in metres per second.
	 * @param braking True if the car is currently braking.
	 * @param steering The steering movement's signum, negative values for left.
	 */
	
	public void updateEffects(float dt,float speed,boolean braking,int steering) {
		
		// Braking camber
		
		if ((braking) && (speed>BRAKING_CAMBER_SPEED)) {
			getTransform().getPosition().y-=BRAKING_CAMBER_POS;
			geometry.getTransform().getRotation().x=BRAKING_CAMBER_ROT;
			helmet.getTransform().getRotation().x=BRAKING_CAMBER_ROT*2;
		}
	
		// Off-track wobbling
		
		if (!isOnTrack) {
			float wobbleFactor=WOBBLE_FACTOR*speed;
			float wobble=(float) Math.random()*wobbleFactor-wobbleFactor/2f;
			geometry.getTransform().getRotation().x=wobble;
		}
		
		// Steering wheel
		
		float steerOffset=STEERING_WHEEL_OFFSET*-Math.signum(steering);
		steeringWheel.getTransform().getRotation().z=steerOffset;
	}
	
	/**
	 * Returns true if the car is currently driving on the track. 
	 */
	
	public boolean isOnTrack() {
	
		return isOnTrack;
	}
	
	/**
	 * Returns true when the car is currently positioned on the floor.
	 */
	
	public boolean isOnFloor() {
	
		return isOnFloor;
	}
	
	/**
	 * Returns if an object exists between the car's current position and the
	 * specified new position.
	 */
	
	public boolean isCollidingObject(float newX,float newY,float newZ,float newRot) {
		
		Vector3D position=getTransform().getPosition();
		pickVector.setVector(position.x,position.y+COLLISION_HEIGHT,position.z);
		pickVectorDir.setVector(newX-position.x,0f,newZ-position.z);
		pickVectorDir.normalize();
		
		float pick=pickObject(pickVectorDir);
		
		return ((pick>0f) && (pick<COLLISION_DISTANCE));
	}
	
	/**
	 * Returns if the car will collide with the specified other car. The collision
	 * is checked for between the car's current position and the specified new one.
	 */
	
	public boolean isCollidingCar(float newX,float newY,float newZ,float newRot,Car otherCar) {
	
		Vector3D otherPosition=otherCar.getTransform().getPosition();
		float deltaX=Math.abs(newX-otherPosition.x);
		float deltaZ=Math.abs(newZ-otherPosition.z);
		
		return (Math.sqrt(deltaX*deltaX+deltaZ*deltaZ)<COLLISION_DISTANCE);
	}
		
	/**
	 * Sets the car's name. This is both the display name and the name with
	 * which the car is identified.
	 */
	
	public void setCarName(String name) {
	
		this.name=name;
	}
	
	/**
	 * Returns the car's name. This is both the display name and the name with
	 * which the car is identified.
	 */
	
	public String getCarName() {
	
		return name;
	}
	
	/**
	 * Sets all meta data for this car. The meta data is passed as a map with
	 * key/value pairs.
	 */
	
	public void setInfo(HashMap<String,String> info) {
	
		this.info=info;
	}
	
	/**
	 * Returns the car's meta data with the specified key. When no such meta data
	 * is available, this method will return <code>null</code>.
	 */
	
	public String getInfo(String key) {
	
		return info.get(key);
	}
	
	/**
	 * Sets all cameras for this car. Initially no camera will be selected as the
	 * selected camera.
	 */
	
	public void setCameras(Vector3D[] cameras) {
	
		this.cameras=cameras;
	}
	
	/**
	 * Returns the camera at the specified index. When a camera at the index does
	 * not exist this method returns <code>null</code>. 
	 */
	
	public Vector3D getCamera(int index) {
		
		if ((cameras==null) || (index<0) || (index>=cameras.length)) {
			return null;
		}
	
		return cameras[index];
	}
	
	/**
	 * Should be called whenever the selected camera was changed. The car's 
	 * appearance might differ depending on which camera is used.
	 */
	
	public void cameraChanged(int activeCamera) {
		
		boolean showHelmet=((activeCamera!=CAMERA_COCKPIT) && (activeCamera!=CAMERA_T_CAM));
		helmet.setVisible(showHelmet);
	}
	
	/**
	 * Returns the current gravity speed for this car. This value is only used for
	 * graphical purposes and should not affect gameplay.
	 */
	
	public float getGravity() {
		
		return gravity;
	}
}
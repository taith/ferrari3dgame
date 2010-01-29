//-----------------------------------------------------------------------------
// Ferrari3D
// Car
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.Color3D;
import com.dennisbijlsma.core3d.Sound;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.BufferedImageTexture;
import com.dennisbijlsma.core3d.scene.LODNode;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.core3d.scene.SceneGraphGroupNode;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * The {@code Car} class is the visual representation of a player car. It controls
 * movement of the scene graph node, as well as checking how it interacts with 
 * other cars and the circuit itself.
 */
public class Car extends AbstractCar {
	
	private Model model;
	private LODNode lodNode;
	private SceneGraphNode geometry;
	private Model bodywork;
	private Model tyreLF;
	private Model tyreRF;
	private Model tyreLR;
	private Model tyreRR;
	private Model steeringWheel;
	private Model rearLight;
	private Model helmet;
	private Model shadow;
	
	private SceneGraphNode circuitNode;
	private Vector3D pickVector;
	private Vector3D pickVectorDir;
	private boolean isOnTrack;
	private boolean isOnFloor;
	private boolean isRearLight;
	private float smokeTimer;
	
	private boolean sound;
	private Sound highSound;
	private Sound lowSound;
	private Sound neutralSound;
	
	public static final int CAMERA_COCKPIT = 1;
	public static final int CAMERA_T_CAM = 2;
	public static final int CAMERA_FOLLOW = 3;
	public static final int CAMERA_CHASE = 4;
	public static final int CAMERA_HELICOPTER = 5;
	
	private static final float PICK_HEIGHT = 1f;
	private static final Vector3D PICK_DIRECTION_FLOOR = new Vector3D(0f, -1f, 0f);
	private static final float REFERENCE_PLANE = 1f;
	private static final float WHEEL_BASE = 1.5f;
	private static final float COLLISION_DISTANCE = 2f;
	private static final float COLLISION_HEIGHT = 0.3f;
	private static final float EPSILON = 0.03f;
	
	private static final float WHEEL_LONGITUDINAL = 8f;
	private static final float WHEEL_LATERAL = 0.3f;
	private static final Color3D REAR_LIGHT_ON = new Color3D(100, 0, 0);
	private static final Color3D REAR_LIGHT_OFF = new Color3D(150, 150, 150);
	private static final BufferedImageTexture STEERING_WHEEL_TEXTURE = 
			Utils.loadTexture("data/graphics/steeringwheel.png");
	private static final Vector3D SHADOW_SIZE = new Vector3D(3.4f, 1.8f, 0f);
	private static final BufferedImageTexture SHADOW_TEXTURE = 
			Utils.loadTexture("data/graphics/shadow.png");
	private static final float SHADOW_OFFSET = 0.01f;
	private static final BufferedImageTexture SMOKE_TEXTURE = 
			Utils.loadTexture("data/graphics/smoke.png");
	private static final float SMOKE_COOLDOWN_TIME = 3f;
	private static final float SMOKE_ACCELERATE_SPEED = 15f;
	private static final float SMOKE_LOCK_SPEED = 50f;
	private static final float BRAKING_CAMBER_POS = 0.02f;
	private static final float BRAKING_CAMBER_ROT = -0.01f;
	private static final float BRAKING_CAMBER_SPEED = 50f;
	private static final float WOBBLE_FACTOR = 0.002f;
	private static final float STEERING_WHEEL_OFFSET = 0.5f;
	
	private static final Sound CAR_SOUND_HIGH = Utils.loadSound("data/sounds/car_high.ogg");
	private static final Sound CAR_SOUND_LOW = Utils.loadSound("data/sounds/car_low.ogg");
	private static final Sound CAR_SOUND_NEUTRAL = Utils.loadSound("data/sounds/car_neutral.ogg");
	private static final Sound GEAR_SOUND = Utils.loadSound("data/sounds/gear.ogg");
	private static final Sound HIT_SOUND = Utils.loadSound("data/sounds/hit.ogg");
	private static final Sound AMBIENT_SOUND = Utils.loadSound("data/sounds/ambient.ogg");
	private static final Sound CROWD_SOUND = Utils.loadSound("data/sounds/crowd.ogg");
	
	/**
	 * Creates a new {@code Car} with the specified geometry. Not all meta fields 
	 * of the object will be set when using the constructor.
	 * @param geometry The geometry node of the car.
	 * @param lodNode The LOD switch node containing the geometry.
	 * @param circuitNode The node in the scene to perform picking operations on.
	 */
	public Car(SceneGraphGroupNode geometry, LODNode lodNode, SceneGraphNode circuitNode) {
		
		super();
		
		this.lodNode = lodNode;
		this.geometry = geometry;
		this.circuitNode = circuitNode;
		
		pickVector = new Vector3D();
		pickVectorDir = new Vector3D();
		isOnTrack = true;
		isOnFloor = true;
		
		// Create geometry
		
		model = new Model("car");
		model.addChild(lodNode);
		
		bodywork = (Model) geometry.getChild("bodywork");
		tyreLF = (Model) geometry.getChild("wheelLF");
		tyreRF = (Model) geometry.getChild("wheelRF");
		tyreLR = (Model) geometry.getChild("wheelLR");
		tyreRR = (Model) geometry.getChild("wheelRR");
		steeringWheel = (Model) geometry.getChild("steeringWheel");
		rearLight = (Model) geometry.getChild("rearLight");
		helmet = (Model) geometry.getChild("helmet");
		
		bodywork.setBounds(Model.Bounds.ORIENTED_BOX);
		steeringWheel.applyTexture(STEERING_WHEEL_TEXTURE);
		
		shadow = Primitive.createQuad(SHADOW_SIZE.getX(), SHADOW_SIZE.getY());
		shadow.getTransform().getRotation().y = 1.57f;
		shadow.applyTexture(SHADOW_TEXTURE);
		shadow.applyBlend();
		model.addChild(shadow);
		
		// Initialize
		
		isRearLight = true;
		smokeTimer = 0f;
	}
	
	/**
	 * Picks for collisions between this car and the circuit's floor. The
	 * returned value will be the distance between this graphic and the floor, or
	 * 0 when no collision was found.
	 */
	private float pickCircuit() {
		Vector3D position = model.getTransform().getPosition();
		pickVector.setVector(position.x, PICK_HEIGHT, position.z);
		return circuitNode.pickDistance(pickVector, PICK_DIRECTION_FLOOR, true);
	}
	
	/**
	 * Same as {@link #pickCircuit()}, but picks from one of the tyre's positions
	 * instead of from the center of the car.
	 */
	private float pickCircuitFromTyre(Model tyre) {
		Vector3D worldPosition = tyre.getWorldTransform().getPosition();
		pickVector.setVector(worldPosition.x, PICK_HEIGHT, worldPosition.z);		
		return circuitNode.pickDistance(pickVector, PICK_DIRECTION_FLOOR, true);
	}
	
	/**
	 * Picks for collisions between this car and other objects. The picking 
	 * will be performed between the car's current location in the specified
	 * direction. The returned value is the distance to the collision.
	 */
	private float pickObject(Vector3D direction) {
		return circuitNode.pickDistance(pickVector, pickVectorDir, true);
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
	public void updatePhysics(float dt, float speed) {
		
		Vector3D position = model.getTransform().getPosition();
		Vector3D geometryRotation = geometry.getTransform().getRotation();
		
		// Picking
		
		boolean full = lodNode.isHighLevelActive();
		float pickCar = pickCircuit();
		float pickLF = full ? pickCircuitFromTyre(tyreLF) : pickCar;
		float pickRF = full ? pickCircuitFromTyre(tyreRF) : pickCar;
		float pickLR = full ? pickCircuitFromTyre(tyreLR) : pickCar;
		float pickRR = full ? pickCircuitFromTyre(tyreRR) : pickCar;	
		
		// Terrain following
		
		isOnTrack = (pickCar < REFERENCE_PLANE + EPSILON);
		
		float referenceY = PICK_HEIGHT - (pickLF + pickRF + pickLR + pickRR) / 4f;
		
		float frontY = (pickLF + pickRF) / 2f;
		float rearY = (pickLR + pickRR) / 2f;
		float longitudinal = (float) Math.tan((rearY - frontY) / WHEEL_BASE);
		
		float leftY = (pickLF + pickLR) / 2f;
		float rightY = (pickLR + pickRR) / 2f;
		float lateral = (float) Math.tan((leftY - rightY) / (WHEEL_BASE / 2f));
		
		geometryRotation.x = longitudinal;
		geometryRotation.z = lateral;
		shadow.getTransform().getPosition().y = referenceY - position.y + SHADOW_OFFSET;
		
		// Gravity
		
		isOnFloor = (Math.abs(position.y - referenceY) < EPSILON);
		position.y = referenceY;
	}
	
	/**
	 * Updates the postions for all wheels. This involves both longitudinal
	 * movement (the turning of the wheels) as well as lateral movement (because
	 * the wheels may move depending on steering movement).
	 * @param dt Delta time for this frame.
	 * @param speed The current speed (in m/s).
	 * @param steering The signum of the current steering movement.
	 */
	public void updateWheels(float dt, float speed, int steering) {
		
		// Longitudinal
		
		float wheelSpeedLong = (speed * dt) * WHEEL_LONGITUDINAL;
		tyreLF.getTransform().getRotation().x += wheelSpeedLong;
		tyreRF.getTransform().getRotation().x += wheelSpeedLong;
		tyreLR.getTransform().getRotation().x += wheelSpeedLong;
		tyreRR.getTransform().getRotation().x += wheelSpeedLong;
		
		// Lateral
		
		float wheelLateral = -Math.signum(steering) * WHEEL_LATERAL;
		tyreLF.getTransform().getRotation().y = wheelLateral;
		tyreRF.getTransform().getRotation().y = wheelLateral;
		helmet.getTransform().getRotation().y = wheelLateral;
	}
	
	/**
	 * Updates the state of the rear light. There is a number of possible
	 * situations when the rear light should be on, the most common is when the
	 * car is braking.
	 * @param enabled True if the light should be on, false when off.
	 */
	public void updateRearLight(boolean enabled) {
		if (isRearLight != enabled) {
			isRearLight = enabled;
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
	public void updateSmoke(float dt, float speed, boolean accelerating, boolean braking,
			int wheel) {
		
		boolean lockedAccel = ((speed < SMOKE_ACCELERATE_SPEED) && (speed  >0f) && (accelerating));
		boolean lockedBraking = ((speed > SMOKE_LOCK_SPEED) && (braking));
		boolean isSmoke = (lockedAccel || lockedBraking);
		
		// Update timer
		
		if (isSmoke) {
			smokeTimer=  0f;
		} else {
			smokeTimer += dt;
			if (smokeTimer > SMOKE_COOLDOWN_TIME) {
				isSmoke = true;
				smokeTimer = 0f;
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
	public void updateEffects(float dt, float speed, boolean braking, int steering) {
		
		// Braking camber
		
		if (braking && (speed > BRAKING_CAMBER_SPEED)) {
			model.getTransform().getPosition().y -= BRAKING_CAMBER_POS;
			geometry.getTransform().getRotation().x = BRAKING_CAMBER_ROT;
			helmet.getTransform().getRotation().x = BRAKING_CAMBER_ROT * 2;
		}
	
		// Off-track wobbling
		
		if (!isOnTrack) {
			float wobbleFactor = WOBBLE_FACTOR * speed;
			float wobble = (float) Math.random() * wobbleFactor - wobbleFactor / 2f;
			geometry.getTransform().getRotation().x = wobble;
		}
		
		// Steering wheel
		
		float steerOffset = STEERING_WHEEL_OFFSET * -Math.signum(steering);
		steeringWheel.getTransform().getRotation().z = steerOffset;
	}
	
	/**
	 * Updates the car sound for the current frame. This method will control both
	 * the engine sound as well as some sound effects.
	 */
	public void updateSound(float rpm, float minRPM, float maxRPM, int gear, boolean accelerating,
			boolean slow) {
		
		if (!isSoundEnabled()) {
			return;
		}
		
		if ((highSound == null) || (lowSound == null) || (neutralSound == null)) {
			highSound = CAR_SOUND_HIGH;
			highSound.setLoop(true);
			highSound.play();
			lowSound = CAR_SOUND_LOW;
			lowSound.setLoop(true);
			lowSound.play();
			neutralSound = CAR_SOUND_NEUTRAL;
			neutralSound.setLoop(true);
			neutralSound.play();
		}
		
		//TODO heavily using magic constants here
		
		float highVolume = (gear < 4) ? 0f : (gear - 3) * 0.07f;
		highSound.setVolume(highVolume);
		lowSound.setVolume(slow ? 0f : (accelerating ? 1f : 0.5f));
		neutralSound.setVolume(slow ? 0.5f : 0f);
				
		float delta = (rpm - minRPM) / (maxRPM - minRPM);
		if (delta < 0f) { delta = 0f; }
		if (delta > 1f) { delta = 1f; }
		lowSound.setPitch(1.3f + 0.4f * delta);
		highSound.setPitch(1.3f + 0.5f * delta);
	}
	
	@Override
	public boolean isOnTrack() {
		return isOnTrack;
	}
	
	@Override
	public boolean isOnFloor() {
		return isOnFloor;
	}
	
	/**
	 * Returns if an object exists between the car's current position and the
	 * specified new position.
	 */
	@Override
	public boolean isCollidingObject(float newX, float newY, float newZ) {
		
		Vector3D position = model.getTransform().getPosition();
		pickVector.setVector(position.x, position.y + COLLISION_HEIGHT, position.z);
		pickVectorDir.setVector(newX - position.x, 0f, newZ - position.z);
		pickVectorDir.normalize();
		
		float pick = pickObject(pickVectorDir);
		boolean collision = ((pick > 0f) && (pick < COLLISION_DISTANCE));
		
		//TODO move this somewhere else
		if (collision && isSoundEnabled()) {
			HIT_SOUND.play();
		}
		
		return collision;
	}
	
	/**
	 * Returns if the car will collide with the specified other car. The collision
	 * is checked for the car's potential new position.
	 */
	@Override
	public boolean isCollidingCar(float newX, float newY, float newZ, AbstractCar otherCar) {
		
		Vector3D position = model.getTransform().getPosition();
		pickVector.setVector(position.x, position.y + COLLISION_HEIGHT, position.z);
		pickVectorDir.setVector(newX - position.x, 0f, newZ - position.z);
		pickVectorDir.normalize();
		
		float distance = ((Car) otherCar).getBodyworkNode().pickDistance(pickVector, pickVectorDir, true);
		return ((distance > 0f) && (distance < COLLISION_DISTANCE));
	}
	
	@Override
	public void setPosition(float x, float z) {
		model.getTransform().getPosition().setX(x);
		model.getTransform().getPosition().setZ(z);
	}
	
	@Override
	public void setOrientation(float angle) {
		model.getTransform().getRotation().setY(angle);
	}
	
	@Override
	public void doGearChange(boolean up) {
		if (!up && isSoundEnabled()) {
			GEAR_SOUND.play();
		}
	}
	
	@Override
	public void doNextLap() {
		if (isSoundEnabled()) {
			CROWD_SOUND.play();
		}
	}
	
	/**
	 * Should be called whenever the selected camera was changed. The car's 
	 * appearance might differ depending on which camera is used.
	 */
	public void cameraChanged(int activeCamera) {
		boolean showHelmet = ((activeCamera != CAMERA_COCKPIT) && (activeCamera != CAMERA_T_CAM));
		helmet.setVisible(showHelmet);
	}
	
	public Model getModel() {
		return model;
	}
	
	/**
	 * Returns the bodywork node. This can be used for collision detection, as
	 * it will be much faster if the tyres are not taken into consideration. 
	 */
	private Model getBodyworkNode() {
		return bodywork;
	}
	
	public void setSoundEnabled(boolean sound) {
		this.sound = sound;
	}
	
	public boolean isSoundEnabled() {
		return sound;
	}
}

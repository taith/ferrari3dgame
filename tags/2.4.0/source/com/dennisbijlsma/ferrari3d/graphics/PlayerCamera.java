//-----------------------------------------------------------------------------
// Ferrari3D
// PlayerCamera
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Camera;

/**
 * Controls the camera that follows cars. A car will be set as 'target', which
 * means that it will be followed by the camera. Apart from all cameras that are
 * defined by the car, two special types of camera are also available: the
 * track camera's and reverse camera.
 */

public class PlayerCamera {

	private Camera camera;
	private Circuit circuit;
	private Car target;
	private ImmutableVector3D selectedCamera;
	private int selectedCameraIndex;
	private Vector3D targetPosition;
	private Vector3D targetRotation;	
	private float offsetH;
	private float offsetV;
	private boolean trackCamera;
	private boolean rearCamera;
	
	private static final float OFFSET_H_STEP = 0.05f;
	private static final float OFFSET_V_STEP = 0.10f;
	
	public PlayerCamera(Camera camera, Circuit circuit) {
		
		this.camera = camera;
		this.circuit = circuit;
		
		target = null;
		selectedCamera = new Vector3D();
		selectedCameraIndex = -1;
		
		targetPosition = new Vector3D();
		targetRotation = new Vector3D();
		offsetH = 0f;
		offsetV = 0f;
		trackCamera = false;
		rearCamera = false;
	}
	
	public void update() {
		
		// Track camera
		
		if (trackCamera) {
			ImmutableVector3D targetTrackCamera = getClosestTrackCamera();
			camera.aim(targetTrackCamera, targetPosition);
			return;
		}
		
		// Car camera
	
		if ((offsetV == 0f) && (offsetH == 0f)) {
			camera.follow(target.getModel(), selectedCamera.getX(), selectedCamera.getY(), 
					selectedCamera.getZ());
		} else {
			float angle = target.getModel().getTransform().getRotation().y;
			Vector3D d = new Vector3D();
			d.x = (float) (targetPosition.x + Math.cos(angle + offsetH) * selectedCamera.getX());
			d.y = selectedCamera.getY();
			d.z = (float) (targetPosition.z + Math.sin(angle + offsetH) * selectedCamera.getX());
			camera.aim(d, targetPosition);
		}
	}
	
	public Camera getCamera() {
		return camera;
	}
	
	public void setTarget(Car target) {
		
		if (target == null) {
			throw new IllegalArgumentException("Camera must follow a car");
		}
	
		this.target = target;
		targetPosition = target.getModel().getTransform().getPosition();
		targetRotation = target.getModel().getTransform().getRotation();
	}
	
	public Car getTarget() {
		return target;
	}
	
	public Vector3D getTargetPosition() {
		return targetPosition;
	}
	
	public Vector3D getTargetRotation() {
		return targetRotation;
	}
	
	public void setSelectedCamera(int index) {
		
		selectedCamera = target.getCamera(index - 1);
		selectedCameraIndex = index;
		target.cameraChanged(selectedCameraIndex);
		
		if (rearCamera) {
			rearCamera = false;
		}
	}
	
	public ImmutableVector3D getSelectedCameraVector() {
		return selectedCamera;
	}
	
	public void setOffsetH(int signum) {
		if (signum == 0) { offsetH = 0f; }
		if (signum < 0) { offsetH -= OFFSET_H_STEP; }
		if (signum > 0) { offsetH += OFFSET_H_STEP; }
	}
	
	public void setOffsetV(int signum) {
		if (signum == 0) { offsetV = 0f; }
		if (signum < 0) { offsetV -= OFFSET_H_STEP; }
		if (signum > 0) { offsetV += OFFSET_H_STEP; }
	}
	
	public void setTrackCamera(boolean trackCamera) {
		
		this.trackCamera = trackCamera;
		
		if (trackCamera) {
			target.cameraChanged(-1);
		} else {
			target.cameraChanged(selectedCameraIndex);
		}
	}
	
	public boolean getTrackCamera() {
		return trackCamera;
	}
	
	public ImmutableVector3D getClosestTrackCamera() {
		return circuit.getClosestCamera(targetPosition);
	}
	
	public void setRearCamera(boolean rearCamera) {
		this.rearCamera = rearCamera;
		selectedCamera = new Vector3D(-selectedCamera.getX(), selectedCamera.getY(),
				selectedCamera.getZ());
	}
	
	public boolean getRearCamera() {
		return rearCamera;
	}
}

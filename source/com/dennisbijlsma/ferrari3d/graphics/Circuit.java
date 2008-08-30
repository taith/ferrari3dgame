//--------------------------------------------------------------------------------
// Ferrari3D
// Circuit
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.*;

import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.ferrari3d.util.*;

/**
 * The visual representation of the circuit. Apart from the geometry this class 
 * also stores all circuit data, such as points and track cameras. 
 */

public class Circuit extends Model implements ICircuit {
	
	private String circuitName;
	private CircuitPoint[] points;
	private Vector3D[] cameras;
	private Vector3D[] startinggrid;

	/**
	 * Creates a new <code>Circuit</code> object for the specified data. The data 
	 * consists of the circuit geometry, as well as meta data that was loaded from
	 * an .f3d file.
	 * @param circuitName The name of the circuit.
	 */
	
	public Circuit(String circuitName,SceneGraphNode geometry,CircuitPoint[] points,
			Vector3D[] cameras,Vector3D[] startinggrid) {
			
		super("circuit");

		this.circuitName=circuitName;
		this.points=points;
		this.cameras=cameras;
		this.startinggrid=startinggrid;
		
		addChild(geometry);
		
		// Debug information
		
		if (!Settings.getInstance().debug) {
			return;
		}
		
		for (int i=0; i<points.length; i++) {
			BasicModel bm=new BasicModel("__point"+i,BasicModel.TYPE_BOX,new Vector3D(0.2f,0.05f,0.2f));
			bm.getTransform().setPosition(points[i].pointX,0f,points[i].pointY);
			bm.setMaterial(new Material(points[i].isSuggestedSpeed() ? Color.CYAN : Color.RED));
			addChild(bm);

			BasicModel alt=new BasicModel("__altpoint"+i,BasicModel.TYPE_BOX,new Vector3D(0.2f,0.05f,0.2f));
			alt.getTransform().setPosition(points[i].altX,0f,points[i].altY);
			alt.setMaterial(new Material(Color.BLUE));
			addChild(alt);
		}
	}
		
	/**
	 * {@inheritDoc}
	 */
	
	public String getCircuitName() {
	
		return circuitName;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public CircuitPoint getPoint(int index) {
		
		if ((index<0) || (index>=points.length)) {
			index=0;
		}
	
		return points[index];
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public CircuitPoint[] getPoints() {
	
		return points;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public CircuitPoint getIntermediate(int index) {
				
		for (CircuitPoint i : points) {
			if (i.intermediate) {
				if (index==0) {
					return i;
				} else {
					index--;
				}
			}
		}
			
		return null;
	}
	
	/**
	 * Returns the camera closest to the specified point. When multiple cameras 
	 * exist at the same distance a random one is returned.
	 */
	
	public Vector3D getClosestCamera(Vector3D p) {
		
		Vector3D closest=cameras[0];
	
		for (Vector3D i : cameras) {
			if (Utils.getDistance(i.x,i.z,p.x,p.z)<Utils.getDistance(closest.x,closest.z,p.x,p.z)) {
				closest=i;
			}
		}
		
		return closest;
	}
	
	/**
	 * Returns all track cameras as an array.
	 */
	
	public Vector3D[] getCameras() {
	
		return cameras;
	}
	
	/**
	 * Returns all starting grid positions as an array.
	 */
	
	public Vector3D[] getStartingGrid() {
		
		return startinggrid;
	}
}
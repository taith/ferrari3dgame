//-----------------------------------------------------------------------------
// Ferrari3D
// Circuit
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.core3d.Color3D;
import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.core3d.scene.SceneGraphGroupNode;
import com.dennisbijlsma.ferrari3d.CircuitData;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * The visual representation of the circuit. Apart from the geometry this class 
 * also stores all circuit data, such as points and track cameras. 
 */
public class Circuit implements CircuitData {
	
	private Model model;
	
	private String circuitName;
	private CircuitPoint[] points;
	private Vector3D[] cameras;
	private Vector3D[] startinggrid;

	/**
	 * Creates a new {@code Circuit} object for the specified data. The data 
	 * consists of the circuit geometry, as well as meta data that was loaded from
	 * an .f3d file.
	 * @param circuitName The name of the circuit.
	 */
	public Circuit(String circuitName, SceneGraphGroupNode geometry, CircuitPoint[] points,
			Vector3D[] cameras, Vector3D[] startinggrid) {
		
		if (geometry.getChild("circuit") == null) {
			throw new IllegalArgumentException("Invalid circuit geometry");
		}
		
		model = new Model("circuit");
		model.addChild(geometry);

		this.circuitName = circuitName;
		this.points = points;
		this.cameras = cameras;
		this.startinggrid = startinggrid;
		
		// Debug information
		
		if (!Settings.getInstance().debug) {
			return;
		}
		
		for (int i=0; i<points.length; i++) {
			Primitive bm = Primitive.createBox(0.2f, 0.05f, 0.2f);
			bm.setName("__point" + i);
			bm.getTransform().setPosition(points[i].pointX, 0f, points[i].pointY);
			bm.applyColor(points[i].isSuggestedSpeed() ? Color3D.CYAN : Color3D.RED);
			model.addChild(bm);

			Primitive alt = Primitive.createBox(0.2f, 0.05f, 0.2f);
			alt.setName("__altpoint" + i);
			alt.getTransform().setPosition(points[i].altX, 0f, points[i].altY);
			alt.applyColor(Color3D.BLUE);
			model.addChild(alt);
		}
	}
	
	public Model getModel() {
		return model;
	}
		
	/** {@inheritDoc} */
	public String getCircuitName() {
		return circuitName;
	}
	
	/** {@inheritDoc} */
	public CircuitPoint getPoint(int index) {
		if ((index < 0) || (index >= points.length)) {
			index = 0;
		}
		return points[index];
	}
	
	/** {@inheritDoc} */
	public int getNumPoints() {
		return points.length;
	}
	
	/** {@inheritDoc} */
	public CircuitPoint getIntermediate(int index) {
		for (CircuitPoint i : points) {
			if (i.isIntermediate()) {
				if (index == 0) {
					return i;
				} else {
					index--;
				}
			}
		}
		return null;
	}
	
	/** {@inheritDoc} */
	public ImmutableVector3D getClosestCamera(ImmutableVector3D v) {
		
		Vector3D closest = cameras[0];
		float closestDistance = closest.distance(v);
	
		for (Vector3D i : cameras) {
			if (i.distance(v) < closestDistance) {
				closest = i;
				closestDistance = i.distance(v);
			}
		}
		
		return closest;
	}
	
	/** {@inheritDoc} */
	public ImmutableVector3D getStartingGridPosition(int index) {
		return startinggrid[index];
	}
}

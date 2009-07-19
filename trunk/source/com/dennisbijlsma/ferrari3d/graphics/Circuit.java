//--------------------------------------------------------------------------------
// Ferrari3D
// Circuit
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;

import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * The visual representation of the circuit. Apart from the geometry this class 
 * also stores all circuit data, such as points and track cameras. 
 */

public class Circuit extends Model {
	
	private String circuitName;
	private SceneGraphNode geometry;
	private CircuitPoint[] points;
	private Vector3D[] cameras;
	private Vector3D[] startinggrid;

	/**
	 * Creates a new <code>Circuit</code> object for the specified data. The data 
	 * consists of the circuit geometry, as well as meta data that was loaded from
	 * an .f3d file.
	 * @param circuitName The name of the circuit.
	 */
	
	public Circuit(String circuitName, SceneGraphNode geometry, CircuitPoint[] points,
			Vector3D[] cameras, Vector3D[] startinggrid) {
			
		super("circuit");
		
		if (geometry.getChild("circuit") == null) {
			throw new IllegalArgumentException("Invalid circuit geometry");
		}

		this.circuitName = circuitName;
		this.geometry = geometry;
		this.points = points;
		this.cameras = cameras;
		this.startinggrid = startinggrid;
		
		addChild(geometry);
		
		// Debug information
		
		if (!Settings.getInstance().debug) {
			return;
		}
		
		for (int i=0; i<points.length; i++) {
			Primitive bm=new Primitive("__point"+i,Primitive.Shape.BOX,new Vector3D(0.2f,0.05f,0.2f));
			bm.getTransform().setPosition(points[i].pointX,0f,points[i].pointY);
			bm.applyColor(points[i].isSuggestedSpeed() ? Color.CYAN : Color.RED);
			addChild(bm);

			Primitive alt=new Primitive("__altpoint"+i,Primitive.Shape.BOX,new Vector3D(0.2f,0.05f,0.2f));
			alt.getTransform().setPosition(points[i].altX,0f,points[i].altY);
			alt.applyColor(Color.BLUE);
			addChild(alt);
		}
	}
		
	/**
	 * Returns the circuit's name. This is both the display name and the name
	 * with which the circuit is identified.
	 */
	
	public String getCircuitName() {
		return circuitName;
	}
	
	/**
	 * Returns the circuit point at the specified index. When no point is 
	 * available at this index, the first point will be returned.
	 */
	
	public CircuitPoint getPoint(int index) {
		
		if ((index < 0) || (index >= points.length)) {
			index = 0;
		}
	
		return points[index];
	}
	
	/**
	 * Returns the number of points for this circuit. Values passed to {@see
	 * getPoint(int)} should be between 0 and this number - 1.
	 */
	
	public int getNumPoints() {
		return points.length;
	}
	
	/**
	 * Returns the intermediate at the specified index. When no intermediate is
	 * available this method will return <code>null</code>.
	 */
	
	public CircuitPoint getIntermediate(int index) {
				
		for (CircuitPoint i : points) {
			if (i.intermediate) {
				if (index == 0) {
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
		
		Vector3D closest = cameras[0];
		float closestDistance = Utils.getDistance(closest.x, closest.z, p.x,p.z);
	
		for (Vector3D i : cameras) {
			if (Utils.getDistance(i.x, i.z, p.x, p.z) < closestDistance) {
				closest = i;
				closestDistance = Utils.getDistance(closest.x, closest.z, p.x,p.z);
			}
		}
		
		return closest;
	}
	
	/**
	 * Returns the starting grid position at the specified index.
	 * @throws IndexOutOfBoundsException when the index is outside the range 0 - 20.
	 */
	
	public Vector3D getStartingGrid(int index) {
		return startinggrid[index];
	}
}
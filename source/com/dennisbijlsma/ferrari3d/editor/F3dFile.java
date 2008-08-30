//--------------------------------------------------------------------------------
// Ferrari3D
// F3dFile
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.io.*;
import java.util.*;

import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.ferrari3d.util.*;

/**
 * Wrapper around a loaded asset, which is stored in a .f3d file. The file 
 * contains both scene graph geometry, as well as some meta data. This class 
 * stores both, as well as keeping a link to the original resource from where
 * it was loaded.
 */

public class F3dFile {

	private String name;
	private File source;
	private SceneGraphNode node;
	
	public HashMap<String,String> meta;	
	public HashMap<String,String> carInfo;
	public ArrayList<Vector3D> carCameras;
	public ArrayList<CircuitPoint> points;
	public ArrayList<Vector3D> startgrid;
	public ArrayList<Vector3D> circuitCameras;
	
	public static final String META_AUTHOR="author";
	public static final String META_DATE="date";
	public static final String META_VERSION="version";
	public static final String META_PREVIEW="preview";	
	public static final String RESOURCE_PREFIX="resources/";
	
	/**
	 * Creates a new <code>F3dFile</code> object. This constructor should only 
	 * be called by the <code>F3dLoader</code> class.
	 * @param name The textual identifier of this object.
	 * @param source The source file where this object was originally loaded from.
	 */
	
	protected F3dFile(String name,File source) {
		
		setName(name);
		setSourceFile(source);
		setNode(new Model(name));
	
		meta=new HashMap<String,String>();
		carInfo=new HashMap<String,String>();
		carCameras=new ArrayList<Vector3D>();
		points=new ArrayList<CircuitPoint>();
		startgrid=new ArrayList<Vector3D>();
		circuitCameras=new ArrayList<Vector3D>();
	}
	
	/**
	 * Creates a new <code>F3dFile</code> object without a local file attached. It
	 * will not be possible to re-save this file.
	 * @param name The textual identifier of this object.
	 */
	
	protected F3dFile(String name) {
	
		this(name,null);
	}
	
	/**
	 * Sets the name of this <code>F3dFile</code>. This name can be used to 
	 * identify the file.
	 */
	
	public void setName(String name) {
	
		this.name=name;
	}
	
	/**
	 * Returns the name of this <code>F3dFile</code>. This name can be used to 
	 * identify the file.
	 */
	
	public String getName() {
	
		return name;
	}
	
	/**
	 * Sets the source file where this object was originally loaded from. Setting
	 * <code>null</code> will make it impossible to resave this file.
	 */
	
	public void setSourceFile(File source) {
	
		this.source=source;
	}
	
	/**
	 * Returns the source file where this object was originally loaded from. 
	 * Setting <code>null</code> will make it impossible to resave this file.
	 */
	
	public File getSourceFile() {
	
		return source;
	}
		
	/**
	 * Sets the scene graph node of the file. By calling this method the original
	 * scene graph node will be replaced.
	 */
	
	public void setNode(SceneGraphNode node) {
	
		this.node=node;
	}
	
	/**
	 * Returns the scene graph node that was created by loading this file. Note 
	 * that this may have been changed by calling the <code>setNode(
	 * SceneGraphNode)</code> method.
	 */
	
	public SceneGraphNode getNode() { 
		
		return node; 
	}
	
	/**
	 * Returns if this f3d file is a valid car model. This involves checking if 
	 * all required child nodes are present.
	 */
	
	public boolean isCarModel() {
		
		boolean isWheelsFront=((node.getChild("wheelLF")!=null) && (node.getChild("wheelRF")!=null));
		boolean isWheelsRear=((node.getChild("wheelLR")!=null) && (node.getChild("wheelRR")!=null));
		boolean isSteeringWheel=(node.getChild("steeringWheel")!=null);
		boolean isRearLight=(node.getChild("rearLight")!=null);
		
		return (isWheelsFront && isWheelsRear && isSteeringWheel && isRearLight);
	}
	
	/**
	 * Returns if this f3d file is a valid circuit model. Currently all models are
	 * accepted as circuits. 
	 */
	
	public boolean isCircuitModel() {
	
		return true;
	}
}
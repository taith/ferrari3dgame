//--------------------------------------------------------------------------------
// Ferrari3D
// F3dFile
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.util.ResourceFile;

/**
 * Represents a content XML file. Ferrari3D uses this file format to load a number
 * of submodels and their transforms, as well as some meta data. Instances of this
 * class are created by the {@link ContentLoader} class.
 */

public class ContentFile {

	private String name;
	private ResourceFile resource;
	private Model node;
	private Map<String,String> meta;
	
	public Map<String,String> carInfo;
	public List<Vector3D> carCameras;
	public List<CircuitPoint> points;
	public List<Vector3D> startgrid;
	public List<Vector3D> circuitCameras;
	
	public static final String META_AUTHOR="author";
	public static final String META_DATE="date";
	public static final String META_VERSION="version";
	public static final String META_PREVIEW="preview";
	
	/**
	 * Creates a new {@code ContentFile} for the specified node. The node will
	 * be linked to the specified resource file.
	 * @param name The file name of the content.
	 * @param resource The location of the XML file.
	 * @param node The node created from the XML file.
	 */
	
	protected ContentFile(String name,ResourceFile resource,Model node) {
	
		this.name=name;
		this.resource=resource;
		this.node=node;
	
		meta=new HashMap<String,String>();
		
		carInfo=new HashMap<String,String>();
		carCameras=new ArrayList<Vector3D>();
		points=new ArrayList<CircuitPoint>();
		startgrid=new ArrayList<Vector3D>();
		circuitCameras=new ArrayList<Vector3D>();
	}
	
	/**
	 * Returns the name of this content file. This is equal to the file name
	 * from which the content was loaded.
	 */
	
	public String getName() {
		
		return name;
	}
	
	/**
	 * Returns the resource file that this file represents. Note that this file
	 * could exist either in the classpath or in the local file system.
	 */
	
	public ResourceFile getResource() {
		
		return resource;
	}
	
	/**
	 * Returns a pointer to a subresource with the specified name. The file 
	 * should exist in the 'resources' subdirectory.
	 */
	
	protected ResourceFile getSubResource(String name) {
	
		return new ResourceFile(getPathPrefix()+name);
	}
	
	/**
	 * Returns the path prefix for files used by this content file. The name of
	 * the file should be appended to this path. 
	 */
	
	String getPathPrefix() {
		
		String dir=resource.getPath().substring(0,resource.getPath().lastIndexOf('/'));
		return dir+"/resources/";
	}
	
	/**
	 * Returns if the file from which this content was loaded is writable. If so,
	 * the file can be edited and saved.
	 */
	
	public boolean isWritable() {
		
		return resource.existsLocal();
	}
	
	/**
	 * Changes the node to which this content is linked.
	 * @throws IllegalStateException when the content was already loaded.
	 */
	
	void setNode(Model node) {
		
		if (this.node!=null) {
			throw new IllegalStateException("Content already loaded");
		}
		
		this.node=node;
	}

	/**
	 * Returns the node that was loaded from the content file. The node may
	 * contain child nodes.
	 * @throws IllegalStateException when the content has not completed loading.
	 */
	
	public Model getNode() {
		
		if (node==null) {
			throw new IllegalStateException("Content not loaded yet");
		}
		
		return node;
	}
	
	/**
	 * Sets the meta field with the specified key. The key should be one of the
	 * <code>META_*</code> constants.
	 */
	
	public void setMeta(String key,String value) {
	
		meta.put(key,value);
	}
	
	/**
	 * Returns the meta field with the specified key. The key should be one of 
	 * the <code>META_*</code> constants.
	 */
	
	public String getMeta(String key) {
	
		return meta.get(key);
	}
	
	/**
	 * Returns if this file is a valid car model.
	 */
	
	public boolean checkCarModel() {
		
		boolean isWheelsFront=((node.getChild("wheelLF")!=null) && (node.getChild("wheelRF")!=null));
		boolean isWheelsRear=((node.getChild("wheelLR")!=null) && (node.getChild("wheelRR")!=null));
		boolean isSteeringWheel=(node.getChild("steeringWheel")!=null);
		boolean isRearLight=(node.getChild("rearLight")!=null);
		boolean isHelmet=(node.getChild("helmet")!=null);
		
		return (isWheelsFront && isWheelsRear && isSteeringWheel && isRearLight && isHelmet);
	}
	
	/**
	 * Returns if this file is a valid circuit model.
	 */
	
	public boolean checkCircuitModel() {
		
		boolean isCircuitNode=(node.getChild("circuit")!=null);
	
		return isCircuitNode;
	}
}
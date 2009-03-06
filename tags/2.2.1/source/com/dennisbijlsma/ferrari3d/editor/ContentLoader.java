//--------------------------------------------------------------------------------
// Ferrari3D
// ContentLoader
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.dennisbijlsma.core3d.data.ImmutableVector3D;
import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.scene.Billboard;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Quad;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.scene.SharedModel;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.DataLoader;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.Resources;
import com.dennisbijlsma.util.XMLParser;

/**
 * Parses content files. The files used by Ferrari3D are stored in a special XML
 * format, which stores the links to multiple models and their transforms.<br><br>
 * Note that this class can cache XML files, models and textures for improved
 * performance when loading multiple instances of them.
 */

public class ContentLoader {
	
	private Map<ResourceFile,Model> modelCache;
	private Map<ResourceFile,Image> textureCache;
	private Map<ResourceFile,Document> xmlCache;	
	
	/**
	 * Creates a new <code>ContentLoader</code> instance.
	 */
	
	public ContentLoader() {
	
		modelCache=new HashMap<ResourceFile,Model>();
		textureCache=new HashMap<ResourceFile,Image>();
		xmlCache=new HashMap<ResourceFile,Document>();
	}

	/**
	 * Loads a content file from the specified location. The file is parsed, after
	 * which the models are created and the root model is returned as a <code>
	 * ContentFile</code> object.
	 * @param name The name is used to identify the file.
	 * @param resource The relative path to where the file is stored.
	 * @param shared When true, this file will be loaded from the cache next time.
	 * @return The loaded file as a <code>ContentFile</code>.
	 * @throws Exception when the content file could not be loaded.
	 */
	
	public ContentFile load(String name,ResourceFile resource,boolean shared) throws Exception {
		
		ContentFile file=new ContentFile(name,resource,null);
								
		// Parse XML
		
		Document document=getXML(resource);
		Element rootNode=document.getDocumentElement();
		Element metaNode=XMLParser.getFirstChild(rootNode,"meta");
		Element carNode=XMLParser.getFirstChild(rootNode,"carData");
		Element circuitNode=XMLParser.getFirstChild(rootNode,"circuitData");
		Element scenegraphNode=XMLParser.getFirstChild(rootNode,"scenegraph");
				
		// Meta data
		
		file.setMeta(ContentFile.META_AUTHOR,XMLParser.getChildValue(metaNode,ContentFile.META_AUTHOR));
		file.setMeta(ContentFile.META_DATE,XMLParser.getChildValue(metaNode,ContentFile.META_DATE));
		file.setMeta(ContentFile.META_VERSION,XMLParser.getChildValue(metaNode,ContentFile.META_VERSION));
		file.setMeta(ContentFile.META_PREVIEW,XMLParser.getChildValue(metaNode,ContentFile.META_PREVIEW));
	
		//TODO version check
		
		// Car data
		
		for (Element i : XMLParser.getChildNodes(XMLParser.getFirstChild(carNode,"carCameras"))) {
			file.carCameras.add(parseVector3D(i));
		}
		
		for (Element i : XMLParser.getChildNodes(XMLParser.getFirstChild(carNode,"carInfo"))) {
			file.carInfo.put(i.getNodeName(),XMLParser.getNodeValue(i));
		}
				
		// Circuit data
		
		for (Element i : XMLParser.getChildNodes(XMLParser.getFirstChild(circuitNode,"points"))) {
			file.points.add(parseCircuitPoint(i));
		}
		
		for (Element i : XMLParser.getChildNodes(XMLParser.getFirstChild(circuitNode,"startgrid"))) {
			file.startgrid.add(parseVector3D(i));
		}
		
		for (Element i : XMLParser.getChildNodes(XMLParser.getFirstChild(circuitNode,"circuitCameras"))) {
			file.circuitCameras.add(parseVector3D(i));
		}
				
		// Create scene graph
				
		Model model=new Model("");
		file.setNode(model);
		
		for (Element i : XMLParser.getChildNodes(scenegraphNode,"node")) {
			model.addChild(parseSceneGraphNode(i,file.getPathPrefix(),shared));
		}
		
		return file;
	}
		
	/**
	 * Parses a scene graph node from XML.
	 */
	
	private SceneGraphNode parseSceneGraphNode(Element node,String path,boolean shared) throws IOException {
		
		SceneGraphNode result=null;
		String id=XMLParser.getNodeAttribute(node,"id");
		String type=XMLParser.getNodeAttribute(node,"type");
		Vector3D position=parseVector3D(XMLParser.getFirstChild(node,"position"));
		Vector3D rotation=parseVector3D(XMLParser.getFirstChild(node,"rotation"));
		float scale=Float.parseFloat(XMLParser.getChildValue(node,"scale"));
		
		if (type.equals("node")) { 
			result=new SceneGraphNode(id); 
		}
		
		if (type.equals("model")) {
			String modelURL=XMLParser.getChildValue(node,"url");
			result=getModel(Resources.getResourceFile(path+modelURL),shared);
			result.setName(id);
			((Model) result).applyBlend();
		}
		
		if ((type.equals("basicmodel")) || (type.equals("primitive"))) { 
			int shapeType=Integer.parseInt(XMLParser.getChildValue(node,"shapetype"));
			Vector3D dimensions=parseVector3D(XMLParser.getFirstChild(node,"dimensions"));
			result=new Primitive(id,Primitive.Shape.values()[shapeType-1],dimensions);
			((Primitive) result).applyBlend();
		}
		
		if (type.equals("billboard")) {
			float width=Float.parseFloat(XMLParser.getChildValue(node,"width"));
			float height=Float.parseFloat(XMLParser.getChildValue(node,"height"));
			String textureURL=XMLParser.getChildValue(node,"texture");
			Quad quad=new Quad("quad",width,height);
			quad.setTexture(Utils.loadImage(Resources.getResourceFile(path+textureURL)));
			quad.setTextureURL(textureURL);
			result=new Billboard(id,quad);
		}
		
		result.getTransform().setPosition(position);
		result.getTransform().setRotation(rotation);
		result.getTransform().setScale(scale);
		
		return result;
	}
	
	/**
	 * Parses a <code>Vector3D</code> from XML.
	 */
	
	private Vector3D parseVector3D(Element node) {
		
		String[] temp=XMLParser.getNodeValue(node).split(",");
		float x=Float.parseFloat(temp[0]);
		float y=Float.parseFloat(temp[1]);
		float z=Float.parseFloat(temp[2]);
		
		return new Vector3D(x,y,z);
	}
	
	/**
	 * Parses a <code>CircuitPoint</code> from XML.
	 */
	
	private CircuitPoint parseCircuitPoint(Element node) {
			
		CircuitPoint point=new CircuitPoint();
		String pointTag=XMLParser.getChildValue(node,"point");
		String altPointTag=XMLParser.getChildValue(node,"altPoint");
		String speedTag=XMLParser.getChildValue(node,"speed");

		point.pointX=Float.parseFloat(pointTag.split(",")[0]);
		point.pointY=Float.parseFloat(pointTag.split(",")[1]);
		point.intermediate=XMLParser.getChildValue(node,"intermediate").equals("true");
		
		if ((altPointTag!=null) && (altPointTag.length()>0)) {
			point.altX=Float.parseFloat(altPointTag.split(",")[0]);
			point.altY=Float.parseFloat(altPointTag.split(",")[1]);
		}
		
		if ((speedTag!=null) && (speedTag.length()>0)) {
			point.speed=Float.parseFloat(speedTag);
		}
				
		return point;
	}
	
	/**
	 * Saves a <code>ContentFile</code> to the specified local file.
	 * @param content The content file that should be saved.
	 * @param target The local file to where the content will be saved. 
	 * @throws Exception when the file could not be saved.
	 */
	
	public void save(ContentFile content,File target) throws Exception {
		
		StringBuilder sb=new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		sb.append("<Ferrari3D>");
		
		// Meta data
		
		sb.append("<meta>");
		sb.append(printTag(ContentFile.META_AUTHOR,content.getMeta(ContentFile.META_AUTHOR)));
		sb.append(printTag(ContentFile.META_DATE,content.getMeta(ContentFile.META_DATE)));
		sb.append(printTag(ContentFile.META_VERSION,content.getMeta(ContentFile.META_VERSION)));
		sb.append(printTag(ContentFile.META_PREVIEW,content.getMeta(ContentFile.META_PREVIEW)));
		sb.append("</meta>");
		
		// Car data
		
		sb.append("<carData>");
		sb.append("<carCameras>");
		for (Vector3D i : content.carCameras) {
			sb.append(printTag("camera",printVector3D(i)));
		}
		sb.append("</carCameras>");
		sb.append("<carInfo>");
		for (String i : content.carInfo.keySet()) {
			sb.append(printTag(i,content.carInfo.get(i)));
		}
		sb.append("</carInfo>");
		sb.append("</carData>");
		
		// Circuit data
		
		sb.append("<circuitData>");
		sb.append("<points>");
		for (CircuitPoint i : content.points) {
			sb.append("<point>");
			sb.append(printCircuitPoint(i));
			sb.append("</point>");
		}
		sb.append("</points>");
		sb.append("<startgrid>");
		for (Vector3D i : content.startgrid) {
			sb.append(printTag("gridpos",printVector3D(i)));
		}
		sb.append("</startgrid>");
		sb.append("<circuitCameras>");
		for (Vector3D i : content.circuitCameras) {
			sb.append(printTag("camera",printVector3D(i)));
		}
		sb.append("</circuitCameras>");
		sb.append("</circuitData>");
		
		// Scene graph
		
		sb.append("<scenegraph>");
		for (SceneGraphNode i : content.getNode().getChildren()) {			
			printSceneGraphNode(sb,i);
		}
		sb.append("</scenegraph>");
		
		sb.append("</Ferrari3D>");
		
		// Remove old XML file
		
		if (target.exists()) {
			if (!target.delete()) {
				throw new IOException("Could not delete file '"+target.getAbsoluteFile()+"'");
			}
		}

		// Write new XML file
		
		Document document=XMLParser.parseXML(sb.toString());
		OutputStream stream=new FileOutputStream(target);
		XMLParser.printXML(document,stream);
		stream.close();
	}
	
	/**
	 * Prints the specified scene graph node to XML.
	 */
	
	private void printSceneGraphNode(StringBuilder sb,SceneGraphNode node) {
		
		if (node.getName().startsWith("__")) { 
			return; 
		}	
	
		String type="node";
		if (node instanceof Model) { type="model"; }
		if (node instanceof Primitive) { type="primitive"; }
		if (node instanceof Billboard) { type="billboard"; }
		
		sb.append("<node id=\""+node.getName()+"\" type=\""+type+"\">");
		sb.append(printTag("position",printVector3D(node.getTransform().getPosition())));
		sb.append(printTag("rotation",printVector3D(node.getTransform().getRotation())));
		sb.append(printTag("scale",""+node.getTransform().getScale().x)); // Uniform scaling only
		if (type.equals("model")) {
			Model model=(Model) node;
			sb.append(printTag("url",new File(model.getGeometryResource().toString()).getName()));
		}
		if (type.equals("primitive")) {
			Primitive basic=(Primitive) node; 
			sb.append(printTag("shapetype",""+(basic.getType().ordinal()+1)));
			sb.append(printTag("dimensions",printVector3D(basic.getDimensions())));
		}
		if (type.equals("billboard")) {
			Billboard bb=(Billboard) node;
			sb.append(printTag("width",""+((Quad) bb.getModel()).getWidth()));
			sb.append(printTag("height",""+((Quad) bb.getModel()).getHeight()));
			sb.append(printTag("texture",((Quad) bb.getModel()).getTextureURL()));
		}
		sb.append("</node>");
	}
	
	/**
	 * Prints the specified vector to XML.
	 */
	
	private String printVector3D(ImmutableVector3D v) {
		
		StringBuilder sb=new StringBuilder();
		sb.append(v.getX());
		sb.append(',');
		sb.append(v.getY());
		sb.append(',');
		sb.append(v.getZ());
		return sb.toString();
	}
	
	/**
	 * Prints the specified circuit point to XML.
	 */
	
	private String printCircuitPoint(CircuitPoint cp) {
		
		StringBuilder sb=new StringBuilder();
		sb.append(printTag("point",cp.pointX+","+cp.pointY));
		if (cp.isAltPoint()) {
			sb.append(printTag("altPoint",cp.altX+","+cp.altY));
		}
		if (cp.isSuggestedSpeed()) {
			sb.append(printTag("speed",""+cp.speed));
		}
		sb.append(printTag("intermediate",""+cp.intermediate));
				
		return sb.toString();
	}
	
	/**
	 * Creates a string representation of a single line XML node.
	 */
	
	private String printTag(String name,String value) {
		
		StringBuilder sb=new StringBuilder();
		sb.append("<"+name+">");
		sb.append(value);
		sb.append("</"+name+">");
		return sb.toString();
	}
	
	/**
	 * Loads a model. When the model was loaded before it is returned from the
	 * cache.
	 * @throws IOException when the model could not be loaded.
	 */
	
	private Model getModel(ResourceFile resource,boolean shared) throws IOException {
		
		URL url=resource.getURL();
		
		if (!shared) {
			return new Model("",url,url);
		}
		
		if (modelCache.get(resource)==null) {
			Model geometry=new Model("",url,url);
			modelCache.put(resource,geometry);
		}
		
		return new SharedModel("",modelCache.get(resource));
	}
	
	/**
	 * Loads a texture image. When the image was loaded before it is returned
	 * from the cache.
	 * @throws IOException when the image could not be loaded.
	 */
	
	private Image getTexture(ResourceFile resource) throws IOException {
		
		if (!textureCache.containsKey(resource)) {
			InputStream stream=resource.getStream();
			Image image=DataLoader.loadImage(stream);
			stream.close();
			textureCache.put(resource,image);
		}
		
		return textureCache.get(resource);
	}
	
	/**
	 * Loads an XML document. When the document was loaded before it is returned
	 * from the cache.
	 * @throws Exception when the XML could not be parsed.
	 */
	
	private Document getXML(ResourceFile resource) throws Exception {
		
		if (!xmlCache.containsKey(resource)) {
			InputStream stream=resource.getStream();
			Document document=XMLParser.parseXML(stream);
			stream.close();
			xmlCache.put(resource,document);
		}
		
		return xmlCache.get(resource);
	}
	
	/**
	 * Creates a new content file at the specified location.
	 * @throws IllegalArgumentException when the target file already exists.
	 */
	
	public ContentFile newFile(File file) {
	
		if (file.exists()) {
			throw new IllegalArgumentException("File already exists: "+file.getAbsolutePath());
		}
		
		ResourceFile resource=new ResourceFile(file);
		Model node=new Model(file.getName());
	
		return new ContentFile(file.getName(),resource,node);
	}
}
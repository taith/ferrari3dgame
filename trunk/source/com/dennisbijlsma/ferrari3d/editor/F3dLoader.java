//--------------------------------------------------------------------------------
// Ferrari3D
// F3dLoader
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;

import com.dennisbijlsma.core3d.data.*;
import com.dennisbijlsma.core3d.scene.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.xml.*;

/**
 * Loads and parses .f3d files. Loaded textures and XML are stored into a cache,
 * so that loading the same model multiple times will reuse these files rather
 * than load them again. This class produces instances of the <code>F3dFile</code>
 * class.
 */

public class F3dLoader {
	
	private Map<String,Model> modelCache;
	private Map<String,Image> textureCache;
	private Map<String,Document> xmlCache;	
	private Map<Image,String> textureURLs;
	
	private static final F3dLoader INSTANCE=new F3dLoader();
	
	/**
	 * Private constructor as this class should not be initialized. 
	 */
	
	private F3dLoader() {
	
		modelCache=new HashMap<String,Model>();
		textureCache=new HashMap<String,Image>();
		xmlCache=new HashMap<String,Document>();
		textureURLs=new HashMap<Image,String>();
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static F3dLoader getInstance() {
	
		return INSTANCE;
	}

	/**
	 * Loads an .f3d file from the specified location. The location URL can
	 * either specify a local file, a remote URL, or a location inside a JAR
	 * file. Note that only local files can be re-saved.
	 * @param location The location where the .f3d file is stored.
	 * @param local If true, the file can be re-saved later.
	 * @param share When true, geometry data for this file will be shared.
	 * @return A <code>F3dFile</code> object for the specified location.
	 * @throws Exception when the file could not be loaded. 
	 */
	
	public F3dFile load(String location,boolean local,boolean share) throws Exception {
		
		F3dFile file;
		String prefix;
		
		if (local) {
			File f=new File(location);
			file=new F3dFile(f.getName(),f);
			prefix=f.getParent()+"/"+F3dFile.RESOURCE_PREFIX;
		} else {
			file=new F3dFile(new File(location).getName());
			prefix=location.substring(0,location.lastIndexOf('/'))+"/"+F3dFile.RESOURCE_PREFIX;
		}
								
		// Load file from XML
		
		Document document=getXML(Resources.getResource(location));
		Node rootNode=XMLParser.getFirstChildNode(document,"Ferrari3D");
		Node metaNode=XMLParser.getFirstChildNode(rootNode,"meta");
		Node carNode=XMLParser.getFirstChildNode(rootNode,"carData");
		Node circuitNode=XMLParser.getFirstChildNode(rootNode,"circuitData");
		Node scenegraphNode=XMLParser.getFirstChildNode(rootNode,"scenegraph");
				
		// Meta data
		
		file.setMeta(F3dFile.META_AUTHOR,XMLParser.getChildValue(metaNode,F3dFile.META_AUTHOR));
		file.setMeta(F3dFile.META_DATE,XMLParser.getChildValue(metaNode,F3dFile.META_DATE));
		file.setMeta(F3dFile.META_VERSION,XMLParser.getChildValue(metaNode,F3dFile.META_VERSION));
		file.setMeta(F3dFile.META_PREVIEW,XMLParser.getChildValue(metaNode,F3dFile.META_PREVIEW));
		
		if (!file.getMeta(F3dFile.META_VERSION).equals(Editor.VERSION)) {
			Settings.getInstance().getLogger().info("Data file '"+location+"' has different version");
		}
		
		// Car data
		
		for (Node i : XMLParser.getChildNodes(XMLParser.getFirstChildNode(carNode,"carCameras"))) {
			file.carCameras.add(parseVector3D(i));
		}
		
		for (Node i : XMLParser.getChildNodes(XMLParser.getFirstChildNode(carNode,"carInfo"))) {
			file.carInfo.put(i.getNodeName(),XMLParser.getNodeValue(i));
		}
				
		// Circuit data
		
		for (Node i : XMLParser.getChildNodes(XMLParser.getFirstChildNode(circuitNode,"points"))) {
			file.points.add(parseCircuitPoint(i));
		}
		
		for (Node i : XMLParser.getChildNodes(XMLParser.getFirstChildNode(circuitNode,"startgrid"))) {
			file.startgrid.add(parseVector3D(i));
		}
		
		for (Node i : XMLParser.getChildNodes(XMLParser.getFirstChildNode(circuitNode,"circuitCameras"))) {
			file.circuitCameras.add(parseVector3D(i));
		}
				
		// Scene graph
				
		file.setNode(new SceneGraphNode(file.getName()));
		
		for (Node i : XMLParser.getChildNodes(scenegraphNode)) {
			file.getNode().addChild(parseSceneGraphNode(i,prefix,share));
		}
		
		return file;
	}
		
	/**
	 * Parses a scene graph node from XML.
	 * @param node The XML node to construct the scene graph from.
	 * @param prefix The location where resource files are stored.
	 * @param share If true, geometry data for this node will be shared.
	 * @return The loaded scene graph.
	 * @throws IOException when the model could not be loaded.
	 */
	
	private SceneGraphNode parseSceneGraphNode(Node node,String prefix,boolean share) throws IOException {
		
		SceneGraphNode result=null;
		String id=XMLParser.getAttribute(node,"id");
		String type=XMLParser.getAttribute(node,"type");
		Vector3D position=parseVector3D(XMLParser.getFirstChildNode(node,"position"));
		Vector3D rotation=parseVector3D(XMLParser.getFirstChildNode(node,"rotation"));
		float scale=Float.parseFloat(XMLParser.getChildValue(node,"scale"));
		
		if (type.equals("node")) { 
			result=new SceneGraphNode(id); 
		}
		
		if (type.equals("model")) {
			String modelURL=XMLParser.getChildValue(node,"url");
			result=getModel(Resources.getResource(prefix+modelURL),share);
			result.setName(id);
			((Model) result).applyBlend();
		}
		
		if ((type.equals("basicmodel")) || (type.equals("primitive"))) { 
			int shapeType=Integer.parseInt(XMLParser.getChildValue(node,"shapetype"));
			Vector3D dimensions=parseVector3D(XMLParser.getFirstChildNode(node,"dimensions"));
			result=new Primitive(id,Primitive.ShapeType.values()[shapeType-1],dimensions);
			((Primitive) result).applyBlend();
		}
		
		if (type.equals("billboard")) {
			//TODO
		}
		
		result.getTransform().setPosition(position);
		result.getTransform().setRotation(rotation);
		result.getTransform().setScale(scale);
		
		return result;
	}
	
	/**
	 * Parses a <code>Vector3D</code> from XML.
	 * @param node The XML node to parse.
	 * @return The created <code>Vector3D</code>.
	 */
	
	private Vector3D parseVector3D(Node node) {
		
		String[] temp=XMLParser.getNodeValue(node).split(",");
		float x=Float.parseFloat(temp[0]);
		float y=Float.parseFloat(temp[1]);
		float z=Float.parseFloat(temp[2]);
		
		return new Vector3D(x,y,z);
	}
	
	/**
	 * Parses a <code>CircuitPoint</code> from XML.
	 * @param node The XML node to parse.
	 * @return The created <code>CircuitPoint</code>.
	 */
	
	private CircuitPoint parseCircuitPoint(Node node) {
			
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
	 * Loads an XML document and returns it as a DOM-parsed document.
	 * @param url The location of the XML document.
	 * @return The XML document,
	 */
	
	private Document parseXML(URL url) {
		
		try {
			return XMLParser.parseXML(url.openStream());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Saves the specified <code>F3dFile</code>. All resource files are assumed 
	 * to be in the directory <code>/resources</code>.
	 * @param file The file to save.
	 * @throws Exception when the file could not be saved.
	 */
	
	public void save(F3dFile file) throws Exception {
		
		if (file.getSourceFile()==null) {
			throw new Exception("File has no local file binding");
		}
		
		PrintWriter writer=new PrintWriter(file.getSourceFile(),"UTF-8");
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
		writer.println("<Ferrari3D>");
		
		// Meta data
		
		writer.println("\t<meta>");
		writer.println(printTag(F3dFile.META_AUTHOR,file.getMeta(F3dFile.META_AUTHOR),2));
		writer.println(printTag(F3dFile.META_DATE,file.getMeta(F3dFile.META_DATE),2));
		writer.println(printTag(F3dFile.META_VERSION,file.getMeta(F3dFile.META_VERSION),2));
		writer.println(printTag(F3dFile.META_PREVIEW,file.getMeta(F3dFile.META_PREVIEW),2));
		writer.println("\t</meta>");
		
		// Car data
		
		writer.println("\t<carData>");
		writer.println("\t\t<carCameras>");
		for (Vector3D i : file.carCameras) {
			writer.println(printTag("camera",printVector3D(i),3));
		}
		writer.println("\t\t</carCameras>");
		writer.println("\t\t<carInfo>");
		for (String i : file.carInfo.keySet()) {
			writer.println(printTag(i,file.carInfo.get(i),3));
		}
		writer.println("\t\t</carInfo>");
		writer.println("\t</carData>");
		
		// Circuit data
		
		writer.println("\t<circuitData>");
		writer.println("\t\t<points>");
		for (CircuitPoint i : file.points) {
			writer.println("\t\t\t<point>");
			writer.println(printCircuitPoint(i));
			writer.println("\t\t\t</point>");
		}
		writer.println("\t\t</points>");
		writer.println("\t\t<startgrid>");
		for (Vector3D i : file.startgrid) {
			writer.println(printTag("gridpos",printVector3D(i),3));
		}
		writer.println("\t\t</startgrid>");
		writer.println("\t\t<circuitCameras>");
		for (Vector3D i : file.circuitCameras) {
			writer.println(printTag("camera",printVector3D(i),3));
		}
		writer.println("\t\t</circuitCameras>");
		writer.println("\t</circuitData>");
		
		// Scene graph
		
		writer.println("\t<scenegraph>");
		for (SceneGraphNode i : file.getNode().getChildren()) {			
			printSceneGraphNode(writer,i);
		}
		writer.println("\t</scenegraph>");
		
		writer.println("</Ferrari3D>");
		writer.close();
	}
	
	/**
	 * Prints a scene graph node as XML. The XML is appended to the specified
	 * <code>PrintWriter</code>.
	 * @param writer The <code>PrintWriter</code> to write XML to.
	 * @param node The object to convert to XML. 
	 */
	
	private void printSceneGraphNode(PrintWriter writer,SceneGraphNode node) {
		
		if (node.getName().startsWith("__")) { 
			return; 
		}	
	
		String type="node";
		if (node instanceof Model) { type="model"; }
		if (node instanceof Primitive) { type="primitive"; }
		if (node instanceof Billboard) { type="billboard"; }
		
		writer.println("\t\t<node id=\""+node.getName()+"\" type=\""+type+"\">");
		writer.println(printTag("position",printVector3D(node.getTransform().getPosition()),3));
		writer.println(printTag("rotation",printVector3D(node.getTransform().getRotation()),3));
		writer.println(printTag("scale",""+node.getTransform().getScale().x,3)); // Uniform scaling only
		if (type.equals("model")) {
			Model model=(Model) node;
			writer.println(printTag("url",new File(model.getGeometryResource()).getName(),3));
		}
		if (type.equals("primitive")) {
			Primitive basic=(Primitive) node; 
			writer.println(printTag("shapetype",""+(basic.getType().ordinal()+1),3));
			writer.println(printTag("dimensions",printVector3D(basic.getDimensions()),3));
		}
		if (type.equals("billboard")) {
			Billboard billboard=(Billboard) node;
			writer.println(printTag("dimensions",printVector3D(billboard.getModel().getDimensions()),3));
		}
		writer.println("\t\t</node>");
	}
	
	/**
	 * Prints a <code>Vector3D</code> node as XML. The XML is appended to the 
	 * specified <code>PrintWriter</code>.
	 * @param writer The <code>PrintWriter</code> to write XML to.
	 * @param p The object to convert to XML. 
	 */
	
	private String printVector3D(Vector3D p) {
		
		return p.x+","+p.y+","+p.z;
	}
	
	/**
	 * Prints a <code>CircuitPoint</code> node as XML. The XML is appended to the 
	 * specified <code>PrintWriter</code>.
	 * @param writer The <code>PrintWriter</code> to write XML to.
	 * @param cp The object to convert to XML. 
	 */
	
	private String printCircuitPoint(CircuitPoint cp) {
		
		String xml=printTag("point",cp.pointX+","+cp.pointY,4);
		
		if (cp.isAltPoint()) {
			xml+="\n"+printTag("altPoint",cp.altX+","+cp.altY,4);
		}
		
		if (cp.isSuggestedSpeed()) {
			xml+="\n"+printTag("speed",""+cp.speed,4);
		}
		
		xml+="\n"+printTag("intermediate",""+cp.intermediate,4);
				
		return xml;
	}
	
	/**
	 * Prints a <code>Shader</code> node as XML. The XML is appended to the 
	 * specified <code>PrintWriter</code>.
	 * @param writer The <code>PrintWriter</code> to write XML to.
	 * @param s The object to convert to XML. 
	 */
	
	private String printShader(Shader s) {
		
		if (s==null) {
			return "none";
		} else {	
			return ""+s.getType();
		}
	}
	
	/**
	 * Returns the specified XML node in textual form. 
	 */
	
	private String printTag(String name,String value,int indent) {
		
			
		String result="";		
		for (int i=0; i<indent; i++) { result+="\t"; }
		
		return result+"<"+name+">"+value+"</"+name+">";
	}
		
	/**
	 * Loads a model, converts it to a different format, and re-saves it. This
	 * method can be used to re-save models so that they can be loaded more
	 * quickly. File types are derives from the file extensions.
	 * @param source The file to load.
	 * @param target The location to save the file to.
	 * @throws Exception when the file could not be converted.
	 */
	
	public void convertModel(File source,File target) throws Exception {
		
		SceneGraphNode sourceModel=MediaLoader.loadModel(source);
		MediaLoader.saveModel(target,sourceModel);
	}
	
	/**
	 * Clears all existing caches for this loader. This can be used to reset the
	 * loader to its original state.
	 */
	
	public void clearCache() {
	
		textureCache.clear();
		xmlCache.clear();
	}
	
	/**
	 * Returns a model. When the model (identified by URL) is in the cache that
	 * version is returned, otherwise it is loaded and returned. Optionally model
	 * data can be shared across multiple instances.
	 */
	
	private Model getModel(URL key,boolean share) throws IOException {
	
		return MediaLoader.loadModel(key.openStream(),key.toString(),key.toString());
	}
	
	/**
	 * Loads a texture. When the texture (identifier by URL) is in the cache that
	 * version is returned, otherwise it is loaded and returned. 
	 */
	
	private Image getTexture(URL key) {
		
		if (!textureCache.containsKey(key.toString())) {
			textureCache.put(key.toString(),MediaLoader.loadImage(key));
		}
		
		return textureCache.get(key.toString());
	}
	
	/**
	 * Loads an XML file. When the XML file (identifier by URL) is in the cache 
	 * that version is returned, otherwise it is loaded and returned. 
	 */
	
	private Document getXML(URL key) {
		
		if (!xmlCache.containsKey(key.toString())) {
			xmlCache.put(key.toString(),parseXML(key));
		}
		
		return xmlCache.get(key.toString());
	}
}
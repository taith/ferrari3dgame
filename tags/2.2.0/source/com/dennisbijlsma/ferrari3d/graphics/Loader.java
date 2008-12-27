//--------------------------------------------------------------------------------
// Ferrari3D
// Loader
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.renderer.RenderSystem;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.LODNode;
import com.dennisbijlsma.core3d.scene.LightSource;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.ferrari3d.editor.F3dFile;
import com.dennisbijlsma.ferrari3d.editor.F3dLoader;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.Utils2D;

/**
 * Utility class for loading an constructing graphical assets. This class would 
 * typically be used when the game is initializing.<br><br>
 * Unlike the <code>F3dLoader</code> class, the <code>Loader</code> does not 
 * contain any complex parsing code. It simply loads resources and uses them to
 * build the game's objects.
 */

public class Loader {
	
	private static final float CAR_SCALE=0.05f;
	private static final float CIRCUIT_SCALE=10f;
	private static final Color LIGHT_COLOR=new Color(200,200,200);
	private static final float BACKGROUND_SIZE=1000f;
	private static final String BACKGROUND_URL_1="data/graphics/horizon.jpg";
	private static final String BACKGROUND_URL_2="data/graphics/sky.jpg";
	private static final Color FOG_COLOR=new Color(200,200,200);
	private static final float FOG_START=1000f;
	private static final float FOG_END=1300f;
	private static final Vector3D LENSFLARE_POSITION=new Vector3D(0f,300f,-200f);
	private static final String LENSFLARE_URL_1="data/graphics/flare1.png";
	private static final String LENSFLARE_URL_2="data/graphics/flare2.png";
	private static final String LENSFLARE_URL_3="data/graphics/flare3.png";
	private static final String LENSFLARE_URL_4="data/graphics/flare4.png";
	private static final float LOD_DISTANCE=80f;
	private static final Vector3D LOD_SIZE=new Vector3D(0.4f,0.3f,1f);
	private static final Color LOD_COLOR=new Color(255,0,0);
	private static final Font INDICATOR_FONT=new Font("Verdana",Font.PLAIN,12);
	private static final Color INDICATOR_BACKGROUND=new Color(0,0,0,128);
	private static final Color INDICATOR_FOREGROUND=new Color(255,255,255);
	private static final Vector3D INDICATOR_SIZE=new Vector3D(1f,0.25f,0.01f);
	private static final float INDICATOR_HEIGHT=1f;
	
	/**
	 * Private constructor as this class should not be initialized.
	 */
	
	private Loader() {
		
	}
	
	/**
	 * Initializes the scene graph. This method should be the first one that is 
	 * called when the game is initialized. It adds basic things such as the 
	 * camera, lighting and the background image.
	 * @param scene The scene graph to initialize.
	 * @param viewport The viewport for the scene graph (used to create the camera).
	 * @param fullDetail When false, only basic initialization is performed.
	 */
	
	public static void createWorld(SceneGraph scene,Display viewport,boolean fullDetail) {
						
		// Camera
		
		if (!Settings.getInstance().splitscreen) {
			scene.addCamera(RenderSystem.getRenderer().createCamera(viewport));
		} else {
			scene.addCamera(RenderSystem.getRenderer().createCamera(viewport,Camera.SplitScreen.TOP));
			scene.addCamera(RenderSystem.getRenderer().createCamera(viewport,Camera.SplitScreen.BOTTOM));
		}
		
		// Lighting

		LightSource light=RenderSystem.getRenderer().createLight(LightSource.LightType.DIRECTIONAL,
				new Vector3D(0f,-1f,0.25f),new Color(200,200,200),new Color(255,255,255));
		scene.addLight(light);
		
		if (!fullDetail) {
			return;
		}
						
		// Background

		Image background1=Utils.loadImage(BACKGROUND_URL_1);
		Image background2=Utils.loadImage(BACKGROUND_URL_2);
		scene.addSkyBox(background1,background2,BACKGROUND_SIZE,BACKGROUND_SIZE/2f);
		
		// Fogging
		
		scene.addFogging(FOG_COLOR,FOG_START,FOG_END);
		
		// Lens flare
		
		Image[] flareImages=new Image[4];
		flareImages[0]=Utils.loadImage(LENSFLARE_URL_1);
		flareImages[1]=Utils.loadImage(LENSFLARE_URL_2);
		flareImages[2]=Utils.loadImage(LENSFLARE_URL_3);
		flareImages[3]=Utils.loadImage(LENSFLARE_URL_4);
		scene.addLensFlare(LENSFLARE_POSITION,flareImages);
	}

	/**
	 * Loads and creates a car with the specified name. This method loads and 
	 * parses the .f3d file for the car, and then creates a new <codeCar</code>
	 * object for it.
	 * @param carName The name of the car to load.
	 * @param scene The scene graph that will contain the car (needed for LOD).
	 * @param camera The main camera to follow the car (needed for LOD).
	 * @return A new <code>Car</code> object.
	 */
	
	public static Car loadCar(String carName,SceneGraph scene,Camera camera) {
		
		// Load or create geometry
		
		F3dFile file=null;
		
		try {
			file=F3dLoader.getInstance().load("cars/"+carName+"/"+carName+".f3d",false,true);
		} catch (Exception e) {
			Settings.getInstance().getLogger().error("Problem during loading of car file",e);
		}
				
		Model carNode=file.getNode();
		carNode.getTransform().setScale(CAR_SCALE);
		
		Primitive lodBox=new Primitive("lodBox",Primitive.ShapeType.BOX,LOD_SIZE);
		lodBox.applyColor(LOD_COLOR);
		float lodDistance=(!Settings.getInstance().splitscreen) ? LOD_DISTANCE : 100000f; //TODO
		LODNode lodNode=new LODNode("lodNode",carNode,lodBox,camera.getPosition(),lodDistance);
		
		// Load data
		
		HashMap<String,String> carInfo=file.carInfo;
		Vector3D[] carCameras=file.carCameras.toArray(new Vector3D[0]);
		
		Car car=new Car(carNode,lodNode,scene.getRootNode().getChild("circuit"));
		car.setCarName(carName);
		car.setInfo(carInfo);
		car.setCameras(carCameras);
		
		return car;
	}
	
	/**
	 * Loads and creates a circuit with the specified name. This method loads and 
	 * parses the .f3d file for the circuit, and then creates a new <codeCircuit
	 * </code>object for it.
	 * @param circuitName The name of the circuit to load.
	 * @param scene The scene graph that will contain the car (needed for sprites).
	 * @return A new <code>Circuit</code> object.
	 */
	
	public static Circuit loadCircuit(String circuitName,SceneGraph scene) {
		
		// Load geometry
		
		F3dFile file=null;
		
		try {
			file=F3dLoader.getInstance().load("circuits/"+circuitName+"/"+circuitName+".f3d",false,false);
		} catch (Exception e) {
			Settings.getInstance().getLogger().error("Problem during loading of circuit file",e);
		}
				
		SceneGraphNode circuitNode=file.getNode();
		circuitNode.getTransform().setScale(CIRCUIT_SCALE);
		
		// Load data

		CircuitPoint[] points=file.points.toArray(new CircuitPoint[0]);
		Vector3D[] cameras=file.circuitCameras.toArray(new Vector3D[0]);
		Vector3D[] startinggrid=file.startgrid.toArray(new Vector3D[0]);
		
		float f=CIRCUIT_SCALE;
		for (CircuitPoint i : points) { i.pointX*=f; i.pointY*=f; i.altX*=f; i.altY*=f; }
		for (Vector3D i : cameras) { i.setVector(i.x*f,i.y,i.z*f); }
		for (Vector3D i : startinggrid) { i.setVector(i.x*f,i.y*f,i.z*f); }
		
		Circuit circuit=new Circuit(circuitName,circuitNode,points,cameras,startinggrid);
				
		return circuit;
	}
	
	/**
	 * Creates a new player indicator graphic for the specified node. Typically 
	 * these indicators would be attached to cars.
	 * @param node The node to attach the indicator to.
	 * @param text The text on the indicator.
	 */
	
	public static void createIndicator(SceneGraphNode node,String text) {
	
		BufferedImage image=new BufferedImage(128,32,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2=image.createGraphics();
		g2.setColor(INDICATOR_BACKGROUND);
		g2.fillRect(0,0,image.getWidth(),image.getHeight());
		g2.setFont(INDICATOR_FONT);
		g2.setColor(INDICATOR_FOREGROUND);
		Utils2D.drawAlignedString(g2,text,image.getWidth()/2,25,'c');
		g2.dispose();
		
		Primitive indicator=new Primitive("indicator",Primitive.ShapeType.BOX,INDICATOR_SIZE);
		indicator.applyTexture(image);
		indicator.getTransform().getPosition().y=INDICATOR_HEIGHT;
		node.addChild(indicator);
	}
}
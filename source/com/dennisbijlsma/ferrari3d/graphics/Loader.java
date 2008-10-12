//--------------------------------------------------------------------------------
// Ferrari3D
// Loader
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import com.dennisbijlsma.core3d.data.*;
import com.dennisbijlsma.core3d.display.*;
import com.dennisbijlsma.core3d.scene.*;
import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.editor.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;

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
	
	public static void createWorld(SceneGraph scene,Viewport viewport,boolean fullDetail) {
						
		// Camera
		
		Camera camera=new Camera(viewport);
		camera.aim(new Vector3D(0f,0f,-10f),new Vector3D());
		scene.addCamera(camera);
		
		// Lighting

		LightSource light=new LightSource(LightSource.Type.DIRECTIONAL,new Vector3D(0f,-1f,0.25f));
		scene.addLight(light);
		
		if (!fullDetail) {
			return;
		}
						
		// Background

		Image background1=Utils.loadImage(BACKGROUND_URL_1);
		Image background2=Utils.loadImage(BACKGROUND_URL_2);
		scene.addSkyBox(background1,background2,BACKGROUND_SIZE,BACKGROUND_SIZE/2f);
		
		// Fogging
		
		scene.addFog(FOG_COLOR,FOG_START,FOG_END);
		
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
	 * @return A new <code>Car</code> object.
	 */
	
	public static Car loadCar(String carName,SceneGraph scene) {
		
		// Load or create geometry
		
		F3dFile file=null;
		
		try {
			file=F3dLoader.getInstance().load("cars/"+carName+"/"+carName+".f3d",false,true);
		} catch (Exception e) {
			Settings.getInstance().getLogger().error("Problem during loading of car file",e);
		}
				
		SceneGraphNode carNode=file.getNode();
		carNode.getTransform().setScale(CAR_SCALE);
		
		Primitive lod=new Primitive("lod",Primitive.ShapeType.BOX,LOD_SIZE);
		lod.applyColor(LOD_COLOR);
		
		LODNode lodNode=new LODNode("lodNode",scene.getCamera(),carNode);
		lodNode.addLOD(lod,LOD_DISTANCE);
		
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
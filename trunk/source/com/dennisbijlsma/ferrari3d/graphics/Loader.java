//-----------------------------------------------------------------------------
// Ferrari3D
// Loader
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.util.Map;

import com.dennisbijlsma.core3d.Color3D;
import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.BufferedImageTexture;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.LODNode;
import com.dennisbijlsma.core3d.scene.LightSource;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.core3d.scene.SceneGraphGroupNode;
import com.dennisbijlsma.ferrari3d.editor.ContentFile;
import com.dennisbijlsma.ferrari3d.editor.ContentLoader;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.ResourceFile;

/**
 * Utility class for loading an constructing graphical assets.
 * <p>
 * Unlike {@code ContentLoader}, this class does not contain any parsing code. 
 * It simply loads resources and uses them to build the game's objects.
 */
public class Loader {
	
	private static ContentLoader loader = new ContentLoader();
	
	private static final float CAR_SCALE = 0.05f;
	private static final float CIRCUIT_SCALE = 10f;
	private static final float BACKGROUND_SIZE = 1000f;
	private static final String BACKGROUND_URL_1 = "data/graphics/horizon.jpg";
	private static final String BACKGROUND_URL_2 = "data/graphics/sky.jpg";
	private static final Color3D FOG_COLOR = new Color3D(200, 200, 200);
	private static final float FOG_START = 1000f;
	private static final float FOG_END = 1300f;
	private static final Vector3D LENSFLARE_POSITION = new Vector3D(200f, 350f, -200f);
	private static final String LENSFLARE_URL_1 = "data/graphics/flare1.png";
	private static final String LENSFLARE_URL_2 = "data/graphics/flare2.png";
	private static final String LENSFLARE_URL_3 = "data/graphics/flare3.png";
	private static final String LENSFLARE_URL_4 = "data/graphics/flare4.png";
	private static final float LOD_DISTANCE = 80f;
	private static final Vector3D LOD_SIZE = new Vector3D(0.4f, 0.3f, 1f);
	private static final Color3D LOD_COLOR = new Color3D(255, 0, 0);
	
	/**
	 * Private constructor as this class should not be initialized.
	 */
	private Loader() { 
		
	}
	
	/**
	 * Initializes the scene graph. This method should be the first one that is 
	 * called when the game is initialized. It adds basic things such as the 
	 * camera and lighting.
	 * @param scene The scene graph to initialize.
	 * @param display The display for the scene graph (used to create the camera).
	 */
	public static void createWorld(SceneGraph scene, Display display) {
						
		// Camera (default)
		
		recreateCameras(scene, display, true);
		
		// Lighting

		LightSource light = LightSource.createDirectionalLight(new Vector3D(0f, -1f, 0.25f));
		scene.addLight(light);
	}
	
	/**
	 * Recreates the camera's for the virtual world. This method must be called
	 * if splitscreen mode is started or stopped.
	 * @param overrule Overrules the settings and creates a single camera.
	 */
	public static void recreateCameras(SceneGraph scene, Display display, boolean overrule) {
		
		scene.removeAllCameras();
	
		if (Settings.getInstance().splitscreen && !overrule) {
			scene.addCamera(new Camera(display, Camera.SplitScreen.TOP));
			scene.addCamera(new Camera(display, Camera.SplitScreen.BOTTOM));
		} else {
			scene.addCamera(new Camera(display));
		}
		
		for (Camera i : scene.getCameras()) {
			i.aim(new Vector3D(0f, 0f, 0f), new Vector3D(0f, 0f, 10f));
		}
	}
	
	/**
	 * Creates all background geometry. The background consists of the skybox, 
	 * as well as some graphical effects such as fogging and lens flare.
	 * @param scene The scene graph to attach the background to.
	 */
	public static void createBackground(SceneGraph scene) {
	
		// Background

		BufferedImageTexture background1 = Utils.loadTexture(BACKGROUND_URL_1);
		BufferedImageTexture background2 = Utils.loadTexture(BACKGROUND_URL_2);
		scene.addSkyBox(background1, background2, BACKGROUND_SIZE, BACKGROUND_SIZE / 2f);
		
		// Fogging
		
		scene.addFogging(FOG_COLOR, FOG_START, FOG_END);
		
		// Lens flare
		
		BufferedImageTexture[] flareImages = {
				Utils.loadTexture(LENSFLARE_URL_1),
				Utils.loadTexture(LENSFLARE_URL_2),
				Utils.loadTexture(LENSFLARE_URL_3),
				Utils.loadTexture(LENSFLARE_URL_4),				
		};
		scene.addLensFlare(LENSFLARE_POSITION, flareImages);
	}

	/**
	 * Loads and creates a car with the specified name. 
	 * @param carName The name of the car to load.
	 * @param scene The scene graph that will contain the car (needed for LOD).
	 * @param camera The main camera to follow the car (needed for LOD).
	 * @return A new <code>Car</code> object.
	 */
	public static Car loadCar(String carName, SceneGraph scene, Camera camera) {
		
		// Load or create geometry
		
		ContentFile file = null;
		
		try {
			ResourceFile resource = new ResourceFile("cars/" + carName + "/" + carName + ".xml");
			file = loader.load(carName, resource, true);
		} catch (Exception e) {
			throw new IllegalStateException("Problem during loading of car file", e);
		}
				
		Model carNode = file.getNode();
		carNode.getTransform().setScale(CAR_SCALE);
		
		Primitive lodBox = Primitive.createBox(LOD_SIZE);
		lodBox.applyColor(LOD_COLOR);
		float lodDistance = !Settings.getInstance().splitscreen ? LOD_DISTANCE : 100000f; //TODO
		LODNode lodNode = new LODNode("lodNode", carNode, lodBox, camera.getPosition(), lodDistance);
		
		// Load data
		
		Map<String,String> carInfo = file.carInfo;
		Vector3D[] carCameras = file.carCameras.toArray(new Vector3D[0]);
		
		Car car = new Car(carNode, lodNode, scene.getRootNode().getChild("circuit"));
		car.setCarName(carName);
		car.setInfo(carInfo);
		car.setCameras(carCameras);
		return car;
	}
	
	/**
	 * Loads and creates a circuit with the specified name.
	 * @param circuitName The name of the circuit to load.
	 * @param scene The scene graph that will contain the car (needed for sprites).
	 * @return A new <code>Circuit</code> object.
	 */
	public static Circuit loadCircuit(String circuitName, SceneGraph scene) {
		
		// Load geometry
		
		ContentFile file = null;
		
		try {
			ResourceFile resource = new ResourceFile("circuits/" + circuitName + "/" + 
					circuitName + ".xml");
			file = loader.load(circuitName, resource, false);
		} catch (Exception e) {
			throw new IllegalStateException("Problem during loading of circuit file", e);
		}
				
		SceneGraphGroupNode circuitNode = file.getNode();
		circuitNode.getTransform().setScale(CIRCUIT_SCALE);
		
		// Load data

		CircuitPoint[] points = file.points.toArray(new CircuitPoint[0]);
		Vector3D[] cameras = file.circuitCameras.toArray(new Vector3D[0]);
		Vector3D[] startinggrid = file.startgrid.toArray(new Vector3D[0]);
		
		float f = CIRCUIT_SCALE;
		for (CircuitPoint i : points) {
			i.pointX *= f; i.pointY *= f; i.altX *= f; i.altY *= f; 
		}
		for (Vector3D i : cameras) { 
			i.setVector(i.x * f, i.y, i.z * f); 
		}
		for (Vector3D i : startinggrid) { 
			i.setVector(i.x * f, i.y * f, i.z* f); 
		}
		
		return new Circuit(circuitName, circuitNode, points, cameras, startinggrid);		
	}
	
	/**
	 * Creates a circuit which can be used to test.
	 */
	public static Circuit createTestCircuit() {
	
		SceneGraphGroupNode circuitNode = new SceneGraphGroupNode("test");
		circuitNode.getTransform().setScale(CIRCUIT_SCALE);
		SceneGraphGroupNode circuitSubNode = new SceneGraphGroupNode("circuit");
		circuitNode.addChild(circuitSubNode);
		
		CircuitPoint[] points = new CircuitPoint[3];
		points[0] = new CircuitPoint(0f, 0f, 0f, 0f, 0f, true);
		points[1] = new CircuitPoint(0f, 0f, 0f, 0f, 0f, true);
		points[2] = new CircuitPoint(1000f, 1000f, 0f, 0f, 0f, true);
		
		return new Circuit("test", circuitNode, points, new Vector3D[0], new Vector3D[0]);		
	}
}

package com.dennisbijlsma.ferrari3d.test;

import java.awt.*;
import java.io.*;
import java.util.*;

import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.core3d.demo.*;
import com.dennisbijlsma.ferrari3d.*;
import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.util.*;



public class TestGraphics extends GameCore {
	
	private Contestant contestant;
	private Car car;
	private PlayerCamera camera;
	
	private static final boolean ALLOW_MOVE=true;
	private static final float START_POSITION=2.5f;
	private static final int NUM_TILES=10;
	private static final String TARGET_TILE_LEFT_FRONT="tile_-1_0";
	private static final String TARGET_TILE_RIGHT_FRONT="tile_-1_-1";
	private static final String TARGET_TILE_LEFT_REAR="tile_1_0";
	private static final String TARGET_TILE_RIGHT_REAR="tile_1_-1";
	private static final String OBSTACLE="obstacle";
	private static final float TILE_STEP=0.1f;
	private static final float CAMERA_SPEED=0.1f;
	private static final int ACTIVE_CAMERA=Car.CAMERA_HELICOPTER;
	
	
	
	public static void main(String[] args) {
		
		SceneGraph scene=new SceneGraph();
		InputController controller=new InputController();
		
		Viewport viewport=new Viewport(Viewport.RenderSystem.LWJGL,Viewport.Display.WINDOW,640,480,true);
		viewport.setTitle("Ferrari3D | Test Graphics");
		viewport.setIcon(Settings.ICON);
		viewport.setMouseCursor(true);

		TestGraphics test=new TestGraphics(scene,viewport,controller);
		test.startGame();
	}

	
	
	public TestGraphics(SceneGraph scene,Viewport viewport,InputController controller) {
	
		super(scene,viewport,controller);
	}



	protected void initGame() {
		
		long startTime=Utils.getTimestamp();
		
		// Create world
		
		Loader.createWorld(getSceneGraph(),getViewport(),false);
		
		// Create circuit/level
		
		SceneGraphNode level=new SceneGraphNode("level");
		SceneGraphNode floor=createCheckerboardFloor();
		level.addChild(floor);
		BasicModel obstacle=new BasicModel(OBSTACLE,BasicModel.TYPE_BOX,new Vector3D(0.5f,0.5f,1.5f));
		obstacle.getTransform().getPosition().x=-5f;
		obstacle.getTransform().getPosition().y=0.5f;
		level.addChild(obstacle);

		CircuitPoint[] points=new CircuitPoint[5];
		points[0]=new CircuitPoint(0f,0f,0f,0f,0f,true);
		points[1]=new CircuitPoint(0f,10f,0f,0f,0f,false);
		points[2]=new CircuitPoint(0f,100f,0f,0f,0f,true);
		points[3]=new CircuitPoint(0f,1000f,0f,0f,0f,false);
		points[4]=new CircuitPoint(0f,10000f,0f,0f,0f,true);
		
		Circuit circuit=new Circuit("testCircuit",level,points,new Vector3D[0],new Vector3D[0]);
		getSceneGraph().getRootNode().addChild(circuit);
		
		// Create contestant/car/camera
		
		Settings settings=Settings.getInstance();
		settings.setVersion(Ferrari3D.VERSION);
		
		try {
			settings.init();
			settings.load();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		settings.controlset=0;
		settings.autoGears=true;
		settings.autoReverse=true;
		
		Session session=new Session(Session.SessionMode.TIME);
		session.setStarted();
		
		car=Loader.loadCar("Ferrari 248",getSceneGraph());
		getSceneGraph().getRootNode().addChild(car);
		
		contestant=new Player("testPlayer",session,car,circuit,getController());
		session.addContestant(contestant);
		
		camera=new PlayerCamera(getSceneGraph().getCamera(),circuit);
		
		// Create HUD
		
		ViewportHUD hud=new ViewportHUD(getViewport(),getSceneGraph(),0,0,256,256) {
			protected void paintHUD(Graphics2D g2) {
				paintTestHUD(g2);
			}
		};
		getViewport().addHUD(hud);
		
		// Init
		
		contestant.setOrientation(1.57f);
		contestant.getPosition().x=START_POSITION;
		
		circuit.getTransform().getPosition().x=0.2f;
		circuit.getTransform().getPosition().z=0.4f;
		
		getSceneGraph().getCamera().move(new Vector3D(0f,1f,-4f),false);
		
		// Print statistics
		
		long endTime=Utils.getTimestamp();
		
		//System.out.println("Loading time: "+(endTime-startTime)+" ms");
		//System.out.println("Scene graph:");
		//System.out.println(getSceneGraph().getRootNode());		
	}



	protected void updateGame(float dt) {
		
		// Camera controls
		
		InputController controller=getController();
		Camera cam=getSceneGraph().getCamera();
		float delta=CAMERA_SPEED;
		
		if ((controller.isMouseButton(InputController.MOUSE_BUTTON_LEFT)) && (controller.getMouseDeltaX()!=0f)) {
			cam.move(new Vector3D(delta*-controller.getMouseDeltaX(),0f,0f),true);
		}
		
		if ((controller.isMouseButton(InputController.MOUSE_BUTTON_LEFT)) && (controller.getMouseDeltaY()!=0f)) {
			cam.move(new Vector3D(0f,0f,delta*-controller.getMouseDeltaY()),true);
		}
		
		if (controller.getMouseWheel()!=0) {
			cam.move(new Vector3D(0f,delta*controller.getMouseWheel(),0f),true);
		}
		
		/*if (controller.isKeyReleased(Controller.KEY_I)) { camera.getSelectedCameraVector().y+=0.05f; } 
		if (controller.isKeyReleased(Controller.KEY_K)) { camera.getSelectedCameraVector().y-=0.05f; } 
		if (controller.isKeyReleased(Controller.KEY_J)) { camera.getSelectedCameraVector().x+=0.05f; } 
		if (controller.isKeyReleased(Controller.KEY_L)) { camera.getSelectedCameraVector().x-=0.05f; } 
		if (controller.isKeyReleased(Controller.KEY_U)) { camera.getSelectedCameraVector().z+=0.1f; } 
		if (controller.isKeyReleased(Controller.KEY_O)) { camera.getSelectedCameraVector().z-=0.1f; } 
		System.out.println(camera.getSelectedCameraVector());*/
		
		// Tile controls
		
		boolean shift=controller.isKeyPressed(InputController.KEY_LSHIFT);
		
		SceneGraphNode tiles=getSceneGraph().getRootNode().getChild("circuit").getChild("level").getChild("checkerboard");
		SceneGraphNode tileLeft=tiles.getChild(!shift ? TARGET_TILE_LEFT_FRONT : TARGET_TILE_LEFT_REAR);
		SceneGraphNode tileRight=tiles.getChild(!shift ? TARGET_TILE_RIGHT_FRONT : TARGET_TILE_RIGHT_REAR);
		float tileY=-1f;
		
		if (controller.isKeyReleased(InputController.KEY_0)) { tileY=0f; }
		if (controller.isKeyReleased(InputController.KEY_1)) { tileY=1f*TILE_STEP; }
		if (controller.isKeyReleased(InputController.KEY_2)) { tileY=2f*TILE_STEP; }
		if (controller.isKeyReleased(InputController.KEY_3)) { tileY=3f*TILE_STEP; }
		if (controller.isKeyReleased(InputController.KEY_4)) { tileY=4f*TILE_STEP; }
		if (controller.isKeyReleased(InputController.KEY_5)) { tileY=5f*TILE_STEP; }
		
		if (tileY>=0f) {
			tileLeft.getTransform().getPosition().y=tileY;
			//tileRight.getTransform().getPosition().y=tileY;
		}
		
		// Update contestant
		
		contestant.update(dt);
		
		if (!ALLOW_MOVE) {
			contestant.getPosition().x=0f;
			contestant.getPosition().z=0f;
		}
		
		contestant.getPosition().y=contestant.getCar().getTransform().getPosition().y;
		contestant.setOrientation(1.57f);
		car.getTransform().setPosition(contestant.getPosition());
		car.getTransform().getRotation().y=1.57f;
	}
	
	
	
	private void paintTestHUD(Graphics2D g2) {
		
		//g2.setColor(new Color(0,0,0,128));
		//g2.fillRoundRect(0,10,256-10,256-10,10,10);
		
		g2.setColor(Color.WHITE);
		
		g2.drawString("Framerate: "+Math.round(getFramerate())+" fps",10,30);
		g2.drawString("Polygons: "+getPolygons(),10,50);
		g2.drawString("Memory: "+Math.round(Utils.getConsumedMemory()/1000)+" kb",10,70);
		
		g2.drawString("Speed: "+Math.round(contestant.getSpeed(false))+" m/s",10,110);
		g2.drawString("Speed: "+Math.round(contestant.getSpeed(true))+" kmh",10,130);
		g2.drawString("Angular: "+contestant.getAngular(),10,150);
		g2.drawString("Gravity: "+contestant.getCar().getGravity(),10,170);
	}
	
	
	
	private SceneGraphNode createCheckerboardFloor() {
		
		SceneGraphNode checkerboard=new SceneGraphNode("checkerboard");
		Demo.createCheckerboardFloor(checkerboard,NUM_TILES,1f);
		
		return checkerboard;
	}
}
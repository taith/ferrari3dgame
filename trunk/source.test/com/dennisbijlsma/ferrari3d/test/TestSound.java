//-----------------------------------------------------------------------------
// Ferrari3D
// TestSound
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import java.awt.Color;
import java.awt.Graphics2D;

import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.DisplayWindow;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.Sound;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.Overlay;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.ferrari3d.Contestant;
import com.dennisbijlsma.ferrari3d.Player;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * Tests the sound system. 
 */
public class TestSound extends GameCore {
	
	private Contestant contestant;
	
	private static final Sound CAR_SOUND_HIGH = Utils.loadSound("data/sounds/car_high.ogg");
	private static final Sound CAR_SOUND_LOW = Utils.loadSound("data/sounds/car_low.ogg");
	private static final Sound CAR_SOUND_NEUTRAL = Utils.loadSound("data/sounds/car_neutral.ogg");
	private static final Sound GEAR_SOUND = Utils.loadSound("data/sounds/gear.ogg");
	private static final Sound HIT_SOUND = Utils.loadSound("data/sounds/hit.ogg");
	private static final Sound AMBIENT_SOUND = Utils.loadSound("data/sounds/ambient.ogg");
	private static final Sound CROWD_SOUND = Utils.loadSound("data/sounds/crowd.ogg");
	
	public static void main(String[] args) {
		TestSound test = new TestSound();
		test.startGame();
	}
	
	public TestSound() {
		super();
	}
	
	@Override
	protected Display initDisplay() {
		return new DisplayWindow(640, 480, false);
	}
	
	@Override
	public void initGameState() {
		
		Display display = getDisplay();
		SceneGraph scene = getSceneGraph();
		
		scene.addCamera(new Camera(display));
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
			Settings.getInstance().sound = true;
			Settings.getInstance().volume = 100;
		} catch (Exception e) {
			throw new AssertionError();
		}
		
		Loader.createWorld(scene, display);
		Circuit circuit = Loader.createTestCircuit();
		scene.getRootNode().addChild(circuit.getModel());
		Car car = Loader.loadCar("Ferrari 248", scene, scene.getCamera(0));
		scene.getRootNode().addChild(car.getModel());
		
		Session session = new Session(Session.SessionMode.TIME, 3);
		session.setStarted();
		
		contestant = new Player("test", session, getController(), Settings.getInstance().getControlSet());
		contestant.setCar(car);
		contestant.setCircuitData(circuit);
		
		Overlay hud = new Overlay(scene.getCamera(0), 10, 10, 128, 64) {
			public void paint(Graphics2D g2) {
				g2.setColor(Color.WHITE);
				g2.drawString("Speed: " + Math.round(contestant.getSpeed()) + " m/s", 10, 20);
				g2.drawString("Gear: " + contestant.getGear(), 10, 40);
				g2.drawString("RPM: " + contestant.getRPM(), 10, 60);
			}
		};
		hud.start();
		scene.addOrthoQuad(hud, scene.getCamera(0));
	}
		
	@Override
	public void updateGameState(float dt) {
		
		contestant.update(dt);
		
		Controller controller = getContext().getController();
		if (controller.isKeyReleased(Controller.KEY_1)) { CAR_SOUND_HIGH.play(); }
		if (controller.isKeyReleased(Controller.KEY_2)) { CAR_SOUND_LOW.play(); }
		if (controller.isKeyReleased(Controller.KEY_3)) { CAR_SOUND_NEUTRAL.play(); }
		if (controller.isKeyReleased(Controller.KEY_4)) { GEAR_SOUND.play(); }
		if (controller.isKeyReleased(Controller.KEY_5)) { HIT_SOUND.play(); }
		if (controller.isKeyReleased(Controller.KEY_6)) { AMBIENT_SOUND.play(); }
		if (controller.isKeyReleased(Controller.KEY_7)) { CROWD_SOUND.play(); }
	}
}

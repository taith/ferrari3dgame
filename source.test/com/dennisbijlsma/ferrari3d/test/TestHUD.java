package com.dennisbijlsma.ferrari3d.test;

import java.awt.Graphics2D;

import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.GameState;
import com.dennisbijlsma.core3d.Timer;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.ferrari3d.CarPhysics;
import com.dennisbijlsma.ferrari3d.Drone;
import com.dennisbijlsma.ferrari3d.HUD;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * Test the appearance of the HUD as well as its performance.
 */

public class TestHUD extends GameCore implements GameState {
	
	private Session session;
	private HUD hud;
	
	public static void main(String[] args) {
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
			Settings.getInstance().showFramerate = true;
		} catch (Exception e) {
			throw new AssertionError();
		}
	
		TestHUD test = new TestHUD();
		test.changeGameState(test);
		test.startGame();
	}
	
	public TestHUD() {
		super(new Display(800, 600, false));
	}
	
	public void initGameState() {
		
		Display display = getContext().getDisplay();
		SceneGraph scene = getContext().getSceneGraph();
		final Timer timer = getContext().getTimer();
		
		Loader.createWorld(scene, display);
		Car car = Loader.loadCar("Ferrari 248", scene, scene.getCamera(0));
		Circuit circuit = Loader.loadCircuit("Monza", scene);
		Drone contestant = new Drone("test", session, new CarPhysics(), car, circuit);
		
		session = new Session(Session.SessionMode.TIME);
		session.addContestant(contestant);
		
		hud = new HUD(display, scene.getCamera(0), session) {
			public void paint(Graphics2D g2) {
				timer.tick();
				System.out.println(timer.getTimeSpan());
				super.paint(g2);
			}
		};
		hud.setTarget(contestant);
		scene.addOverlay(hud);
	}
		
	public void updateGameState(float dt) {	
		hud.setGameData(getCurrentFPS(), getCurrentUPS(), 0, 0, 0);
		if (getContext().getController().isKeyReleased(Controller.KEY_M)) {
			hud.setMessage("Test message");
		}
	}
	
	public void cleanupGameState() { }
}
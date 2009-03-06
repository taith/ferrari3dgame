package com.dennisbijlsma.ferrari3d.test;

import java.awt.Color;
import java.awt.Graphics2D;

import com.dennisbijlsma.core3d.game.Controller;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.game.GameCore;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.Overlay;
import com.dennisbijlsma.ferrari3d.Contestant;
import com.dennisbijlsma.ferrari3d.Player;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.graphics.SoundManager;
import com.dennisbijlsma.ferrari3d.util.Settings;



public class TestSound extends GameCore {

	
	
	public static void main(String[] args) {
	
		Display display=new Display(640,480,false);
		display.setWindowTitle("Ferrari3D | TestSound");
		
		TestSound test=new TestSound(display);
		test.startGame();
	}
	
	
	
	public TestSound(Display display) {
	
		super(display);
	}
	
	
	
	@Override
	public void initGame() {
		
		getSceneGraph().addCamera(new Camera(getDisplay()));
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Settings.getInstance().sound=true;
		Settings.getInstance().volume=100;
		
		Loader.createWorld(getSceneGraph(),getDisplay(),false);
		Circuit circuit=Loader.createTestCircuit();
		getSceneGraph().getRootNode().addChild(circuit);
		Car car=Loader.loadCar("Ferrari 248",getSceneGraph(),getSceneGraph().getCamera(0));
		getSceneGraph().getRootNode().addChild(car);
		Session session=new Session(Session.SessionMode.TIME);
		session.setStarted();
		final Contestant contestant=new Player("test",session,car,circuit,
				getController(),Settings.getInstance().getControlSet());
		addGameEntity(contestant);
		
		Overlay hud=new Overlay(getDisplay(),getSceneGraph().getCamera(0),10,10,128,64) {
			public void paint(Graphics2D g2) {
				g2.setColor(Color.WHITE);
				g2.drawString("Speed: "+contestant.getSpeed(true)+" kmh",10,20);
				g2.drawString("Gear: "+contestant.getGear(),10,40);
				g2.drawString("RPM: "+contestant.getRPM(),10,60);
			}
		};
		hud.start();
		getSceneGraph().addOverlay(hud);
	}
	
	
	
	@Override
	public void updateGame(float dt) {
		
		Controller controller=getController();
		
		if (controller.isKeyReleased(Controller.KEY_1)) { playSound(SoundManager.SoundKey.CAR_HIGH); }
		if (controller.isKeyReleased(Controller.KEY_2)) { playSound(SoundManager.SoundKey.CAR_LOW); }
		if (controller.isKeyReleased(Controller.KEY_3)) { playSound(SoundManager.SoundKey.CAR_NEUTRAL); }
		if (controller.isKeyReleased(Controller.KEY_4)) { playSound(SoundManager.SoundKey.GEAR); }
		if (controller.isKeyReleased(Controller.KEY_5)) { playSound(SoundManager.SoundKey.HIT); }
		if (controller.isKeyReleased(Controller.KEY_6)) { playSound(SoundManager.SoundKey.AMBIENT); }
		if (controller.isKeyReleased(Controller.KEY_7)) { playSound(SoundManager.SoundKey.CROWD); }
	}
	
	
	
	private void playSound(SoundManager.SoundKey key) {
	
		SoundManager.getInstance().playSound(key);
	}
}
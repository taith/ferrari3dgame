package com.dennisbijlsma.ferrari3d.test;

import java.awt.Graphics2D;

import com.dennisbijlsma.core3d.game.Controller;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.game.GameCore;
import com.dennisbijlsma.ferrari3d.Drone;
import com.dennisbijlsma.ferrari3d.HUD;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.util.Settings;



public class TestHUD extends GameCore {
	
	private Session session;
	private HUD hud;

	
	
	public static void main(String[] args) {
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
			Settings.getInstance().showFramerate=true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		TestHUD test=new TestHUD();
		test.startGame();
	}
	
	
	
	public TestHUD() {
	
		super(new Display(800,600,false));
	}
	
	
	
	@Override
	public void initGame() {
		
		Loader.createWorld(getSceneGraph(),getDisplay(),false);
		Car car=Loader.loadCar("Ferrari 248",getSceneGraph(),getSceneGraph().getCamera(0));
		Circuit circuit=Loader.loadCircuit("Monza",getSceneGraph());
		Drone contestant=new Drone("test",session,car,circuit);
		
		session=new Session(Session.SessionMode.TIME);
		session.addContestant(contestant);
		
		hud=new HUD(getDisplay(),getSceneGraph().getCamera(0),session) {
			public void paint(Graphics2D g2) {
				getTimer().tick();
				System.out.println(getTimer().getTimeSpan());
				super.paint(g2);
			}
		};
		hud.setTarget(contestant);
		getSceneGraph().addOverlay(hud);
	}
	
	
	
	@Override
	public void updateGame(float dt) {
		
		hud.setGameData(getCurrentFPS(),getCurrentUPS(),0,0,0);

		Controller controller=getController();
		
		if (controller.isKeyReleased(Controller.KEY_M)) {
			hud.setMessage("Test message");
		}
	}
}
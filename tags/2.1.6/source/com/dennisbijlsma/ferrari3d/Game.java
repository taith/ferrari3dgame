//--------------------------------------------------------------------------------
// Ferrari3D
// Game
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.Map;

import com.dennisbijlsma.core3d.display.Viewport;
import com.dennisbijlsma.core3d.game.GameCore;
import com.dennisbijlsma.core3d.game.GameEvent;
import com.dennisbijlsma.core3d.game.GameEventListener;
import com.dennisbijlsma.core3d.game.InputController;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.graphics.PlayerCamera;
import com.dennisbijlsma.ferrari3d.graphics.Splashscreen;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.Message;
import com.dennisbijlsma.util.swing.PopUp;

/**
 * The main game loop which controls all controls, contestants and other events. 
 * The game loop is started after the menu has been closed, and is terminated when
 * the session is finished. During the loop, the system controls, game controls 
 * and game logic are updated every frame. Also, multiplayer data is sent and
 * received.
 */

public class Game extends GameCore implements GameEventListener {
	
	private Ferrari3D parent;
	private Session session;	
	private Settings settings;
	private Multiplayer multiplayer;
	
	private SceneGraph scene;
	private Viewport viewport;
	private InputController controller;
	
	private HUD hud;
	private PlayerCamera camera;	
	private float mpSendTimer;
	private float startTimer;
	private float finishTimer;
	
	private static final String CONTROL_MENU=InputController.KEY_ESCAPE;
	private static final String CONTROL_PAUSE=InputController.KEY_P;
	private static final String CONTROL_CHAT=InputController.KEY_M;
	private static final String CONTROL_CAMERA_1=InputController.KEY_1;
	private static final String CONTROL_CAMERA_2=InputController.KEY_2;
	private static final String CONTROL_CAMERA_3=InputController.KEY_3;
	private static final String CONTROL_CAMERA_4=InputController.KEY_4;
	private static final String CONTROL_CAMERA_5=InputController.KEY_5;
	private static final String CONTROL_CAMERA_NEXT=InputController.KEY_PAGE_UP;
	private static final String CONTROL_CAMERA_PREV=InputController.KEY_PAGE_DOWN;
	private static final String CONTROL_CAMERA_HOME=InputController.KEY_HOME;
	private static final String CONTROL_TRACK_CAMERA=InputController.KEY_T;
	private static final String CONTROL_REAR_CAMERA=InputController.KEY_V;
	private static final int GAME_UPS=50;
	private static final float MULTIPLAYER_FRAME_TIME=0.04f;
	private static final float START_TIMER=15f;
	private static final float FINISH_TIMER=10f;

	/**
	 * Creates a new game. This game will be linked to the specified <code>Session
	 * </object>. When the game is finished it will terminate and signal its 
	 * parent to return to the menu.
	 * @param parent The parent of the game which should start the menu.
	 * @param session The <code>Session</code> to use for this game.
	 * @param viewport See <code>GameCore</code> constructor.
	 */
	
	public Game(Ferrari3D parent,Session session,Viewport viewport) {
		
		super(viewport,GAME_UPS);
		
		this.parent=parent;
		this.session=session;
		this.settings=Settings.getInstance();
		this.multiplayer=Multiplayer.getInstance();
		
		this.scene=getSceneGraph();
		this.viewport=getViewport();
		this.controller=getController();
		
		mpSendTimer=0f;
		startTimer=(session.getMode()==Session.SessionMode.RACE) ? START_TIMER : 0f;
		finishTimer=0f;
		
		addGameEventListener(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void initGame() {
		
		// Show splash screen
		
		Splashscreen splash=new Splashscreen();
		splash.showSplashScreen();
		splash.setMessage(settings.getText("splashscreen.loading"),0);
								
		// Create world
		
		splash.setMessage(settings.getText("splashscreen.world"),10);
		
		Loader.createWorld(scene,viewport,true);
		
		// Load circuit
		
		splash.setMessage(settings.getText("splashscreen.circuit"),40);
		
		Circuit circuit=Loader.loadCircuit(settings.circuit,scene);
		scene.getRootNode().addChild(circuit);
				
		// Load human cars
		
		splash.setMessage(settings.getText("splashscreen.cars"),70);
		
		Car playerCar=Loader.loadCar(settings.car,scene);
		scene.getRootNode().addChild(playerCar);		
		Player player=new Player(settings.name,session,playerCar,circuit,controller);
		session.addContestant(player);
		
		// Load multiplayer cars
		
		for (Connection i : multiplayer.getConnectionManager().getConnections()) {
			Car droneCar=Loader.loadCar(i.getMeta("car"),scene);
			scene.getRootNode().addChild(droneCar);
			Drone drone=new Drone(i.getMeta("id"),session,droneCar,circuit);
			session.addContestant(drone);
			addGameEventListener(drone);
		}
		
		// Load AI cars
		
		if (settings.aiActive) {
			for (int i=session.getNumContestants(); i<Settings.MAX_CONTESTANTS; i++) {
				Car aiCar=Loader.loadCar(Settings.getInstance().getRandomCar(),scene);
				scene.getRootNode().addChild(aiCar);		
				AI ai=new AI("AI-"+i,session,aiCar,circuit);
				session.addContestant(ai);
			}
		}
		
		// Create HUD
		
		splash.setMessage(settings.getText("splashscreen.init"),100);
		
		hud=new HUD(viewport,session,scene);
		viewport.addHUD(hud);
		
		// Create camera
		
		camera=new PlayerCamera(scene.getCamera(),circuit);
		camera.setTarget(session.getPlayer().getCar());
		camera.setSelectedCamera(settings.defaultCamera);
		
		// Create player indicators
		
		if (Settings.getInstance().indicators) {
			for (Contestant i : session.getContestantsSet()) {
				if (!(i instanceof Player)) {
					Loader.createIndicator(i.getCar(),i.getID());
				}
			}
		}
		
		// Start

		for (Contestant i : session.getContestantsSet()) {
			i.setPosition(i.getCircuit().getStartingGrid()[session.getRacePosition(i)-1]);
			i.setOrientation(1.57f);
		}
		
		viewport.setMouseCursor(!settings.fullscreen);
				
		splash.hideSplashScreen();
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void updateGame(float dt) {

		updateControls(dt);
		updateMultiplayerReceive(dt);
		updateMultiplayerSend(dt);

		if (!isPaused()) {
			updateGameLogic(dt);
		}
	}
	
	/**
	 * Updates the game controls. These only include generic controls such as 
	 * the paused button. The controls for the player(s) are handled by the
	 * contestants themselves.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateControls(float dt) {
		
		// System controls
				
		if (controller.isKeyReleased(CONTROL_MENU)) { 
			exit(); 
		}		
		
		if (controller.isKeyReleased(CONTROL_PAUSE)) { 
			paused(!isPaused()); 
		}
		
		if (controller.isKeyReleased(CONTROL_CHAT)) { 
			String chatMessage=PopUp.input(null,Settings.getInstance().getText("game.chatmessage"),"");		
			if ((chatMessage!=null) && (chatMessage.length()!=0)) {
				multiplayer.sendChatMessage(session.getPlayer().getID(),chatMessage);
			} 
		}
		
		// Camera controls
		
		Car camTarget=camera.getTarget();
		
		if (controller.isKeyReleased(CONTROL_CAMERA_1)) { camera.setSelectedCamera(Car.CAMERA_COCKPIT); }
		if (controller.isKeyReleased(CONTROL_CAMERA_2)) { camera.setSelectedCamera(Car.CAMERA_T_CAM); }
		if (controller.isKeyReleased(CONTROL_CAMERA_3)) { camera.setSelectedCamera(Car.CAMERA_FOLLOW); }
		if (controller.isKeyReleased(CONTROL_CAMERA_4)) { camera.setSelectedCamera(Car.CAMERA_CHASE); }
		if (controller.isKeyReleased(CONTROL_CAMERA_5)) { camera.setSelectedCamera(Car.CAMERA_HELICOPTER); }		
		
		if ((controller.isKeyReleased(CONTROL_CAMERA_NEXT)) || (controller.isKeyReleased(InputController.KEY_LBRACKET))) { 
			camera.setTarget(getFrontContestant(getContestant(camTarget),true).getCar());
		}
		
		if ((controller.isKeyReleased(CONTROL_CAMERA_PREV)) || (controller.isKeyReleased(InputController.KEY_RBRACKET))) { 
			camera.setTarget(getBackContestant(getContestant(camTarget),true).getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_CAMERA_HOME)) {
			camera.setTarget(session.getPlayer().getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_TRACK_CAMERA)) {
			camera.setTrackCamera(!camera.getTrackCamera());
		}
		
		if (controller.isKeyReleased(CONTROL_REAR_CAMERA)) {
			camera.setRearCamera(!camera.getRearCamera());
		}
		
		if (controller.isKeyPressed(InputController.KEY_NUMPAD_4)) { camera.setOffsetH(-1); }
		if (controller.isKeyPressed(InputController.KEY_NUMPAD_6)) { camera.setOffsetH(1); }
		if (controller.isKeyPressed(InputController.KEY_NUMPAD_8)) { camera.setOffsetV(-1); }
		if (controller.isKeyPressed(InputController.KEY_NUMPAD_2)) { camera.setOffsetV(1); }		
		if (controller.isKeyPressed(InputController.KEY_NUMPAD_5)) { camera.setOffsetH(0); camera.setOffsetV(0); }
	}
	
	/**
	 * Updates the game logic for this frame. This involves updating all 
	 * contestants in the current session, as well as doing some general checks
	 * and updates.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateGameLogic(float dt) {
		
		// Update contestants
		
		for (Contestant i : session.getContestantsSet()) {
			i.update(dt);
		}
		
		// Check for start of session
		
		if (startTimer>0f) {
			startTimer-=dt;
		} else {
			if (!session.isStarted()) {
				startSession();
			}
		}
				
		// Check for end of session
		
		if (!session.isFinished()) {
			for (Contestant i : session.getContestantsSet()) {
				if (i.getLap()>settings.laps) {
					finishSession();
				}
			}
		} else {
			if (finishTimer>0f) {
				finishTimer-=dt;
				if (finishTimer<=0f) {
					exit();
				}
			}
		}
				
		// Update camera
		
		camera.update();
		
		// Update HUD
		
		hud.setTarget(getContestant(camera.getTarget()));
		hud.setGameData(getCurrentFPS(),getCurrentUPS(),0,startTimer,finishTimer);
	}
	
	/**
	 * Sends update data for this frame. Depending on the time elapsed since the
	 * previous frame this method may no nothing.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateMultiplayerSend(float dt) {
		
		if (mpSendTimer<MULTIPLAYER_FRAME_TIME) {
			mpSendTimer+=dt;
			return;
		} else {
			mpSendTimer=0f;
		}
		
		multiplayer.sendUpdateMessage(session.getPlayer());
		
		if (multiplayer.isServer()) {
			for (Contestant i : session.getContestantsSet()) {
				if (i instanceof AI) {
					multiplayer.sendUpdateMessage(i);
				}
			}
		}
	}
	
	/**
	 * Flushes all received multiplayer messages. Only messages that have been 
	 * received since the last frame will be processed. This method notifies all
	 * multiplayer listeners.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateMultiplayerReceive(float dt) {
	
		for (Message i : multiplayer.flushReceivedMessages()) {
			fireGameEvent(new GameEvent(i.getType(),i));
		}
	}
	
	/**
	 * {@inheritDoc} 
	 */
	
	@Override
	public void onGameEvent(GameEvent event) {
		
		String type=((Message) event.getSource()).getType();
		Map<String,String> parameters=((Message) event.getSource()).getAllParameters();
		String id=parameters.get("id");
		
		if (type.equals(Multiplayer.MESSAGE_STOP)) { 
			exit(); 
		}			
		
		if (type.equals(Multiplayer.MESSAGE_START_SESSION)) { 
			startSession(); 
		}			
		
		if (type.equals(Multiplayer.MESSAGE_STOP_SESSION)) { 
			finishSession(); 
		}		
		
		if (type.equals(Multiplayer.MESSAGE_PAUSE)) { 
			paused(parameters.get("state").equals("true")); 
		}
		
		if (type.equals(Multiplayer.MESSAGE_CHAT)) {
			hud.setMessage(parameters.get("from")+": "+parameters.get("message"));
		}
		
		if (type.equals(Multiplayer.MESSAGE_DISCONNECT)) {
			exit();
			hud.setMessage(settings.getText("game.disconnectmessage",id));
		}
	}
	
	/**
	 * Starts the session. This method is called after everything that should be
	 * done pre-session has been finished. Note that this means that the start of
	 * the session could be different to the start of the game.
	 */
	
	private void startSession() {
	
		session.setStarted();
		
		if (multiplayer.isServer()) {
			multiplayer.sendStartSessionMessage();
		}
	}
	
	/**
	 * Finishes the current game. This will mark the <code>Session</code> as 
	 * finished, send stop messages to multiplayer peers. 
	 */
	
	private void finishSession() {
		
		session.setFinished();
				
		if (multiplayer.isServer()) {
			multiplayer.sendStopSessionMessage();
		}
		
		finishTimer=FINISH_TIMER;
	}
	
	/**
	 * Exits the game and returns to the menu. This method will either be
	 * activated after the session has ended, or in the event that the player
	 * terminated the game.
	 */
	
	private void exit() {
				
		if (multiplayer.isServer()) {
			multiplayer.sendStopMessage();
		}
	
		hud.cleanup();
		
		parent.stopGame();
	}
	
	/**
	 * Pauses the game. See <code>GameCore.setPaused(boolean)</code> for more
	 * information.
	 */
	
	private void paused(boolean paused) {
	
		super.setPaused(paused);
		
		multiplayer.sendPauseMessage(paused);
		
		if (paused) {
			hud.setMessage(Settings.getInstance().getText("game.paused"));
		}
	}
	
	/**
	 * Returns the <code>Contestant</code> for the specified car object. When the
	 * contestant cannot be found this method returns <code>null</code>.
	 */
	
	private Contestant getContestant(Car car) {
	
		for (Contestant i : session.getContestantsSet()) {
			if (i.getCar()==car) {
				return i;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the <code>Contestant</code> with a position of one less than the
	 * specified one. 
	 */
	
	private Contestant getFrontContestant(Contestant base,boolean cycle) {
	
		int targetPos=session.getRacePosition(base)-1;
		
		if (targetPos<1) {
			targetPos=cycle ? session.getNumContestants() : 1;
		}
		
		return session.getRacePositionID(targetPos);
	}
	
	/**
	 * Returns the <code>Contestant</code> with a position of one more than the
	 * specified one. 
	 */
	
	private Contestant getBackContestant(Contestant base,boolean cycle) {
		
		int targetPos=session.getRacePosition(base)+1;
		
		if (targetPos>session.getNumContestants()) {
			targetPos=cycle ? 1 : session.getNumContestants();
		}
		
		return session.getRacePositionID(targetPos);
	}
}
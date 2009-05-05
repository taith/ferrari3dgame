//--------------------------------------------------------------------------------
// Ferrari3D
// Game
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.Context3D;
import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.GameEventListener;
import com.dennisbijlsma.core3d.GameState;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.graphics.PlayerCamera;
import com.dennisbijlsma.ferrari3d.graphics.SoundManager;
import com.dennisbijlsma.ferrari3d.graphics.Splashscreen;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.Message;
import com.dennisbijlsma.util.swing.Popups;

/**
 * Main game loop class. The loop is active while the game is running, and will
 * pass a number of phases including the updating of controls, receiving and
 * sending of multiplayer data, and keeping general information about the session. 
 */

public class Game implements GameState, GameEventListener {

	private Core core;
	private Session session;	
	private Multiplayer multiplayer;
	private Settings settings;
	
	private Player player1;
	private Player player2;
	private HUD hud;
	private HUD hud2; //TODO
	private PlayerCamera camera;
	private PlayerCamera camera2; //TODO
	private float mpSendTimer;
	private float startTimer;
	private float finishTimer;
	private boolean exitFlag;
	
	private static final int CONTROL_MENU=Controller.KEY_ESCAPE;
	private static final int CONTROL_PAUSE=Controller.KEY_P;
	private static final int CONTROL_CHAT=Controller.KEY_M;
	private static final int CONTROL_CAMERA_1=Controller.KEY_1;
	private static final int CONTROL_CAMERA_2=Controller.KEY_2;
	private static final int CONTROL_CAMERA_3=Controller.KEY_3;
	private static final int CONTROL_CAMERA_4=Controller.KEY_4;
	private static final int CONTROL_CAMERA_5=Controller.KEY_5;
	private static final int CONTROL_CAMERA_NEXT=Controller.KEY_BRACELEFT;
	private static final int CONTROL_CAMERA_PREV=Controller.KEY_BRACERIGHT;
	private static final int CONTROL_TRACK_CAMERA=Controller.KEY_T;
	private static final int CONTROL_REAR_CAMERA=Controller.KEY_V;
	private static final int GAME_UPS=50;
	private static final float MULTIPLAYER_FRAME_TIME=0.04f;
	private static final float START_TIMER=15f;
	private static final float FINISH_TIMER=10f;

	/**
	 * Creates a new game state for the specified session.
	 * @param game The parent {@code GameCore}.
	 * @param session The session used by the game.
	 */
	
	public Game(Core core, Session session) {

		this.core = core;
		this.session = session;
		this.settings = Settings.getInstance();
		this.multiplayer = Multiplayer.getInstance();
		
		mpSendTimer = 0f;
		startTimer = (session.getMode() == Session.SessionMode.RACE) ? START_TIMER : 0f;
		finishTimer = 0f;
		exitFlag = false;
	}
	
	/**
	 * Sets up the game. This will add all cars and circuit geometry to the scene
	 * graph.<br><br>
	 * During the loading of the game a splash screen is shown.
	 */
	
	public void initGameState() {
		
		Splashscreen splash = new Splashscreen();
		splash.showSplashScreen();
		splash.setMessage(settings.getText("splashscreen.loading"), 0);
		
		SceneGraph scene = getContext().getSceneGraph();
		Controller controller = getContext().getController();
		Camera mainCamera = scene.getCamera(0);
		
		// Create background geometry
		
		Loader.createBackground(scene);
		
		// Load circuit
		
		splash.setMessage(settings.getText("splashscreen.circuit"), 40);
		
		Circuit circuit = Loader.loadCircuit(settings.circuit, scene);
		scene.getRootNode().addChild(circuit);
				
		// Load human cars
		
		splash.setMessage(settings.getText("splashscreen.cars"), 70);
		
		Car playerCar = Loader.loadCar(settings.car, scene, mainCamera);
		scene.getRootNode().addChild(playerCar);		
		
		int[] controls = settings.getControlSet();
		player1 = new Player(settings.name, session, new CarPhysics(), playerCar, 
				circuit, controller, controls);
		addContestant(player1);
		
		if (settings.splitscreen) {
			Car playerCar2 = Loader.loadCar(settings.carPlayer2, scene,scene.getCamera(1));
			scene.getRootNode().addChild(playerCar2);
			
			int[] controls2 = settings.controlsets[1];
			player2 = new Player(settings.namePlayer2, session, new CarPhysics(), 
					playerCar2, circuit, controller, controls2);
			addContestant(player2);
		}
		
		// Load multiplayer cars
		
		for (Connection i : multiplayer.getConnections()) {
			Car droneCar = Loader.loadCar(i.getMeta("car"), scene, mainCamera);
			scene.getRootNode().addChild(droneCar);
			
			Drone drone = new Drone(i.getMeta("id"), session, new CarPhysics(), droneCar, circuit);
			addContestant(drone);
			core.addGameEventListener(drone);
		}
		
		// Load AI cars
		
		if (settings.aiActive) {
			for (int i = session.getNumContestants(); i < Settings.MAX_CONTESTANTS; i++) {
				String aiCarName = settings.cars.get(0);
				Car aiCar = Loader.loadCar(aiCarName, scene, mainCamera);
				scene.getRootNode().addChild(aiCar);		
				
				AI ai = new AI("AI-" + i, session, new CarPhysics(), aiCar, circuit);
				addContestant(ai);
			}
		}
		
		// Create HUD
		
		splash.setMessage(settings.getText("splashscreen.init"), 100);
		
		hud = new HUD(getContext().getDisplay(), mainCamera, session);
		getContext().getSceneGraph().addOverlay(hud, mainCamera);
		
		if (settings.splitscreen) {
			hud2 = new HUD(getContext().getDisplay(), scene.getCamera(1), session);
			getContext().getSceneGraph().addOverlay(hud2, scene.getCamera(1));
		}
		
		// Create camera(s)
		
		camera = new PlayerCamera(mainCamera, circuit);
		camera.setTarget(player1.getCar());
		camera.setSelectedCamera(settings.defaultCamera);
		
		if (settings.splitscreen) {
			camera2 = new PlayerCamera(scene.getCamera(1), circuit);
			camera2.setTarget(player2.getCar());
			camera2.setSelectedCamera(settings.defaultCamera);
		}
		
		// Create player indicators
		
		if (Settings.getInstance().indicators) {
			for (Contestant i : session.getContestants()) {
				if (!(i instanceof Player)) {
					Loader.createIndicator(i.getCar(), i.getName());
				}
			}
		}
		
		// Load sounds
		
		if (settings.sound) {
			SoundManager.getInstance();
		}
		
		// Start

		for (Contestant i : session.getContestants()) {
			i.setPosition(circuit.getStartingGrid(session.getRacePosition(i) - 1));
			i.setOrientation(1.57f);
		}
		
		splash.hideSplashScreen();
		
		core.addGameEventListener(this);
	}
	
	/**
	 * Adds the specified contestant to the game. This method will add it to the
	 * session and will make sure it is notified of game updates.
	 */
	
	private void addContestant(Contestant c) {
		session.addContestant(c);
	}
	
	/**
	 * Called every frame during the game loop. This will update controls, send
	 * and receive multiplayer data, and check global game logic.
	 */
	
	public void updateGameState(float dt) {
		
		updateControls(dt);
		updateMultiplayerReceive(dt);
		updateMultiplayerSend(dt);

		if (!core.isPaused()) {
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
		
		Controller controller = getContext().getController();
		
		// System controls
				
		if (controller.isKeyReleased(CONTROL_MENU)) { 
			exit(); 
		}		
		
		if (controller.isKeyReleased(CONTROL_PAUSE)) { 
			paused(!core.isPaused()); 
		}
		
		if (controller.isKeyReleased(CONTROL_CHAT)) { 
			String chatMessage = Popups.inputMessage(null, settings.getText("game.chatmessage"), "");		
			if ((chatMessage != null) && (!chatMessage.trim().isEmpty())) {
				multiplayer.sendChatMessage(settings.name, chatMessage);
			} 
		}
		
		// Camera controls
		
		Car camTarget = camera.getTarget();
		
		if (controller.isKeyReleased(CONTROL_CAMERA_1)) { camera.setSelectedCamera(Car.CAMERA_COCKPIT); }
		if (controller.isKeyReleased(CONTROL_CAMERA_2)) { camera.setSelectedCamera(Car.CAMERA_T_CAM); }
		if (controller.isKeyReleased(CONTROL_CAMERA_3)) { camera.setSelectedCamera(Car.CAMERA_FOLLOW); }
		if (controller.isKeyReleased(CONTROL_CAMERA_4)) { camera.setSelectedCamera(Car.CAMERA_CHASE); }
		if (controller.isKeyReleased(CONTROL_CAMERA_5)) { camera.setSelectedCamera(Car.CAMERA_HELICOPTER); }		
		
		if (controller.isKeyReleased(CONTROL_CAMERA_NEXT)) {
			camera.setTarget(getFrontContestant(getContestant(camTarget)).getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_CAMERA_PREV)) {
			camera.setTarget(getBackContestant(getContestant(camTarget)).getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_TRACK_CAMERA)) {
			camera.setTrackCamera(!camera.getTrackCamera());
			SoundManager.getInstance().playSound(SoundManager.SoundKey.AMBIENT);
		}
		
		if (controller.isKeyReleased(CONTROL_REAR_CAMERA)) {
			camera.setRearCamera(!camera.getRearCamera());
		}
		
		if (controller.isKeyPressed(Controller.KEY_NUMPAD_4)) { camera.setOffsetH(-1); }
		if (controller.isKeyPressed(Controller.KEY_NUMPAD_6)) { camera.setOffsetH(1); }
		if (controller.isKeyPressed(Controller.KEY_NUMPAD_8)) { camera.setOffsetV(-1); }
		if (controller.isKeyPressed(Controller.KEY_NUMPAD_2)) { camera.setOffsetV(1); }		
		if (controller.isKeyPressed(Controller.KEY_NUMPAD_5)) { camera.setOffsetH(0); camera.setOffsetV(0); }
	}
	
	/**
	 * Updates the game logic for this frame. This involves updating all 
	 * contestants in the current session, as well as doing some general checks
	 * and updates.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateGameLogic(float dt) {
		
		// Update contestants
		
		for (Contestant i : session.getContestants()) {
			i.update(dt);
		}
		
		// Check for start of session
		
		if (startTimer > 0f) {
			startTimer -= dt;
		} else {
			if (!session.isStarted()) {
				startSession();
			}
		}
				
		// Check for end of session
		
		if (!session.isFinished()) {
			for (Contestant i : session.getContestants()) {
				if (i.getLap() > settings.laps) {
					finishSession();
				}
			}
		} else {
			if (finishTimer > 0f) {
				finishTimer -= dt;
				if (finishTimer <= 0f) {
					exit();
				}
			}
		}
				
		// Update camera
		
		camera.update();
		if (camera2 != null) {
			camera2.update();
		}
		
		// Update HUD
		
		if (hud != null) {
			hud.setTarget(getContestant(camera.getTarget()));
			hud.setGameData(core.getCurrentFPS(), core.getCurrentUPS(), 0, startTimer, finishTimer);
		}
		
		if (hud2 != null) {
			hud2.setTarget(player2);
			hud2.setGameData(core.getCurrentFPS(), core.getCurrentUPS(), 0, startTimer, finishTimer);
		}
	}
	
	/**
	 * Sends update data for this frame. Depending on the time elapsed since the
	 * previous frame this method may no nothing.
	 * @param dt The delta time for this frame.
	 */
	
	private void updateMultiplayerSend(float dt) {
		
		if (mpSendTimer < MULTIPLAYER_FRAME_TIME) {
			mpSendTimer += dt;
			return;
		} else {
			mpSendTimer = 0f;
		}
		
		multiplayer.sendUpdateMessage(player1);
		if (player2 != null) {
			multiplayer.sendUpdateMessage(player2);
		}
		
		if (multiplayer.isServer()) {
			for (Contestant i : session.getContestants()) {
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
			core.fireGameEvent(i.getType(), i);
		}
	}
	
	/**
	 * Removes all {@link GameEntity}s and {@link GameEventListener}s that were 
	 * added by this class.
	 */
	
	public void cleanupGameState() {
		
		core.removeGameEventListener(this);
		
		for (Contestant i : session.getContestants()) {
			if (i instanceof Drone) {
				core.removeGameEventListener((Drone) i);
			}
		}
	}
	
	/** {@inheritDoc} */
	
	public void onGameEvent(String type, Object source) {
		
		Message message = (Message) source;
		String messageType = message.getType();
		String id = message.getParameter("id");
		
		if (messageType.equals(Multiplayer.MESSAGE_STOP)) { 
			exit(); 
		}			
		
		if (messageType.equals(Multiplayer.MESSAGE_START_SESSION)) { 
			startSession(); 
		}			
		
		if (messageType.equals(Multiplayer.MESSAGE_STOP_SESSION)) { 
			finishSession(); 
		}		
		
		if (messageType.equals(Multiplayer.MESSAGE_PAUSE)) { 
			paused(message.getParameter("state").equals("true")); 
		}
		
		if (messageType.equals(Multiplayer.MESSAGE_CHAT)) {
			hud.setMessage(message.getParameter("from") + ": " + message.getParameter("message"));
		}
		
		if (messageType.equals(Multiplayer.MESSAGE_DISCONNECT)) {
			hud.setMessage(settings.getText("game.disconnectmessage", id));
			exit();
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
		
		finishTimer = FINISH_TIMER;
	}
	
	/**
	 * Exits the game and returns to the menu. This method will either be
	 * activated after the session has ended, or in the event that the player
	 * terminated the game.
	 */
	
	private void exit() {
		
		if (exitFlag) {
			return;
		} else {
			exitFlag = true;
			session.setFinished();
		}
				
		if (multiplayer.isServer()) {
			multiplayer.sendStopMessage();
		}
	
		core.changeToMenuState();
	}
	
	/**
	 * Pauses the game. See <code>GameCore.setPaused(boolean)</code> for more
	 * information.
	 */
	
	private void paused(boolean paused) {
	
		core.setPaused(paused);
		
		multiplayer.sendPauseMessage(paused);
		
		if (paused) {
			if (hud != null) { hud.setMessage(settings.getText("game.paused")); }
			if (hud2 != null) { hud2.setMessage(settings.getText("game.paused")); }
		}
	}
	
	/**
	 * Returns the <code>Contestant</code> for the specified car object. When the
	 * contestant cannot be found this method returns <code>null</code>.
	 */
	
	private Contestant getContestant(Car car) {
	
		for (Contestant i : session.getContestants()) {
			if (i.getCar() == car) {
				return i;
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the <code>Contestant</code> with a position of one less than the
	 * specified one. 
	 */
	
	private Contestant getFrontContestant(Contestant base) {
		int targetPos=session.getRacePosition(base) - 1;
		if (targetPos < 1) {
			targetPos = session.getNumContestants();
		}
		return session.getRacePositionID(targetPos);
	}
	
	/**
	 * Returns the <code>Contestant</code> with a position of one more than the
	 * specified one. 
	 */
	
	private Contestant getBackContestant(Contestant base) {
		int targetPos = session.getRacePosition(base) + 1;
		if (targetPos > session.getNumContestants()) {
			targetPos = 1;
		}
		return session.getRacePositionID(targetPos);
	}
	
	/** {@inheritDoc} */
	
	public Context3D getContext() {
		return core.getContext();
	}
}
//-----------------------------------------------------------------------------
// Ferrari3D
// Game
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.HashMap;
import java.util.Map;

import com.dennisbijlsma.core3d.Context3D;
import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.GameState;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.ferrari3d.graphics.AbstractCar;
import com.dennisbijlsma.ferrari3d.graphics.Car;
import com.dennisbijlsma.ferrari3d.graphics.Circuit;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.graphics.PlayerCamera;
import com.dennisbijlsma.ferrari3d.graphics.Splashscreen;
import com.dennisbijlsma.ferrari3d.util.Replay;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.messaging.Message;
import com.dennisbijlsma.messaging.Participant;
import nl.colorize.util.swing.Popups;

/**
 * Main game loop class. The loop is active while the game is running, and will
 * pass a number of phases including the updating of controls, receiving and
 * sending of multiplayer data, and keeping general information about the session. 
 */
public class Game implements GameState, MessageListener {

	private Ferrari3D context;
	private Session session;	
	private Settings settings;
	private Multiplayer multiplayer;
	
	private Player player1;
	private Player player2;
	private HUD hud;
	private HUD hud2; //TODO
	private PlayerCamera camera;
	private PlayerCamera camera2; //TODO
	private float startTimer;
	private float finishTimer;
	private float multiplayerTimer;
	private boolean exitFlag;
	
	private Map<Contestant,Replay> replayData;
	private boolean isReplay;
	private float replayTimer;
	
	private static final int CONTROL_MENU = Controller.KEY_ESCAPE;
	private static final int CONTROL_PAUSE = Controller.KEY_P;
	private static final int CONTROL_REPLAY = Controller.KEY_R;
	private static final int CONTROL_CHAT = Controller.KEY_M;
	private static final int CONTROL_CAMERA_1 = Controller.KEY_1;
	private static final int CONTROL_CAMERA_2 = Controller.KEY_2;
	private static final int CONTROL_CAMERA_3 = Controller.KEY_3;
	private static final int CONTROL_CAMERA_4 = Controller.KEY_4;
	private static final int CONTROL_CAMERA_5 = Controller.KEY_5;
	private static final int CONTROL_CAMERA_NEXT = Controller.KEY_BRACELEFT;
	private static final int CONTROL_CAMERA_PREV = Controller.KEY_BRACERIGHT;
	private static final int CONTROL_TRACK_CAMERA = Controller.KEY_T;
	private static final int CONTROL_REAR_CAMERA = Controller.KEY_V;
	private static final float START_TIMER = 15f;
	private static final float FINISH_TIMER = 10f;

	/**
	 * Creates a new game state for the specified session.
	 * @param session The session used by the game.
	 */
	public Game(Ferrari3D context, Session session) {

		this.context = context;
		this.session = session;
		this.settings = Settings.getInstance();
		this.multiplayer = Multiplayer.getInstanceForSession(session);
		this.multiplayer.addMessageListener(this);
		
		startTimer = (session.getMode() == Session.SessionMode.RACE) ? START_TIMER : 0f;
		finishTimer = 0f;
		multiplayerTimer = 0f;
		exitFlag = false;
		
		replayData = new HashMap<Contestant,Replay>();
		isReplay = false;
		replayTimer = 0f;
	}
	
	/**
	 * Sets up the game. This will add all cars and circuit geometry to the scene
	 * graph.
	 */
	public void initGameState() {
		
		Splashscreen splash = new Splashscreen();
		splash.showSplashScreen();
		splash.setProgress(90);
		
		// Setup world
		
		SceneGraph scene = getContext().getSceneGraph();
		Controller controller = getContext().getController();
		
		Loader.recreateCameras(scene, getContext().getDisplay(), false);
		Loader.createBackground(scene);
	
		Camera mainCamera = scene.getCamera(0);
		
		// Load circuit
		
		Circuit circuit = Loader.loadCircuit(settings.circuit, scene);
		scene.getRootNode().addChild(circuit.getModel());
				
		// Load human cars
		
		Car playerCar = Loader.loadCar(settings.car, scene, mainCamera);
		scene.getRootNode().addChild(playerCar.getModel());		
		
		player1 = new Player(settings.name, session, controller, settings.getControlSet());
		addContestant(player1, playerCar, circuit);
		
		if (settings.splitscreen) {
			Car playerCar2 = Loader.loadCar(settings.carPlayer2, scene, scene.getCamera(1));
			scene.getRootNode().addChild(playerCar2.getModel());
			
			player2 = new Player(settings.namePlayer2, session, controller, settings.controlsets[1]);
			addContestant(player2, playerCar2, circuit);
		}
		
		// Load multiplayer cars
		
		for (Participant i : multiplayer.getParticipants()) {
			if (!i.isLocal()) {
				Car droneCar = Loader.loadCar(i.getMeta("car"), scene, mainCamera);
				scene.getRootNode().addChild(droneCar.getModel());
			
				Drone drone = new Drone(i.getMeta("id"), session);
				addContestant(drone, droneCar, circuit);
				multiplayer.addMessageListener(drone);
			}
		}
		
		// Load AI cars
		
		if (settings.aiActive) {
			for (int i = session.getNumContestants(); i < Settings.MAX_CONTESTANTS; i++) {
				String aiCarName = settings.cars.get(0);
				Car aiCar = Loader.loadCar(aiCarName, scene, mainCamera);
				scene.getRootNode().addChild(aiCar.getModel());		
				
				AI ai = new AI("AI-" + i, session);
				addContestant(ai, aiCar, circuit);
			}
		}
		
		// Create HUD
		
		hud = new HUD(mainCamera, session);
		hud.setTarget(player1);
		hud.start();
		getContext().getSceneGraph().addOrthoQuad(hud, mainCamera);
		
		if (settings.splitscreen) {
			hud2 = new HUD(scene.getCamera(1), session);
			hud2.setTarget(player2);
			hud2.start();
			getContext().getSceneGraph().addOrthoQuad(hud2, scene.getCamera(1));
		}
		
		// Create camera(s)
		
		camera = new PlayerCamera(mainCamera, circuit);
		camera.setTarget((Car) player1.getCar());
		camera.setSelectedCamera(settings.defaultCamera);
		
		if (settings.splitscreen) {
			camera2 = new PlayerCamera(scene.getCamera(1), circuit);
			camera2.setTarget((Car) player2.getCar());
			camera2.setSelectedCamera(settings.defaultCamera);
		}
		
		// Start

		for (Contestant i : session.getContestants()) {
			i.setPosition(circuit.getStartingGridPosition(session.getRacePosition(i) - 1));
			i.setOrientation(1.57f);
		}
		
		splash.hideSplashScreen();
	}
	
	/**
	 * Adds the specified contestant to the game. This method will add it to the
	 * session and will make sure it is notified of game updates.
	 */
	private void addContestant(Contestant c, AbstractCar car, CircuitData circuit) {
		c.setCar(car);
		c.setCircuitData(circuit);
		session.addContestant(c);
		if (settings.enableReplays) {
			replayData.put(c, new Replay());
		}
	}
	
	/**
	 * Called every frame during the game loop. This will update controls, send
	 * and receive multiplayer data, and check global game logic.
	 */
	public void updateGameState(float dt) {
		
		updateControls(dt);
		updateMultiplayerSend(dt);
		
		multiplayer.flushReceivedMessages();

		if (!context.isPaused() && !isReplay) {
			updateGameLogic(dt);
		}
		
		if (isReplay) {
			updateReplay(dt);
		}
		
		updateGraphics();
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
			paused(!context.isPaused()); 
		}
		
		if (controller.isKeyReleased(CONTROL_REPLAY)) {
			if (settings.enableReplays) {
				if (!settings.splitscreen && (multiplayer.getNumParticipants() <= 1)) { 
					isReplay = true;
					replayTimer = replayData.get(player1).getReplayStartTime();
				}
			}
		}
		
		if (controller.isKeyReleased(CONTROL_CHAT)) { 
			String chatMessage = Popups.inputMessage(null, settings.getText("game.chatmessage"), "");		
			if ((chatMessage != null) && (chatMessage.trim().length() > 0)) {
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
			camera.setTarget((Car) getFrontContestant(getContestant(camTarget)).getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_CAMERA_PREV)) {
			camera.setTarget((Car) getBackContestant(getContestant(camTarget)).getCar());
		}
		
		if (controller.isKeyReleased(CONTROL_TRACK_CAMERA)) {
			camera.setTrackCamera(!camera.getTrackCamera());
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
			if (settings.enableReplays) {
				updateReplayData(dt, i, replayData.get(i));
			}
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
				if (i.getLap() > session.getLaps()) {
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
	}
	
	/**
	 * Updates any global graphical objects, including cameras and HUDs.
	 */
	private void updateGraphics() {
	
		// Update camera
		
		camera.update();
		if (camera2 != null) {
			camera2.update();
		}
		
		// Update HUD
		
		if (hud != null) {
			hud.setTarget(getContestant(camera.getTarget()));
			hud.setGameData(context.getCurrentFPS(), context.getCurrentUPS(), 0, startTimer, finishTimer);
			if (isReplay) {
				hud.setMessage(settings.getText("game.replay"));
			}
		}
		
		if (hud2 != null) {
			hud2.setTarget(player2);
			hud2.setGameData(context.getCurrentFPS(), context.getCurrentUPS(), 0, startTimer, finishTimer);
			if (isReplay) {
				hud2.setMessage(settings.getText("game.replay"));
			}
		}
	}
	
	/**
	 * Sends update data for this frame. Depending on the time elapsed since the
	 * previous frame this method may no nothing.
	 * @param dt The delta time for this frame.
	 */
	private void updateMultiplayerSend(float dt) {
		
		if (multiplayerTimer < multiplayer.getUpdateInterval()) {
			multiplayerTimer += dt;
			return;
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
		
		multiplayerTimer = 0f;
	}
	
	/**
	 * Updates the current positions of the contestants while the game is showing
	 * a replay.
	 */
	private void updateReplay(float dt) {
		
		replayTimer += dt;
		
		if (replayTimer >= replayData.get(player1).getReplayEndTime()) {
			isReplay = false;
			paused(true);
			return;
		}
		
		for (Contestant i : replayData.keySet()) {
			Replay replay = replayData.get(i);
			float[] data = replay.get(replayTimer);
			i.setPosition(new Vector3D(data[0], data[1], data[2]));
			i.setDirection(data[3]);
			i.setOrientation(data[4]);
			i.getCar().setPosition(data[0], data[2]);
			i.getCar().setOrientation(data[3]);
		}
	}
	
	/**
	 * Updates the replay data with the position of the specified contestant.
	 */
	private void updateReplayData(float dt, Contestant c, Replay replay) {
		replay.put(dt, c.getPosition().getX(), c.getPosition().getY(), c.getPosition().getZ(),
				c.getDirection(), c.getOrientation());
	}
	
	/**
	 * Removes all {@link GameEntity}s and {@link GameEventListener}s that were 
	 * added by this class.
	 */
	public void cleanupGameState() {
		
		multiplayer.removeMessageListener(this);
		for (Contestant i : session.getContestants()) {
			if (i instanceof Drone) {
				multiplayer.removeMessageListener((Drone) i);
			}
		}
		
		if (hud != null) { hud.stop(); }
		if (hud2 != null) { hud2.stop(); }
	}
	
	/** {@inheritDoc} */
	public void messageReceived(Message message) {
		
		String messageType = message.getType();
		String id = message.getParameter("id");
		
		if (messageType.equals(Multiplayer.MESSAGE_STOP)) { 
			exit(); 
		} else if (messageType.equals(Multiplayer.MESSAGE_START_SESSION)) { 
			startSession(); 
		} else if (messageType.equals(Multiplayer.MESSAGE_STOP_SESSION)) { 
			finishSession(); 
		} else if (messageType.equals(Multiplayer.MESSAGE_PAUSE)) { 
			paused(message.getParameter("state").equals("true")); 
		} else if (messageType.equals(Multiplayer.MESSAGE_CHAT)) {
			hud.setMessage(message.getParameter("from") + ": " + message.getParameter("message"));
		} else if (messageType.equals(Multiplayer.MESSAGE_DISCONNECT)) {
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
	
		context.changeToMenuState();
	}
	
	/**
	 * Pauses the game. See <code>GameCore.setPaused(boolean)</code> for more
	 * information.
	 */
	private void paused(boolean paused) {
	
		context.setPaused(paused);
		
		multiplayer.sendPauseMessage(paused);
		
		if (paused) {
			if (hud != null) { hud.setMessage(settings.getText("game.paused")); }
			if (hud2 != null) { hud2.setMessage(settings.getText("game.paused")); }
		} else {
			if (hud != null) { hud.setMessage(null); }
			if (hud2 != null) { hud2.setMessage(null); }
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
	
	private Contestant getFrontContestant(Contestant base) {
		int targetPos = session.getRacePosition(base) - 1;
		if (targetPos < 1) {
			targetPos = session.getNumContestants();
		}
		return session.getContestantAtRacePosition(targetPos);
	}
	
	private Contestant getBackContestant(Contestant base) {
		int targetPos = session.getRacePosition(base) + 1;
		if (targetPos > session.getNumContestants()) {
			targetPos = 1;
		}
		return session.getContestantAtRacePosition(targetPos);
	}
	
	public Context3D getContext() {
		return context;
	}
}

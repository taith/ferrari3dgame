//-----------------------------------------------------------------------------
// Ferrari3D
// Menu
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.MenuState;
import com.dennisbijlsma.core3d.ui.UIPanel;
import com.dennisbijlsma.core3d.ui.UISystem;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.menu.UIMenu;
import com.dennisbijlsma.ferrari3d.menu.UIMenuBackground;
import com.dennisbijlsma.ferrari3d.menu.UIMenuButton;
import com.dennisbijlsma.ferrari3d.menu.UIMenuChat;
import com.dennisbijlsma.ferrari3d.menu.UIMenuFlow;
import com.dennisbijlsma.ferrari3d.menu.UIMenuOption;
import com.dennisbijlsma.ferrari3d.menu.UIMenuTable;
import com.dennisbijlsma.ferrari3d.menu.UIMenuText;
import com.dennisbijlsma.ferrari3d.menu.UIMenuWidget;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.TrackRecord;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.messaging.JoinSessionException;
import com.dennisbijlsma.messaging.Message;
import com.dennisbijlsma.messaging.Participant;
import com.dennisbijlsma.messaging.Server;
import nl.colorize.util.Platform;
import nl.colorize.util.FormatUtils;
import nl.colorize.util.swing.Popups;

/**
 * The game's menu system. Each menu screen is only constructed at the moment
 * when it is first used, to reduce the initial loading time.
 */
//TODO the menu system in older versions was created in AWT and Swing, and used 
//     Beanshell script for logic. The new menu system uses native jMonkeyEngine
//     widgets and has all logic stored in this class. Because of these enormous
//     changes the state of this class is bad, and large reafactorings will be
//     performed on it for future versions.
public class Menu extends MenuState implements MessageListener {
	
	private Ferrari3D context;
	private Session session;
	private Settings settings;
	private Multiplayer multiplayer;
	
	private Map<Screen,UIMenu> menus;
	private Screen selected;
	private UIMenuBackground background;
	private int recordsPage;

	public enum Screen {
		MAIN,
		START_SESSION,
		JOIN_SESSION,
		LOBBY,
		SETTINGS,
		INFORMATION,
		RESULTS,
		RECORDS
	}
	
	private enum Action {
		EXIT,
		START_EDITOR,
		SYSTEM_INFO,
		END_SESSION,
		RECORDS,
		JOIN_SESSION,
		START_SESSION,
		START_GAME
	}

	/**
	 * Creates a new menu for the specified session object.
	 */
	public Menu(Ferrari3D context, Session session) {
		
		super(context);

		this.context = context;
		this.session = session;
		this.settings = Settings.getInstance();
		this.multiplayer = Multiplayer.getInstanceForSession(session);
		this.multiplayer.addMessageListener(this);
		
		menus = new HashMap<Screen,UIMenu>();
		selected = null;		
	}
	
	/**
	 * Rescales the menu so that it scales to the size of the display.
	 */
	@Override
	public void initGameState() {
		super.initGameState();
		getUI().rescaleToDisplay(Settings.WINDOW_WIDTH, Settings.WINDOW_HEIGHT);
	}
	
	/**
	 * Initializes the menu system. Only the global components will be loaded
	 * initially, the menu screens are not created until they are used.
	 */
	protected void initUI() {
		
		background = new UIMenuBackground();
		getUI().getRootPanel().addWidget(background.getWidget());
		for (UIMenuWidget i : background.getAdditionalWidgets()) {
			getUI().getRootPanel().addWidget(i.getWidget());
		}
				
		createMenu(Screen.MAIN);
		setSelectedMenu(Screen.MAIN);
	}
	
	/**
	 * Flushes all received multiplayer messages.
	 */
	@Override
	public void updateGameState(float dt) {
		
		super.updateGameState(dt);
		
		multiplayer.flushReceivedMessages();
		
		// Force quit
		if (context.getController().isKeyPressed(Controller.KEY_SHIFT) &&
				context.getController().isKeyPressed(Controller.KEY_Q)) {
			context.quit();
		}
	}
	
	/**
	 * Invoked (from the game thread) when a multiplayer message has been received.
	 */
	public void messageReceived(Message message) {
		
		if (message.getType().equals(Multiplayer.MESSAGE_INIT)) {
			multiplayer.sendConnectMessage();
		} else if (message.getType().equals(Multiplayer.MESSAGE_CONNECT)) {
			receiveConnectMessage(multiplayer.getParticipant(message.getParameter("id")), message);
			refreshLobbyMenu(multiplayer.isServer(), false);
			setSelectedMenu(Screen.LOBBY);
			menus.get(Screen.LOBBY).setLoading(false);
		} else if (message.getType().equals(Multiplayer.MESSAGE_DISCONNECT)) {
			Popups.messageFromSwingThread(settings.getText("menu.message.clientdisconnect", 
						message.getParameter("id")));
			setSelectedMenu(Screen.MAIN);
		} else if (message.getType().equals(Multiplayer.MESSAGE_START)) {
			startGame();
		}
	}
	
	/**
	 * Creates the menu screen with the specified key. This method can be called
	 * even if the menu already exists, in such a case this method does nothing.
	 */
	private void createMenu(Screen id) {
		
		// If the menu was already created before, exit
		if (menus.get(id) != null) {
			return;
		}
		
		UIMenu menu = new UIMenu();
		menus.put(id, menu);
		
		switch (id) {
			case MAIN : createMainMenu(menu); break;
			case START_SESSION : createStartSessionMenu(menu); break;
			case JOIN_SESSION : createJoinSessionMenu(menu); break;
			case LOBBY : createLobbyMenu(menu); break;
			case SETTINGS : createSettingsMenu(menu); break;
			case INFORMATION : createInformationMenu(menu); break;
			case RESULTS : createResultsMenu(menu); break;
			case RECORDS : createRecordsMenu(menu); break;
			default : throw new AssertionError();
		}
		
		// Refresh UI

		UISystem ui = getUI();
		ui.getRootPanel().addSubPanel(menu.getContainer());
		if (ui.isAddedToSceneGraph()) {
			// The menu system has already been created
			ui.addToSceneGraph(menu.getContainer(), ui.getNextWidgetDepth());
			ui.rescaleToDisplay(Settings.WINDOW_WIDTH, Settings.WINDOW_HEIGHT);
		}
	}
	
	/**
	 * Creates the main menu, that contains the logo and buttons to access all
	 * the other menus.
	 */
	private void createMainMenu(UIMenu menu) {

		menu.setTitle(settings.getText("menu.main.title"));

		createButton(menu, settings.getText("menu.exit.title"), Action.EXIT);
		createButton(menu, settings.getText("menu.editor.title"), Action.START_EDITOR);
		createButton(menu, settings.getText("menu.information.title"), Screen.INFORMATION);
		createButton(menu, settings.getText("menu.settings.title"), Screen.SETTINGS);
		createButton(menu, settings.getText("menu.joinsession.title"), Screen.JOIN_SESSION);
		createButton(menu, settings.getText("menu.startsession.title"), Screen.START_SESSION);

		UIMenuText logoWidget = new UIMenuText(512);
		logoWidget.paintImageRelative(Utils.loadImage("data/graphics/logo.png"), 0.5f, 0.25f, 'c');
		logoWidget.paintTextRelative(settings.getText("menu.copyright"), 0.5f, 0.91f, 'c');
		logoWidget.paintTextRelative(Ferrari3D.VERSION.toString(), 0f, 0.91f, 'l');
		createWidget(menu, 0, logoWidget);

		menu.getButton(1).setEnabled(!Utils.isWebstart());
	}
	
	/**
	 * Creates the start session menu. It contains a number of options to select
	 * a car and circuit.
	 */
	private void createStartSessionMenu(UIMenu menu) {
		
		String[] CAR_NAMES = settings.cars.toArray(new String[0]);
		String[] CIRCUIT_NAMES = settings.circuits.toArray(new String[0]);
		String[] MODES = new String[]{null, "Time", "Race"};
		String[] AI_LEVELS = new String[]{null, "Easy", "Normal", "Hard"};
		String[] MULTIPLAYER_MODES = new String[]{null, "Local network", "Internet"};
		
		menu.setTitle(settings.getText("menu.startsession.title"));
		
		createPanel(menu,settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu,settings.getText("menu.startsession.title.circuit"));
		createPanel(menu,settings.getText("menu.startsession.title.multiplayer"));
		
		createButton(menu,settings.getText("menu.back"), Screen.MAIN);
		createButton(menu,settings.getText("menu.records"), Action.RECORDS);
		createButton(menu,settings.getText("menu.start"), Action.START_SESSION);
		
		UIMenuOption nameWidget=new UIMenuOption(settings.getText("menu.startsession.name"),"name",String.class);
		UIMenuFlow carWidget=new UIMenuFlow(settings.getText("menu.startsession.car"),getCarThumbnails(),CAR_NAMES);
		UIMenuFlow circuitWidget=new UIMenuFlow(settings.getText("menu.startsession.circuit"),getCircuitThumbnails(),
				CIRCUIT_NAMES);
		UIMenuOption modeWidget=new UIMenuOption(settings.getText("menu.startsession.racetype"),"mode",1,
				MODES.length-1,MODES);
		UIMenuOption lapsWidget=new UIMenuOption(settings.getText("menu.startsession.laps"),"laps",
				Settings.MIN_LAPS,Settings.MAX_LAPS,null);
		UIMenuOption aiWidget=new UIMenuOption(settings.getText("menu.startsession.aiactive"),"aiActive",Boolean.class);
		UIMenuOption aiLevelWidget=new UIMenuOption(settings.getText("menu.startsession.ailevel"),"aiLevel",1,
				AI_LEVELS.length-1,AI_LEVELS);
		UIMenuOption splitscreenWidget=new UIMenuOption(settings.getText("menu.startsession.splitscreen"),
				"splitscreen",Boolean.class);
		final UIMenuOption name2Widget=new UIMenuOption(settings.getText("menu.startsession.namePlayer2"),
				"namePlayer2",String.class);
		final UIMenuFlow car2Widget=new UIMenuFlow(settings.getText("menu.startsession.carPlayer2"),
				getCarThumbnails(),CAR_NAMES);
		UIMenuOption multiplayerModeWidget = new UIMenuOption(settings.getText("menu.startsession.multiplayerMode"),
				"multiplayerMode", 1, MULTIPLAYER_MODES.length - 1, MULTIPLAYER_MODES);
		final UIMenuOption multiplayerSessionWidget = new UIMenuOption(settings.getText(
				"menu.startsession.multiplayerSession"), "multiplayerSession", String.class);
		
		createWidget(menu, 0, nameWidget);
		createWidget(menu, 0, carWidget);
		createWidget(menu, 1, circuitWidget);
		createWidget(menu, 1, new UIMenuText());
		createWidget(menu, 1, modeWidget);
		createWidget(menu, 1, lapsWidget);
		createWidget(menu, 1, aiWidget);
		createWidget(menu, 1, aiLevelWidget);
		createWidget(menu, 2, splitscreenWidget);
		createWidget(menu, 2, name2Widget);
		createWidget(menu, 2, car2Widget);
		createWidget(menu, 2, new UIMenuText());
		createWidget(menu, 2, multiplayerModeWidget);
		createWidget(menu, 2, multiplayerSessionWidget);
		createWidget(menu, 2, new UIMenuText(settings.getText("menu.startsession.multinote")));

		name2Widget.setEnabled(settings.splitscreen);
		car2Widget.setEnabled(settings.splitscreen);
		splitscreenWidget.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				name2Widget.setEnabled(settings.splitscreen);
				car2Widget.setEnabled(settings.splitscreen);
			}
		});
		
		multiplayerSessionWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET);
		multiplayerModeWidget.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				multiplayerSessionWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET);		
			}
		});
	}
	
	/**
	 * Creates the join session menu. It's similar to the start session menu, but
	 * also allows to select a multiplayer host.
	 */
	private void createJoinSessionMenu(UIMenu menu) {
		
		String[] MULTIPLAYER_MODES = new String[]{null, "Local network", "Internet"};
		
		menu.setTitle(settings.getText("menu.joinsession.title"));
		createPanel(menu, settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu, settings.getText("menu.joinsession.title.host"));
		
		createButton(menu, settings.getText("menu.back"), Screen.MAIN);
		createButton(menu, settings.getText("menu.joinsession.connect"), Action.JOIN_SESSION);
		
		createWidget(menu, 0, new UIMenuOption(settings.getText("menu.startsession.name"), 
				"name", String.class));
		createWidget(menu, 0, new UIMenuFlow(settings.getText("menu.startsession.car"), 
				getCarThumbnails(), settings.cars));
		
		
		UIMenuOption modeWidget = new UIMenuOption(settings.getText("menu.startsession.multiplayerMode"),
				"multiplayerMode", 1, MULTIPLAYER_MODES.length - 1, MULTIPLAYER_MODES);
		final UIMenuOption ipWidget = new UIMenuOption(settings.getText("menu.joinsession.serverip"), 
				"multiplayerServer", String.class);
		final UIMenuOption portWidget = new UIMenuOption(settings.getText("menu.joinsession.serverport"),
				"" + Settings.LOCAL_MULTIPLAYER_PORT, UIMenuOption.Buttons.NONE);
		final UIMenuOption sessionWidget = new UIMenuOption(settings.getText("menu.startsession.multiplayerSession"),
				"multiplayerSession", String.class);
		
		createWidget(menu, 1, modeWidget);
		createWidget(menu, 1, ipWidget);
		createWidget(menu, 1, portWidget);
		createWidget(menu, 1, sessionWidget);
		
		ipWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL);
		portWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL);
		sessionWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET);
		modeWidget.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				ipWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL);
				portWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL);
				sessionWidget.setEnabled(settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET);		
			}
		});
	}
	
	/**
	 * Creates the session lobby menu. It shows all information entered in the 
	 * start session menu. In multiplayer sessions, other clients that connect are
	 * also shown, and chat with each other.
	 */
	private void createLobbyMenu(UIMenu menu) {

		String[] MODES = new String[]{null, "Time", "Race"};
		String[] AI_LEVELS = new String[]{null, "Easy", "Normal", "Hard"};
		
		menu.setTitle(settings.getText("menu.lobby.title"));
		
		createButton(menu,settings.getText("menu.back"), Screen.MAIN);
		createButton(menu,settings.getText("menu.start"), Action.START_GAME);
		
		UIMenuTable table = new UIMenuTable(128, new String[]{settings.getText("menu.lobby.name"),
				settings.getText("menu.lobby.car"), settings.getText("menu.lobby.ip"),
				settings.getText("menu.lobby.version")}, new int[]{30,30,25,15});
		table.setMaxRows(6);
		createWidget(menu, 0, table);
		
		createWidget(menu, 0, new UIMenuOption(settings.getText("menu.startsession.circuit"),
				settings.circuit, UIMenuOption.Buttons.NONE));
		createWidget(menu, 0, new UIMenuOption(settings.getText("menu.startsession.trackrecord"),
				settings.getText("menu.lobby.loadingrecords"), UIMenuOption.Buttons.NONE));
		createWidget(menu, 0, new UIMenuOption(settings.getText("menu.startsession.racetype"),
				MODES[settings.mode] + " (" + settings.laps + " laps)", UIMenuOption.Buttons.NONE));
		createWidget(menu, 0, new UIMenuOption(settings.getText("menu.startsession.aiactive"),
				UIMenuOption.toBooleanText(settings.aiActive) + " (" + AI_LEVELS[settings.aiLevel] + 
				")", UIMenuOption.Buttons.NONE));
		createWidget(menu, 0, new UIMenuChat(multiplayer));
	}
	
	private void refreshLobbyMenu(boolean startButtonEnabled, boolean backButtonEnabled) {
		
		if (menus.get(Screen.LOBBY) == null) {
			createMenu(Screen.LOBBY);
		}
		
		// Refresh contestants table
		
		UIMenuTable table = (UIMenuTable) menus.get(Screen.LOBBY).getWidget(0);
		table.clearRows();
		
		String localhost = Server.getLocalHost();
		
		table.addRow(settings.name, settings.car, localhost, Ferrari3D.VERSION.toString());
		
		if (settings.splitscreen) {
			table.addRow(settings.namePlayer2, settings.carPlayer2, localhost, Ferrari3D.VERSION.toString());
		}
		
		for (Participant i : multiplayer.getParticipants()) {
			if (!i.isLocal() && (i.getMeta("id") != null)) {
				table.addRow(i.getMeta("id"), i.getMeta("car"), i.getMeta("ip"), i.getMeta("version"));
			}
		}
		
		table.repaint();
		
		// Refresh track record
		
		UIMenuOption recordWidget = (UIMenuOption) menus.get(Screen.LOBBY).getWidget(2);
		TrackRecord[] records = settings.getRecords(settings.circuit);
		
		if ((records != null) && (records.length > 0)) {
			recordWidget.setDisplayedValue(Utils.timeFormat(records[0].getTime()) + 
					" (" + records[0].getDriverName() + ")");
		}
		
		// Set buttons enabled
		
		menus.get(Screen.LOBBY).getButton(1).setEnabled(startButtonEnabled);
		menus.get(Screen.LOBBY).getButton(0).setEnabled(backButtonEnabled);
	}
	
	/**
	 * Creates the settings menu, which has a lot of options for changing game
	 * settings, organized across multiple categories.
	 */
	private void createSettingsMenu(UIMenu menu) {
	
		String[] LANGUAGES = new String[]{null, "English"};
		String[] UNITS = new String[]{null, "Kmh", "Mph", "m/s"};
		String[] CAMERAS = new String[]{null, "Cockpit", "T-Cam", "Follow", "Chase", "Helicopter"};
		String[] GRAPHICS = new String[]{null, "Low", "Medium", "High"};
		String[] CONTROLSETS = new String[]{"Standard", "Alternative"};
		
		menu.setTitle(settings.getText("menu.settings.title"));
		createPanel(menu,settings.getText("menu.settings.title.general"));
		createPanel(menu,settings.getText("menu.settings.title.graphics"));
		createPanel(menu,settings.getText("menu.settings.title.sound"));
		createPanel(menu,settings.getText("menu.settings.title.controls"));
		
		createButton(menu, settings.getText("menu.back"), Screen.MAIN);
		
		UIMenuOption languageWidget=new UIMenuOption(settings.getText("menu.settings.language"),
				LANGUAGES[settings.language],UIMenuOption.Buttons.NONE);
		UIMenuOption unitsWidget=new UIMenuOption(settings.getText("menu.settings.units"),"units",1,
				UNITS.length-1,UNITS);
		UIMenuOption cameraWidget=new UIMenuOption(settings.getText("menu.settings.camera"),
				"defaultCamera",1,CAMERAS.length-1,CAMERAS);
		UIMenuOption replayWidget = new UIMenuOption(settings.getText("menu.settings.replays"),
				"enableReplays", Boolean.class);
		UIMenuOption satWidget=new UIMenuOption(settings.getText("menu.settings.showsat"),"showSAT",Boolean.class);		
		UIMenuOption framerateWidget=new UIMenuOption(settings.getText("menu.settings.showframerate"),
				"showFramerate",Boolean.class);		
		UIMenuOption debugWidget=new UIMenuOption(settings.getText("menu.settings.debugmode"),"debug",Boolean.class);				
		UIMenuOption rendererWidget=new UIMenuOption(settings.getText("menu.settings.renderer"),"jMonkeyEngine",
				UIMenuOption.Buttons.NONE);			
		UIMenuOption fullscreenWidget=new UIMenuOption(settings.getText("menu.settings.fullscreen"),
				"fullscreen",Boolean.class);
		UIMenuOption resolutionWidget=new UIMenuOption(settings.getText("menu.settings.resolution"),
				settings.resolution.width+"x"+settings.resolution.height,UIMenuOption.Buttons.NONE);
		UIMenuOption graphicsWidget=new UIMenuOption(settings.getText("menu.settings.graphics"),
				"graphics",1,GRAPHICS.length-1,GRAPHICS);
		UIMenuOption soundWidget=new UIMenuOption(settings.getText("menu.settings.sound"),"sound",Boolean.class);
		UIMenuOption controlsetWidget=new UIMenuOption(settings.getText("menu.settings.controlset"),
				"controlset",0,CONTROLSETS.length-1,CONTROLSETS);
		UIMenuOption autogearsWidget=new UIMenuOption(settings.getText("menu.settings.autogears"),
				"autoGears",Boolean.class);		
		UIMenuOption autoreverseWidget=new UIMenuOption(settings.getText("menu.settings.autoreverse"),
				"autoReverse",Boolean.class);
		
		UIMenuText controlsText = new UIMenuText(512);		
		controlsText.paintText("Accelerate: up", 0, 20, 'l');
		controlsText.paintText("Brake: down", 0, 40, 'l');
		controlsText.paintText("Steer left: left", 0, 60, 'l');
		controlsText.paintText("Steer right: right", 0, 80, 'l');
		controlsText.paintText("Gear up: A", 0, 100, 'l');
		controlsText.paintText("Gear down: Z", 0, 120, 'l');
		controlsText.paintText("End session: Escape", 300, 20, 'l');
		controlsText.paintText("Pause: P", 300, 40, 'l');
		controlsText.paintText("Change camera: 1-5", 300, 60, 'l');
		controlsText.paintText("AI camera: [ / ]", 300, 80, 'l');
		controlsText.paintText("Track camera: T", 300, 100, 'l');
		controlsText.paintText("Rear camera: V", 300, 120, 'l');
		controlsText.paintText("Replay: R", 300, 140, 'l');
		controlsText.paintText("In-game chat: M", 300, 160, 'l');
		
		createWidget(menu,0,languageWidget);
		createWidget(menu,0,unitsWidget);
		createWidget(menu,0,cameraWidget);
		createWidget(menu,0,replayWidget);
		createWidget(menu,0,new UIMenuText());
		createWidget(menu,0,satWidget);
		createWidget(menu,0,framerateWidget);
		createWidget(menu,0,debugWidget);
		createWidget(menu,1,rendererWidget);
		createWidget(menu,1,fullscreenWidget);
		createWidget(menu,1,resolutionWidget);
		createWidget(menu,1,graphicsWidget);
		createWidget(menu,1,new UIMenuText());
		createWidget(menu,1,new UIMenuText(settings.getText("menu.message.restartforchanges")));
		createWidget(menu,2,soundWidget);
		createWidget(menu,3,controlsetWidget);
		createWidget(menu,3,autogearsWidget);
		createWidget(menu,3,autoreverseWidget);
		createWidget(menu,3,new UIMenuText());
		createWidget(menu,3,controlsText);
	}
	
	/**
	 * Creates the information menu, which shows the copyright text and some 
	 * system information.
	 */
	private void createInformationMenu(UIMenu menu) {
	
		menu.setTitle(settings.getText("menu.information.title"));
		
		createButton(menu,settings.getText("menu.back"), Screen.MAIN);
		createButton(menu,settings.getText("menu.information.systeminfo"), Action.SYSTEM_INFO);
		
		UIMenuText infoText = new UIMenuText(512);		
		infoText.paintText(settings.getText("menu.title"), 0, 20, 'l');
		infoText.paintText(settings.getText("menu.copyright"), 0, 40, 'l');
		infoText.paintText(settings.getText("menu.version") + " " + Ferrari3D.VERSION, 0, 60, 'l');
		infoText.paintText("This software is considered the intellectual property of its author", 0, 100, 'l');
		infoText.paintText("It may not be distributed or changed in any way without the", 0, 120, 'l');
		infoText.paintText("explicit approval of the author.", 0, 140, 'l');
		infoText.paintText("This software or its author are not related to the Ferrari brand", 0, 160, 'l');
		infoText.paintText("in any way.", 0, 180, 'l');
		infoText.paintText("Ferrari3D uses the following software:", 0, 220, 'l');
		infoText.paintText("Java (java.sun.com), jMonkeyEngine (jmonkeyengine.com),", 0, 240, 'l');
		infoText.paintText("LWJGL (lwjgl.org), JOrbis (jcraft.com), Quaqua (randelshofer.ch).", 0, 260, 'l');
		infoText.paintImageRelative(Utils.loadImage("data/graphics/java.png"), 1f, 0f, 'r');
		createWidget(menu, 0, infoText);
	}
	
	/**
	 * Creates the results menu, which contains the top times for the currently
	 * selected circuit.
	 */
	private void createResultsMenu(UIMenu menu) {
		
		menu.setTitle(settings.getText("menu.results.title"));
		createPanel(menu, settings.getText("menu.results.title.classification"));
		createPanel(menu, settings.getText("menu.results.title.details"));
		
		createButton(menu, settings.getText("menu.results.done"), Action.END_SESSION);
		
		UIMenuTable resultsTable = new UIMenuTable(512, new String[]{settings.getText("menu.results.name"),
				settings.getText("menu.results.car"), settings.getText("menu.results.laps"),
				settings.getText("menu.results.bestlap")}, new int[]{30, 30, 20, 20});
		createWidget(menu, 0, resultsTable);
		
		UIMenuTable detailsTable = new UIMenuTable(512, new String[]{settings.getText("menu.results.lap"),
				settings.getText("menu.results.sector1"), settings.getText("menu.results.sector2"),
				settings.getText("menu.results.sector3"), settings.getText("menu.results.laptime")},
				new int[]{20, 20, 20, 20, 20});
		createWidget(menu, 1, detailsTable);
	}
	
	private void refreshResultsMenu() {
		
		if (!session.isFinished()) {
			// No point in creating the results menu
			return;
		}
		
		if (menus.get(Screen.RESULTS) == null) {
			createMenu(Screen.RESULTS);
		}
		
		UIMenuTable resultsTable = (UIMenuTable) menus.get(Screen.RESULTS).getWidget(2);
		resultsTable.clearRows();
		
		UIMenuTable detailsTable = (UIMenuTable) menus.get(Screen.RESULTS).getWidget(3);
		detailsTable.clearRows();
	
		for (int i = 1; i <= session.getNumContestants(); i++) {
			Contestant target = session.getContestantAtRacePosition(i);
			String name = i + ". " + target.getName();
			String lap = "" + (target.getLap() - 1);
			String time = Utils.timeFormat(target.getFastestLaptime());
			resultsTable.addRow(name, target.getCarName(), lap, time);
		}	
		
		Player player = getPlayer();
		for (int i = 1; i < player.getLap(); i++) {
			String[] td = new String[5];
			td[0] = "Lap " + i;
			td[1] = Utils.timeFormat(player.getLaptime(i).getSectorTime(0), false);
			td[2] = Utils.timeFormat(player.getLaptime(i).getSectorTime(1), false);
			td[3] = Utils.timeFormat(player.getLaptime(i).getSectorTime(2), false);
			td[4] = Utils.timeFormat(player.getLaptime(i));
			detailsTable.addRow(td);
		}
		
		resultsTable.repaint();
		detailsTable.repaint();
	}
	
	@SuppressWarnings("deprecation")
	private void createRecordsMenu(UIMenu menu) {
		
		menu.setTitle(settings.getText("menu.records.title"));
		
		createPanel(menu,settings.circuits.get(0));
		
		createButton(menu,settings.getText("menu.back"), Screen.START_SESSION);
		
		UIMenuTable recordsTable=new UIMenuTable(512,new String[]{settings.getText("menu.records.name"),
				settings.getText("menu.records.car"),settings.getText("menu.records.laptime"),
				settings.getText("menu.records.date")},new int[]{30,25,20,25});
		recordsTable.setMaxRows(10);
		
		UIMenuOption pagingWidget = new UIMenuOption(settings.getText("menu.records.page"), recordsPage + 1, 
				UIMenuOption.Buttons.PLUSMIN);
		pagingWidget.getPlusButton().getWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				recordsPage++;
				refreshRecordsMenu();
			}
		});
		pagingWidget.getMinusButton().getWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				recordsPage = Math.max(recordsPage - 1, 0);
				refreshRecordsMenu();
			}
		});
		
		createWidget(menu, 0, recordsTable);
		createWidget(menu, 0, new UIMenuText());
		createWidget(menu, 0, pagingWidget);
	}
	
	
	
	private void refreshRecordsMenu() {
		
		if (menus.get(Screen.RECORDS) == null) {
			createMenu(Screen.RECORDS);
		}
		
		UIMenuTable recordsTable = (UIMenuTable) menus.get(Screen.RECORDS).getWidget(1);
		UIMenuOption pagingWidget = (UIMenuOption) menus.get(Screen.RECORDS).getWidget(3);
		TrackRecord[] records = settings.getRecords(settings.circuit);
		
		if ((records == null) || (records.length == 0)) {
			return;
		}
		
		recordsTable.clearRows();
	
		for (int i = recordsPage * 10; i < Math.min(recordsPage * 10 + 10, records.length); i++) {
			String driverName = (i + 1) + ". " + records[i].getDriverName();
			String carName = records[i].getCarName();
			String laptime = Utils.timeFormat(records[i].getTime());
			String date = records[i].getDate();
			recordsTable.addRow(driverName, carName, laptime, date);
		}
		
		recordsTable.repaint();
		
		pagingWidget.setDisplayedValue("" + (recordsPage + 1));
	}
	
	private UIPanel createPanel(UIMenu menu, String title) {
		return menu.getPanel(menu.addPanel(title));
	}

	private UIMenuButton createButton(UIMenu menu, String label) {
		UIMenuButton button = new UIMenuButton(label, UIMenuButton.ButtonType.NORMAL);
		menu.addButton(button);
		return button;
	}

	private UIMenuButton createButton(UIMenu menu, String label, final Screen targetMenu) {
		UIMenuButton button = createButton(menu, label);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				setSelectedMenu(targetMenu);
			}
		});
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the 
	 * specified action will be executed.
	 */
	private UIMenuButton createButton(UIMenu menu, String label, final Action action) {
		UIMenuButton button = createButton(menu, label);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				doAction(action);
			}
		});
		return button;
	}
	
	/**
	 * Attaches the specified widget to a menu. The widget can be created either 
	 * in this class, or in the script file.
	 */
	private void createWidget(UIMenu menu, int panelIndex, UIMenuWidget widget) {
		menu.addWidget(panelIndex, widget);
	}
	
	/**
	 * Executes an action. This method can be called by menu GUI components that
	 * want to execute a specific task. The name of the action should be on of 
	 * the {@code ACTION_XXX} fields.
	 */
	private void doAction(Action action) {
		switch (action) {
			case EXIT : context.quit(); break;
			case START_EDITOR : context.startEditor(false); break;
			case SYSTEM_INFO : showSystemInfo(); break;
			case END_SESSION : endSession(); break;
			case RECORDS : showRecords(); break;
			case JOIN_SESSION : joinSession(); break;
			case START_SESSION : startSession(); break;
			case START_GAME : startGame(); break;
			default : throw new AssertionError();
		}
	}
	
	/**
	 * Creates a new background thread that executes the specified task. During
	 * this task a indeterminate progress bar will be shown.
	 */
	private void doBackground(final Screen targetMenu, final Runnable r) {
		
		menus.get(selected).setLoading(true);
		
		Thread t = new Thread() {
			public void run() {
				r.run();
				
				context.addGameThreadTask(new Runnable() {
					public void run() {
						menus.get(selected).setLoading(false);
						setSelectedMenu(targetMenu);
					}
				});
			}
		};
		t.start();
	}
	
	/**
	 * Sets the selected menu to the menu with the specified ID. When such as menu
	 * does not exist this method will do nothing.
	 */
	public void setSelectedMenu(Screen selected) {
		
		this.selected = selected;
		
		// If the menu does not exist yet, create it
		if (menus.get(selected) == null) {
			createMenu(selected);
		}

		UIMenu menu = menus.get(selected);
		getUI().getRootPanel().setVisibleSubPanel(menu.getContainer());
		menu.setSelectedPanel(0);
		background.setTitle(menu.getTitle());
	}

	public Screen getSelectedMenu() {
		return selected;
	}
	
	/**
	 * Starts the game. When the game is running as a server it will send an 
	 * event to all other players.
	 */
	private void startGame() {
		if (multiplayer.isServer()) {
			multiplayer.sendStartMessage();
		}
		context.changeToGameState();
	}
	
	private void startSession() {
		if (!checkName(settings.name)) {
			Popups.messageFromSwingThread(settings.getText("menu.message.invalidname"));
			return;
		}
		startMultiplayer(true);
	}
	
	private void joinSession() {
		if (!checkName(settings.name)) {
			Popups.messageFromSwingThread(settings.getText("menu.message.invalidname"));
			return;
		}
		startMultiplayer(false);
	}
	
	/**
	 * Ends the existing session. This will update some things that have changes 
	 * since the previous session (e.g. track records), and select the main menu.
	 */
	private void endSession() {
		
		doBackground(Screen.MAIN, new Runnable() {
			public void run() {
				for (Contestant i : session.getContestants()) {
					TrackRecord record = new TrackRecord(i.getName(), i.getCarName(), i.getCircuitName(), 
							i.getFastestLaptime(), Ferrari3D.VERSION, FormatUtils.datetimeFormat("dd-MM-yyyy"));
					try {
						settings.saveTrackRecord(record);
					} catch (IOException e) {
						settings.getLogger().warning("Could not save records", e);
					}
				}

				settings.reloadTrackRecords();
				changeSession(context.recreateSession());
			}
		});		
	}
	
	private void startMultiplayer(boolean isServer) {
		if (isServer) {
			try {
				multiplayer.startAsServer();
				refreshLobbyMenu(true, false);
				setSelectedMenu(Screen.LOBBY);
			} catch (IOException e) {
				settings.getLogger().warning("Exception while starting multiplayer server", e);
				Popups.errorMessage(settings.getText("menu.message.servererror"));
			} catch (JoinSessionException e) {
				settings.getLogger().warning("Exception while starting multiplayer server", e);
			}
		} else {
			try {
				multiplayer.startAsClient(false);
				refreshLobbyMenu(false, false);
				setSelectedMenu(Screen.LOBBY);
				menus.get(Screen.LOBBY).setLoading(true);
			} catch (IOException e) {
				settings.getLogger().warning("Exception while starting multiplayer client", e);
				Popups.errorMessage(settings.getText("menu.message.connecterror"));
			} catch (JoinSessionException e) {
				if (JoinSessionException.REASON_NO_SUCH_SESSION.equals(e.getReason())) {
					Popups.errorMessage(settings.getText("menu.message.nosuchsession", 
							settings.multiplayerSession));
				} else if (JoinSessionException.REASON_PARTICIPANT_ALREADY_EXISTS.equals(e.getReason())) {
					Popups.errorMessage(settings.getText("menu.message.namealreadyexists", settings.name));
				} else {
					settings.getLogger().warning("Exception while starting multiplayer client", e);
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void receiveConnectMessage(Participant participant, Message message) {
		
		// Parse message
		
		participant.setMeta("id", message.getParameter("id"));
		participant.setMeta("car", message.getParameter("car"));
		participant.setMeta("circuit", message.getParameter("circuit"));
		participant.setMeta("ip", message.getParameter("ip"));
		participant.setMeta("version", message.getParameter("version"));
		
		if (!multiplayer.isServer()) {
			settings.mode = Integer.parseInt(message.getParameter("mode"));
			settings.aiActive = message.getParameter("aiActive").equals("true");
			settings.aiLevel = Integer.parseInt(message.getParameter("aiLevel"));
			settings.laps = Integer.parseInt(message.getParameter("laps"));
			
			session.setLaps(settings.laps);
		}
		
		// Checks
		
		if (!multiplayer.isServer()) {
			if (!message.getParameter("version").equals(Ferrari3D.VERSION.toString())) {
				Popups.errorMessage(settings.getText("menu.message.connectversion"));
				multiplayer.stop();
			} else if (message.getParameter("id").equals(settings.name)) {
				Popups.errorMessage(settings.getText("menu.message.connectid"));
				multiplayer.stop();
			}
		}
	}
	
	private void showSystemInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append(settings.getText("menu.systeminfo.title") + "\n");
		sb.append('\n');
		sb.append(settings.getText("menu.systeminfo.java", Platform.getJavaVersion()) + "\n");
		sb.append(settings.getText("menu.systeminfo.os", Platform.getPlatform()) + "\n");
		sb.append(settings.getText("menu.systeminfo.user", Platform.getUserAccount()) + "\n");
		sb.append(settings.getText("menu.systeminfo.locale", Locale.getDefault().getDisplayName()) + "\n");
		sb.append('\n');
		sb.append(settings.getText("menu.systeminfo.processors", "" + Platform.getSystemProcessors()) + "\n");
		sb.append(settings.getText("menu.systeminfo.arch", System.getProperty("os.arch")) + "\n");
		sb.append(settings.getText("menu.systeminfo.memory", 
				FormatUtils.memoryFormat(Utils.getConsumedMemory())) + "\n");
		Popups.messageFromSwingThread(sb.toString());
	}
	
	/**
	 * Shows the records menu. This method will (re)load the list of records, 
	 * and then select the menu.
	 */
	private void showRecords() {
		
		doBackground(Screen.RECORDS,new Runnable() { 
			public void run() {
				settings.reloadTrackRecords();
				context.addGameThreadTask(new Runnable() {
					public void run() {
						refreshRecordsMenu();
					}
				});
			}
		});		
	}
	
	/**
	 * Checks the player name for a number of criteria. When the name is invalid,
	 * this method will return false and a warning message should be shown.
	 */
	private boolean checkName(String name) {
		if ((name == null) || (name.length() == 0)) {
			return false;
		}
		if (name.indexOf('"') != -1 || name.indexOf('&') != -1 || name.indexOf('/') != -1 ||
				name.indexOf('\\') != -1 || name.indexOf('<') != -1 || name.indexOf('>') != -1) {
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the first instance of {@link Player} in the current session.
	 * @deprecated This method does not work when having multiple players. 
	 */
	@Deprecated
	private Player getPlayer() {
		for (Contestant i : session.getContestants()) {
			if (i instanceof Player) {
				return (Player) i;
			}
		}
		return null;
	}
	
	private Image[] getCarThumbnails() {	
		Image[] carThumbnails = new Image[settings.cars.size()];
		for (int i = 0; i < carThumbnails.length; i++) {
			carThumbnails[i] = Utils.loadImage("cars/" + settings.cars.get(i) + "/preview.jpg");
		}
		return carThumbnails;
	}
	
	private Image[] getCircuitThumbnails() {
		Image[] circuitThumbnails = new Image[settings.circuits.size()];
		for (int i = 0; i < circuitThumbnails.length; i++) {
			circuitThumbnails[i] = Utils.loadImage("circuits/" + settings.circuits.get(i) + "/preview.jpg");
		}
		return circuitThumbnails;
	}
	
	/**
	 * Changes the session that is used by the menu system.
	 */
	public void changeSession(Session session) {
		this.session = session;
		refreshResultsMenu();
	}
}

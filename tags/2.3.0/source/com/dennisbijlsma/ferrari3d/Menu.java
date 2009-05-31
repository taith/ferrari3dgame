//--------------------------------------------------------------------------------
// Ferrari3D
// Menu
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.Image;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import com.dennisbijlsma.ferrari3d.util.LapTime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.TrackRecord;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.Platform;
import com.dennisbijlsma.util.TextFormatter;
import com.dennisbijlsma.util.swing.Popups;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionAdapter;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Server;
import com.dennisbijlsma.xmlserver.Message;

/**
 * The game's menu system.
 */

//TODO the menu system in older versions was created in AWT and Swing, and used 
//     Beanshell script for logic. The new menu system uses native jMonkeyEngine
//     widgets and has all logic stored in this class. Because of these enormous
//     changes the state of this class is bad, and large reafactorings will be
//     performed on it for future versions.

public class Menu extends MenuState {
	
	private Core core;
	private Session session;
	private Settings settings;
	
	private Map<Screen,UIMenu> menus;
	private Screen selected;
	private UIMenuBackground background;

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
	
	public enum Action {
		EXIT,
		START_EDITOR,
		DEFAULT_SETTINGS,
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
	
	public Menu(Core core, Session session) {
		
		super(core.getContext());
		
		this.core = core;
		this.session = session;
		this.settings = Settings.getInstance();
		
		menus = new HashMap<Screen,UIMenu>();
		selected = null;
	}
	
	/**
	 * Overrides to make sure the menu system is rescaled to match the display's size.
	 */
	
	@Override
	public void initGameState() {
		super.initGameState();
		getUISystem().rescaleToDisplay(Settings.WINDOW_WIDTH, Settings.WINDOW_HEIGHT);
	}
	
	/**
	 * Initializes the menu system. Only the global components will be loaded
	 * initially, the menu screens are not created until they are used.
	 */
	
	protected void initUI() {
		
		background = new UIMenuBackground();
		getUISystem().getRootPanel().addWidget(background.getWidget());
		for (UIMenuWidget i : background.getAdditionalWidgets()) {
			getUISystem().getRootPanel().addWidget(i.getWidget());
		}
				
		createMenu(Screen.MAIN);
		setSelectedMenu(Screen.MAIN);
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
		
		// Create menu component
		
		UIMenu menu = new UIMenu();
		menus.put(id, menu);
		
		// Create menu
		
		switch (id) {
			case MAIN : createMainMenu(menu); break;
			case START_SESSION : createStartSessionMenu(menu); break;
			case JOIN_SESSION : createJoinSessionMenu(menu); break;
			case LOBBY : createLobbyMenu(menu); break;
			case SETTINGS : createSettingsMenu(menu); break;
			case INFORMATION : createInformationMenu(menu); break;
			case RESULTS : createResultsMenu(menu); break;
			case RECORDS : createRecordsMenu(menu); break;
			default : throw new IllegalStateException("Invalid screen: " + id);
		}
		
		// Refresh UI

		UISystem ui = getUISystem();
		ui.getRootPanel().addSubPanel(menu.getContainer());
		if (ui.getNode() != null) {
			// The menu system has already been created
			ui.addToSceneGraph(menu.getContainer(), getUISystem().getNextWidgetDepth());
			ui.updatePositions();
		}
	}
	
	
	
	private void createMainMenu(UIMenu menu) {

		menu.setTitle(settings.getText("menu.main.title"));

		createButton(menu, settings.getText("menu.exit.title"), 'x', Action.EXIT);
		createButton(menu, settings.getText("menu.editor.title"), 'E', Action.START_EDITOR);
		createButton(menu, settings.getText("menu.information.title"), 'I', Screen.INFORMATION);
		createButton(menu, settings.getText("menu.settings.title"), 't', Screen.SETTINGS);
		createButton(menu, settings.getText("menu.joinsession.title"), 'J', Screen.JOIN_SESSION);
		createButton(menu, settings.getText("menu.startsession.title"), 'S', Screen.START_SESSION);

		UIMenuText logoWidget = new UIMenuText(512);
		logoWidget.paintImageRelative(Utils.loadImage("data/graphics/logo.png"), 0.5f, 0.25f, 'c');
		logoWidget.paintTextRelative(settings.getText("menu.copyright"), 0.5f, 0.91f, 'c');
		logoWidget.paintTextRelative(Ferrari3D.VERSION, 0f, 0.91f, 'l');
		createWidget(menu, 0, logoWidget);

		menu.getButton(1).setEnabled(!Utils.isWebstart());
	}
	
	
	
	private void createStartSessionMenu(UIMenu menu) {
		
		final String[] CAR_NAMES=settings.cars.toArray(new String[0]);
		final String[] CIRCUIT_NAMES=settings.circuits.toArray(new String[0]);
		final String[] MODES=new String[] {null,"Time","Race"};
		final String[] AI_LEVELS=new String[] {null,"Easy","Normal","Hard"};
		
		menu.setTitle(settings.getText("menu.startsession.title"));
		
		createPanel(menu,settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu,settings.getText("menu.startsession.title.circuit"));
		createPanel(menu,settings.getText("menu.startsession.title.multiplayer"));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.MAIN);
		createButton(menu,settings.getText("menu.records"),'R',Action.RECORDS);
		createButton(menu,settings.getText("menu.start"),'S',Action.START_SESSION);
		
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
		UIMenuText noticeWidget=new UIMenuText();
		noticeWidget.paintText(settings.getText("menu.startsession.multinote"));
		
		createWidget(menu,0,nameWidget);
		createWidget(menu,0,new UIMenuText());
		createWidget(menu,0,carWidget);
		createWidget(menu,1,circuitWidget);
		createWidget(menu,1,new UIMenuText());
		createWidget(menu,1,modeWidget);
		createWidget(menu,1,lapsWidget);
		createWidget(menu,1,aiWidget);
		createWidget(menu,1,aiLevelWidget);
		createWidget(menu,2,splitscreenWidget);
		createWidget(menu,2,name2Widget);
		createWidget(menu,2,new UIMenuText());
		createWidget(menu,2,car2Widget);
		createWidget(menu,2,new UIMenuText());
		createWidget(menu,2,noticeWidget);
		
		splitscreenWidget.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				name2Widget.setEnabled(settings.splitscreen);
				car2Widget.setEnabled(settings.splitscreen);
			}
		});
		
		name2Widget.setEnabled(settings.splitscreen);
		car2Widget.setEnabled(settings.splitscreen);
	}
	
	
	
	private void createJoinSessionMenu(UIMenu menu) {
		
		final String[] CAR_NAMES=settings.cars.toArray(new String[0]);
		
		menu.setTitle(settings.getText("menu.joinsession.title"));
		
		createPanel(menu,settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu,settings.getText("menu.joinsession.title.server"));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.MAIN);
		createButton(menu,settings.getText("menu.joinsession.connect"),'C',Action.JOIN_SESSION);
		
		// Add widgets
		
		UIMenuOption nameWidget=new UIMenuOption(settings.getText("menu.startsession.name"),"name",String.class);
		UIMenuFlow carWidget=new UIMenuFlow(settings.getText("menu.startsession.car"),getCarThumbnails(),CAR_NAMES);
		UIMenuOption hostWidget=new UIMenuOption(settings.getText("menu.joinsession.serverip"),"server",String.class);
		UIMenuOption portWidget=new UIMenuOption(settings.getText("menu.joinsession.serverport"),
				""+Settings.MULTIPLAYER_PORT,UIMenuOption.Buttons.NONE);
		
		createWidget(menu,0,nameWidget);
		createWidget(menu,0,new UIMenuText());
		createWidget(menu,0,carWidget);
		createWidget(menu,1,hostWidget);
		createWidget(menu,1,portWidget);
	}
	
	
	
	private void createLobbyMenu(UIMenu menu) {

		final String[] MODES=new String[] {null,"Time","Race"};
		final String[] AI_LEVELS=new String[] {null,"Easy","Normal","Hard"};
		
		menu.setTitle(settings.getText("menu.lobby.title"));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.MAIN);
		createButton(menu,settings.getText("menu.start"),'S',Action.START_GAME);
		
		UIMenuTable table=new UIMenuTable(128,new String[]{settings.getText("menu.lobby.name"),settings.getText("menu.lobby.car"),
				settings.getText("menu.lobby.ip"),settings.getText("menu.lobby.version")},new int[]{30,30,25,15});
		UIMenuOption circuitWidget=new UIMenuOption(settings.getText("menu.startsession.circuit"),settings.circuit,
				UIMenuOption.Buttons.NONE);
		UIMenuOption recordWidget=new UIMenuOption(settings.getText("menu.startsession.trackrecord"),
				settings.getText("menu.lobby.loadingrecords"),UIMenuOption.Buttons.NONE);
		UIMenuOption modeWidget=new UIMenuOption(settings.getText("menu.startsession.racetype"),MODES[settings.mode]+
				" ("+settings.laps+" laps)",UIMenuOption.Buttons.NONE);
		UIMenuOption aiWidget=new UIMenuOption(settings.getText("menu.startsession.aiactive"),
				UIMenuOption.toBooleanText(settings.aiActive)+" ("+AI_LEVELS[settings.aiLevel]+")",
				UIMenuOption.Buttons.NONE);
		UIMenuChat chatWidget=new UIMenuChat(Multiplayer.getInstance());
		
		createWidget(menu,0,table);		
		createWidget(menu,0,circuitWidget);
		createWidget(menu,0,recordWidget);
		createWidget(menu,0,modeWidget);
		createWidget(menu,0,aiWidget);		
		createWidget(menu,0,new UIMenuText());
		createWidget(menu,0,chatWidget);
	}
	
	
	
	private void refreshLobbyMenu(boolean startButtonEnabled, boolean backButtonEnabled) {
		
		if (menus.get(Screen.LOBBY) == null) {
			createMenu(Screen.LOBBY);
		}
		
		// Refresh contestants table
		
		UIMenuTable table = (UIMenuTable) menus.get(Screen.LOBBY).getWidget(0);
		String localhost = Server.getLocalHost().getHostAddress();
		
		table.addRow(settings.name, settings.car, localhost, Ferrari3D.VERSION);
		
		if (settings.splitscreen) {
			table.addRow(settings.namePlayer2, settings.carPlayer2, localhost, Ferrari3D.VERSION);
		}
		
		for (Connection i : Multiplayer.getInstance().getConnections()) {
			table.addRow(i.getMeta("id"), i.getMeta("car"), i.getMeta("ip"), i.getMeta("version"));
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
		
		menus.get(Screen.LOBBY).getButton(0).setEnabled(startButtonEnabled);
		menus.get(Screen.LOBBY).getButton(1).setEnabled(backButtonEnabled);
	}
	
	
	
	private void createSettingsMenu(UIMenu menu) {
	
		final String[] LANGUAGES=new String[] {null,"English"};
		final String[] UNITS=new String[] {null,"Kmh","Mph","m/s"};
		final String[] CAMERAS=new String[] {null,"Cockpit","T-Cam","Follow","Chase","Helicopter"};
		final String[] GRAPHICS=new String[] {null,"Low","Medium","High"};
		final String[] CONTROLSETS=new String[] {"Standard","Alternative"};
		
		menu.setTitle(settings.getText("menu.settings.title"));
		
		createPanel(menu,settings.getText("menu.settings.title.general"));
		createPanel(menu,settings.getText("menu.settings.title.graphics"));
		createPanel(menu,settings.getText("menu.settings.title.sound"));
		createPanel(menu,settings.getText("menu.settings.title.controls"));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.MAIN);
		createButton(menu,settings.getText("menu.settings.defaults"),'D');
		
		UIMenuOption languageWidget=new UIMenuOption(settings.getText("menu.settings.language"),
				LANGUAGES[settings.language],UIMenuOption.Buttons.NONE);
		UIMenuOption unitsWidget=new UIMenuOption(settings.getText("menu.settings.units"),"units",1,
				UNITS.length-1,UNITS);
		UIMenuOption cameraWidget=new UIMenuOption(settings.getText("menu.settings.camera"),
				"defaultCamera",1,CAMERAS.length-1,CAMERAS);
		UIMenuOption satWidget=new UIMenuOption(settings.getText("menu.settings.showsat"),"showSAT",Boolean.class);		
		UIMenuOption radarWidget=new UIMenuOption(settings.getText("menu.settings.showradar"),
				"showRadar",Boolean.class);		
		UIMenuOption framerateWidget=new UIMenuOption(settings.getText("menu.settings.showframerate"),
				"showFramerate",Boolean.class);		
		UIMenuOption indicatorWidget=new UIMenuOption(settings.getText("menu.settings.indicators"),
				"indicators",Boolean.class);
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
		
		UIMenuText controlsText=new UIMenuText(512);		
		controlsText.paintText("Accelerate: "+settings.getControlSet()[0],0,20,'l');
		controlsText.paintText("Brake: "+settings.getControlSet()[1],0,40,'l');
		controlsText.paintText("Steer left: "+settings.getControlSet()[2],0,60,'l');
		controlsText.paintText("Steer right: "+settings.getControlSet()[3],0,80,'l');
		controlsText.paintText("Gear up: "+settings.getControlSet()[4],0,100,'l');
		controlsText.paintText("Gear down: "+settings.getControlSet()[5],0,120,'l');
		controlsText.paintText("End session: Escape",300,20,'l');
		controlsText.paintText("Pause: P",300,40,'l');
		controlsText.paintText("Change camera: 1-5",300,60,'l');
		controlsText.paintText("AI camera: [ / ]",300,80,'l');
		controlsText.paintText("Track camera: T",300,100,'l');
		controlsText.paintText("Rear camera: V",300,120,'l');
		controlsText.paintText("In-game chat: M",300,140,'l');
		
		createWidget(menu,0,languageWidget);
		createWidget(menu,0,unitsWidget);
		createWidget(menu,0,cameraWidget);
		createWidget(menu,0,new UIMenuText());
		createWidget(menu,0,satWidget);
		createWidget(menu,0,radarWidget);
		createWidget(menu,0,framerateWidget);
		createWidget(menu,0,indicatorWidget);
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
	
	
	
	private void createInformationMenu(UIMenu menu) {
	
		menu.setTitle(settings.getText("menu.information.title"));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.MAIN);
		createButton(menu,settings.getText("menu.information.systeminfo"),'I',Action.SYSTEM_INFO);
		
		UIMenuText infoText=new UIMenuText(512);		
		infoText.paintText(settings.getText("menu.title"),0,20,'l');
		infoText.paintText(settings.getText("menu.copyright"),0,40,'l');
		infoText.paintText(settings.getText("menu.version")+" "+Ferrari3D.VERSION,0,60,'l');
		infoText.paintText("This software is considered the intellectual property of its author",0,100,'l');
		infoText.paintText("It may not be distributed or changed in any way without the",0,120,'l');
		infoText.paintText("explicit approval of the author.",0,140,'l');
		infoText.paintText("This software or its author are not related to the Ferrari brand",0,160,'l');
		infoText.paintText("in any way.",0,180,'l');
		infoText.paintText("Ferrari3D uses the following software:",0,220,'l');
		infoText.paintText("Java (java.sun.com), jMonkeyEngine (jmonkeyengine.com),",0,240,'l');
		infoText.paintText("LWJGL (lwjgl.org), JOrbis (jcraft.com), Quaqua (randelshofer.ch).",0,260,'l');
		infoText.paintImageRelative(Utils.loadImage("data/graphics/java.png"),1f,0f,'r');
		createWidget(menu,0,infoText);
	}
	
	
	
	private void createResultsMenu(UIMenu menu) {
		
		menu.setTitle(settings.getText("menu.results.title"));
		
		createPanel(menu,settings.getText("menu.results.title.classification"));
		createPanel(menu,settings.getText("menu.results.title.details"));
		
		createButton(menu,settings.getText("menu.results.done"),'E',Action.END_SESSION);
		
		UIMenuTable resultsTable=new UIMenuTable(512,new String[]{settings.getText("menu.results.name"),settings.
				getText("menu.results.car"),settings.getText("menu.results.laps"),settings.
				getText("menu.results.bestlap")},new int[]{30,30,20,20});
		createWidget(menu,0,resultsTable);
		
		UIMenuTable detailsTable=new UIMenuTable(512,new String[]{settings.getText("menu.results.lap"),settings.
				getText("menu.results.sector1"),settings.getText("menu.results.sector2"),settings.
				getText("menu.results.sector3"),settings.getText("menu.results.laptime")},
				new int[]{20,20,20,20,20});
		createWidget(menu,1,detailsTable);
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
		UIMenuTable detailsTable = (UIMenuTable) menus.get(Screen.RESULTS).getWidget(3);
	
		for (int i = 1; i <= session.getNumContestants(); i++) {
			Contestant target = session.getRacePositionID(i);
			String name = i + ". " + target.getName();
			String lap = "" + (target.getLap() - 1);
			String time = Utils.timeFormat(target.getBestLaptime());
			resultsTable.addRow(name, target.getCar().getName(), lap, time);
		}	
		
		Player player = getPlayer();
		for (int i = 1; i < player.getLap(); i++) {
			String[] td = new String[5];
			td[0] = "Lap " + i;
			td[1] = Utils.timeFormat(player.getLaptime(i).getSectorTime(0));
			td[2] = Utils.timeFormat(player.getLaptime(i).getSectorTime(1));
			td[3] = Utils.timeFormat(player.getLaptime(i).getSectorTime(2));
			td[4] = Utils.timeFormat(player.getLaptime(i).getTime());
			detailsTable.addRow(td);
		}
		
		resultsTable.repaint();
		detailsTable.repaint();
	}
	
	
	
	private void createRecordsMenu(UIMenu menu) {
		
		menu.setTitle(settings.getText("menu.records.title"));
		
		createPanel(menu,settings.circuits.get(0));
		
		createButton(menu,settings.getText("menu.back"),'B',Screen.START_SESSION);
		
		UIMenuTable recordsTable=new UIMenuTable(512,new String[]{settings.getText("menu.records.name"),
				settings.getText("menu.records.car"),settings.getText("menu.records.laptime"),
				settings.getText("menu.records.date")},new int[]{30,25,20,25});
		recordsTable.setMaxRows(20);
		createWidget(menu,0,recordsTable);
	}
	
	
	
	private void refreshRecordsMenu() {
		
		if (menus.get(Screen.RECORDS) == null) {
			createMenu(Screen.RECORDS);
		}
		
		UIMenuTable recordsTable = (UIMenuTable) menus.get(Screen.RECORDS).getWidget(1);
		TrackRecord[] records = settings.getRecords(settings.circuit);
		
		if ((records == null) || (records.length == 0)) {
			return;
		}
		
		recordsTable.clearRows();
	
		for (int i = 0; i < records.length; i++) {
			String driverName = (i + 1) + ". " + records[i].getDriverName();
			String carName = records[i].getCarName();
			String laptime = Utils.timeFormat(records[i].getTime());
			String date = records[i].getDate();
			recordsTable.addRow(driverName, carName, laptime, date);
		}
		
		recordsTable.repaint();
	}
	
	/**
	 * Creates a new panel and attaches it to the specified menu screen.
	 */

	private UIPanel createPanel(UIMenu menu,String title) {
		
		int panelIndex=menu.addPanel(title);
		return menu.getPanel(panelIndex);
	}
	
	/**
	 * Creates a new menu button for the specified menu screen.
	 */

	private UIMenuButton createButton(UIMenu menu,String label,char shortkey) {
		
		UIMenuButton button=new UIMenuButton(label,UIMenuButton.ButtonType.NORMAL,shortkey);
		menu.addButton(button);
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the
	 * button will change the selected menu to a new value. 
	 */

	private UIMenuButton createButton(UIMenu menu,String label,char shortkey,
			final Screen targetMenu) {
		
		UIMenuButton button=createButton(menu,label,shortkey);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				setSelectedMenu(targetMenu);
			}
		});
		
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the 
	 * specified action will be executed.
	 */

	private UIMenuButton createButton(UIMenu menu,String label,char shortkey,
			final Action action) {
		
		UIMenuButton button=createButton(menu,label,shortkey);
		button.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				doAction(action);
			}
		});
		
		return button;
	}
	
	/**
	 * Attaches the specified widget to a menu. The widget can be created either 
	 * in this class, or in the script file.
	 */

	private void createWidget(UIMenu menu,int panelIndex,UIMenuWidget widget) {
	
		menu.addWidget(panelIndex,widget);
	}
	
	/**
	 * Executes an action. This method can be called by menu GUI components that
	 * want to execute a specific task. The name of the action should be on of 
	 * the <code>ACTION_*</code> fields.
	 */

	private void doAction(Action action) {
		
		switch (action) {
			case EXIT : core.exit(); break;
			case START_EDITOR : core.startEditor(); break;
			case DEFAULT_SETTINGS : settings.defaults(); break;
			case SYSTEM_INFO : showSystemInfo(); break;
			case END_SESSION : endSession(); break;
			case RECORDS : showRecords(); break;
			case JOIN_SESSION : joinSession(); break;
			case START_SESSION : startSession(); break;
			case START_GAME : startGame(); break;
			default : throw new IllegalStateException("Invalid action: "+action);
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
				
				core.addGameThreadTask(new Runnable() {
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
		getUISystem().getRootPanel().setVisibleSubPanel(menu.getContainer());
		menu.setSelectedPanel(0);
		background.setTitle(menu.getTitle());
	}
	
	/**
	 * Returns the ID of the currently active menu. When no menu is currently 
	 * active this method will return null.
	 */

	public Screen getSelectedMenu() {
		
		return selected;
	}
	
	/**
	 * Starts the game. When the game is running as a server it will send an 
	 * event to all other players.
	 */
	
	private void startGame() {
	
		if (Multiplayer.getInstance().isServer()) {
			Multiplayer.getInstance().sendStartMessage();
		}
	
		core.changeToGameState();
	}
	
	/**
	 * Starts a new session.
	 * @deprecated This method contains some hard-coded links to menu components.
	 */
	
	@Deprecated
	private void startSession() {
		
		if (!checkName(settings.name)) {
			Popups.messageFromSwingThread(settings.getText("menu.message.invalidname"));
			return;
		}
	
		createMultiplayerHandler(true);
		
		refreshLobbyMenu(false, true);
		setSelectedMenu(Screen.LOBBY);
	}
	
	/**
	 * Joins an existing session.
	 * @deprecated This method contains some hard-coded links to menu components.
	 */
	
	@Deprecated
	private void joinSession() {
		
		if (!checkName(settings.name)) {
			Popups.messageFromSwingThread(settings.getText("menu.message.invalidname"));
			return;
		}
		
		createMultiplayerHandler(false);
	}
	
	/**
	 * Ends the existing session. This will update some things that have changes 
	 * since the previous session (e.g. track records), and select the main menu.
	 */

	private void endSession() {
		
		doBackground(Screen.MAIN, new Runnable() {
			public void run() {
				for (Contestant i : session.getContestants()) {
					String driverName = i.getName();
					String carName = i.getCar().getCarName();
					String circuitName = i.getCircuit().getCircuitName();
					LapTime laptime = i.getBestLaptime();
					String version = Ferrari3D.VERSION;
					String date = TextFormatter.dateFormat(false);
					TrackRecord tr = new TrackRecord(driverName, carName, circuitName, laptime, version, date);
					
					try {
						settings.saveTrackRecord(tr);
					} catch (IOException e) {
						settings.getLogger().warning("Could not save records", e);
					}
				}

				settings.reloadTrackRecords();
				changeSession(core.recreateSession());
			}
		});		
	}
	
	/**
	 * Sets up the multiplayer environment. This will create a server object when
	 * running as a server, or join an existing server when running as a client.
	 * @deprecated This method contains some hard-coded links to menu components 
	 *             and should be done in the Multiplayer class. 
	 */
	
	@Deprecated
	private void createMultiplayerHandler(boolean isServer) {
		
		final Server server=Multiplayer.getInstance();
		
		// Create connection listener
			
		ConnectionListener listener=new ConnectionAdapter() {
			@Override
			public void messageReceived(Connection c,Message message) {
				if (message.getType().equals(Multiplayer.MESSAGE_INIT)) {
					server.send(c,setConnectMessage());
				}
				if (message.getType().equals(Multiplayer.MESSAGE_DISCONNECT)) {
					Popups.messageFromSwingThread(settings.getText("menu.message.clientdisconnect", c.getHost()));
					core.addGameThreadTask(new Runnable() {
						public void run() {
							setSelectedMenu(Screen.MAIN);
						}
					});
				}
				if (message.getType().equals(Multiplayer.MESSAGE_CONNECT)) {
					getConnectMessage(c,message);
					
					if (!Multiplayer.getInstance().isServer()) {
						core.addGameThreadTask(new Runnable() {
							public void run() {
								refreshLobbyMenu(false, false);
								setSelectedMenu(Screen.LOBBY);
							}
						});
					} else {
						core.addGameThreadTask(new Runnable() {
							public void run() {
								refreshLobbyMenu(false, true);
								setSelectedMenu(Screen.LOBBY);
							}
						});
					}
				}
				if (message.getType().equals(Multiplayer.MESSAGE_START)) {
					core.addGameThreadTask(new Runnable() {
						public void run() {
							startGame();
						}
					});
				}
			}
		};
		server.addConnectionListener(listener);
		
		// Start/join session
		
		if (isServer) {
			server.startServer();
		} else {
			try {
				server.startClient(settings.server,Settings.MULTIPLAYER_PORT);
			} catch (IOException e) {
				Popups.errorMessage(settings.getText("menu.message.connecterror")+" '"+settings.server+"'.");
			}
		}
	}
	
	/**
	 * Creates and returns a new connect message.
	 * @deprecated This should be done in the multiplayer class.
	 */
	
	@Deprecated
	private Message setConnectMessage() {
	
		Message message=new Message();
		message.setType(Multiplayer.MESSAGE_CONNECT);
		message.setParameter("id",settings.name);
		message.setParameter("car",settings.car);
		message.setParameter("circuit",settings.circuit);
		message.setParameter("mode",""+settings.mode);
		message.setParameter("aiActive",""+settings.aiActive);
		message.setParameter("aiLevel",""+settings.aiLevel);
		message.setParameter("laps",""+settings.laps);
		message.setParameter("ip",Multiplayer.getLocalHost().getHostAddress());
		message.setParameter("version",Ferrari3D.VERSION);
		return message;
	}
	
	/**
	 * Parses a received connect message.
	 * @Deprecated This should be done in the Multiplayer class.
	 */
	
	@Deprecated
	private void getConnectMessage(Connection connection,Message message) {
		
		// Parse message
		
		connection.setMeta("id",message.getParameter("id"));
		connection.setMeta("car",message.getParameter("car"));
		connection.setMeta("circuit",message.getParameter("circuit"));
		connection.setMeta("ip",message.getParameter("ip"));
		connection.setMeta("version",message.getParameter("version"));
		
		settings.mode=Integer.parseInt(message.getParameter("mode"));
		settings.aiActive=message.getParameter("aiActive").equals("true");
		settings.aiLevel=Integer.parseInt(message.getParameter("aiLevel"));
		settings.laps=Integer.parseInt(message.getParameter("laps"));
		
		// Checks
		
		if (Multiplayer.getInstance().isServer()) {
			return;
		}
		
		if (!message.getParameter("version").equals(Ferrari3D.VERSION)) {
			Popups.errorMessage(settings.getText("menu.message.connectversion"));
			Multiplayer.getInstance().stop();
		}
		
		if (message.getParameter("id").equals(settings.name)) {
			Popups.errorMessage(settings.getText("menu.message.connectid"));
			Multiplayer.getInstance().stop();
		}
	}
	
	/**
	 * Shows a dialog window displaying information about the current system. This
	 * includes fields like Java version and operating system.
	 */
	
	private void showSystemInfo() {
		
		Runtime runtime=Runtime.getRuntime();
		int processors=runtime.availableProcessors();
		String arch=System.getProperty("os.arch");
		long memory=runtime.totalMemory();
		
		StringBuilder sb=new StringBuilder();
		sb.append(settings.getText("menu.systeminfo.title")+"\n");
		sb.append("\n");
		sb.append(settings.getText("menu.systeminfo.java")+" "+Platform.getJavaVersion()+"\n");
		sb.append(settings.getText("menu.systeminfo.os")+" "+Platform.getPlatform()+"\n");
		sb.append(settings.getText("menu.systeminfo.user")+" "+Platform.getUserAccount()+"\n");
		sb.append(settings.getText("menu.systeminfo.locale")+" "+Platform.getUserLocale().getDisplayName()+"\n");
		sb.append("\n");
		sb.append(settings.getText("menu.systeminfo.processors")+" "+processors+"\n");
		sb.append(settings.getText("menu.systeminfo.arch")+" "+arch+"\n");
		sb.append(settings.getText("menu.systeminfo.memory")+" "+TextFormatter.memoryFormat(memory)+"\n");
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
				core.addGameThreadTask(new Runnable() {
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
	
		if ((name==null) || (name.equals("null")) || (name.length()==0)) {
			return false;
		}
		
		if (name.indexOf('\'')!=-1) { return false; }
		if (name.indexOf('"')!=-1) { return false; }
		if (name.indexOf('&')!=-1) { return false; }
		if (name.indexOf('/')!=-1) { return false; }
		if (name.indexOf('\\')!=-1) { return false; }
		if (name.indexOf('<')!=-1) { return false; }
		if (name.indexOf('>')!=-1) { return false; }
		
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
	
	/**
	 * Returns an array with thumbnail images for all cars.
	 */
	
	private Image[] getCarThumbnails() {
		
		Image[] carThumbnails=new Image[settings.cars.size()];
		for (int i=0; i<carThumbnails.length; i++) {
			String name=settings.cars.get(i);
			carThumbnails[i]=Utils.loadImage("cars/"+name+"/preview.jpg");
		}
		
		return carThumbnails;
	}
	
	/**
	 * Returns an array with thumbnail images for all circuits.
	 */
	
	private Image[] getCircuitThumbnails() {
		
		Image[] circuitThumbnails=new Image[settings.circuits.size()];
		for (int i=0; i<circuitThumbnails.length; i++) {
			String name=settings.circuits.get(i);
			circuitThumbnails[i]=Utils.loadImage("circuits/"+name+"/preview.jpg");
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
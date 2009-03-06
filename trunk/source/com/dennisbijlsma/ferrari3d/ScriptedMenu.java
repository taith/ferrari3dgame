//--------------------------------------------------------------------------------
// Ferrari3D
// ScriptedMenu
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.FocusManager;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;

import com.dennisbijlsma.ferrari3d.menu.MenuButton;
import com.dennisbijlsma.ferrari3d.menu.MenuChat;
import com.dennisbijlsma.ferrari3d.menu.MenuFlow;
import com.dennisbijlsma.ferrari3d.menu.MenuOption;
import com.dennisbijlsma.ferrari3d.menu.MenuPanel;
import com.dennisbijlsma.ferrari3d.menu.MenuScreen;
import com.dennisbijlsma.ferrari3d.menu.MenuTable;
import com.dennisbijlsma.ferrari3d.menu.MenuText;
import com.dennisbijlsma.ferrari3d.menu.MenuWidget;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.TrackRecord;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.Resources;
import com.dennisbijlsma.util.TextFormatter;
import com.dennisbijlsma.util.swing.Popups;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionAdapter;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Server;
import com.dennisbijlsma.xmlserver.Message;

/**
 * Menu framework. This class uses Beanshell script files in previous versions, 
 * explaining the name. Future versions of this class will be refactored to
 * move back to a Java-only menu system.
 */

@SuppressWarnings("deprecation")
public class ScriptedMenu extends JPanel {
	
	private Ferrari3D parent;
	private Session session;
	private Settings settings;
	
	private Map<String,MenuScreen> menus;
	private String selected;
	
	private Image backgroundImage;

	public static final String MENU_EMPTY="empty";
	public static final String MENU_MAIN="main";
	public static final String MENU_START_SESSION="start_session";
	public static final String MENU_JOIN_SESSION="join_session";
	public static final String MENU_LOBBY="session_lobby";
	public static final String MENU_SETTINGS="settings";
	public static final String MENU_INFORMATION="information";
	public static final String MENU_RESULTS="results";
	public static final String MENU_RECORDS="records";
	
	private static final int ACTION_EXIT=1;
	private static final int ACTION_START_EDITOR=2;
	private static final int ACTION_DEFAULT_SETTINGS=3;
	private static final int ACTION_SYSTEM_INFO=4;
	private static final int ACTION_END_SESSION=5;
	private static final int ACTION_RECORDS=6;
	private static final int ACTION_JOIN_SESSION=7;
	private static final int ACTION_START_SESSION=8;
	private static final int ACTION_START_GAME=9;
	
	private static final String SCRIPT_LOCATION="data/menu/";
	private static final String SCRIPT_ENCODING="UTF-8";

	/**
	 * Creates a new menu framework. This will start loading all menus from the
	 * separate script files.
	 * @param parent The game that controls this menu.
	 * @param session The currently active session.
	 */
	
	public ScriptedMenu(Ferrari3D parent,Session session) {
	
		super(new CardLayout());
		
		this.parent=parent;
		this.session=session;
		this.settings=Settings.getInstance();
		
		menus=new HashMap<String,MenuScreen>();
		selected=null;
		
		backgroundImage=Utils.loadImage("data/graphics/menu_background.jpg");
		
		// Init component
		
		setOpaque(true);
		setFocusable(true);
		requestFocus();

		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				shortkeyInvoked(e.getKeyChar());
			}
		});
		
		// Create menus
		
		MenuScreen emptyMenu=new MenuScreen("");
		menus.put(MENU_EMPTY,emptyMenu);
		add(MENU_EMPTY,emptyMenu);
		
		createMenu(MENU_MAIN);
		createMenu(MENU_START_SESSION);
		createMenu(MENU_JOIN_SESSION);
		createMenu(MENU_LOBBY);
		createMenu(MENU_SETTINGS);
		createMenu(MENU_INFORMATION);
		createMenu(MENU_RESULTS);
		createMenu(MENU_RECORDS);

		setSelectedMenu(MENU_MAIN);
	}
	
	/**
	 * Creates the menu file with the specified ID. The menu should be located in 
	 * a file with the same name, located in the menu folder. This method will
	 * load the file, and then convert it to displayable contents.<br><br>
	 * This method can be called multiple times for the same ID, this will result
	 * in the menu being reloaded.
	 * @param id The ID of the menu to load.
	 */
	
	private void createMenu(String id) {
		
		// Reload menu when it already exists
		
		if (menus.get(id)!=null) {
			if (id.equals(getSelectedMenu())) {
				setSelectedMenu(MENU_EMPTY);
			}
			remove(menus.get(id));
			menus.remove(id);
		}
		
		// Create menu component
		
		MenuScreen menu=new MenuScreen("");
		menus.put(id,menu);
		add(id,menu);
		
		// Create menu
		
		if (id.equals(MENU_MAIN)) { createMainMenu(menu); }
		if (id.equals(MENU_START_SESSION)) { createStartSessionMenu(menu); }
		if (id.equals(MENU_JOIN_SESSION)) { createJoinSessionMenu(menu); }
		if (id.equals(MENU_LOBBY)) { createLobbyMenu(menu); }
		if (id.equals(MENU_SETTINGS)) { createSettingsMenu(menu); }
		if (id.equals(MENU_INFORMATION)) { createInformationMenu(menu); }
		if (id.equals(MENU_RESULTS)) { createResultsMenu(menu); }
		if (id.equals(MENU_RECORDS)) { createRecordsMenu(menu); }
	}
	
	/**
	 * main.bsh
	 */
	
	private void createMainMenu(MenuScreen menu) {

		menu.setTitle(settings.getText("menu.main.title"));

		createButton(menu,settings.getText("menu.exit.title"),'x',ACTION_EXIT);
		createButton(menu,settings.getText("menu.editor.title"),'E',ACTION_START_EDITOR);
		createButton(menu,settings.getText("menu.information.title"),'I',MENU_INFORMATION);
		createButton(menu,settings.getText("menu.settings.title"),'t',MENU_SETTINGS);
		createButton(menu,settings.getText("menu.joinsession.title"),'J',MENU_JOIN_SESSION);
		createButton(menu,settings.getText("menu.startsession.title"),'S',MENU_START_SESSION);

		MenuText logoWidget=new MenuText(550,200);		
		logoWidget.paintImageRelative(Utils.loadImage("data/graphics/logo.png"),0.5f,0.5f,'c');
		createWidget(menu,0,logoWidget);
		
		MenuText textWidget=new MenuText(550,-1);		
		textWidget.paintTextRelative(settings.getText("menu.copyright"),0.5f,0.95f,'c');
		textWidget.paintTextRelative(Ferrari3D.VERSION,0f,0.95f,'l');
		createWidget(menu,0,textWidget);

		menu.getButton(1).setEnabled(!Utils.isWebstart());
	}
	
	/**
	 * start_session.bsh
	 */
	
	private void createStartSessionMenu(MenuScreen menu) {
		
		final String[] CAR_NAMES=settings.cars.toArray(new String[0]);
		final String[] CIRCUIT_NAMES=settings.circuits.toArray(new String[0]);
		final String[] MODES=new String[] {null,"Time","Race"};
		final String[] AI_LEVELS=new String[] {null,"Easy","Normal","Hard"};
		
		menu.setTitle(settings.getText("menu.startsession.title"));
		
		createPanel(menu,settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu,settings.getText("menu.startsession.title.circuit"));
		createPanel(menu,settings.getText("menu.startsession.title.multiplayer"));
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_MAIN);
		createButton(menu,settings.getText("menu.records"),'R',ACTION_RECORDS);
		createButton(menu,settings.getText("menu.start"),'S',ACTION_START_SESSION);
		
		MenuOption nameWidget=new MenuOption(settings.getText("menu.startsession.name"),settings,"name","string");
		MenuFlow carWidget=new MenuFlow(settings.getText("menu.startsession.car"),"cars/____/preview.jpg",CAR_NAMES);
		MenuFlow circuitWidget=new MenuFlow(settings.getText("menu.startsession.circuit"),"circuits/____/preview.jpg",
				CIRCUIT_NAMES);
		MenuOption modeWidget=new MenuOption(settings.getText("menu.startsession.racetype"),settings,"mode",1,
				MODES.length-1,MODES);
		MenuOption lapsWidget=new MenuOption(settings.getText("menu.startsession.laps"),settings,"laps",
				Settings.MIN_LAPS,Settings.MAX_LAPS,null);
		MenuOption aiWidget=new MenuOption(settings.getText("menu.startsession.aiactive"),settings,"aiActive",
				"boolean");
		MenuOption aiLevelWidget=new MenuOption(settings.getText("menu.startsession.ailevel"),settings,"aiLevel",1,
				AI_LEVELS.length-1,AI_LEVELS);
		MenuOption splitscreenWidget=new MenuOption(settings.getText("menu.startsession.splitscreen"),settings,
				"splitscreen","boolean");
		final MenuOption name2Widget=new MenuOption(settings.getText("menu.startsession.namePlayer2"),settings,
				"namePlayer2","string");
		final MenuFlow car2Widget=new MenuFlow(settings.getText("menu.startsession.carPlayer2"),
				"cars/____/preview.jpg",CAR_NAMES);
		MenuText noticeWidget=new MenuText();
		noticeWidget.paintText(settings.getText("menu.startsession.multinote"));
		
		createWidget(menu,0,nameWidget);
		createWidget(menu,0,new MenuText());
		createWidget(menu,0,carWidget);
		createWidget(menu,1,circuitWidget);
		createWidget(menu,1,new MenuText());
		createWidget(menu,1,modeWidget);
		createWidget(menu,1,lapsWidget);
		createWidget(menu,1,aiWidget);
		createWidget(menu,1,aiLevelWidget);
		createWidget(menu,2,splitscreenWidget);
		createWidget(menu,2,name2Widget);
		createWidget(menu,2,new MenuText());
		createWidget(menu,2,car2Widget);
		createWidget(menu,2,new MenuText());
		createWidget(menu,2,noticeWidget);
		
		name2Widget.setEnabled(settings.splitscreen);
		car2Widget.setEnabled(settings.splitscreen);
		splitscreenWidget.addListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				name2Widget.setEnabled(settings.splitscreen);
				car2Widget.setEnabled(settings.splitscreen);
			}
		});
	}
	
	/**
	 * join_session.bsh
	 */
	
	private void createJoinSessionMenu(MenuScreen menu) {
		
		final String[] CAR_NAMES=settings.cars.toArray(new String[0]);
		
		menu.setTitle(settings.getText("menu.joinsession.title"));
		
		createPanel(menu,settings.getText("menu.startsession.title.driverandcar"));
		createPanel(menu,settings.getText("menu.joinsession.title.server"));
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_MAIN);
		createButton(menu,settings.getText("menu.joinsession.connect"),'C',ACTION_JOIN_SESSION);
		
		// Add widgets
		
		MenuOption nameWidget=new MenuOption(settings.getText("menu.startsession.name"),settings,"name","string");
		MenuFlow carWidget=new MenuFlow(settings.getText("menu.startsession.car"),"cars/____/preview.jpg",CAR_NAMES);
		MenuOption hostWidget=new MenuOption(settings.getText("menu.joinsession.serverip"),settings,"server","string");
		MenuOption portWidget=new MenuOption(settings.getText("menu.joinsession.serverport"),
				""+Settings.MULTIPLAYER_PORT,0);
		
		createWidget(menu,0,nameWidget);
		createWidget(menu,0,new MenuText());
		createWidget(menu,0,carWidget);
		createWidget(menu,1,hostWidget);
		createWidget(menu,1,portWidget);
	}
	
	/**
	 * session_lobby.bsh
	 */
	
	private void createLobbyMenu(MenuScreen menu) {

		final String[] MODES=new String[] {null,"Time","Race"};
		final String[] AI_LEVELS=new String[] {null,"Easy","Normal","Hard"};
		
		menu.setTitle(settings.getText("menu.lobby.title"));
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_MAIN);
		createButton(menu,settings.getText("menu.start"),'S',ACTION_START_GAME);
		
		MenuTable table=new MenuTable(new String[]{settings.getText("menu.lobby.name"),settings.getText("menu.lobby.car"),
				settings.getText("menu.lobby.ip"),settings.getText("menu.lobby.version")},new int[]{30,30,25,15},8);
		MenuOption circuitWidget=new MenuOption(settings.getText("menu.startsession.circuit"),settings.circuit,0);
		MenuOption recordWidget=new MenuOption(settings.getText("menu.startsession.trackrecord"),
				settings.getText("menu.lobby.loadingrecords"),0);
		MenuOption modeWidget=new MenuOption(settings.getText("menu.startsession.racetype"),MODES[settings.mode]+
				" ("+settings.laps+" laps)",0);
		MenuOption aiWidget=new MenuOption(settings.getText("menu.startsession.aiactive"),
				MenuOption.getBooleanText(settings.aiActive)+" ("+AI_LEVELS[settings.aiLevel]+")",0);
		MenuChat chatWidget=new MenuChat(Multiplayer.getInstance());
		
		createWidget(menu,0,table);		
		createWidget(menu,0,circuitWidget);
		createWidget(menu,0,recordWidget);
		createWidget(menu,0,modeWidget);
		createWidget(menu,0,aiWidget);		
		createWidget(menu,0,new MenuText());
		createWidget(menu,0,chatWidget);
		
		if (settings.getRecords(settings.circuit)!=null) {
			TrackRecord record=settings.getRecords(settings.circuit)[0];
			recordWidget.setValue(Utils.timeFormat(record.getLaptime())+" ("+record.getDriverName()+")");
		}
		
		table.addRow(new String[]{settings.name,settings.car,Server.getLocalHost().getHostAddress(),
				Ferrari3D.VERSION});
		if (settings.splitscreen) {
			table.addRow(new String[]{settings.namePlayer2,settings.carPlayer2,Server.getLocalHost().
					getHostAddress(),Ferrari3D.VERSION});
		}
		for (Connection i : Multiplayer.getInstance().getConnections()) {
			table.addRow(new String[]{i.getMeta("id"),i.getMeta("car"),i.getMeta("ip"),i.getMeta("version")});
		}
	}
	
	/**
	 * settings.bsh
	 */
	
	private void createSettingsMenu(MenuScreen menu) {
	
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
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_MAIN);
		createButton(menu,settings.getText("menu.settings.defaults"),'D');
		
		MenuOption languageWidget=new MenuOption(settings.getText("menu.settings.language"),
				LANGUAGES[settings.language],2);
		MenuOption unitsWidget=new MenuOption(settings.getText("menu.settings.units"),settings,"units",1,
				UNITS.length-1,UNITS);
		MenuOption cameraWidget=new MenuOption(settings.getText("menu.settings.camera"),settings,
				"defaultCamera",1,CAMERAS.length-1,CAMERAS);
		MenuOption satWidget=new MenuOption(settings.getText("menu.settings.showsat"),settings,"showSAT","boolean");		
		MenuOption radarWidget=new MenuOption(settings.getText("menu.settings.showradar"),settings,
				"showRadar","boolean");		
		MenuOption framerateWidget=new MenuOption(settings.getText("menu.settings.showframerate"),settings,
				"showFramerate","boolean");		
		MenuOption indicatorWidget=new MenuOption(settings.getText("menu.settings.indicators"),settings,
				"indicators","boolean");
		MenuOption debugWidget=new MenuOption(settings.getText("menu.settings.debugmode"),settings,"debug","boolean");				
		MenuOption rendererWidget=new MenuOption(settings.getText("menu.settings.renderer"),"jMonkeyEngine",0);			
		MenuOption fullscreenWidget=new MenuOption(settings.getText("menu.settings.fullscreen"),settings,
				"fullscreen","boolean");
		MenuOption resolutionWidget=new MenuOption(settings.getText("menu.settings.resolution"),
				settings.resolution.width+"x"+settings.resolution.height,2);
		MenuOption graphicsWidget=new MenuOption(settings.getText("menu.settings.graphics"),settings,
				"graphics",1,GRAPHICS.length-1,GRAPHICS);
		MenuOption soundWidget=new MenuOption(settings.getText("menu.settings.sound"),settings,"sound","boolean");
		MenuOption controlsetWidget=new MenuOption(settings.getText("menu.settings.controlset"),settings,
				"controlset",0,CONTROLSETS.length-1,CONTROLSETS);
		MenuOption autogearsWidget=new MenuOption(settings.getText("menu.settings.autogears"),settings,
				"autoGears","boolean");		
		MenuOption autoreverseWidget=new MenuOption(settings.getText("menu.settings.autoreverse"),settings,
				"autoReverse","boolean");
		
		MenuText controlsText=new MenuText(550,500);		
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
		createWidget(menu,0,new MenuText());
		createWidget(menu,0,satWidget);
		createWidget(menu,0,radarWidget);
		createWidget(menu,0,framerateWidget);
		createWidget(menu,0,indicatorWidget);
		createWidget(menu,0,debugWidget);
		createWidget(menu,1,rendererWidget);
		createWidget(menu,1,fullscreenWidget);
		createWidget(menu,1,resolutionWidget);
		createWidget(menu,1,graphicsWidget);
		createWidget(menu,2,soundWidget);
		createWidget(menu,3,controlsetWidget);
		createWidget(menu,3,autogearsWidget);
		createWidget(menu,3,autoreverseWidget);
		createWidget(menu,3,new MenuText());
		createWidget(menu,3,controlsText);
	}
	
	/**
	 * information.bsh
	 */
	
	private void createInformationMenu(MenuScreen menu) {
	
		menu.setTitle(settings.getText("menu.information.title"));
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_MAIN);
		createButton(menu,settings.getText("menu.information.systeminfo"),'I',ACTION_SYSTEM_INFO);
		
		MenuText infoText=new MenuText(550,500);		
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
	
	/**
	 * results.bsh
	 */
	
	private void createResultsMenu(MenuScreen menu) {
		
		menu.setTitle(settings.getText("menu.results.title"));
		
		createPanel(menu,settings.getText("menu.results.title.classification"));
		createPanel(menu,settings.getText("menu.results.title.details"));
		
		createButton(menu,settings.getText("menu.results.endsession"),'E',ACTION_END_SESSION);
		
		if (session!=null) {
			MenuTable resultsTable=new MenuTable(new String[]{settings.getText("menu.results.name"),settings.
					getText("menu.results.car"),settings.getText("menu.results.laps"),settings.
					getText("menu.results.bestlap")},new int[]{30,30,20,20},8);
			for (int i=1; i<=session.getNumContestants(); i++) {
				Contestant target=session.getRacePositionID(i);
				String[] td=new String[4];
				td[0]=i+". "+target.getID();
				td[1]=target.getCar().getCarName();
				td[2]=""+(target.getLap()-1);
				td[3]=Utils.timeFormat(target.getBestLaptime());
				resultsTable.addRow(td);
			}
			createWidget(menu,0,resultsTable);
					
			MenuTable detailsTable=new MenuTable(new String[]{settings.getText("menu.results.lap"),settings.
					getText("menu.results.sector1"),settings.getText("menu.results.sector2"),settings.
					getText("menu.results.sector3"),settings.getText("menu.results.laptime")},
					new int[]{20,20,20,20,20},20);
			Player player=getPlayer();
			for (int i=1; i<player.getLap(); i++) {
				String[] td=new String[5];
				td[0]="Lap "+i;
				td[1]=Utils.timeFormat(player.getLaptime(i).getSector(0));
				td[2]=Utils.timeFormat(player.getLaptime(i).getSector(1));
				td[3]=Utils.timeFormat(player.getLaptime(i).getSector(2));
				td[4]=Utils.timeFormat(player.getLaptime(i).getLaptime());
				detailsTable.addRow(td);
			}
			createWidget(menu,1,detailsTable);
		}
	}
	
	/**
	 * records.bsh
	 */
	
	private void createRecordsMenu(MenuScreen menu) {
		
		menu.setTitle(settings.getText("menu.records.title"));
		
		createPanel(menu,settings.circuits.get(0));
		
		createButton(menu,settings.getText("menu.back"),'B',MENU_START_SESSION);
		
		MenuTable recordsTable=new MenuTable(new String[]{settings.getText("menu.records.name"),
				settings.getText("menu.records.car"),settings.getText("menu.records.laptime"),
				settings.getText("menu.records.date")},new int[]{30,25,20,25},15);
		createWidget(menu,0,recordsTable);
		
		TrackRecord[] records=settings.getRecords(settings.circuit);
		if ((records!=null) && (records.length>0)) {
			for (int i=0; i<records.length; i++) {
				String driverName=(i+1)+". "+records[i].getDriverName();
				String carName=records[i].getCarName();
				String laptime=Utils.timeFormat(records[i].getLaptime());
				String date=records[i].getDate();
				recordsTable.addRow(new String[]{driverName,carName,laptime,date});
			}
		}
	}
	
	/**
	 * Creates a new panel and attaches it to the specified menu screen.
	 */

	private MenuPanel createPanel(MenuScreen menu,String title) {
		
		MenuPanel panel=new MenuPanel(title);
		menu.addPanel(panel);
		
		return panel;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen.
	 */

	private MenuButton createButton(MenuScreen menu,String label,char shortkey) {
		
		MenuButton button=new MenuButton(label,false,shortkey);
		menu.addButton(button);
		
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the
	 * button will change the selected menu to a new value. 
	 */

	private MenuButton createButton(MenuScreen menu,String label,char shortkey,final String targetMenu) {
		
		MenuButton button=createButton(menu,label,shortkey);
		button.setListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSelectedMenu(targetMenu);
			}
		});
		
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the 
	 * specified action will be executed.
	 */

	private MenuButton createButton(MenuScreen menu,String label,char shortkey,final int action) {
		
		MenuButton button=createButton(menu,label,shortkey);
		button.setListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doAction(action);
			}
		});
		
		return button;
	}
	
	/**
	 * Attaches the specified widget to a menu. The widget can be created either 
	 * in this class, or in the script file.
	 */

	private void createWidget(MenuScreen menu,MenuPanel panel,MenuWidget widget) {
	
		if (panel!=null) {
			panel.addWidget(widget);
		} else {
			menu.addWidget(widget);
		}
	}
	
	/**
	 * Attaches the specified widget to a menu. This method does exactly the same 
	 * as <code>createWidget(MenuScreen,MenuPanel,MenuWidget)</code>.
	 */

	private void createWidget(MenuScreen menu,int panelIndex,MenuWidget widget) {
	
		createWidget(menu,menu.getPanel(panelIndex),widget);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		g.drawImage(backgroundImage,0,0,getWidth(),getHeight(),null);
	}
	
	/**
	 * Executes an action. This method can be called by menu GUI components that
	 * want to execute a specific task. The name of the action should be on of 
	 * the <code>ACTION_*</code> fields.
	 */

	private void doAction(int action) {
		
		switch (action) {
			case ACTION_EXIT : parent.exit(); break;
			case ACTION_START_EDITOR : parent.startEditor(); break;
			case ACTION_DEFAULT_SETTINGS : settings.defaults(); break;
			case ACTION_SYSTEM_INFO : showSystemInfo(); break;
			case ACTION_END_SESSION : endSession(); break;
			case ACTION_RECORDS : showRecords(); break;
			case ACTION_JOIN_SESSION : joinSession(); break;
			case ACTION_START_SESSION : startSession(); break;
			case ACTION_START_GAME : startGame(); break;
			default : break;
		}
	}
	
	/**
	 * Creates a new background thread that executes the specified task. During
	 * this task a indeterminate progress bar will be shown.
	 */
	
	private void doBackground(final String targetMenu,final Runnable r) {
		
		Thread t=new Thread() {
			public void run() {
				menus.get(selected).setLoading(true);
				r.run();
				menus.get(selected).setLoading(false);
				setSelectedMenu(targetMenu);
			}
		};
		t.start();
	}
	
	/**
	 * Sets the selected menu to the menu with the specified ID. When such as menu
	 * does not exist this method will do nothing.
	 */

	public void setSelectedMenu(String selected) {
		
		this.selected=selected;
		
		if (selected!=null) {
			CardLayout layout=(CardLayout) getLayout();
			layout.show(this,selected);
		}
	}
	
	/**
	 * Returns the ID of the currently active menu. When no menu is currently 
	 * active this method will return null.
	 */

	public String getSelectedMenu() {
		
		return selected;
	}
	
	/**
	 * Called whenever a shortkey is invoked on this component. If the current
	 * menu screen contains a component which is sensitive to this key, it 
	 * should be fired.
	 * @param shortkey A char which represents the key that was pressed.
	 */
	
	private void shortkeyInvoked(char shortkey) {
		
		Component c=FocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (c instanceof JTextField) { return; }
		if (c instanceof JTextArea) { return; }
	
		for (MenuButton i : menus.get(selected).getButtons()) {
			if (Character.toLowerCase(i.getShortKey())==shortkey) {
				i.pressed();
				break;
			}
		}
	}
	
	/**
	 * Starts the game. When the game is running as a server it will send an 
	 * event to all other players.
	 */
	
	private void startGame() {
		
		setSelectedMenu(MENU_EMPTY);
	
		if (Multiplayer.getInstance().isServer()) {
			Multiplayer.getInstance().sendStartMessage();
		}
	
		parent.startGame();
	}
	
	/**
	 * Starts a new session.
	 * @deprecated This method contains some hard-coded links to menu components.
	 */
	
	@Deprecated
	private void startSession() {
		
		if (!checkName(settings.name)) {
			Popups.message(null,settings.getText("menu.message.invalidname"));
			return;
		}
	
		createMultiplayerHandler(true);
		
		createMenu(MENU_LOBBY);
		menus.get(MENU_LOBBY).getButton(0).setEnabled(false);
		menus.get(MENU_LOBBY).getButton(1).setEnabled(true);
		setSelectedMenu(MENU_LOBBY);
	}
	
	/**
	 * Joins an existing session.
	 * @deprecated This method contains some hard-coded links to menu components.
	 */
	
	@Deprecated
	private void joinSession() {
		
		if (!checkName(settings.name)) {
			Popups.message(null,settings.getText("menu.message.invalidname"));
			return;
		}
		
		createMultiplayerHandler(false);
	}
	
	/**
	 * Ends the existing session. This will update some things that have changes 
	 * since the previous session (e.g. track records), and select the main menu.
	 */

	private void endSession() {
		
		final ArrayList<TrackRecord> newRecords=new ArrayList<TrackRecord>();
	
		for (Contestant i : session.getContestants()) {
			String driverName=i.getID();
			String carName=i.getCar().getCarName();
			String circuitName=i.getCircuit().getCircuitName();
			Laptime laptime=i.getBestLaptime();
			String version=Ferrari3D.VERSION;
			String date=TextFormatter.dateFormat(false);
			
			newRecords.add(new TrackRecord(driverName,carName,circuitName,laptime,version,date));
		}
		
		doBackground(MENU_MAIN,new Runnable() {
			public void run() {
				for (TrackRecord i : newRecords) {
					try {
						TrackRecord.setRecords(i);
					} catch (Exception e) {
						settings.getLogger().warning("Could not save records",e);
					}
				}

				settings.reloadRecords();
				parent.exit();
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
					Popups.message(null,settings.getText("menu.message.clientdisconnect",c.getHost()));
					setSelectedMenu(MENU_MAIN);
				}
				if (message.getType().equals(Multiplayer.MESSAGE_CONNECT)) {
					getConnectMessage(c,message);
					
					if (!Multiplayer.getInstance().isServer()) {
						createMenu(MENU_LOBBY);
						menus.get(MENU_LOBBY).getButton(0).setEnabled(false);
						menus.get(MENU_LOBBY).getButton(1).setEnabled(false);
						setSelectedMenu(MENU_LOBBY);
					} else {
						createMenu(MENU_LOBBY);
						menus.get(MENU_LOBBY).getButton(0).setEnabled(false);
						menus.get(MENU_LOBBY).getButton(1).setEnabled(true);
						setSelectedMenu(MENU_LOBBY);
					}
				}
				if (message.getType().equals(Multiplayer.MESSAGE_START)) {
					startGame();
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
				Popups.message(null,settings.getText("menu.message.connecterror")+" '"+settings.server+"'.");
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
			Popups.errorMessage(null,settings.getText("menu.message.connectversion"));
			Multiplayer.getInstance().stop();
		}
		
		if (message.getParameter("id").equals(settings.name)) {
			Popups.errorMessage(null,settings.getText("menu.message.connectid"));
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
		
		String system=settings.getText("menu.systeminfo.title")+"\n\n";
		system+=settings.getText("menu.systeminfo.java")+" "+Resources.getJavaVersion()+"\n";
		system+=settings.getText("menu.systeminfo.os")+" "+Resources.getPlatform()+"\n";
		system+=settings.getText("menu.systeminfo.user")+" "+Resources.getUserAccount()+"\n";
		system+=settings.getText("menu.systeminfo.locale")+" "+Resources.getUserLocale().getDisplayName()+"\n";
		system+="\n";
		system+=settings.getText("menu.systeminfo.processors")+" "+processors+"\n";
		system+=settings.getText("menu.systeminfo.arch")+" "+arch+"\n";
		system+=settings.getText("menu.systeminfo.memory")+" "+TextFormatter.memoryFormat(memory)+"\n";
				
		Popups.message(null,system);
	}
	
	/**
	 * Shows the records menu. This method will (re)load the list of records, 
	 * and then select the menu.
	 */
	
	private void showRecords() {
		
		doBackground(MENU_RECORDS,new Runnable() { 
			public void run() {
				settings.reloadRecords();
				createMenu(MENU_RECORDS);
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
}
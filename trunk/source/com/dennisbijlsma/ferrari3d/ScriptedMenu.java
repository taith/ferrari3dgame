//--------------------------------------------------------------------------------
// Ferrari3D
// ScriptedMenu
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.CardLayout;
import java.awt.Component;
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

import bsh.EvalError;
import bsh.Interpreter;

import com.dennisbijlsma.ferrari3d.menu.MenuButton;
import com.dennisbijlsma.ferrari3d.menu.MenuPanel;
import com.dennisbijlsma.ferrari3d.menu.MenuScreen;
import com.dennisbijlsma.ferrari3d.menu.MenuWidget;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.TrackRecord;
import com.dennisbijlsma.util.data.DataLoader;
import com.dennisbijlsma.util.data.ResourceFile;
import com.dennisbijlsma.util.data.Resources;
import com.dennisbijlsma.util.data.TextFormatter;
import com.dennisbijlsma.util.swing.PopUp;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.ConnectionManager;
import com.dennisbijlsma.xmlserver.Message;
import com.dennisbijlsma.xmlserver.XMLMessage;

/**
 * Provides a framework for a menu system that can be created in script files. This
 * class uses the GUI components as found in <code>com.dennisbijlsma.ferrari3d.menu
 * </code>, and links these components to the script files. In turn the scripts can
 * call some methods in this class directly, these methods are tagged with the 
 * <code>Scriptable</code> annotation.
 */

public class ScriptedMenu extends JPanel {
	
	private Ferrari3D parent;
	private Session session;
	private Settings settings;
	
	private Map<String,MenuScreen> menus;
	private String selected;

	public static final String MENU_EMPTY="empty";
	public static final String MENU_MAIN="main";
	public static final String MENU_START_SESSION="start_session";
	public static final String MENU_JOIN_SESSION="join_session";
	public static final String MENU_LOBBY="session_lobby";
	public static final String MENU_SETTINGS="settings";
	public static final String MENU_INFORMATION="information";
	public static final String MENU_RESULTS="results";
	public static final String MENU_RECORDS="records";
	
	public static final int ACTION_EXIT=1;
	public static final int ACTION_START_EDITOR=2;
	public static final int ACTION_DEFAULT_SETTINGS=3;
	public static final int ACTION_SYSTEM_INFO=4;
	public static final int ACTION_END_SESSION=5;
	public static final int ACTION_RECORDS=6;
	public static final int ACTION_JOIN_SESSION=7;
	public static final int ACTION_START_SESSION=8;
	public static final int ACTION_START_GAME=9;
	
	private static final String SCRIPT_LOCATION="data/menu/";
	private static final String SCRIPT_ENCODING="UTF-8";
	
	public @interface Scriptable { }

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
	
	protected void createMenu(String id) {
		
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
		
		// Create script interpreter		
		
		try {
			ResourceFile resource=Resources.getResourceFile(SCRIPT_LOCATION+id+".bsh");
			String file=DataLoader.loadString(resource.getStream(),SCRIPT_ENCODING);
			
			Interpreter bsh=new Interpreter();
			bsh.eval("import com.dennisbijlsma.ferrari3d.*");
			bsh.eval("import com.dennisbijlsma.ferrari3d.menu.*");
			bsh.eval("import com.dennisbijlsma.ferrari3d.util.*");
			bsh.set("framework",this);
			bsh.set("session",session);
			bsh.set("settings",settings);
			bsh.set("menu",menu);
			bsh.eval(file);
		} catch (EvalError e) {
			settings.getLogger().warning("Exception in script file '"+id+".js':",e);
		} catch (IOException e) {
			settings.getLogger().warning("Could not load script file '"+id+".js':",e);
		}
	}
	
	/**
	 * Creates a new panel and attaches it to the specified menu screen.
	 */

	@Scriptable
	public MenuPanel createPanel(MenuScreen menu,String title) {
		
		MenuPanel panel=new MenuPanel(title);
		menu.addPanel(panel);
		
		return panel;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen.
	 */

	@Scriptable
	public MenuButton createButton(MenuScreen menu,String label,char shortkey) {
		
		MenuButton button=new MenuButton(label,false,shortkey);
		menu.addButton(button);
		
		return button;
	}
	
	/**
	 * Creates a new menu button for the specified menu screen. When clicked, the
	 * button will change the selected menu to a new value. 
	 */

	@Scriptable
	public MenuButton createButton(MenuScreen menu,String label,char shortkey,final String targetMenu) {
		
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

	@Scriptable
	public MenuButton createButton(MenuScreen menu,String label,char shortkey,final int action) {
		
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

	@Scriptable
	public void createWidget(MenuScreen menu,MenuPanel panel,MenuWidget widget) {
	
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

	@Scriptable
	public void createWidget(MenuScreen menu,int panelIndex,MenuWidget widget) {
	
		createWidget(menu,menu.getPanel(panelIndex),widget);
	}
	
	/**
	 * Executes an action. This method can be called by menu GUI components that
	 * want to execute a specific task. The name of the action should be on of 
	 * the <code>ACTION_*</code> fields.
	 */

	@Scriptable
	public void doAction(int action) {
		
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
	
	@Scriptable
	public void doBackground(final String targetMenu,final Runnable r) {
		
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

	@Scriptable
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

	@Scriptable
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
			PopUp.message(this,settings.getText("menu.message.invalidname"));
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
			PopUp.message(this,settings.getText("menu.message.invalidname"));
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
	
		for (Contestant i : session.getContestantsSet()) {
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
	private void createMultiplayerHandler(boolean server) {
		
		final ConnectionManager manager=Multiplayer.getInstance().getConnectionManager();
		
		// Create connection listener
			
		ConnectionListener listener=new ConnectionListener() {
			public void messageReceived(Connection c,Message message) {
				if (message.getType().equals(Multiplayer.MESSAGE_INIT)) {
					manager.sendMessage(c,setConnectMessage());
				}
				if (message.getType().equals(Multiplayer.MESSAGE_DISCONNECT)) {
					PopUp.message(null,settings.getText("menu.message.clientdisconnect",c.getHost()));
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

			public void connected(Connection c) { }
			public void disconnected(Connection c) { }
			public void messageSent(Connection c,Message m) { }
		};
		manager.addConnectionListener(listener);
		
		// Start/join session
		
		if (server) {
			Multiplayer.getInstance().connectAsServer();
		} else {
			if (!Multiplayer.getInstance().connectAsClient()) {
				PopUp.message(this,settings.getText("menu.message.connecterror")+" '"+settings.server+"'.");
			}
		}
	}
	
	/**
	 * Creates and returns a new connect message.
	 * @deprecated This should be done in the multiplayer class.
	 */
	
	@Deprecated
	private Message setConnectMessage() {
	
		XMLMessage message=new XMLMessage();
		message.setType(Multiplayer.MESSAGE_CONNECT);
		message.setParameter("id",settings.name);
		message.setParameter("car",settings.car);
		message.setParameter("circuit",settings.circuit);
		message.setParameter("mode",""+settings.mode);
		message.setParameter("aiActive",""+settings.aiActive);
		message.setParameter("aiLevel",""+settings.aiLevel);
		message.setParameter("laps",""+settings.laps);
		message.setParameter("ip",ConnectionManager.getLocalIP());
		message.setParameter("version",Ferrari3D.VERSION);
		message.setUseHeader(false);
		
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
		
		if (!message.getParameter("version").equals(settings.getVersion())) {
			PopUp.errorMessage(this,settings.getText("menu.message.connectversion"));
			Multiplayer.getInstance().disconnect();
		}
		
		if (message.getParameter("id").equals(settings.name)) {
			PopUp.errorMessage(this,settings.getText("menu.message.connectid"));
			Multiplayer.getInstance().disconnect();
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
				
		PopUp.message(this,system);
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
}
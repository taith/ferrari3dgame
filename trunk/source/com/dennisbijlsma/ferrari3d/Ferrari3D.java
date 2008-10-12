//--------------------------------------------------------------------------------
// Ferrari3D
// ©2008 dennisbijlsma.com
// Version 2.1.6 (b010)
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.editor.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.core3d.display.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;
import com.dennisbijlsma.util.swing.mac.*;

/**
 * Ferrari3D is a simple racing game in full 3D. It is possible to race either 
 * against computer-controlled opponents, or other players (via local network). 
 * The number of included cars and circuits can be extended with the included 
 * editor. Unlike most games, Windows, Mac OS X and Linux are supported.
 */

public class Ferrari3D {
	
	public static final String VERSION="2.1.6";
	
	private Session session;
	private Game game;
	private ScriptedMenu menu;
	private JFrame window;
	
	/**
	 * {@inheritDoc}
	 */
	
	public static void main(String[] args) {

		new Ferrari3D();
	}
	
	/**
	 * Creates a new game controller. The process essentially consists of three 
	 * parts, the game itself, the menu and the editor. This class contains start
	 * and stop methods for all three.
	 */
	
	public Ferrari3D() {
		
		// Load settings
		
		final Settings settings=Settings.getInstance();
		
		try {
			settings.setVersion(VERSION);
			settings.init();
			settings.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Set look and feel
		
		try {
			initLAF();
		} catch (Exception e) {
			settings.getLogger().warning("Could not set look and feel",e);
		}
		
		// Create net thread
				
		Thread t=new Thread("Ferrari3D-Net") {
			public void run() {				
				settings.checkForUpdates();
				settings.sendReport();
				settings.reloadRecords();
			}
		};
		t.start();
		
		// Create window
		
		window=new JFrame();
		window.setTitle(settings.getText("game.title"));
		window.setSize(Settings.WINDOW_SIZE);	
		window.setLocationRelativeTo(null);
		window.setIconImage(Settings.ICON);
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);		
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		window.setLayout(new BorderLayout());
		
		if (settings.fullscreen) {
			window.setSize(Utils.getCurrentDisplayMode());
			window.setLocation(0,0);
			window.setResizable(false);
			window.setUndecorated(true);
		}
		
		// Start
		
		settings.getLogger().info("Ferrari3D (version "+VERSION+")");
		startMenu(ScriptedMenu.MENU_MAIN);
		window.setVisible(true);
		
		if ((Resources.isSolaris()) || (Resources.getPlatform()==Resources.Platform.UNKNOWN)) {
			String name=Resources.getPlatformName();
			PopUp.message(window,settings.getText("menu.message.notsupported",name));
			settings.getLogger().error("Platform not supported: '"+name+"'");
		}
	}
	
	/**
	 * Starts the game. This will terminate the menu, should it be active, and
	 * create a new viewport to start the session. Just before the game is started
	 * all settings are saved.
	 */
	
	protected void startGame() {
		
		Settings.getInstance().save();
		
		// Stop menu
		
		if (menu!=null) {
			stopMenu();			
		}
		
		// Create game thread
		
		switch (Settings.getInstance().mode) {
			case Settings.MODE_TIME : session=new Session(Session.SessionMode.TIME); break;		
			case Settings.MODE_RACE : session=new Session(Session.SessionMode.RACE); break;
			default : break;
		}
		
		game=new Game(this,session,createViewport());
		
		Thread t=new Thread("Ferrari3D-Game") {
			public void run() {				
				game.startGame();
			}
		};
		t.start();
	}
	
	/**
	 * Stops the game. This will disconnect any multiplayer clients, stop the
	 * session, destroy the game viewport and create a new menu window to display
	 * the results.
	 */
	
	protected void stopGame() {
		
		// Stop multiplayer

		try {
			Thread.sleep(1000);
			Multiplayer.getInstance().disconnect();
			Multiplayer.getInstance().reset();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Stop game
		
		game.stopGame();
		game=null;
		
		startMenu(ScriptedMenu.MENU_RESULTS);		
	}
	
	/**
	 * Starts the menu with the specified initially selected screen. A window will
	 * also be created to display the menu.
	 */
	
	protected void startMenu(String m) {
		
		menu=new ScriptedMenu(this,session);
		menu.setSelectedMenu(m);
		window.add(menu,BorderLayout.CENTER);
		
		menu.revalidate();
		window.repaint();
	}
	
	/**
	 * Stops the menu. The menu window will be destroyed, which will also stop
	 * the game unless other windows are still active.
	 */
	
	protected void stopMenu() {
		
		window.remove(menu);
		menu=null;
	}
	
	/**
	 * Starts the editor. This will stop the game and the menu, should they be
	 * active.
	 */
	
	protected void startEditor() {
	
		Editor editor=new Editor();
		editor.setVisible(true);
		
		window.dispose();
	}
	
	/**
	 * Exits the game and terminates the JVM. Depending on the platform, a message
	 * dialog may appear. Before exiting all settings will be saved.
	 */
	
	public void exit() {
		
		Settings settings=Settings.getInstance();
		
		if ((!Resources.isMacOSX()) && (!PopUp.confirm(null,settings.getText("game.exit")))) {
			return;
		}
		
		settings.save();
		settings.getLogger().info("Exiting");
		System.exit(0);
	}
	
	/**
	 * Initializes the look-and-feel. This will set up the system look-and-feel,
	 * and set some platform-specific system properties. This method must be called
	 * before the first Swing window is shown.
	 * @throws Exception if, for whatever reason, setting the look and feel fails.
	 */
	
	private void initLAF() throws Exception {
		
		// Set system properties
		
		if (Resources.isMacOSX()) {
			System.setProperty(OSXHandler.SYSTEM_PROPERTY_MENUBAR,"true");
			System.setProperty(OSXHandler.SYSTEM_PROPERTY_GROWBOX,"false");
			System.setProperty(OSXHandler.SYSTEM_PROPERTY_QUARTZ,"true");
			System.setProperty(OSXHandler.SYSTEM_PROPERTY_QUAQUA_FOCUS,"true");
		}
		
		// Set Look-And-Feel
		
		if ((Resources.isWindows()) || (Resources.isLinux())) {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		
		if (Resources.isMacOSX()) {
			UIManager.setLookAndFeel(OSXHandler.QUAQUA_LAF);
		}
		
		// Mac OS X application menu
		
		if (Resources.isMacOSX()) {
			OSXHandler.setAboutHandler(this,getClass().getMethod("showAboutDialog"));
			OSXHandler.setPreferencesHandler(null,null);
			OSXHandler.setQuitHandler(this,getClass().getMethod("exit",new Class[0]));
		}
	}
	
	/**
	 * Creates and returns a <code>Viewport</code> for the game. The viewport will
	 * be constructed using the settings for resolution and fullscreen.
	 */
	
	private Viewport createViewport() {
		
		Settings settings=Settings.getInstance();
		int wWidth=Settings.WINDOW_SIZE.width;
		int wHeight=Settings.WINDOW_SIZE.height;
		int fsWidth=settings.resolution.width;
		int fsHeight=settings.resolution.height;
		boolean aa=(Settings.getInstance().graphics!=Settings.GRAPHICS_LOW);
		boolean cursor=(!Settings.getInstance().fullscreen);
	
		if (settings.fullscreen) {
			return new Viewport(window,Viewport.Display.FULLSCREEN,fsWidth,fsHeight,aa);
		} else {
			return new Viewport(window,Viewport.Display.WINDOW,wWidth,wHeight,aa);
		}
	}
	
	/**
	 * Shows an information dialog about the game.
	 */
	
	public void showAboutDialog() {
	
		PopUp.message(null,Settings.getInstance().getText("menu.about",VERSION));
	}
}
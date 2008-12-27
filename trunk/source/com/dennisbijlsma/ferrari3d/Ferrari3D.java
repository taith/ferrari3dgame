//--------------------------------------------------------------------------------
// Ferrari3D
// ©2008 dennisbijlsma.com
// Version 2.2.0
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.JFrame;
import javax.swing.UIManager;

import com.dennisbijlsma.ferrari3d.editor.Editor;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.renderer.Renderer3D;
import com.dennisbijlsma.core3d.renderer.RenderSystem;
import com.dennisbijlsma.core3d.renderer.jme.JMonkeyEngineRenderer;
import com.dennisbijlsma.util.data.Resources;
import com.dennisbijlsma.util.swing.PopUp;
import com.dennisbijlsma.util.swing.mac.MacHandler;

/**
 * Ferrari3D is a simple racing game in full 3D. It is possible to race either 
 * against computer-controlled opponents, or other players (via local network). 
 * The number of included cars and circuits can be extended with the included 
 * editor. Windows, Mac OS X and Linux are all supported.
 */

//TODO Java 5 compatability areas:
// - ResourceBundle.Control -> Localization
// - Properties(Reader) -> DataLoader
// - LinearGradientPaint -> MenuWidget (2x)
// - Desktop -> Utils (2x)
// - @Override annotations in interfaces

public class Ferrari3D {
	
	public static final String VERSION="2.2.0";
	
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
		
		// Create net thread
				
		Thread t=new Thread("Ferrari3D-Net") {
			public void run() {				
				settings.checkForUpdates();
				settings.sendReport();
				settings.reloadRecords();
			}
		};
		t.start();
		
		// Init AWT and Swing
		
		try {
			if (!Resources.isMacOSX()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				UIManager.setLookAndFeel(MacHandler.QUAQUA_LAF);
			}
		} catch (Exception e) {
			settings.getLogger().warning("Could not set look and feel",e);
		}
		
		if (Resources.isMacOSX()) {
			MacHandler.setSystemProperties(false,true);
			MacHandler.setAboutText(settings.getText("menu.about",VERSION));
			MacHandler.setPreferencesHandler(null,null);
			MacHandler.setQuitHandler(this,"exit");
		}
		
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
	}
	
	/**
	 * Starts the game. This will terminate the menu, should it be active, and
	 * create a new viewport to start the session. Just before the game is started
	 * all settings are saved.
	 */
	
	protected void startGame() {
		
		try {
			Settings.getInstance().save();
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not save settings file",e);
		}
		
		// Stop menu
		
		if (menu!=null) {
			window.setVisible(false);
			stopMenu();		
		}
		
		// Init game thread
		
		switch (Settings.getInstance().mode) {
			case Settings.MODE_TIME : session=new Session(Session.SessionMode.TIME); break;		
			case Settings.MODE_RACE : session=new Session(Session.SessionMode.RACE); break;
			default : throw new IllegalArgumentException("Invalid session mode");
		}

		createRenderer();
		
		game=new Game(this,session,createDisplay());
		game.startGame();
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
		
		window.setVisible(true);
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
		
		try {
			settings.save();
		} catch (IOException e) {
			settings.getLogger().warning("Could not save settings file",e);
		}
		
		settings.getLogger().info("Exiting");
		System.exit(0);
	}
	
	/**
	 * Returns a <code>Renderer3D</code> for the game. This will perform the
	 * rendering of the scene graph.
	 */
	
	private Renderer3D createRenderer() {
		
		Renderer3D renderer=new JMonkeyEngineRenderer();
		RenderSystem.setRenderer(renderer);
		return renderer;
	}
	
	/**
	 * Sets up the display system to use for the game.
	 */
	
	private Display createDisplay() {
		
		Settings settings=Settings.getInstance();
		int wWidth=Settings.WINDOW_SIZE.width;
		int wHeight=Settings.WINDOW_SIZE.height;
		int fsWidth=settings.resolution.width;
		int fsHeight=settings.resolution.height;
		
		Renderer3D renderer=RenderSystem.getRenderer();
		Display viewport=null;
		if (settings.fullscreen) {
			viewport=renderer.createDisplay(Display.DisplayType.FULLSCREEN,fsWidth,fsHeight);
		} else {
			viewport=renderer.createDisplay(Display.DisplayType.WINDOW,wWidth,wHeight);
		}
		viewport.setWindowTitle(window.getTitle());
		viewport.setWindowIcon(window.getIconImage());
		return viewport;
	}
}
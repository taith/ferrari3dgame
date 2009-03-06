//--------------------------------------------------------------------------------
// Ferrari3D
// ©2009 dennisbijlsma.com
// Version 2.2.1
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dennisbijlsma.ferrari3d.editor.ModelEditor;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.util.DataLoader;
import com.dennisbijlsma.util.Resources;
import com.dennisbijlsma.util.swing.Popups;
import com.dennisbijlsma.util.swing.mac.MacHandler;

/**
 * Ferrari3D is a simple and poorly named racing game, where you can race against 
 * the clock or other cars. Those other cars can be computer controlled or other 
 * players, multiplayer races can be held both in splitscreen and over local 
 * network. Track records are stored online, creating and overall best times list. 
 * If desired, new cars and circuits can be created using the editor. Ferrari3D 
 * supports Windows, Mac OS X and Linux.
 */

public final class Ferrari3D {
	
	private Session session;
	private Game game;
	private ScriptedMenu menu;
	private JFrame window;
	
	public static final String VERSION="2.2.1";
	
	/**
	 * Main application entry point.
	 */
	
	public static void main(String[] args) {
		
		initLookAndFeel();

		new Ferrari3D();
	}
	
	/**
	 * Initializes the game. This will load the settings file, create a background
	 * thread for network operations, and set up the initial user interface.
	 */
	
	public Ferrari3D() {
		
		final Settings settings=Settings.getInstance();
		
		try {
			settings.init();
			settings.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t,final Throwable e) {
				settings.getLogger().error("Uncaught exception in thread "+t.getName(),e);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						sendErrorReport(e);
					}
				});
			}			
		});
				
		runNetThread();
		
		if (Resources.isMacOSX()) {
			MacHandler.setAboutText(settings.getText("menu.about",VERSION));
			MacHandler.setPreferencesHandler(null,null);
			MacHandler.setQuitHandler(this,"exit");
		}
		
		settings.getLogger().info("Ferrari3D (version "+VERSION+")");
		
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
		
		// Start menu
		
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
			Multiplayer.getInstance().stop();
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
			
		new ModelEditor();
		
		window.dispose();
	}
	
	/**
	 * Exits the game by terminating the JVM. Before exiting all settings will
	 * be saved.
	 */
	
	public void exit() {
		
		Settings settings=Settings.getInstance();
		
		try {
			settings.save();
		} catch (IOException e) {
			settings.getLogger().error("Could not save settings file",e);
		}
		
		settings.getLogger().info("Exiting");
		System.exit(0);
	}
	
	/**
	 * Starts the background thread for network operations. This will check for
	 * updates, send reports and load track records. These operations are done
	 * in a background thread to improve startup time and not block the GUI.
	 */
	
	private void runNetThread() {
	
		Thread t=new Thread("Ferrari3D-Net") {
			public void run() {	
				checkForUpdates();
				
				Settings settings=Settings.getInstance();
				settings.reloadRecords();

				if (!settings.report) {
					sendReport();
					settings.report=true;
				}
			}
		};
		t.start();
	}
	
	/**
	 * Checks the server if updates are available. If so, a dialog window is
	 * shown to notify the user of the update.
	 */
	
	private void checkForUpdates() {
	
		try {
			final Properties p=DataLoader.loadProperties(new URL(Settings.UPDATES_URL),Settings.CHARSET);
			if (Settings.isNewerVersion(VERSION,p.getProperty("update.version"))) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (Popups.confirmMessage(null,Settings.getInstance().getText("game.newversion",
								p.getProperty("update.version")))) {
							Utils.openBrowser(p.getProperty("update.url"));
							System.exit(0);
						}						
					}
				});
			}
		} catch (IOException e) {
			// Ignore
		}
	}
	
	/**
	 * Sends a report to the server containing information about the current
	 * platform. This is only done on the first launch.
	 */
	
	private void sendReport() {
	
		Map<String,String> parameters=new HashMap<String,String>();
		parameters.put("version",Ferrari3D.VERSION);
		parameters.put("platform",Resources.getPlatform());
		parameters.put("java",Resources.getJavaVersion());
		parameters.put("webstart",""+Utils.isWebstart());
		
		try {
			DataLoader.openURL(Settings.REPORT_URL,parameters,Settings.CHARSET);
			Settings.getInstance().getLogger().info("Report sent");
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not send report",e);
		}
	}
	
	/**
	 * Sends an error report to the server. The user is asked to enter information
	 * about the problem, which is included in the report.
	 */
	
	@SuppressWarnings("static-access")
	private void sendErrorReport(Throwable exception) {
		
		Settings settings=Settings.getInstance();
		
		// Show dialog
		
		String details=Popups.textareaMessage(null,settings.getText("game.reporterror.title"),
				settings.getText("game.reporterror"),new String[]{settings.
				getText("game.reporterror.report"),settings.getText("game.reporterror.dontreport")});
		if (details==null) { 
			return; 
		}
		
		// Send error report
		
		String stacktrace=settings.getLogger().getStackTrace(exception);
		if (stacktrace.length()>300) {
			stacktrace=stacktrace.substring(0,300);
		}
		
		Map<String,String> parameters=new HashMap<String,String>();
		parameters.put("version",Ferrari3D.VERSION);
		parameters.put("platform",Resources.getPlatform());
		parameters.put("java",Resources.getJavaVersion());
		parameters.put("webstart",""+Utils.isWebstart());
		parameters.put("exception",stacktrace);
		parameters.put("details",details);
		
		try {
			DataLoader.openURL(Settings.REPORT_URL,parameters,Settings.CHARSET);
			settings.getLogger().info("Error report sent");
		} catch (IOException e) {
			settings.getLogger().warning("Could not send error report",e);
		}
	}
	
	/**
	 * Sets up the display system to use for the game. The display's appearance
	 * will depend on the current settings.
	 */
	
	private Display createDisplay() {
		
		Settings settings=Settings.getInstance();
		int width=Settings.WINDOW_SIZE.width;
		int height=Settings.WINDOW_SIZE.height;
		if (settings.fullscreen) {
			width=settings.resolution.width;
			height=settings.resolution.height;
		}
		
		Display display=new Display(width,height,settings.fullscreen);
		display.setWindowTitle(window.getTitle());
		display.setWindowIcon(window.getIconImage());
		return display;
	}
	
	/**
	 * Initializes the application's Swing look-and-feel.
	 * @throws RuntimeException when the look-and-feel could not be set.
	 */
	
	private static void initLookAndFeel() {
		
		try {
			if (!Resources.isMacOSX()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				MacHandler.setSystemProperty(MacHandler.SYSTEM_PROPERTY_MENUBAR,true);
				MacHandler.setSystemProperty(MacHandler.SYSTEM_PROPERTY_QUARTZ,true);
				UIManager.setLookAndFeel(MacHandler.QUAQUA_LAF);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not set look-and-feel",e);
		}
	}
}
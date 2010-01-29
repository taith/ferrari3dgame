//-----------------------------------------------------------------------------
// Ferrari3D
// Ferrari3D
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.DisplayWindow;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.InitDisplayException;
import com.dennisbijlsma.ferrari3d.editor.Editor;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.Charsets;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.Platform;
import nl.colorize.util.SimpleLogger;
import nl.colorize.util.URLParams;
import nl.colorize.util.Version;
import nl.colorize.util.swing.Popups;
import nl.colorize.util.swing.ApplicationMenuHandler;
import nl.colorize.util.swing.MacSupport;

/**
 * Ferrari3D is a simple and poorly named racing game, where you can race against 
 * the clock or other cars. Those other cars can be computer controlled or other 
 * players, multiplayer races can be held both in splitscreen and over local 
 * network. Track records are stored online, creating and overall best times list. 
 * If desired, new cars and circuits can be created using the editor. Ferrari3D 
 * supports Windows, Mac OS X and Linux.
 */
public class Ferrari3D extends GameCore implements ApplicationMenuHandler {
	
	private Session session;
	private Menu menu;
	private Game game;
	private Settings settings;
	
	public static final Version VERSION = new Version("2.4.2");
	
	/**
	 * Main application entry point.
	 */
	public static void main(String[] args) {
		initSwing();
		boolean editor = ((args.length == 1) && args[0].equals("-editor"));
		new Ferrari3D(!editor);
	}
	
	/**
	 * Initializes the game. This will load the settings file, create a background
	 * thread for network operations, and set up the initial user interface.
	 * @param startGame When true starts the game, when false starts the editor.
	 */
	public Ferrari3D(boolean startGame) {
		
		super();
		
		// Load settings
		
		settings = Settings.getInstance();
		
		try {
			settings.init();
			settings.load();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		
		// Log uncaught exceptions
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				settings.getLogger().severe("Uncaught exception in thread " + t.getName(), e);
				// Intentionally not called from the Swing thread.
				sendErrorReport(e);
			}			
		});
		
		// Send report, check for updates, load records
				
		runNetThread();
		
		// Mac OS X application menu
		
		if (Platform.isMacOSX()) {
			MacSupport.setApplicationMenuHandler(this);
		}
		
		// Start
		
		settings.getLogger().info(String.format("Ferrari3D (version %s)", VERSION));
		settings.getLogger().info(String.format("Java: %s", Platform.getJavaVersion()));
		settings.getLogger().info(String.format("Platform: %s", Platform.getPlatform()));

		if (startGame) {
			startGame();
		} else {
			startEditor(true);
		}
	}
	
	/**
	 * Initializes the display that will be used by the game. If the game is set
	 * to be running in windowed mode, the default window size will be used. If
	 * full-screen, the existing screen dimensions will be used.
	 */
	@Override
	protected Display initDisplay() {
		
		int width = Settings.WINDOW_WIDTH;
		int height = Settings.WINDOW_HEIGHT;
		if (settings.fullscreen) {
			width = settings.resolution.width;
			height = settings.resolution.height;
		}
		
		DisplayWindow display = new DisplayWindow(width, height, settings.fullscreen);
		display.setWindowTitle(settings.getText("game.title"));
		display.setWindowIcon(Settings.ICON);
		display.setMouseCursorVisible(true);
		return display;
	}
	
	/**
	 * Initializes the environment (camera, lights, etc.) and then starts the
	 * game by activating the menu state.
	 */
	@Override
	public void initGameState() {
		Loader.createWorld(getSceneGraph(), getDisplay());
		recreateSession();
		changeToMenuState();
	}
	
	/**
	 * Empty implementation, all game logic is implemented by the game and menu
	 * states.
	 */
	@Override
	public void updateGameState(float dt) {
		
	}
	
	/**
	 * Changes the game's state to the menu. If the game was running when this
	 * method is called it will be stopped. The first time this method is called
	 * the menu system is created, calls after that will reuse that instance.
	 */
	protected void changeToMenuState() {
		
		if (game != null) {
			getSceneGraph().clear();
			game = null;
			Loader.recreateCameras(getSceneGraph(), getDisplay(), true);
		}
		
		if (menu == null) {
			menu = new Menu(this, session);
		} else {
			menu.changeSession(session);
		}
		
		changeActiveGameState(menu);
		
		if (session.isFinished()) {
			menu.setSelectedMenu(Menu.Screen.RESULTS);
		}
	}
	
	/**
	 * Changes the game's state to in-game. If the menu is active when this 
	 * method is called it will be stopped.
	 */
	protected void changeToGameState() {
		game = new Game(this, session);
		changeActiveGameState(game);
	}
	
	/**
	 * Starts the editor. Because the display system cannot be reopened this
	 * method must not be called after the game has been started. In other
	 * words, the game and editor cannot be used in the same run.
	 * @param coldStart True if the display is not yet opened. 
	 */
	protected void startEditor(boolean coldStart) {
		
		if (coldStart) {
			new Editor();
			return;
		}

		//TODO The display system cannot be reopened, so perform the highly dubious
		//     trick of starting a new JVM and killing this one. This is only done
		//     on Windows, other platforms just show a message.
		if (Platform.isWindows()) {
			String cmd = "javaw -Xmx128m -Djava.library.path=libraries/native -jar ferrari3d.jar -editor";
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				throw new AssertionError(e);
			}
			quit();
		} else {
			Popups.message(null, settings.getText("menu.message.editor"));
		}
	}
	
	/**
	 * Creates a new session object. If an old session existed it is replaced.
	 * The new session will use the session mode that is currently selected.s
	 * @return The newly created {@code Session}.
	 */
	protected Session recreateSession() {
		switch (settings.mode) {
			case Settings.MODE_TIME : 
				session = new Session(Session.SessionMode.TIME, settings.laps); 
				return session;
			case Settings.MODE_RACE : 
				session = new Session(Session.SessionMode.RACE, settings.laps); 
				return session;
			default : 
				throw new AssertionError();
		}
	}
	
	/**
	 * Exits the application and terminates the JVM. Before exiting the application
	 * settings are saved.
	 */
	public void quit() {
		try {
			settings.save();
		} catch (Exception e) {
			
		}
		settings.getLogger().info("Exiting");
		System.exit(0);
	}
	
	public void about() {
		Popups.message(null, settings.getText("menu.about", VERSION.toString()));
	}
	
	/**
	 * Invoked when an exception occurs during the game loop. The exception will
	 * be handled in the same way as an uncaught exception.
	 */
	@Override
	protected void onException(Throwable e) {
		Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
	}
	
	/**
	 * Starts the background thread for network operations. This will check for
	 * updates, send reports and load track records. These operations are done
	 * in a background thread to improve startup time and not block the GUI.
	 */
	private void runNetThread() {
	
		Thread t = new Thread("Ferrari3D-Net") {
			public void run() {	
				checkForUpdates();
				
				Settings settings = Settings.getInstance();
				settings.reloadTrackRecords();

				if (!settings.report) {
					sendReport();
					settings.report = true;
					settings.save();
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
			final Properties p = LoadUtils.loadProperties(new URL(Settings.UPDATES_URL).openStream(), 
					Charsets.UTF8);
			if (new Version(p.getProperty("update.version")).isNewer(VERSION)) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (Popups.confirmMessage(null, Settings.getInstance().
								getText("game.newversion", p.getProperty("update.version")))) {
							Utils.openBrowser(p.getProperty("update.url"));
							System.exit(0);
						}						
					}
				});
			}
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not check for updates", e);
		}
	}
	
	/**
	 * Sends a report to the server containing information about the current
	 * platform. This is only done on the first launch.
	 */
	private void sendReport() {
	
		URLParams parameters = new URLParams(URLParams.Method.POST);
		parameters.add("version", Ferrari3D.VERSION.toString());
		parameters.add("platform", Platform.getPlatform());
		parameters.add("java", Platform.getJavaVersion());
		parameters.add("webstart", "" + Utils.isWebstart());
		
		try {
			LoadUtils.openURL(Settings.REPORT_URL, parameters, Charsets.UTF8);
			Settings.getInstance().getLogger().info("Report sent");
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not send report", e);
		}
	}
	
	/**
	 * Sends an error report to the server. The user is asked to enter information
	 * about the problem, which is included in the report.
	 */
	private void sendErrorReport(Throwable exception) {
		
		Settings settings = Settings.getInstance();
		
		// Show dialog
		
		String message = settings.getText("game.reporterror");
		if (exception.getCause() instanceof InitDisplayException) {
			message = settings.getText("game.reporterrorDisplay");
		}
		String details = Popups.textareaMessage(null, settings.getText("game.reporterror.title"),
				message, new String[]{settings.getText("game.reporterror.report"), 
				settings.getText("game.reporterror.dontreport")});
		if (details == null) { 
			return; 
		}
		
		// Send error report
		
		URLParams parameters = new URLParams(URLParams.Method.POST);
		parameters.add("version", Ferrari3D.VERSION.toString());
		parameters.add("platform", Platform.getPlatform());
		parameters.add("java", Platform.getJavaVersion());
		parameters.add("webstart", "" + Utils.isWebstart());
		parameters.add("exception", SimpleLogger.getStackTrace(exception));
		parameters.add("details", details);
		
		try {
			LoadUtils.openURL(Settings.REPORT_URL, parameters, Charsets.UTF8);
			settings.getLogger().info("Error report sent");
		} catch (IOException e) {
			settings.getLogger().warning("Could not send error report", e);
		}
	}
	
	/**
	 * Initializes Swing by setting the look and feel, and any platform-dependent
	 * system properties. This method must be called before Swing is started.
	 */
	private static void initSwing() {		
		try {
			if (!Platform.isMacOSX()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				MacSupport.setSystemProperty(MacSupport.SYSTEM_PROPERTY_MENUBAR, true);
				MacSupport.setSystemProperty(MacSupport.SYSTEM_PROPERTY_QUARTZ, true);
				MacSupport.setSystemProperty(MacSupport.SYSTEM_PROPERTY_QUAQUA_FOCUS, true);
				UIManager.setLookAndFeel(MacSupport.QUAQUA_LAF);
			}
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}
}

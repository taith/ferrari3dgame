//--------------------------------------------------------------------------------
// Ferrari3D
// ©2009 dennisbijlsma.com
// Version 2.3.0
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.dennisbijlsma.ferrari3d.editor.ModelEditor;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.DataLoader;
import com.dennisbijlsma.util.Platform;
import com.dennisbijlsma.util.StandardLogger;
import com.dennisbijlsma.util.swing.Popups;
import com.dennisbijlsma.util.swing.mac.ApplicationMenuListener;
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
	
	public static final String VERSION = "2.3.0";
	
	/**
	 * Main application entry point.
	 */
	
	public static void main(String[] args) {
		
		initLookAndFeel();

		boolean editor = (args.length == 1 && args[0].equals("editor"));
		new Ferrari3D(!editor);
	}
	
	/**
	 * Initializes the game. This will load the settings file, create a background
	 * thread for network operations, and set up the initial user interface.
	 * @param startGame When true starts the game, when false starts the editor.
	 */
	
	public Ferrari3D(boolean startGame) {
		
		// Load settings
		
		final Settings settings = Settings.getInstance();
		
		try {
			settings.init();
			settings.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Log uncaught exceptions
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				settings.getLogger().error("Uncaught exception in thread " + t.getName(), e);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						sendErrorReport(e);
					}
				});
			}			
		});
		
		// Send report, check for updates, load records
				
		runNetThread();
		
		// Mac OS X application menu
		
		if (Platform.isMacOSX()) {
			MacHandler.addApplicationMenuListener(new ApplicationMenuListener() {
				public void applicationAbout() {
					Popups.messageFromSwingThread(Settings.getInstance().getText("menu.about", VERSION));
				}
			
				public void applicationQuit() {	
					exit();
				}
				
				public void applicationPreferences() { }
			});
			MacHandler.setApplicationPreferencesEnabled(false);
		}
		
		// Start
		
		settings.getLogger().info("Ferrari3D (version " + VERSION + ")");

		if (startGame) {
			startGame();
		} else {
			startEditor();
		}
	}
	
	/**
	 * Starts the game. Because the display system cannot be reopened this
	 * method can only be called once in the same run.
	 */
	
	public void startGame() {
		Core game = new Core(this);
		game.startGame();
	}
	
	/**
	 * Starts the editor. Because the display system cannot be reopened this
	 * method must not be called after the game has been started. In other
	 * words, the game and editor cannot be used in the same run.
	 */
	
	public void startEditor() {
		new ModelEditor();
	}
	
	/**
	 * Exits the application and terminates the JVM. Before exiting the application
	 * settings are saved.
	 */
	
	public void exit() {
		
		Settings settings = Settings.getInstance();
		
		try {
			settings.save();
		} catch (IOException e) {
			settings.getLogger().error("Could not save settings file", e);
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
				
				Settings settings = Settings.getInstance();
				settings.reloadTrackRecords();

				if (!settings.report) {
					sendReport();
					settings.report = true;
				}
			}
		};
		t.start();
	}
	
	/**
	 * Checks the server if updates are available. If so, a dialog window is
	 * shown to notify the user of the update.
	 */
	
	@SuppressWarnings("deprecation")
	private void checkForUpdates() {
	
		try {
			final Properties p = DataLoader.loadProperties(new URL(Settings.UPDATES_URL), Settings.CHARSET);
			if (Settings.isNewerVersion(VERSION, p.getProperty("update.version"))) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (Popups.confirmMessage(null, Settings.getInstance().getText("game.newversion",
								p.getProperty("update.version")))) {
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
	
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("version", Ferrari3D.VERSION);
		parameters.put("platform", Platform.getPlatform());
		parameters.put("java", Platform.getJavaVersion());
		parameters.put("webstart","" + Utils.isWebstart());
		
		try {
			DataLoader.openURL(Settings.REPORT_URL, parameters, Settings.CHARSET);
			Settings.getInstance().getLogger().info("Report sent");
		} catch (IOException e) {
			Settings.getInstance().getLogger().warning("Could not send report",e);
		}
	}
	
	/**
	 * Sends an error report to the server. The user is asked to enter information
	 * about the problem, which is included in the report.
	 */
	
	private void sendErrorReport(Throwable exception) {
		
		Settings settings = Settings.getInstance();
		
		// Show dialog
		
		String details = Popups.textareaMessage(null, settings.getText("game.reporterror.title"),
				settings.getText("game.reporterror"), new String[]{settings.
				getText("game.reporterror.report"), settings.getText("game.reporterror.dontreport")});
		if (details == null) { 
			return; 
		}
		
		// Send error report
		
		String stacktrace = StandardLogger.getStackTrace(exception);
		if (stacktrace.length() > 300) {
			stacktrace = stacktrace.substring(0, 300);
		}
		
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("version", Ferrari3D.VERSION);
		parameters.put("platform", Platform.getPlatform());
		parameters.put("java", Platform.getJavaVersion());
		parameters.put("webstart", "" + Utils.isWebstart());
		parameters.put("exception", stacktrace);
		parameters.put("details", details);
		
		try {
			DataLoader.openURL(Settings.REPORT_URL, parameters, Settings.CHARSET);
			settings.getLogger().info("Error report sent");
		} catch (IOException e) {
			settings.getLogger().warning("Could not send error report", e);
		}
	}
	
	/**
	 * Initializes the application's Swing look-and-feel.
	 * @throws RuntimeException when the look-and-feel could not be set.
	 */
	
	private static void initLookAndFeel() {
		
		try {
			if (!Platform.isMacOSX()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} else {
				MacHandler.setSystemProperty(MacHandler.SYSTEM_PROPERTY_MENUBAR, true);
				MacHandler.setSystemProperty(MacHandler.SYSTEM_PROPERTY_QUARTZ, true);
				UIManager.setLookAndFeel(MacHandler.QUAQUA_LAF);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not set look-and-feel", e);
		}
	}
}
//--------------------------------------------------------------------------------
// Ferrari3D
// Settings
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Dimension;
import java.awt.Image;
import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;

import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;
import com.dennisbijlsma.util.xml.*;

/**
 * Stores all game settings. This class also contains methods for loading and
 * saving these to a file. The exact location where the settings are stored depend
 * on the operating system.
 */

public class Settings {
	
	public String name;
	public String car;
	public String circuit;
	public int mode;
	public boolean aiActive;
	public int aiLevel;
	public int laps;
		
	public int language;
	public int units;
	public int defaultCamera;
	public boolean showSAT;
	public boolean showRadar;
	public boolean showFramerate;
	public boolean indicators;
	public boolean fullscreen;
	public Dimension resolution;
	public int graphics;
	public int controlset;
	public boolean autoGears;
	public boolean autoReverse;
	
	public String server;
	public boolean report;
	public boolean debug;
	
	public List<String> cars;
	public List<String> circuits;
	public String[][] controlsets;

	private String version;
	private BasicLogger logger;
	private ResourceBundle bundle;
	private Map<String,TrackRecord[]> records;
	
	public static final int MODE_TIME=1;
	public static final int MODE_RACE=2;
	public static final int AI_EASY=1;
	public static final int AI_NORMAL=2;
	public static final int AI_HARD=3;
	public static final int MIN_LAPS=1;
	public static final int MAX_LAPS=20;
	public static final int LANGUAGE_ENGLISH=1;
	public static final int UNITS_KMH=1;
	public static final int UNITS_MPH=2;
	public static final int UNITS_MS=3;
	public static final int GRAPHICS_LOW=1;
	public static final int GRAPHICS_MEDIUM=2;
	public static final int GRAPHICS_HIGH=3;

	public static final int MAX_CONTESTANTS=2; //TODO
	public static final int MULTIPLAYER_PORT=22220;
	public static final Dimension WINDOW_SIZE=new Dimension(800,600);
	public static final Dimension EDITOR_SIZE=new Dimension(800,600);
	public static final Image ICON=Utils.loadImage("data/graphics/icon.png");
	public static final Image EDITOR_ICON=Utils.loadImage("data/graphics/icon.png");
	public static final String SETTINGS_URL="settings.properties";
	public static final String CARS_URL="cars/cars.xml";
	public static final String CIRCUITS_URL="circuits/circuits.xml";
	public static final String TEXTS_URL="data/texts/texts_en.properties";
	public static final String WEB_ROOT_URL="http://www.dennisbijlsma.com/data/ferrari3d/";
	public static final String LOAD_RECORDS_URL=WEB_ROOT_URL+"get_records.php";
	public static final String SAVE_RECORDS_URL=WEB_ROOT_URL+"set_records.php";
	public static final String UPDATE_URL=WEB_ROOT_URL+"updates.php";
	public static final String REPORT_URL=WEB_ROOT_URL+"report.php";
	public static final String CHARSET="UTF-8";
	
	private static final Settings INSTANCE=new Settings();
	
	/**
	 * Creates a new <code>Settings</object>.
	 */
	
	private Settings() {
				
		cars=new ArrayList<String>();		
		circuits=new ArrayList<String>();		
		records=new HashMap<String,TrackRecord[]>();
			
		controlsets=new String[3][6];
		controlsets[0][0]="up";
		controlsets[0][1]="down";
		controlsets[0][2]="left";
		controlsets[0][3]="right";
		controlsets[0][4]="a";
		controlsets[0][5]="z";
		controlsets[1][0]="e";
		controlsets[1][1]="d";
		controlsets[1][2]="s";
		controlsets[1][3]="f";
		controlsets[1][4]="q";
		controlsets[1][5]="w";
		controlsets[2][0]="a";
		controlsets[2][1]="z";
		controlsets[2][2]=",";
		controlsets[2][3]=".";
		controlsets[2][4]="x";
		controlsets[2][5]="c";
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static Settings getInstance() {
		
		return INSTANCE;
	}
	
	/**
	 * Loads all cars and circuits from their respective files. This method must
	 * be called before the settings file is loaded, to prevent incorrect 
	 * initialization of some settings.
	 * @throws Exception when the data could not be initialized.
	 */
	
	public void init() throws Exception {
		
		// Load cars
	
		Document carsDocument=XMLParser.parseXML(Resources.getResourceStream(CARS_URL));
		Node carsNode=carsDocument.getDocumentElement();
		
		for (Node i : XMLParser.getChildNodes(carsNode)) {
			cars.add(XMLParser.getAttribute(i,"name"));
		}
		
		// Load circuits
		
		Document circuitsDocument=XMLParser.parseXML(Resources.getResourceStream(CIRCUITS_URL));
		Node circuitsNode=circuitsDocument.getDocumentElement();
		
		for (Node i : XMLParser.getChildNodes(circuitsNode)) {
			circuits.add(XMLParser.getAttribute(i,"name"));
		}
		
		// Load texts
		
		bundle=LocaleUtils.getResourceBundle("texts",Locale.getDefault(),CHARSET,"data/texts/");
		
		// Setup log
		
		if (!Utils.isWebstart()) {
			logger=BasicLogger.getLogger(Settings.class);			
		} else {
			File logfile=Resources.getApplicationData("Ferrari3D","log.txt");
			logger=BasicLogger.getLogger(Settings.class,logfile);
		}
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t,final Throwable e) {
				logger.error("Uncaught exception in thread "+t.getName(),e);
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						sendErrorReport(e);
					}
				});
			}			
		});
	}
	
	/**
	 * Loads the settings from a file. The location of this file is OS-specific.
	 * When the settings file doesnt exist, it is created with all settings set
	 * to default values.
	 */
	
	public void load() {
		
		File file=Resources.getApplicationData("Ferrari3D",SETTINGS_URL);
		
		if (!file.exists()) {
			defaults();
			logger.info("Recreating settings file at '"+file.getAbsolutePath()+"'");
		}
		
		try {
			Properties p=DataLoader.loadProperties(new FileInputStream(file),CHARSET);
			
			if (!version.equals(p.getProperty("version"))) {
				defaults();
				load();
				logger.info("Settings file has incompatible version '"+p.getProperty("version")+"'");
				return;
			}
			
			name=p.getProperty("session.name");
			car=p.getProperty("session.car");
			circuit=p.getProperty("session.circuit");
			mode=Integer.parseInt(p.getProperty("session.mode"));
			aiActive=p.getProperty("session.aiActive").equals("true");
			aiLevel=Integer.parseInt(p.getProperty("session.aiLevel"));
			laps=Integer.parseInt(p.getProperty("session.laps"));
			language=Integer.parseInt(p.getProperty("settings.language"));
			units=Integer.parseInt(p.getProperty("settings.units"));
			defaultCamera=Integer.parseInt(p.getProperty("settings.defaultCamera"));
			showSAT=p.getProperty("settings.showSAT").equals("true");
			showRadar=p.getProperty("settings.showRadar").equals("true");
			showFramerate=p.getProperty("settings.showFramerate").equals("true");
			indicators=p.getProperty("settings.indicators").equals("true");
			fullscreen=p.getProperty("settings.fullscreen").equals("true");
			resolution=Utils.getDisplayMode(p.getProperty("settings.resolution"));
			graphics=Integer.parseInt(p.getProperty("settings.graphics"));
			controlset=Integer.parseInt(p.getProperty("settings.controlset"));
			autoGears=p.getProperty("settings.autoGears").equals("true");
			autoReverse=p.getProperty("settings.autoReverse").equals("true");
			server=p.getProperty("multiplayer.server");
			report="true".equals(p.getProperty("config.report"));
			debug="true".equals(p.getProperty("config.debug"));
		} catch (Exception e) {
			logger.error("Could not load settings file",e);
		}
	}
	
	/**
	 * Saves all settings to a file. The location of this settings file is OS-
	 * specific. 
	 */
	
	public void save() {
		
		File file=Resources.getApplicationData("Ferrari3D",SETTINGS_URL);
		
		try {
			FileOutputStream stream=new FileOutputStream(file);
			PrintWriter writer=new PrintWriter(new OutputStreamWriter(stream,CHARSET));
			writer.println("version="+version);
			writer.println("java="+Resources.getJavaVersion());
			writer.println("platform="+Resources.getPlatformName());
			writer.println("session.name="+name);
			writer.println("session.car="+car);
			writer.println("session.circuit="+circuit);
			writer.println("session.mode="+mode);
			writer.println("session.aiActive="+aiActive);
			writer.println("session.aiLevel="+aiLevel);
			writer.println("session.laps="+laps);
			writer.println("settings.language="+language);
			writer.println("settings.units="+units);
			writer.println("settings.defaultCamera="+defaultCamera);
			writer.println("settings.showSAT="+showSAT);
			writer.println("settings.showRadar="+showRadar);
			writer.println("settings.showFramerate="+showFramerate);
			writer.println("settings.indicators="+indicators);
			writer.println("settings.fullscreen="+fullscreen);
			writer.println("settings.resolution="+resolution.width+"x"+resolution.height);
			writer.println("settings.graphics="+graphics);
			writer.println("settings.controlset="+controlset);
			writer.println("settings.autoGears="+autoGears);
			writer.println("settings.autoReverse="+autoReverse);
			writer.println("multiplayer.server="+server);
			writer.println("multiplayer.port="+MULTIPLAYER_PORT);
			writer.println("config.report="+report);
			writer.println("config.debug="+debug);
			writer.close();
		} catch (Exception e) {
			logger.error("Could not save settings file",e);
		}
	}
	
	/**
	 * Restores all settings to the default values. This method is executed when
	 * the settings file has become courrupted.
	 */
	
	public void defaults() {
		
		name="Kees Kist";
		car="Ferrari 248";
		circuit="Monza";
		mode=MODE_TIME;
		aiActive=true;
		aiLevel=AI_NORMAL;
		laps=3;
		
		language=LANGUAGE_ENGLISH;
		units=UNITS_KMH;
		defaultCamera=3;
		showSAT=true;
		showRadar=false;
		showFramerate=false;
		indicators=false;
		fullscreen=false;
		resolution=Utils.getCurrentDisplayMode();
		graphics=GRAPHICS_MEDIUM;
		controlset=0;
		autoGears=true;
		autoReverse=true;
		
		server="127.0.0.1";
		
		report=false;
		debug=false;
		
		save();
	}
		
	/**
	 * Checks if a new version is available. When this is the case, a dialog
	 * window will be shown asking the user if he wants to download the update.
	 * When true, the default browser will open with the update and the game
	 * will be closed.
	 */
	
	public void checkForUpdates() {
	
		try {
			Properties p=DataLoader.loadProperties(new URL(UPDATE_URL).openStream(),CHARSET);
			
			if (Utils.isNewerVersion(version,p.getProperty("update.version"))) {
				if (PopUp.confirm(null,getText("game.newversion"))) {
					Utils.openBrowser(p.getProperty("update.url"));
					System.exit(0);
				}	
			}
		} catch (IOException e) {
			logger.warning("Could not check for updates",e);
		}
	}
	
	/**
	 * Sends a report to the server. This will be used to log information such 
	 * as used Java version and operating system. The report is only sent at the
	 * first launch.
	 */
	
	public void sendReport() {
		
		if (report) {
			return;
		}
		
		HashMap<String,String> reportParameters=new HashMap<String,String>();
		reportParameters.put("version",version);
		reportParameters.put("platform",Resources.getPlatformName());
		reportParameters.put("java",Resources.getJavaVersion());
		reportParameters.put("webstart",""+Utils.isWebstart());
		
		try {
			DataLoader.openURL(REPORT_URL,reportParameters,"UTF-8");
		} catch (IOException e) {
			logger.warning("Could not send report",e);
		}
		
		report=true;
	}
	
	/**
	 * Sends an error report when an unexpected exception had occured. This method
	 * will send the report to a server, from where it is further handled.
	 */
	
	protected void sendErrorReport(Throwable exception) {
		
		// Notify user
		
		int confirm=PopUp.confirm(null,getText("game.reporterror.title"),getText("game.reporterror"),
				new String[]{getText("game.reporterror.report"),getText("game.reporterror.dontreport"),
				getText("game.reporterror.exit")});
		if (confirm==1) { return; }
		if (confirm==2) { System.exit(0); }
		
		// Send error report
		
		String stacktrace=BasicLogger.getStackTrace(exception);
		if (stacktrace.length()>200) {
			stacktrace=stacktrace.substring(0,200);
		}
		
		HashMap<String,String> reportParameters=new HashMap<String,String>();
		reportParameters.put("version",version);
		reportParameters.put("platform",Resources.getPlatformName());
		reportParameters.put("java",Resources.getJavaVersion());
		reportParameters.put("webstart",""+Utils.isWebstart());
		reportParameters.put("exception",stacktrace);
		
		try {
			DataLoader.openURL(REPORT_URL,reportParameters,"UTF-8");
		} catch (IOException e) {
			logger.warning("Could not send error report",e);
		}
	}
	
	/**
	 * Returns a random car. This method can only be used after the list of cars
	 * has been loaded.
	 */
	
	public String getRandomCar() {
	
		return cars.get((int) Math.round(Math.random()*(cars.size()-1)));
	}
	
	/**
	 * Returns a random circuits. This method can only be used after the list of 
	 * circuits has been loaded.
	 */
	
	public String getRandomCircuit() {
	
		return circuits.get((int) Math.round(Math.random()*(circuits.size()-1)));
	}
	
	/**
	 * Returns the currently selected controlset. Using this method is the same
	 * as using <code>CONTROLSETS[controlset]</code>.
	 */
	
	public String[] getControlSet() {
	
		return controlsets[controlset];
	}
	
	/**
	 * Returns the logger object. This object should be used by all classes to
	 * log information messages and errors.
	 */
	
	public BasicLogger getLogger() {
	
		return logger;
	}
	
	/**
	 * Returns the text with the specified key. The text will automatically be
	 * in the current locale.
	 */
	
	public String getText(String key) {
	
		try {
			return LocaleUtils.getText(bundle,key);
		} catch (MissingResourceException e) {
			logger.warning("Could not find text with key '"+key+"'");
			return "???";
		}
	}
	
	/**
	 * Returns the text with the specified key. The text will automatically be
	 * in the current locale.
	 */
	
	public String getText(String key,String... params) {
		
		try {
			return LocaleUtils.getText(bundle,key,params);
		} catch (MissingResourceException e) {
			logger.warning("Could not find text with key '"+key+"'");
			return "???";
		}
	}
	
	/**
	 * Returns all available texts for the current locale as a map.
	 * @see LocaleUtils.getAllTexts(ResourceBundle)
	 */
	
	public Map<String,String> getAllTexts() {
	
		return LocaleUtils.getAllTexts(bundle);
	}
	
	/**
	 * Returns the track records for a particular circuit. When the records have
	 * not yet been loaded this method will return <code>null</code>. 
	 */
	
	public TrackRecord[] getRecords(String key) {
		
		return records.get(key);
	}
	
	/**
	 * Reloads the list of track records for all circuits. This method should 
	 * only be called after the list of circuits has been loaded, or there will
	 * be no result.
	 */
	
	public void reloadRecords() {
		
		records.clear();
		
		try {
			for (String i : circuits) {
				records.put(i,TrackRecord.getRecords());
			}
		} catch (Exception e) {
			logger.warning("Could not load track records",e);
		}
	}
	
	/**
	 * Sets the version number for this settings file. This parameter must be set
	 * manually as the settings file doesn't know its own context.
	 */
	
	public void setVersion(String version) {
	
		this.version=version;
	}
	
	/**
	 * Returns the version number as currently used by the settings file. This
	 * value might differ from the currently used value.  
	 */
	
	public String getVersion() {
	
		return version;
	}
}
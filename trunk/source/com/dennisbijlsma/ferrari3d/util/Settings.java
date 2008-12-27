//--------------------------------------------------------------------------------
// Ferrari3D
// Settings
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.SwingUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.dennisbijlsma.util.data.AbstractConfig;
import com.dennisbijlsma.util.data.StandardLogger;
import com.dennisbijlsma.util.data.DataLoader;
import com.dennisbijlsma.util.data.Localization;
import com.dennisbijlsma.util.data.ResourceFile;
import com.dennisbijlsma.util.data.Resources;
import com.dennisbijlsma.util.data.XMLParser;
import com.dennisbijlsma.util.swing.PopUp;

/**
 * Stores all game settings. This class also contains methods for loading and
 * saving these to a file. The exact location where the settings are stored depend
 * on the operating system.
 */

public class Settings extends AbstractConfig {
	
	public String name;
	public String car;
	public String circuit;
	public int mode;
	public boolean aiActive;
	public int aiLevel;
	public int laps;
	public boolean splitscreen;
	public String namePlayer2;
	public String carPlayer2;
		
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
	public int[][] controlsets;

	private String version;
	private StandardLogger logger;
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

	public static final int MAX_CONTESTANTS=6;
	public static final int MULTIPLAYER_PORT=22220;
	public static final Dimension WINDOW_SIZE=new Dimension(800,600);
	public static final Dimension EDITOR_SIZE=new Dimension(900,700);
	public static final Image ICON=Utils.loadImage("data/graphics/icon.png");
	public static final Image EDITOR_ICON=Utils.loadImage("data/graphics/icon.png");
	public static final String WEB_ROOT="http://www.dennisbijlsma.com/data/ferrari3d/";
	public static final String LOAD_RECORDS_URL=WEB_ROOT+"get_records.php";
	public static final String SAVE_RECORDS_URL=WEB_ROOT+"set_records.php";
	
	private static final Settings INSTANCE=new Settings();
	private static final String SETTINGS_URL="settings.properties";
	private static final String CARS_URL="cars/cars.xml";
	private static final String CIRCUITS_URL="circuits/circuits.xml";
	private static final String TEXTS_NAME="texts";
	private static final String TEXTS_LOCATION="data/texts/";
	private static final String UPDATES_URL=WEB_ROOT+"updates.php";
	private static final String REPORT_URL=WEB_ROOT+"report.php";
	private static final String CHARSET="UTF-8";
	
	/**
	 * Private singleton constructor.
	 */
	
	private Settings() {
		
		super(Resources.getApplicationData("Ferrari3D",SETTINGS_URL));
		
		setEncoding(CHARSET);
		setTitle("Ferrari3D settings");
				
		cars=new ArrayList<String>();		
		circuits=new ArrayList<String>();		
		records=new HashMap<String,TrackRecord[]>();
			
		controlsets=new int[3][6];
		controlsets[0][0]=KeyEvent.VK_UP;
		controlsets[0][1]=KeyEvent.VK_DOWN;
		controlsets[0][2]=KeyEvent.VK_LEFT;
		controlsets[0][3]=KeyEvent.VK_RIGHT;
		controlsets[0][4]=KeyEvent.VK_A;
		controlsets[0][5]=KeyEvent.VK_Z;
		controlsets[1][0]=KeyEvent.VK_E;
		controlsets[1][1]=KeyEvent.VK_D;
		controlsets[1][2]=KeyEvent.VK_S;
		controlsets[1][3]=KeyEvent.VK_F;
		controlsets[1][4]=KeyEvent.VK_Q;
		controlsets[1][5]=KeyEvent.VK_W;
		controlsets[2][0]=KeyEvent.VK_A;
		controlsets[2][1]=KeyEvent.VK_Z;
		controlsets[2][2]=KeyEvent.VK_COMMA;
		controlsets[2][3]=KeyEvent.VK_PERIOD;
		controlsets[2][4]=KeyEvent.VK_X;
		controlsets[2][5]=KeyEvent.VK_C;
	}
	
	/**
	 * Loads all cars and circuits from their respective files. This method must
	 * be called before the settings file is loaded, to prevent incorrect 
	 * initialization of some settings.
	 * @throws Exception when the data could not be initialized.
	 */
	
	public void init() throws Exception {
		
		// Load cars
	
		Document carsDocument=XMLParser.parseXML(Resources.getResourceFile(CARS_URL).getStream());
		Element carsNode=carsDocument.getDocumentElement();
		
		for (Element i : XMLParser.getChildNodes(carsNode)) {
			cars.add(XMLParser.getNodeAttribute(i,"name"));
		}
		
		// Load circuits
		
		Document circuitsDocument=XMLParser.parseXML(Resources.getResourceFile(CIRCUITS_URL).getStream());
		Element circuitsNode=circuitsDocument.getDocumentElement();
		
		for (Element i : XMLParser.getChildNodes(circuitsNode)) {
			circuits.add(XMLParser.getNodeAttribute(i,"name"));
		}
		
		// Load texts
		
		Locale locale=Locale.getDefault();
		bundle=Localization.getBundle(TEXTS_NAME,locale,new ResourceFile(TEXTS_LOCATION),CHARSET);
		
		// Setup log
		
		logger=StandardLogger.getLogger(Settings.class);			
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t,final Throwable e) {
				logger.error("Uncaught exception in thread "+t.getName(),e);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						sendErrorReport(e);
					}
				});
			}			
		});
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void load(Properties p) {
					
		name=p.getProperty("session.name");
		car=p.getProperty("session.car");
		circuit=p.getProperty("session.circuit");
		mode=Integer.parseInt(p.getProperty("session.mode"));
		aiActive=p.getProperty("session.aiActive").equals("true");
		aiLevel=Integer.parseInt(p.getProperty("session.aiLevel"));
		laps=Integer.parseInt(p.getProperty("session.laps"));
		splitscreen=p.getProperty("session.splitscreen").equals("true");
		namePlayer2=p.getProperty("session.namePlayer2");
		carPlayer2=p.getProperty("session.carPlayer2");
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
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void save(PrintWriter writer) {
		
		writer.println("version = "+version);
		writer.println("java = "+Resources.getJavaVersion());
		writer.println("platform = "+Resources.getPlatform());
		writer.println("session.name = "+name);
		writer.println("session.car = "+car);
		writer.println("session.circuit = "+circuit);
		writer.println("session.mode = "+mode);
		writer.println("session.aiActive = "+aiActive);
		writer.println("session.aiLevel = "+aiLevel);
		writer.println("session.laps = "+laps);
		writer.println("session.splitscreen = "+splitscreen);
		writer.println("session.namePlayer2 = "+namePlayer2);
		writer.println("session.carPlayer2 = "+carPlayer2);
		writer.println("settings.language = "+language);
		writer.println("settings.units = "+units);
		writer.println("settings.defaultCamera = "+defaultCamera);
		writer.println("settings.showSAT = "+showSAT);
		writer.println("settings.showRadar = "+showRadar);
		writer.println("settings.showFramerate = "+showFramerate);
		writer.println("settings.indicators = "+indicators);
		writer.println("settings.fullscreen = "+fullscreen);
		writer.println("settings.resolution = "+resolution.width+"x"+resolution.height);
		writer.println("settings.graphics = "+graphics);
		writer.println("settings.controlset = "+controlset);
		writer.println("settings.autoGears = "+autoGears);
		writer.println("settings.autoReverse = "+autoReverse);
		writer.println("multiplayer.server = "+server);
		writer.println("multiplayer.port = "+MULTIPLAYER_PORT);
		writer.println("config.report = "+report);
		writer.println("config.debug = "+debug);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public void defaults() {
		
		name="Kees Kist";
		car="Ferrari 248";
		circuit="Monza";
		mode=MODE_TIME;
		aiActive=true;
		aiLevel=AI_NORMAL;
		laps=3;
		splitscreen=false;
		namePlayer2="Henk de Vries";
		carPlayer2="Ferrari 248";
		
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
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void reset() throws IOException {
		
		logger.info("Recreating settings file at '"+getConfigFile().getAbsolutePath()+"'");
		super.reset();
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected boolean isCompatibleFileVersion(Properties p) {
	
		return version.equals(p.getProperty("version"));
	}
		
	/**
	 * Checks if a new version is available. When this is the case, a dialog
	 * window will be shown asking the user if he wants to download the update.
	 * When true, the default browser will open with the update and the game
	 * will be closed.
	 */
	
	public void checkForUpdates() {
	
		try {
			final Properties p=DataLoader.loadProperties(new URL(UPDATES_URL).openStream(),CHARSET);
			
			if (isNewerVersion(version,p.getProperty("update.version"))) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (PopUp.confirm(null,getText("game.newversion"))) {
							Utils.openBrowser(p.getProperty("update.url"));
							System.exit(0);
						}		
					}
				});
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
		
		Map<String,String> reportParameters=new HashMap<String,String>();
		reportParameters.put("version",version);
		reportParameters.put("platform",Resources.getPlatform());
		reportParameters.put("java",Resources.getJavaVersion());
		reportParameters.put("webstart",""+Utils.isWebstart());
		
		try {
			DataLoader.openURL(REPORT_URL,reportParameters,CHARSET);
			logger.info("Sending report");
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
		
		// Show dialog
		
		String details=PopUp.input(null,getText("game.reporterror.title"),getText("game.reporterror"),
				new String[]{getText("game.reporterror.report"),getText("game.reporterror.dontreport")});
		
		if (details==null) { 
			return; 
		}
		
		// Send error report
		
		String stacktrace=StandardLogger.getStackTrace(exception);
		if (stacktrace.length()>200) {
			stacktrace=stacktrace.substring(0,200);
		}
		
		Map<String,String> reportParameters=new HashMap<String,String>();
		reportParameters.put("version",version);
		reportParameters.put("platform",Resources.getPlatform());
		reportParameters.put("java",Resources.getJavaVersion());
		reportParameters.put("webstart",""+Utils.isWebstart());
		reportParameters.put("exception",stacktrace);
		reportParameters.put("details",details);
		
		try {
			DataLoader.openURL(REPORT_URL,reportParameters,CHARSET);
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
	
	public int[] getControlSet() {
	
		return controlsets[controlset];
	}
	
	/**
	 * Returns the logger object. This object should be used by all classes to
	 * log information messages and errors.
	 */
	
	public StandardLogger getLogger() {
	
		return logger;
	}
	
	/**
	 * Returns the text with the specified key. The text will automatically be
	 * in the current locale.
	 */
	
	public String getText(String key) {
	
		try {
			return Localization.getText(bundle,key);
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
			return Localization.getText(bundle,key,params);
		} catch (MissingResourceException e) {
			logger.warning("Could not find text with key '"+key+"'");
			return "???";
		}
	}
	
	/**
	 * Returns all available texts for the current locale as a map.
	 */
	
	public Map<String,String> getAllTexts() {
	
		return Localization.getAll(bundle);
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
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static Settings getInstance() {
		
		return INSTANCE;
	}
}
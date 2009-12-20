	//-----------------------------------------------------------------------------
// Ferrari3D
// Settings
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.ferrari3d.Ferrari3D;
import nl.colorize.util.Charsets;
import nl.colorize.util.Configuration;
import nl.colorize.util.DynamicResourceBundle;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.Platform;
import nl.colorize.util.SimpleLogger;
import nl.colorize.util.URLParams;
import nl.colorize.util.XMLUtils;

/**
 * Contains all global settings. This singleton class contains information that
 * needs to be accessed from anywhere, such as the <code>ResourceBundle</code> for
 * all text, the logger, and a number of constants.
 */

public final class Settings extends Configuration {
	
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
	public boolean enableReplays;
	public boolean showSAT;
	public boolean showFramerate;
	public boolean fullscreen;
	public Dimension resolution;
	public int graphics;
	public boolean sound;
	public int volume;
	public int controlset;
	public boolean autoGears;
	public boolean autoReverse;
	public int multiplayerMode;
	public String multiplayerServer;
	public String multiplayerSession;
	public boolean report;
	public boolean debug;
	
	public List<String> cars;
	public List<String> circuits;
	public int[][] controlsets;

	private SimpleLogger logger;
	private DynamicResourceBundle bundle;
	private Map<String,TrackRecord[]> records;
	
	public static final int MODE_TIME = 1;
	public static final int MODE_RACE = 2;
	public static final int AI_EASY = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_HARD = 3;
	public static final int MIN_LAPS = 1;
	public static final int MAX_LAPS = 20;
	public static final int MULTIPLAYER_LOCAL = 1;
	public static final int MULTIPLAYER_INTERNET = 2;
	public static final int LANGUAGE_ENGLISH = 1;
	public static final int UNITS_KMH = 1;
	public static final int UNITS_MPH = 2;
	public static final int UNITS_MS = 3;
	public static final int GRAPHICS_LOW = 1;
	public static final int GRAPHICS_MEDIUM = 2;
	public static final int GRAPHICS_HIGH = 3;

	public static final int MAX_CONTESTANTS = 6;
	public static final int WINDOW_WIDTH = 800;
	public static final int WINDOW_HEIGHT = 600;
	public static final BufferedImage ICON = Utils.loadImage("data/graphics/icon.png");
	public static final BufferedImage EDITOR_ICON = Utils.loadImage("data/graphics/icon.png");
	public static final int LOCAL_MULTIPLAYER_PORT = 22220;
	public static final String INTERNET_MULTIPLAYER_SERVER = "http://ferrari3d-server.appspot.com/";
	public static final String WEB_ROOT = "http://www.dennisbijlsma.com/data/ferrari3d/";
	public static final String LOAD_RECORDS_URL = WEB_ROOT+"get_records.php";
	public static final String SAVE_RECORDS_URL = WEB_ROOT+"set_records.php";
	public static final String UPDATES_URL = WEB_ROOT+"updates.php";
	public static final String REPORT_URL = WEB_ROOT+"report.php";
	
	private static final Settings INSTANCE = new Settings();
	private static final String SETTINGS_URL = "settings.properties";
	private static final String CARS_URL = "cars/cars.xml";
	private static final String CIRCUITS_URL = "circuits/circuits.xml";
	private static final String TEXTS_NAME = "texts";
	private static final String TEXTS_LOCATION = "data/texts/";
	
	/**
	 * Private singleton constructor.
	 */
	
	private Settings() {
		
		super(new ResourceFile(Platform.getApplicationData("Ferrari3D", SETTINGS_URL)), Charsets.UTF8);
				
		cars = new ArrayList<String>();		
		circuits = new ArrayList<String>();		
		records = new HashMap<String,TrackRecord[]>();
			
		controlsets = new int[3][6];
		controlsets[0][0] = Controller.KEY_UP;
		controlsets[0][1] = Controller.KEY_DOWN;
		controlsets[0][2] = Controller.KEY_LEFT;
		controlsets[0][3] = Controller.KEY_RIGHT;
		controlsets[0][4] = Controller.KEY_A;
		controlsets[0][5] = Controller.KEY_Z;
		controlsets[1][0] = Controller.KEY_E;
		controlsets[1][1] = Controller.KEY_D;
		controlsets[1][2] = Controller.KEY_S;
		controlsets[1][3] = Controller.KEY_F;
		controlsets[1][4] = Controller.KEY_Q;
		controlsets[1][5] = Controller.KEY_W;
		controlsets[2][0] = Controller.KEY_A;
		controlsets[2][1] = Controller.KEY_Z;
		controlsets[2][2] = Controller.KEY_COMMA;
		controlsets[2][3] = Controller.KEY_PERIOD;
		controlsets[2][4] = Controller.KEY_X;
		controlsets[2][5] = Controller.KEY_C;
	}
	
	/**
	 * Initializes this object by loading all required resources. No methods or
	 * fields from the class should be used before this method is called.
	 * @throws Exception when one of the resources could not be loaded.
	 */
	
	public void init() throws Exception {
		
		// Load cars
	
		Document carsDocument = XMLUtils.parseXML(new ResourceFile(CARS_URL).getStream());
		Element carsNode = carsDocument.getDocumentElement();
		for (Element i : XMLUtils.getChildNodes(carsNode)) {
			cars.add(XMLUtils.getNodeAttribute(i, "name"));
		}
		
		// Load circuits
		
		Document circuitsDocument = XMLUtils.parseXML(new ResourceFile(CIRCUITS_URL).getStream());
		Element circuitsNode = circuitsDocument.getDocumentElement();
		for (Element i : XMLUtils.getChildNodes(circuitsNode)) {
			circuits.add(XMLUtils.getNodeAttribute(i, "name"));
		}
		
		// ResourceBundle
		
		Locale locale = Locale.getDefault();
		bundle = DynamicResourceBundle.getBundle(TEXTS_NAME, locale, 
				new ResourceFile(TEXTS_LOCATION), Charsets.UTF8);
		
		// Logger
		
		logger = new SimpleLogger(Settings.class);
	}
	
	/**
	 * Callback method that loads all settings from the configuration file.
	 */
	
	@Override
	protected void handleLoad() {		
		
		name = getProperty("session.name");
		car = getProperty("session.car");
		circuit = getProperty("session.circuit");
		mode = getIntProperty("session.mode");
		aiActive = getBooleanProperty("session.aiActive");
		aiLevel = getIntProperty("session.aiLevel");
		laps = getIntProperty("session.laps");
		splitscreen = getBooleanProperty("session.splitscreen");
		namePlayer2 = getProperty("session.namePlayer2");
		carPlayer2 = getProperty("session.carPlayer2");
		
		language = getIntProperty("settings.language");
		units = getIntProperty("settings.units");
		defaultCamera = getIntProperty("settings.defaultCamera");
		enableReplays = getBooleanProperty("settings.enableReplays");
		showSAT = getBooleanProperty("settings.showSAT");
		showFramerate = getBooleanProperty("settings.showFramerate");
		fullscreen = getBooleanProperty("settings.fullscreen");
		resolution = getDimensionProperty("settings.resolution");
		graphics = getIntProperty("settings.graphics");
		sound = getBooleanProperty("settings.sound");
		volume = getIntProperty("settings.volume");
		controlset = getIntProperty("settings.controlset");
		autoGears = getBooleanProperty("settings.autoGears");
		autoReverse = getBooleanProperty("settings.autoReverse");
		
		multiplayerMode = getIntProperty("multiplayer.mode");
		multiplayerServer = getProperty("multiplayer.server");
		multiplayerSession = getProperty("multiplayer.session");
		
		report = getBooleanProperty("config.report");
		debug = getBooleanProperty("config.debug");
	}
	
	/**
	 * Saves the current configuration back to the file. Any exceptions that occur
	 * during saving will be logged but not thrown.
	 */
	
	@Override
	protected void handleSave() {
		
		try {
			PrintWriter writer = new PrintWriter(getFile().toLocalFile(), Charsets.UTF8.displayName());
			writer.println("# Ferrari3D settings");
			writer.println("");
			writer.println("version=" + Ferrari3D.VERSION);
			writer.println("java=" + Platform.getJavaVersion());
			writer.println("platform=" + Platform.getPlatform());
			
			writer.println("session.name=" + name);
			writer.println("session.car=" + car);
			writer.println("session.circuit=" + circuit);
			writer.println("session.mode=" + mode);
			writer.println("session.aiActive=" + aiActive);
			writer.println("session.aiLevel=" + aiLevel);
			writer.println("session.laps=" + laps);
			writer.println("session.splitscreen=" + splitscreen);
			writer.println("session.namePlayer2=" + namePlayer2);
			writer.println("session.carPlayer2=" + carPlayer2);
			
			writer.println("settings.language=" + language);
			writer.println("settings.units=" + units);
			writer.println("settings.defaultCamera=" + defaultCamera);
			writer.println("settings.enableReplays=" + enableReplays);
			writer.println("settings.showSAT=" + showSAT);
			writer.println("settings.showFramerate=" + showFramerate);
			writer.println("settings.fullscreen=" + fullscreen);
			writer.println("settings.resolution=" + resolution.width + "x" + resolution.height);
			writer.println("settings.graphics=" + graphics);
			writer.println("settings.sound=" + sound);
			writer.println("settings.volume=" + volume);
			writer.println("settings.controlset=" + controlset);
			writer.println("settings.autoGears=" + autoGears);
			writer.println("settings.autoReverse=" + autoReverse);
			
			writer.println("multiplayer.mode=" + multiplayerMode);
			writer.println("multiplayer.server=" + multiplayerServer);
			writer.println("multiplayer.session=" + multiplayerSession);
			
			writer.println("config.report=" + report);
			writer.println("config.debug=" + debug);
			writer.close();
		} catch (IOException e) {
			logger.warning("Could not save settings to file", e);
		}
	}
	
	/**
	 * Restores all settings to their default values.
	 */
	
	@Override
	public void defaults() {

		name = "Kees Kist";
		car = "Ferrari 248";
		circuit = "Monza";
		mode = MODE_TIME;
		aiActive = true;
		aiLevel = AI_NORMAL;
		laps = 3;
		splitscreen = false;
		namePlayer2 = "Henk de Vries";
		carPlayer2 = "Ferrari 248";
		
		language = LANGUAGE_ENGLISH;
		units = UNITS_KMH;
		defaultCamera = 3;
		enableReplays = false;
		showSAT = true;
		showFramerate = false;
		fullscreen = false;
		resolution = Utils.getCurrentDisplayMode();
		graphics = GRAPHICS_MEDIUM;
		sound = false;
		volume = 100;
		controlset = 0;
		autoGears = true;
		autoReverse = true;
		
		multiplayerMode = MULTIPLAYER_LOCAL;
		multiplayerServer = "127.0.0.1";
		multiplayerSession = Platform.getUserAccount();
		
		report = false;
		debug = false;
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected boolean isCompatibleFileVersion() {
		return Ferrari3D.VERSION.toString().equals(getProperty("version"));
	}
	
	/**
	 * Returns the global logger. This should be used for all logging that is
	 * done by the game.
	 */
	
	public SimpleLogger getLogger() {
		return logger;
	}
	
	/**
	 * Returns a text from the game's <code>ResourceBundle</code>. The text is
	 * identified by the specified key. Optionally, a number of parameters can
	 * be set within the text. If a text with the specified key does not exist,
	 * this method will return '???'.
	 */
	
	public String getText(String key, String... params) {
		try {
			return bundle.getString(key, params);
		} catch (MissingResourceException e) {
			logger.warning("Could not find text with key '" + key + "'");
			return key;
		}
	}
	
	/**
	 * Returns all texts from the game's <code>ResourceBundle</code> as a map.
	 */
	
	public Map<String,String> getAllTexts() {
		return bundle.getAll();
	}
	
	/**
	 * Returns the track records for a particular circuit. When the records have
	 * not yet been loaded this method will return <code>null</code>. 
	 */
	
	public TrackRecord[] getRecords(String key) {
		return records.get(key);
	}
	
	/**
	 * Reloads the list of track records for all circuits.
	 */
	
	public void reloadTrackRecords() {
		
		records.clear();
		
		try {
			for (String i : circuits) {
				records.put(i, reloadTrackRecords(i));
			}
		} catch (Exception e) {
			logger.warning("Could not load track records", e);
		}
	}
	
	/**
	 * Reloads all track record for the specified circuit.
	 * @throws Exception when the records could not be loaded.
	 */
	
	private TrackRecord[] reloadTrackRecords(String baseCircuit) throws Exception {
		
		List<TrackRecord> records = new ArrayList<TrackRecord>();
	
		String xml = LoadUtils.openURL(Settings.LOAD_RECORDS_URL, Charsets.UTF8);
		Document document = XMLUtils.parseXML(xml);
		
		for (Element i : XMLUtils.getChildNodes(document.getDocumentElement())) {
			String driver = XMLUtils.getChildValue(i, "driverName");
			String car = XMLUtils.getChildValue(i, "carName");
			String circuit = XMLUtils.getChildValue(i, "circuitName");
			Laptime laptime = new Laptime(Integer.parseInt(XMLUtils.getChildValue(i, "laptime")));
			String version = XMLUtils.getChildValue(i, "version");
			String date = XMLUtils.getChildValue(i, "date");
			
			TrackRecord record = new TrackRecord(driver, car, circuit, laptime, version, date);
			records.add(record);
		}
		
		return records.toArray(new TrackRecord[0]);
	}
	
	/**
	 * Saves the specified track record to the server.
	 * @throws IOException when the record could not be saved.
	 */
	
	public void saveTrackRecord(TrackRecord record) throws IOException {
		
		URLParams parameters = new URLParams(URLParams.Method.GET);
		parameters.add("driverName", record.getDriverName());
		parameters.add("carName", record.getCarName());
		parameters.add("circuitName", record.getCircuitName());
		parameters.add("laptime", "" + record.getTime().getTime());
		parameters.add("version", record.getVersion().toString());
		parameters.add("date", record.getDate());
		
		LoadUtils.openURL(Settings.SAVE_RECORDS_URL, parameters, Charsets.UTF8);
	}
	
	public int[] getControlSet() {
		return controlsets[controlset];
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static Settings getInstance() {
		return INSTANCE;
	}
}

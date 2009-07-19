//--------------------------------------------------------------------------------
// Ferrari3D
// Settings
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.util;

import java.awt.Dimension;
import java.awt.Image;
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
import com.dennisbijlsma.util.Config;
import com.dennisbijlsma.util.DynamicResourceBundle;
import com.dennisbijlsma.util.LoadUtils;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.Platform;
import com.dennisbijlsma.util.StandardLogger;
import com.dennisbijlsma.util.XMLParser;

/**
 * Contains all global settings. This singleton class contains information that
 * needs to be accessed from anywhere, such as the <code>ResourceBundle</code> for
 * all text, the logger, and a number of constants.
 */

public final class Settings extends Config {
	
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
	public boolean sound;
	public int volume;
	public int controlset;
	public boolean autoGears;
	public boolean autoReverse;
	public String server;
	public boolean enableReplays;
	public boolean report;
	public boolean debug;
	
	public List<String> cars;
	public List<String> circuits;
	public int[][] controlsets;

	private StandardLogger logger;
	private DynamicResourceBundle bundle;
	private Map<String,TrackRecord[]> records;
	
	public static final int MODE_TIME = 1;
	public static final int MODE_RACE = 2;
	public static final int AI_EASY = 1;
	public static final int AI_NORMAL = 2;
	public static final int AI_HARD = 3;
	public static final int MIN_LAPS = 1;
	public static final int MAX_LAPS = 20;
	public static final int LANGUAGE_ENGLISH = 1;
	public static final int UNITS_KMH = 1;
	public static final int UNITS_MPH = 2;
	public static final int UNITS_MS = 3;
	public static final int GRAPHICS_LOW = 1;
	public static final int GRAPHICS_MEDIUM = 2;
	public static final int GRAPHICS_HIGH = 3;

	public static final int MAX_CONTESTANTS = 6;
	public static final int MULTIPLAYER_PORT = 22220;
	public static final int WINDOW_WIDTH = 800;
	public static final int WINDOW_HEIGHT = 600;
	public static final Image ICON = Utils.loadImage("data/graphics/icon.png");
	public static final Image EDITOR_ICON = Utils.loadImage("data/graphics/icon.png");
	public static final String WEB_ROOT = "http://www.dennisbijlsma.com/data/ferrari3d/";
	public static final String LOAD_RECORDS_URL = WEB_ROOT+"get_records.php";
	public static final String SAVE_RECORDS_URL = WEB_ROOT+"set_records.php";
	public static final String UPDATES_URL = WEB_ROOT+"updates.php";
	public static final String REPORT_URL = WEB_ROOT+"report.php";
	public static final String CHARSET = "UTF-8";
	
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
		
		super(Platform.getApplicationData("Ferrari3D", SETTINGS_URL), CHARSET);
				
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
	
		Document carsDocument = XMLParser.parseXML(Platform.getResourceFile(CARS_URL).getStream());
		Element carsNode = carsDocument.getDocumentElement();
		for (Element i : XMLParser.getChildNodes(carsNode)) {
			cars.add(XMLParser.getNodeAttribute(i, "name"));
		}
		
		// Load circuits
		
		Document circuitsDocument = XMLParser.parseXML(Platform.getResourceFile(CIRCUITS_URL).getStream());
		Element circuitsNode = circuitsDocument.getDocumentElement();
		for (Element i : XMLParser.getChildNodes(circuitsNode)) {
			circuits.add(XMLParser.getNodeAttribute(i, "name"));
		}
		
		// ResourceBundle
		
		Locale locale = Locale.getDefault();
		bundle = DynamicResourceBundle.getBundle(TEXTS_NAME, locale, new ResourceFile(TEXTS_LOCATION), CHARSET);
		
		// Logger
		
		logger = StandardLogger.getLogger(Settings.class);
	}
	
	/** {@inheritDoc} */
	
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
		showSAT = getBooleanProperty("settings.showSAT");
		showRadar = getBooleanProperty("settings.showRadar");
		showFramerate = getBooleanProperty("settings.showFramerate");
		indicators = getBooleanProperty("settings.indicators");
		fullscreen = getBooleanProperty("settings.fullscreen");
		resolution = getDimensionProperty("settings.resolution");
		graphics = getIntProperty("settings.graphics");
		sound = getBooleanProperty("settings.sound");
		volume = getIntProperty("settings.volume");
		controlset = getIntProperty("settings.controlset");
		autoGears = getBooleanProperty("settings.autoGears");
		autoReverse = getBooleanProperty("settings.autoReverse");
		enableReplays = getBooleanProperty("settings.enableReplays");
		server = getProperty("multiplayer.server");
		report = getBooleanProperty("config.report");
		debug = getBooleanProperty("config.debug");
	}
	
	/** {@inheritDoc} */
	
	@Override
	public void save() {
		
		try {
			PrintWriter writer = new PrintWriter(getConfigFile(), getEncoding());
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
			writer.println("settings.showSAT=" + showSAT);
			writer.println("settings.showRadar=" + showRadar);
			writer.println("settings.showFramerate=" + showFramerate);
			writer.println("settings.indicators=" + indicators);
			writer.println("settings.fullscreen=" + fullscreen);
			writer.println("settings.resolution=" + resolution.width + "x" + resolution.height);
			writer.println("settings.graphics=" + graphics);
			writer.println("settings.sound=" + sound);
			writer.println("settings.volume=" + volume);
			writer.println("settings.controlset=" + controlset);
			writer.println("settings.autoGears=" + autoGears);
			writer.println("settings.autoReverse=" + autoReverse);
			writer.println("settings.enableReplays=" + enableReplays);
			writer.println("multiplayer.server=" + server);
			writer.println("multiplayer.port=" + MULTIPLAYER_PORT);
			writer.println("config.report=" + report);
			writer.println("config.debug=" + debug);
			writer.close();
		} catch (IOException e) {
			logger.warning("Could not save settings file", e);
		}
	}
	
	/** {@inheritDoc} */
	
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
		showSAT = true;
		showRadar = false;
		showFramerate = false;
		indicators = false;
		fullscreen = false;
		resolution = Utils.getCurrentDisplayMode();
		graphics = GRAPHICS_MEDIUM;
		sound = false;
		volume = 100;
		controlset = 0;
		autoGears = true;
		autoReverse = true;
		enableReplays = true;
		
		server = "127.0.0.1";
		
		report = false;
		debug = false;
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void recreate() throws IOException {
		logger.info("Recreating settings file at '" + getConfigFile().getAbsolutePath() + "'");
		super.recreate();
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected boolean isCompatibleFileVersion() {
		return Ferrari3D.VERSION.equals(getProperty("version"));
	}
	
	/**
	 * Returns the global logger. This should be used for all logging that is
	 * done by the game.
	 */
	
	public StandardLogger getLogger() {
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
	
		Map<String,String> parameters = new HashMap<String,String>();
		String xml = LoadUtils.openURL(Settings.LOAD_RECORDS_URL, parameters, CHARSET);
		Document document = XMLParser.parseXML(xml);
		
		for (Element i : XMLParser.getChildNodes(document.getDocumentElement())) {
			String driver = XMLParser.getChildValue(i, "driverName");
			String car = XMLParser.getChildValue(i, "carName");
			String circuit = XMLParser.getChildValue(i, "circuitName");
			LapTime laptime = new LapTime(Integer.parseInt(XMLParser.getChildValue(i, "laptime")));
			String version = XMLParser.getChildValue(i, "version");
			String date = XMLParser.getChildValue(i, "date");
			
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
		
		Map<String,String> parameters = new HashMap<String,String>();
		parameters.put("driverName", record.getDriverName());
		parameters.put("carName", record.getCarName());
		parameters.put("circuitName", record.getCircuitName());
		parameters.put("laptime", "" + record.getTime().getTime());
		parameters.put("version", record.getVersion());
		parameters.put("date", record.getDate());
		
		LoadUtils.openURL(Settings.SAVE_RECORDS_URL, parameters, CHARSET);
	}
	
	/**
	 * Returns the currently selected controlset. Using this method is the same
	 * as using <code>CONTROLSETS[controlset]</code>.
	 */
	
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
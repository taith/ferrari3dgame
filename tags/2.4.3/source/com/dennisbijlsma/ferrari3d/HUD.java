//-----------------------------------------------------------------------------
// Ferrari3D
// HUD
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.Map;

import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.Overlay;
import com.dennisbijlsma.ferrari3d.graphics.TVGraphics;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.Platform;
import nl.colorize.util.swing.Utils2D;

/**
 * Controls the heads-up-display for the game. The HUD is painted using Java 2D,
 * and occurs in a different thread than the game thread.
 */
public class HUD extends Overlay {
	
	private Camera camera;
	private Session session;	
	private Contestant target;
	private String message;	
	private int messageTime;
	private Map<String,Float> gamedata;
	
	private Image speedo;
	private Image gears;
	private Image needle1;
	private Image needle2;
	private Image startlights;
	private Image chequeredFlag;
	private Image warningFlag;
	private AffineTransform tempTransform;
	
	private TVGraphics tvGraphics;
	private long lastFrame;
	private int lastIntermediate;
	private Laptime lastFastestLap;

	private static final String GAME_DATA_FRAMERATE = "framerate";
	private static final String GAME_DATA_UPS = "ups";
	private static final String GAME_DATA_POLYGONS = "polygons";
	private static final String GAME_DATA_START_TIMER = "startTimer";
	private static final String GAME_DATA_FINISH_TIMER = "finishTimer";
	private static final int HUD_WIDTH = 1024;
	private static final int HUD_HEIGHT = 128;
	private static final float REFRESH_TIME = 0.1f;
	private static final Font DATA_FONT = new Font("Verdana", Font.PLAIN, 12);
	private static final Font POS_FONT = new Font("Verdana", Font.BOLD, 24);
	private static final Font DEBUG_FONT = new Font("Verdana", Font.BOLD, 10);
	private static final Color FONT_COLOR = new Color(255, 255, 255);
	private static final Color SHADOW_COLOR = new Color(0, 0, 0, 128);
	private static final Color PANEL_COLOR = new Color(0, 0, 0, 128);
	private static final Color MESSAGE_BACKGROUND = new Color(0, 0, 0, 128);
	private static final Color MESSAGE_FOREGROUND = new Color(255, 255, 255);
	private static final int MESSAGE_TIME = 30;
	private static final float SAT_DISTANCE = 250f;
	
	/**
	 * Creates a HUD for the specified camera. A new thread will be started that
	 * repaints the HUD in a loop.
	 * @param camera The camera to which this HUD should be attached.
	 * @param session The session (needed for TV graphics). 
	 */
	public HUD(Camera camera, Session session) {
		
		super(camera, 0, camera.getDisplay().getHeight() - HUD_HEIGHT, 
				getWidth(camera.getDisplay()), HUD_HEIGHT);

		this.camera = camera;
		this.session = session;
		
		tvGraphics = new TVGraphics(session);
		lastFrame = System.currentTimeMillis();
		lastIntermediate = 0;
		lastFastestLap = new Laptime();
		
		gamedata = new HashMap<String,Float>();
		gamedata.put(GAME_DATA_FRAMERATE, 0f);
		gamedata.put(GAME_DATA_POLYGONS, 0f);
		gamedata.put(GAME_DATA_START_TIMER, 0f);
		gamedata.put(GAME_DATA_FINISH_TIMER, 0f);
		
		speedo = Utils.loadImage("data/graphics/speedo.png");
		gears = Utils.loadImage("data/graphics/gears.png");		
		needle1 = Utils.loadImage("data/graphics/needle1.png");
		needle2 = Utils.loadImage("data/graphics/needle2.png");
		startlights = Utils.loadImage("data/graphics/startlights.png");
		chequeredFlag = Utils.loadImage("data/graphics/chequeredflag.png");
		warningFlag = Utils.loadImage("data/graphics/warningflag.png");
		tempTransform = new AffineTransform();
		
		setUpdateTime(REFRESH_TIME);
	}
	
	/**
	 * Paints the HUD. This method is called from a different thread than the
	 * game thread.
	 * @param g2 The graphics context.
	 */
	@Override
	public void paint(Graphics2D g2) {
				
		if (Settings.getInstance().showSAT) {
			paintTVGraphics(g2);
		}
		
		paintSpeedo(g2);
		paintInfo(g2);
		paintMessage(g2);
		
		if (Settings.getInstance().debug) {
			paintDebug(g2);
		}
	}
	
	/**
	 * Paints all 'general' information. This includes the current position and
	 * lap, as well as some graphics (starting lights, chequered flag, etc.).
	 */
	private void paintInfo(Graphics2D g2) {
		
		Settings settings = Settings.getInstance();
		int right = camera.getDisplay().getWidth();
		int center = right / 2;
		
		// Panels
		
		g2.setColor(PANEL_COLOR);
		g2.fillRoundRect(right - 140, getHeight() - 65, 130, 25, 10, 10);
		g2.fillRoundRect(right - 140, getHeight() - 35, 130, 25, 10, 10);
		
		// Texts
		
		String posText = getPosFormat(session.getRacePosition(target));
		Laptime currentLap = target.getCurrentLaptime();
		int currentTime = currentLap.getTime();
		
		if (tvGraphics.getShowingScreen() == TVGraphics.Screen.LAP_TIME_DIFFERENCE) {
			if (target.getIntermediate() == 0) {
				currentLap = target.getLaptime(target.getLap() - 1);
				currentTime = currentLap.getTime();
			} else {
				currentTime = currentLap.getIntermediateTime(target.getIntermediate() - 1);
			}
		}
		
		g2.setFont(DATA_FONT);
		g2.setColor(FONT_COLOR);
		g2.drawString(settings.getText("game.lap"), right - 130, getHeight() - 45);
		g2.drawString(target.getLap() + " / " + session.getLaps(), right - 80, getHeight() - 45);
		g2.drawString(settings.getText("game.time"), right - 130, getHeight() - 15);
		g2.drawString(Utils.timeFormat(currentTime, false), right - 80, getHeight() - 15);
		
		g2.setFont(POS_FONT);
		g2.setColor(SHADOW_COLOR);
		Utils2D.drawStringRight(g2, posText, right - 18, getHeight() - 73);
		g2.setColor(FONT_COLOR);
		Utils2D.drawStringRight(g2, posText, right - 20, getHeight() - 75);
				
		if (Settings.getInstance().showFramerate) {
			float framerate = gamedata.get(GAME_DATA_FRAMERATE);
			String fps = settings.getText("game.framerate") + " " + String.format("%.1f", framerate);
			g2.setFont(DEBUG_FONT);
			g2.setColor(SHADOW_COLOR);			
			Utils2D.drawStringRight(g2, fps, right - 9, 16);
			g2.setColor(FONT_COLOR);			
			Utils2D.drawStringRight(g2, fps, right - 10, 15);
		}
		
		// Starting lights
		
		float startTimer = gamedata.get(GAME_DATA_START_TIMER);
		if (startTimer > 0f) {
			int x = center - 150 / 2;
			int y = 0;
			
			if (startTimer < 1f) {
				g2.drawImage(startlights, x, y, x + 150, y + 80, 0, 0, 150, 80, null);				
			} else {
				g2.drawImage(startlights, x, y, x + 150, y + 80, 0, 80, 150, 160, null);
			}
		}
		
		// Chequered flag
		
		float finishTimer = gamedata.get(GAME_DATA_FINISH_TIMER);
		if (finishTimer > 0f) {
			g2.drawImage(chequeredFlag, center - 100 / 2, 0, 100, 120, null);
		}
		
		// Warning flag
		
		if (target instanceof Player) {
			if (((Player) target).isPenalty()) {
				g2.drawImage(warningFlag, center - 100 / 2, 0, 100, 120, null);
			}
		}
	}
	
	/**
	 * Paints the speedo. The speedo shows the current speed, gear and RPM 
	 * graphically.
	 */
	private void paintSpeedo(Graphics2D g2) {
		
		// Panels
		
		g2.setColor(PANEL_COLOR);
		g2.fillRoundRect(75, 95, 110, 25, 10, 10);
		
		// Speedo image(s)
		
		int x = 10;
		int y = 0;
		
		g2.drawImage(speedo, x, y, null);		
		g2.drawImage(gears, x + 77, y + 75, x + 77 + 21, y + 75 + 31, (target.getGear() + 2) * 25,
				0, (target.getGear() + 2) * 25 + 21, 31, null);
		
		tempTransform.setToTranslation(x, y);
		tempTransform.rotate(0.000202 * target.getRPM() + Math.PI, 60, 59);
		g2.drawImage(needle2, tempTransform, null);
		
		tempTransform.setToTranslation(x, y);
		tempTransform.rotate(0.0131 * (3.6f * target.getSpeed()) + Math.PI, 60, 59);
		g2.drawImage(needle1, tempTransform, null);
		
		// Speed text
				
		String suffix = Settings.getInstance().getText("game.kmh");
		String speedText = Math.abs(Math.round(3.6f * target.getSpeed())) + " " + suffix;
		
		if (Settings.getInstance().units == Settings.UNITS_MPH) {
			suffix = Settings.getInstance().getText("game.mph");
			speedText = Math.abs(Math.round(1.6f * 3.6f * target.getSpeed())) + " " + suffix;
		}
		
		if (Settings.getInstance().units == Settings.UNITS_MS) {
			suffix = Settings.getInstance().getText("game.ms");
			speedText = Math.abs(Math.round(target.getSpeed())) + " " + suffix;
		}
		
		g2.setFont(DATA_FONT);
		g2.setColor(FONT_COLOR);
		g2.drawString(speedText, x + 110, getHeight() - 15);
	}
	
	/**
	 * Paints the TV graphics. This method should only be called when the settings
	 * allow displaying these screens. Which screen is shown depends on the state
	 * of the session.
	 */
	private void paintTVGraphics(Graphics2D g2) {
		
		long currentFrame = System.currentTimeMillis();
		float deltaTime = (currentFrame - lastFrame) / 1000f;
		deltaTime = Math.min(deltaTime, 0.5f);
		lastFrame = currentFrame;
		
		tvGraphics.setGraphics(g2);
		tvGraphics.setArea(0, 0, camera.getDisplay().getWidth(), HUD_HEIGHT);
		tvGraphics.paint(g2, deltaTime);
		
		// Events that occur when entering a new sector
		
		if (lastIntermediate != target.getIntermediate()) {
			lastIntermediate = target.getIntermediate();
			
			if (target.getLap() > 1) {
				if (session.getMode() == Session.SessionMode.TIME) {
					tvGraphics.requestScreen(TVGraphics.Screen.LAP_TIME_DIFFERENCE);
				} else if (session.getRacePosition(target) > 1) {
					tvGraphics.requestScreen(TVGraphics.Screen.POSITION_DIFFERENCE);
				}
			} else {
				tvGraphics.requestScreen(TVGraphics.Screen.INFO);
			}
			
			if (lastIntermediate == 0) {
				if ((target.getLap() == session.getLaps()) || (Math.random() > 0.7)) {
					tvGraphics.requestScreen(TVGraphics.Screen.LAPS_REMAINING);
				}
			}
			
			if (Math.random() > 0.8) {
				tvGraphics.requestScreen(TVGraphics.Screen.STANDINGS);
			} else if (Math.random() > 0.8) {
				tvGraphics.requestScreen(TVGraphics.Screen.LAP_OVERVIEW);
			}
		}
		
		// Check for a new fastest lap
		
		Contestant fastest = session.getFastestLap();
		Laptime fastestLap = fastest.getFastestLaptime();
		if (fastestLap.getTime() < lastFastestLap.getTime()) {
			tvGraphics.requestScreen(TVGraphics.Screen.FASTEST_LAP);
			lastFastestLap = fastestLap;
		}
		
		// Show laptime comparison when near an intermediate
		
		CircuitPoint line = target.getCircuitData().getIntermediate(target.getIntermediate());
		if (target.getPosition().distance(line.pointX, line.pointY) < SAT_DISTANCE) {
			if ((target.getLap() > 1) && (session.getMode() == Session.SessionMode.TIME) && 
					!tvGraphics.isScreenRequested(TVGraphics.Screen.LAP_TIME_DIFFERENCE)) {
				tvGraphics.requestScreen(TVGraphics.Screen.LAP_TIME_COMPARE);
			}
		}
	}
	
	/**
	 * Paints any event message that has been set. If none has been set, this 
	 * method does nothing.
	 */
	private void paintMessage(Graphics2D g2) {
		
		if (message == null) {
			return;
		}

		if (messageTime == 0) {
			message = null;
			return;
		}
		
		messageTime--;
		
		int messageWidth = g2.getFontMetrics(DATA_FONT).stringWidth(message);
		int messageHeight = g2.getFontMetrics(DATA_FONT).getHeight();
		int center = camera.getDisplay().getWidth() / 2;
				
		g2.setColor(MESSAGE_BACKGROUND);
		g2.fillRoundRect(center - messageWidth / 2 - 10, 0, messageWidth + 20,
				messageHeight + 10, 10, 10);
		g2.setFont(DATA_FONT);
		g2.setColor(MESSAGE_FOREGROUND);
		Utils2D.drawStringCentered(g2, message, center, messageHeight);
	}
	
	/**
	 * Paints debug information to the screen. This method should only be called 
	 * when the game is running in debug mode.
	 */
	private void paintDebug(Graphics2D g2) {
		
		Settings settings = Settings.getInstance();		
		Runtime runtime = Runtime.getRuntime();
		int processors = runtime.availableProcessors();
		String arch = System.getProperty("os.arch");
		long memory = Utils.getConsumedMemory();
		float framerate = gamedata.get(GAME_DATA_FRAMERATE);
		float ups = gamedata.get(GAME_DATA_UPS);
		
		g2.setColor(MESSAGE_BACKGROUND);
		g2.fillRect(10, 0, 200, 85);
		g2.setColor(MESSAGE_FOREGROUND);
		g2.drawRect(10, 0, 200, 85);
		g2.setFont(DEBUG_FONT);		
		g2.drawString(settings.getText("game.framerate") + " " + String.format("%.1f", framerate), 20, 15);
		g2.drawString(settings.getText("game.ups") + " " + String.format("%.1f", ups), 20, 30);
		g2.drawString(settings.getText("game.processors") + " " + processors + " (" + arch + ")", 20, 45);
		g2.drawString(settings.getText("game.memory") + " " + (memory / 100000f) / 10f + " mb", 20, 60);
		g2.drawString("Java " + Platform.getJavaVersion() + " / " + Platform.getPlatform(), 20, 75);
	}
	
	public void setTarget(Contestant newTarget) {
		if ((target != newTarget) && (newTarget != null)) {
			target = newTarget;
			tvGraphics.setTarget(newTarget);
			setMessage(newTarget.getName());
		}
	}
	
	public Contestant getTarget() {
		return target;
	}
	
	public void setMessage(String message) {
		this.message = message;
		messageTime = (message != null) ? MESSAGE_TIME : 0;
	}
	
	public void setGameData(float framerate, float ups, float polygons, float startTimer, 
			float finishTimer) {
		gamedata.put(GAME_DATA_FRAMERATE, framerate);
		gamedata.put(GAME_DATA_UPS, ups);
		gamedata.put(GAME_DATA_POLYGONS, polygons);
		gamedata.put(GAME_DATA_START_TIMER, startTimer);
		gamedata.put(GAME_DATA_FINISH_TIMER, finishTimer);
	}

	private String getPosFormat(int p) {
		if (p == 1) { return Settings.getInstance().getText("game.p1"); }
		if (p == 2) { return Settings.getInstance().getText("game.p2"); } 
		if (p == 3) { return Settings.getInstance().getText("game.p3"); }
		return p + Settings.getInstance().getText("game.pn");
	}

	private static int getWidth(Display viewport) {
		if (viewport.getWidth() <= HUD_WIDTH) {
			return HUD_WIDTH;
		} else {
			return HUD_WIDTH * 2;
		}
	}
}

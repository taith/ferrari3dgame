//-----------------------------------------------------------------------------
// Ferrari3D
// TVGraphics
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.ferrari3d.Contestant;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.FormatUtils;
import nl.colorize.util.swing.Utils2D;

/**
 * Paints a number of TV graphics using Java 2D. A number of screens can be
 * requested concurrently, they will then be shown in order of priority.
 */
public class TVGraphics {

	private Session session;
	private Contestant target;
	
	private Graphics2D g2;
	private Rectangle area;
	
	private List<Screen> requestedScreens;
	private Screen showing;
	private float time;
	
	private static final Font FONT = Utils.loadFont("data/fonts/manana.ttf", Font.PLAIN, 16f);
	private static final Font BIG_FONT = FONT.deriveFont(40f);
	private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 128);
	private static final Color NAME_COLOR = new Color(255, 255, 255);
	private static final Color TIME_COLOR = new Color(255, 200, 0);
	private static final Color INFO_COLOR = new Color(0, 200, 255);
	private static final Color POS_BLOCK_BACKGROUND_COLOR = new Color(255, 200, 0);
	private static final Color POS_BLOCK_FOREGROUND_COLOR = new Color(50, 50, 50);
	private static final int LINE_1 = 30;
	private static final int LINE_2 = 55;
	private static final int LINE_3 = 80;
	private static final int LINE_4 = 105;
	private static final int NAME_COLUMN = 230;
	private static final int TIME_COLUMN = 80;
	
	/**
	 * Enum with all screen types. A higher priority number means that the
	 * screen is more important.
	 */
	public enum Screen {
		INFO(0, 4f),
		LAP_TIME(1, 1f),
		LAP_TIME_COMPARE(3, 1f),
		LAP_TIME_DIFFERENCE(5, 4f),
		POSITION_DIFFERENCE(3, 4f),
		STANDINGS(2, 4f),
		LAP_OVERVIEW(2, 4f),
		FASTEST_LAP(4, 4f),
		LAPS_REMAINING(2, 4f);
		
		private int priority;
		private float duration;
		
		private Screen(int priority, float duration) {
			this.priority = priority;
			this.duration = duration;
		}
		
		public int getPriority() {
			return priority;
		}
		
		public float getDuration() {
			return duration;
		}
	}
	
	/**
	 * Creates a new TV graphics object for the specified session. Initially
	 * no graphics context or area will be set.
	 */
	public TVGraphics(Session session) {
		this.session = session;
		requestedScreens = new ArrayList<Screen>();
	}
	
	public Session getSession() {
		return session;
	}
	
	public void setTarget(Contestant newTarget) {
		if (target != newTarget) {
			target = newTarget;
			requestedScreens.clear();
			showing = null;
		}
	}
	
	public Contestant getTarget() {
		return target;
	}
	
	public void setGraphics(Graphics2D g2) {
		this.g2 = g2;
	}
	
	public void setArea(int x, int y, int width, int height) {
		if (area == null) {
			area = new Rectangle();
		}
		area.setBounds(x, y, width, height);
	}
	
	/**
	 * Requests the specified screen to appear. If the same screen was already
	 * requested, this request is ignored. Otherwise, the screen will be queued
	 * and shown after all other requested screens with higher or equal priority
	 * are shown.
	 */
	public void requestScreen(Screen screen) {
		if (!requestedScreens.contains(screen)) {
			requestedScreens.add(screen);
			if (showing == null) {
				showNextScreen();
			}
		}
	}
	
	/**
	 * Returns if the specified screen type is queued to appear.
	 */
	public boolean isScreenRequested(Screen screen) {
		return requestedScreens.contains(screen);
	}
	
	/**
	 * Returns the screen that should be shown. This will be the queued screen
	 * with the highest priority. If no screens are queued this method will
	 * return {@code null}.
	 */
	private Screen nextScreen() {
		Screen next = null;
		for (Screen i : requestedScreens) {
			if ((next == null) || (i.getPriority() > next.getPriority())) {
				next = i;
			}
		}
		return next;
	}
	
	/**
	 * Replaces the screen that is currently being shown with the next screen
	 * that should be shown.
	 */
	private void showNextScreen() {
		if (showing != null) {
			requestedScreens.remove(showing);
		}
		showing = nextScreen();
		time = 0f;
	}
	
	public Screen getShowingScreen() {
		return showing;
	}
	
	/**
	 * Paints the currently active screen to the current graphics context. If
	 * no screens are scheduled to appear this method will do nothing.
	 * @param g2 The graphics context to which should be painted.
	 * @param dt Delta time since the last frame, in seconds.
	 * @throws IllegalStateException if no graphics context has been set.
	 */
	public void paint(Graphics2D g2, float dt) {
		
		if ((g2 == null) || (area == null)) {
			throw new IllegalStateException("Graphics context is not set");
		}
		
		if (target == null) {
			throw new IllegalStateException("Target is not set");
		}
		
		if (showing != null) {
			time += dt;
			if (time >= showing.getDuration()) {
				showNextScreen();
			}
			
			if (showing != null) {
				switch (showing) {
					case INFO : paintInfoScreen(); break;
					case LAP_TIME : paintLapTimeScreen(); break;
					case LAP_TIME_COMPARE : paintLapTimeCompare(); break;
					case LAP_TIME_DIFFERENCE : paintLapTimeDifferenceScreen(); break;
					case POSITION_DIFFERENCE : paintPositionDifferenceScreen(); break;
					case STANDINGS : paintStandingsScreen(); break;
					case LAP_OVERVIEW : paintLapOverviewScreen(); break;
					case FASTEST_LAP : paintFastestLapScreen(); break;
					case LAPS_REMAINING : paintLapsRemaining(); break;
					default : throw new AssertionError();
				}
			}
		}
	}
	
	/**
	 * Paints a general information screen for the targeted contestant.
	 */
	private void paintInfoScreen() {
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(target), -NAME_COLUMN, LINE_1, 'l');
		g2.setColor(INFO_COLOR);
		paintString(formatCarName(target), -NAME_COLUMN, LINE_2, 'l');
		paintPosBlock(session.getRacePosition(target), true, TIME_COLUMN, LINE_1);
	}
	
	/**
	 * Paints a screen showing the current lap time for the targeted contestant.
	 */
	private void paintLapTimeScreen() {
		
		int time = target.getCurrentLaptime().getIntermediateTime(target.getIntermediate());
		
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(target), -NAME_COLUMN, LINE_1, 'l');
		g2.setColor(TIME_COLOR);
		paintString(Utils.timeFormat(time, true), -NAME_COLUMN, LINE_2, 'l');
	}
	
	/**
	 * Shows a comparison between the targeted contestant's current laptime, and
	 * that of the session leader.
	 */
	private void paintLapTimeCompare() {
		
		Contestant leader = session.getFastestLap();
		int intermediate = target.getIntermediate();
		int targetTime = target.getCurrentLaptime().getIntermediateTime(intermediate);
		int bestTime = leader.getFastestLaptime().getIntermediateTime(intermediate);
		
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(target), -NAME_COLUMN, LINE_1, 'l');
		paintString(formatName(leader), NAME_COLUMN, LINE_1, 'r');
		g2.setColor(TIME_COLOR);
		paintString(Utils.timeFormat(targetTime, true), -NAME_COLUMN, LINE_2, 'l');
		paintString(Utils.timeFormat(bestTime, false), NAME_COLUMN, LINE_2, 'r');
		g2.setColor(INFO_COLOR);
		paintString(getIntermediateText(intermediate), 0, LINE_4, 'c');
	}
	
	/**
	 * Shows a comparison between the targeted contestant's current laptime, and
	 * that of the session leader. This screen is similar to 
	 * {@link #paintLapTimeCompare()} but also shows the time difference.
	 */
	private void paintLapTimeDifferenceScreen() {
		
		Contestant leader = session.getFastestLap();
		int intermediate = target.getIntermediate();
		int targetTime = 0;
		int bestTime = 0;
		
		// At start/finish compare to the target's previous lap
		if (intermediate == 0) {
			intermediate = 2;
			targetTime = target.getLastLaptime().getTime();
			bestTime = leader.getFastestLaptime().getTime();
		} else {
			intermediate--;
			targetTime = target.getCurrentLaptime().getIntermediateTime(intermediate);
			bestTime = leader.getFastestLaptime().getIntermediateTime(intermediate);
		}
		
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(target), -NAME_COLUMN, LINE_1, 'l');
		if (target.getIntermediate() != 0) {
			paintString(formatName(leader), NAME_COLUMN, LINE_1, 'r');
		}
		g2.setColor(TIME_COLOR);
		paintString(Utils.timeFormat(targetTime, false), -NAME_COLUMN, LINE_2, 'l');
		if (target.getIntermediate() != 0) {
			paintString(Utils.timeFormat(bestTime, false), NAME_COLUMN, LINE_2, 'r');
		}
		paintString(Utils.timeDiffFormat(targetTime, bestTime), 0, LINE_2, 'c');
		g2.setColor(INFO_COLOR);
		paintString(getIntermediateText(intermediate), 0, LINE_4, 'c');
		if (target.getIntermediate() == 0) {
			paintPosBlock(session.getRacePosition(target), true, TIME_COLUMN, LINE_1);
		}
	}
	
	/**
	 * Paints a screen that shows the difference between the targeted contestant
	 * and the contestant in front. This screen should only be displayed in race
	 * mode.
	 * @throws IllegalStateException when the targeted contestant is leading.
	 */
	private void paintPositionDifferenceScreen() {
		
		int position = session.getRacePosition(target);
		Contestant front = session.getContestantAtRacePosition(position - 1);

		if (position == 1) {
			throw new IllegalStateException("Target contestant is the leader");
		}

		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(front), -NAME_COLUMN, LINE_1, 'l');
		paintString(formatName(target), NAME_COLUMN, LINE_1, 'r');
		paintPosBlock(position - 1, true, -NAME_COLUMN - 60, LINE_1); 
		paintPosBlock(position, true, NAME_COLUMN + 10, LINE_1); 
		//TODO show actual time difference
	}
	
	/**
	 * Paints the standings screen. The top 8 drivers will be displayed, sorted
	 * by fastest lap time.
	 */
	private void paintStandingsScreen() {
		
		Laptime best = session.getFastestLap().getFastestLaptime();
		
		paintBackground();
		
		// Left column
		for (int i = 1; i <= 4; i++) {
			Contestant c = session.getContestantAtRacePosition(i);
			if (c != null) {
				paintPosBlock(i, false, -NAME_COLUMN, getLineY(i));
				g2.setColor(NAME_COLOR);
				paintString(formatName(c), -NAME_COLUMN + 30, getLineY(i), 'l');
				if ((i != 1) && (session.getMode() == Session.SessionMode.TIME)) {
					g2.setColor(TIME_COLOR);
					paintString(Utils.timeDiffFormat(c.getFastestLaptime(), best), 
							-TIME_COLUMN, getLineY(i), 'l');
				}
			}
		}
		
		// Right column
		for (int i = 5; i <= 8; i++) {
			Contestant c = session.getContestantAtRacePosition(i);
			if (c != null) {
				paintPosBlock(i, false, 0, getLineY(i - 4));
				g2.setColor(NAME_COLOR);
				paintString(formatName(c), 30, getLineY(i - 4), 'l');
				if (session.getMode() == Session.SessionMode.TIME) {
					g2.setColor(TIME_COLOR);
					paintString(Utils.timeDiffFormat(c.getFastestLaptime(), best), 
							NAME_COLUMN, getLineY(i - 4), 'r');
				}
			}
		}
	}
	
	/**
	 * Paints a screen that compares the lap times of the targeted contestant
	 * with those of the contestant in front. In the contestant is the leader,
	 * the comparison will be with the second placed contestant.
	 */
	private void paintLapOverviewScreen() {
		
		int position = session.getRacePosition(target);
		Contestant left = session.getContestantAtRacePosition(position - 1);
		Contestant right = target;
		if (position == 1) {
			left = target;
			right = session.getContestantAtRacePosition(2);
		}
		
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(left), -NAME_COLUMN, LINE_1, 'l');
		paintString(formatName(target), NAME_COLUMN, LINE_1, 'r');
		paintPosBlock(session.getRacePosition(left), true, -NAME_COLUMN - 60, LINE_1); 
		paintPosBlock(session.getRacePosition(right), true, NAME_COLUMN + 10, LINE_1);
		g2.setColor(NAME_COLOR);
		paintString(getFastestLapText(), 0, LINE_3, 'c');
		paintString(getLastLapText(), 0, LINE_4, 'c');
		g2.setColor(TIME_COLOR);
		paintString(Utils.timeFormat(left.getFastestLaptime()), -NAME_COLUMN, LINE_3, 'l');
		paintString(Utils.timeFormat(right.getFastestLaptime()), NAME_COLUMN, LINE_3, 'r');
		paintString(Utils.timeFormat(left.getLastLaptime()), -NAME_COLUMN, LINE_4, 'l');
		paintString(Utils.timeFormat(right.getLastLaptime()), NAME_COLUMN, LINE_4, 'r');
	}
	
	/**
	 * Paints the screen that should appear when a new fastest lap time has been
	 * set.
	 */
	private void paintFastestLapScreen() {
		
		Contestant owner = session.getFastestLap();
		Laptime best = owner.getFastestLaptime();
		
		paintBackground();
		g2.setColor(NAME_COLOR);
		paintString(formatName(owner), -NAME_COLUMN, LINE_1, 'l');
		g2.setColor(INFO_COLOR);
		paintString(formatCarName(owner), -NAME_COLUMN, LINE_2, 'l');
		paintString(getFastestLapText(), TIME_COLUMN, LINE_1, 'l');
		g2.setColor(TIME_COLOR);
		paintString(Utils.timeFormat(best), TIME_COLUMN, LINE_2, 'l');
	}
	
	/**
	 * Paints a screen displaying how many laps are left for the targeted 
	 * contestant. This screen should normally be displayed when the start/finish
	 * line is crossed.
	 */
	private void paintLapsRemaining() {
		int remaining = session.getLaps() - target.getLap() + 1;
		paintBackground();
		g2.setColor(TIME_COLOR);
		paintString(getLapsRemainingText(remaining), 0, LINE_2, 'c');
	}
	
	private void paintBackground() {
		g2.setColor(BACKGROUND_COLOR);
		g2.fillRect(area.x, area.y, area.width, area.height);
	}
	
	/**
	 * Paints a string to the graphics context. The default font is set by this 
	 * method.
	 * @param text The text that should be painted.
	 * @param xFromCenter X-coordinate measured from the center of the area.
	 * @param yFromTop Y-coordinate, measured from the top of the area.
	 * @param align "l", "c" or "r" for left- center- or right-aligned.
	 */
	@SuppressWarnings("deprecation")
	private void paintString(String text, int xFromCenter, int yFromTop, char align) {
		int x = (area.x + area.width / 2) + xFromCenter;
		int y = area.y + yFromTop;
		g2.setFont(FONT);
		Utils2D.drawAlignedString(g2, text, x, y, align);
	}
	
	/**
	 * Paints a block with a position number in it.
	 * @param pos The position.
	 * @param big When true, the block will be two lines high.
	 * @param xFromCenter X-coordinate measured from the center of the area.
	 * @param yFromTop Y-coordinate, measured from the top of the area.
	 */
	private void paintPosBlock(int pos, boolean big, int xFromCenter, int yFromTop) {
		
		int x = (area.x + area.width / 2) + xFromCenter;
		int y = area.y + yFromTop;
		
		if (big) {
			g2.setColor(POS_BLOCK_BACKGROUND_COLOR);
			g2.fillRect(x, y - 20, 50, 50);
			g2.setFont(BIG_FONT);
			g2.setColor(POS_BLOCK_FOREGROUND_COLOR);
			Utils2D.drawStringCentered(g2, Integer.toString(pos), x + 25, y + 20);
		} else {
			g2.setColor(POS_BLOCK_BACKGROUND_COLOR);
			g2.fillRect(x, y - 16, 20, 20);
			g2.setFont(FONT);
			g2.setColor(POS_BLOCK_FOREGROUND_COLOR);
			Utils2D.drawStringCentered(g2, Integer.toString(pos), x + 10, y);
		}
	}
	
	private String formatName(Contestant c) {
		return FormatUtils.nameFormat(c.getName(), false).toUpperCase();
	}
	
	private String formatCarName(Contestant c) {
		return c.getCarName().toUpperCase();
	}
	
	private String getIntermediateText(int intermediate) {
		Settings settings = Settings.getInstance();
		switch (intermediate) {
			case 0 : return settings.getText("game.intermediate1").toUpperCase();
			case 1 : return settings.getText("game.intermediate2").toUpperCase();
			case 2 : return settings.getText("game.intermediate3").toUpperCase();
			default : throw new AssertionError();
		}
	}
	
	private String getFastestLapText() {
		return Settings.getInstance().getText("game.fastestlap").toUpperCase();	
	}
	
	private String getLastLapText() {
		return Settings.getInstance().getText("game.lastlap").toUpperCase();	
	}
	
	private String getLapsRemainingText(int remaining) {
		if (remaining == 1) {
			return Settings.getInstance().getText("game.finallap").toUpperCase();	
		}
		return Settings.getInstance().getText("game.lapsremaining", "" + remaining).toUpperCase();
	}
	
	private int getLineY(int lineNumber) {
		switch (lineNumber) {
			case 1 : return LINE_1;
			case 2 : return LINE_2;
			case 3 : return LINE_3;
			case 4 : return LINE_4;
			default : throw new AssertionError();
		}
	}
}

//-----------------------------------------------------------------------------
// Ferrari3D
// TestTvGraphics
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.dennisbijlsma.ferrari3d.Contestant;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.graphics.TVGraphics;
import com.dennisbijlsma.ferrari3d.util.Laptime;
import com.dennisbijlsma.ferrari3d.util.Settings;
import nl.colorize.util.swing.Utils2D;
import nl.colorize.util.swing.anim.Animatable;
import nl.colorize.util.swing.anim.Animation;

/**
 * Test class that displays all possible screens supported by the 
 * {@code TvGraphics} class.
 */

public class TestTvGraphics extends JPanel implements KeyListener, Animatable {
	
	private TVGraphics tvGraphics;
	
	private static final float FRAME_TIME = 0.04f;
	private static final int GRAPHICS_WIDTH = 800;
	private static final int GRAPHICS_HEIGHT = 128;
	private static final Color BACKGROUND_COLOR = new Color(100, 100, 100);
	private static final Color LINE_COLOR = new Color(90, 90, 90);
	private static final Color TEXT_COLOR = new Color(255, 255, 255);
	private static final String[] NAMES = {"Hamilton", "Massa", "Raikkonen", "Alonso", 
		"Kubica", "Vettel", "Webber", "Barrichello"};

	public static void main(String[] args) {
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
		} catch (Exception e) {
			throw new AssertionError();
		}
		
		JFrame frame = new JFrame("TestTvGraphics");
		frame.setSize(Settings.WINDOW_WIDTH, Settings.WINDOW_HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(new TestTvGraphics(), BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	public TestTvGraphics() {
		
		super(null);
		
		setOpaque(true);
		setBackground(BACKGROUND_COLOR);
		setFocusable(true);
		addKeyListener(this);
		requestFocus();
		
		Session session = new Session(Session.SessionMode.TIME, 3);
		for (int i = 0; i < 8; i++) {
			session.addContestant(new MockContestant(NAMES[i], session));
		}
		session.setStarted();
		
		tvGraphics = new TVGraphics(session);
		tvGraphics.setTarget(session.getContestantAtRacePosition(2));
		
		Animation anim = new Animation("animation", 100000f); //TODO
		anim.addAnimatable(this);
		anim.start();
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		Graphics2D g2 = Utils2D.createGraphics(g, true, false);
		
		g2.setColor(LINE_COLOR);
		g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
		
		TVGraphics.Screen[] screens = TVGraphics.Screen.values();
		g2.setColor(TEXT_COLOR);
		for (int i = 0; i < screens.length; i++) {
			g2.drawString((i + 1) + ": " + screens[i].toString(), 20, i * 25 + 30);
		}
		
		tvGraphics.setGraphics(g2);
		tvGraphics.setArea(0, getHeight() - GRAPHICS_HEIGHT, GRAPHICS_WIDTH, GRAPHICS_HEIGHT);
		tvGraphics.paint(g2, FRAME_TIME);
	}
	
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_1 : tvGraphics.requestScreen(TVGraphics.Screen.INFO); break; 
			case KeyEvent.VK_2 : tvGraphics.requestScreen(TVGraphics.Screen.LAP_TIME); break; 
			case KeyEvent.VK_3 : tvGraphics.requestScreen(TVGraphics.Screen.LAP_TIME_COMPARE); break; 
			case KeyEvent.VK_4 : tvGraphics.requestScreen(TVGraphics.Screen.LAP_TIME_DIFFERENCE); break;
			case KeyEvent.VK_5 : tvGraphics.requestScreen(TVGraphics.Screen.POSITION_DIFFERENCE); break;
			case KeyEvent.VK_6 : tvGraphics.requestScreen(TVGraphics.Screen.STANDINGS); break;
			case KeyEvent.VK_7 : tvGraphics.requestScreen(TVGraphics.Screen.LAP_OVERVIEW); break;
			case KeyEvent.VK_8 : tvGraphics.requestScreen(TVGraphics.Screen.FASTEST_LAP); break;
			case KeyEvent.VK_9 : tvGraphics.requestScreen(TVGraphics.Screen.LAPS_REMAINING); break;
			default : break;
		}
	}
	
	public void keyPressed(KeyEvent e) {
		
	}
	
	public void keyTyped(KeyEvent e) {
		
	}
	
	public void animate(Animation anim) {
		repaint();
	}
	
	private int random(int n) {
		return (int) Math.round(Math.random() * n);
	}
	
	/**
	 * Mock implementation of {@code Contestant} so that the TV graphics have 
	 * some test data.
	 */
	
	private static class MockContestant extends Contestant {
		
		private Laptime[] laptimes;
		
		public MockContestant(String name, Session session) {
			super(name, session);
			laptimes = new Laptime[3];
			for (int i = 0; i < laptimes.length; i++) {
				laptimes[i] = generateRandomLapTime();
			}
		}
		
		private Laptime generateRandomLapTime() {
			Laptime laptime = new Laptime();
			laptime.setSectorTime(0, (int) Math.round(Math.random() * 15000 + 20000), true);
			laptime.setSectorTime(1, (int) Math.round(Math.random() * 15000 + 20000), true);
			laptime.setSectorTime(2, (int) Math.round(Math.random() * 15000 + 20000), true);
			return laptime;
		}
		
		protected void updateControls(float dt) { }
		protected void updatePhysics(float dt) { }
		protected void updateLapData(float dt) { }
		protected void updateCarGraphics(float dt) { }
		public String getCarName() { return "Ferrari 248"; }
		public String getCircuitName() { return "Monza"; }
		public int getLap() { return laptimes.length - 1; }
		public int getIntermediate() { return 1; }
		public int getPoint() { return 10; }
		public Laptime getLaptime(int index) { return laptimes[index]; }
		public Laptime getCurrentLaptime() { return laptimes[laptimes.length - 1]; }
		public Laptime getPreviousLaptime() { return laptimes[laptimes.length - 2]; }
		public Laptime getFastestLaptime() { return laptimes[0]; }
	}
}

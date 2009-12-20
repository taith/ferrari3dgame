//-----------------------------------------------------------------------------
// Ferrari3D
// TestAI
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.w3c.dom.Document;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.ferrari3d.AI;
import com.dennisbijlsma.ferrari3d.CircuitData;
import com.dennisbijlsma.ferrari3d.Session;
import com.dennisbijlsma.ferrari3d.editor.ContentLoader;
import com.dennisbijlsma.ferrari3d.graphics.AbstractCar;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.XMLUtils;
import nl.colorize.util.swing.MacHandler;
import nl.colorize.util.swing.Utils2D;
import nl.colorize.util.swing.anim.Animatable;
import nl.colorize.util.swing.anim.Animation;

/**
 * Graphical test to test the behavior of AI-controlled cars. 
 */

public class TestAI extends JPanel implements Animatable, KeyListener {
	
	private Session session;
	private List<AI> cars;
	private TestCircuitData circuitData;
	
	private Vector3D camera;
	private boolean fastForward;
	private boolean zoomIn;
	
	private static final int NUM_CARS = 4;
	private static final ResourceFile TEST_CAR = new ResourceFile("cars/Ferrari 248/Ferrari 248.xml");
	private static final ResourceFile TEST_CIRCUIT = new ResourceFile("circuits/Monza/Monza.xml");
	private static final float DELTA_TIME = 0.04f;
	private static final Color BACKGROUND_COLOR_WHITE = Color.WHITE;
	private static final Color BACKGROUND_COLOR_BLACK = new Color(150, 150, 150);
	private static final Color[] CAR_COLORS = {new Color(255, 200,0), new Color(225, 170, 0),
			new Color(195, 140, 0), new Color(165, 110, 0), new Color(135, 80, 0)};
	private static final Color CIRCUIT_POINT_COLOR = new Color(255, 0, 0);
	private static final Color CIRCUIT_ALT_POINT_COLOR = new Color(150, 0, 0);
	private static final Color CIRCUIT_SPEED_POINT_COLOR = Color.CYAN;
	private static final Color PANEL_BACKGROUND = new Color(0, 0, 0, 192);
	private static final Color PANEL_FOREGROUND = Color.WHITE;

	public static void main(String[] args) throws Exception {
		
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		MacHandler.setSystemProperty(MacHandler.SYSTEM_PROPERTY_QUARTZ, true);
				
		JFrame frame = new JFrame("TestAI");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(new TestAI(), BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	public TestAI() throws Exception {
		
		super(null);
		
		setOpaque(true);
		setBackground(BACKGROUND_COLOR_WHITE);
		setFocusable(true);
		requestFocus();
		addKeyListener(this);
		
		Settings.getInstance().init();
		Settings.getInstance().load();
		
		session = new Session(Session.SessionMode.RACE, 20);
		cars = new ArrayList<AI>();
		TestCar car = new TestCar(TEST_CAR);
		circuitData = new TestCircuitData(TEST_CIRCUIT);
		for (int i = 1; i <= NUM_CARS; i++) {
			AI ai = createAI("AI-" + i, session, car, circuitData);
			ai.setPosition(circuitData.getStartingGridPosition(i));
			ai.setOrientation(1.57f); //TODO magic going on here?
			session.addContestant(ai);
			cars.add(ai);
		}
		session.setStarted();
		
		camera = new Vector3D(400f, 0f, 300f);
		fastForward = false;
		zoomIn = false;
		
		Animation anim = new Animation("animation", 1f);
		anim.setFrameTime(DELTA_TIME);
		anim.setLoopMode(Animation.LoopMode.LOOP);
		anim.addAnimatable(this);
		anim.start();
	}
	
	private AI createAI(String name, Session session, AbstractCar car, CircuitData circuit) {
		AI ai = new AI(name, session);
		ai.setCar(car);
		ai.setCircuitData(circuit);
		return ai;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		Graphics2D g2 = Utils2D.createGraphics(g, true, false);
				
		// Paint background
		
		g2.setColor(BACKGROUND_COLOR_BLACK);
		for (int i = 0; i < getWidth(); i += 80) {
			for (int j = 0; j < getHeight(); j += 80) {
				if ((i % 160 == 0) && (j % 160 == 0)) {
					g2.fillRect(i, j, 80, 80);
				} else if (i % 160 == 0) {
					g2.fillRect(i + 80, j, 80, 80);
				}
			}
		}
		
		// Paint circuit
		
		for (int i = 0; i < circuitData.getNumPoints(); i++) {
			CircuitPoint point = circuitData.getPoint(i);
			
			if (point.isAltPoint()) {
				g2.setColor(CIRCUIT_ALT_POINT_COLOR);
				paintLocation(g2, point.altX, point.altY, 4, "");
			}
			
			if (point.isSuggestedSpeed()) {
				g2.setColor(CIRCUIT_SPEED_POINT_COLOR);
				paintLocation(g2, point.pointX, point.pointY, 6, 
						String.format("%.1f", point.getSuggestedSpeed()));
			} else {
				g2.setColor(CIRCUIT_POINT_COLOR);
				paintLocation(g2, point.pointX, point.pointY, 6, "");
			}
		}
		
		// Update and paint cars

		for (int i = 0; i < cars.size(); i++) {
			AI car = cars.get(i);
			car.update(DELTA_TIME);
			if (fastForward) {
				car.update(DELTA_TIME);
			}
			
			g2.setColor(CAR_COLORS[i]);
			paintLocation(g2, car.getPosition().getX(), car.getPosition().getZ(), 10, 
					String.format("%.1f", car.getSpeed()));
		}
		
		// Paint HUD
		
		int x = getWidth() - 200;
		int y = 50;
		
		g2.setColor(PANEL_BACKGROUND);
		g2.fillRoundRect(x, 20, 180, getHeight() - 40, 20, 20);
		
		x += 10;
		
		for (int i = 0; i < cars.size(); i++) {
			AI car = (AI) session.getContestantAtRacePosition(i + 1);
			
			g2.setColor(CAR_COLORS[i]);
			g2.fillOval(x, y - 15, 15, 15);
			
			g2.setColor(PANEL_FOREGROUND);
			g2.drawString(car.getName(), x + 20, y);
			g2.drawString(String.format("Speed: %.1f m/s", car.getSpeed()), x + 20, y + 20);
			g2.drawString("Behavior: " + getBehaviorString(car), x + 20, y + 40);
			g2.drawString("Laptime: " + Utils.timeFormat(car.getCurrentLaptime()), x + 20, y + 60);
			y += 80;
		}

		g2.setColor(PANEL_FOREGROUND);
		g2.drawString("Zoom: " + (zoomIn ? "Zoomed in" : "Normal"), x, getHeight() - 60);
		g2.drawString("Speed: " + (fastForward ? "Fast-forward" : "Normal"), x, getHeight() - 40);
	}
	
	/**
	 * Converts the specified point in world-space to a coordinate in screen-space,
	 * and paints it as a circle of the specified size.
	 */
	
	private void paintLocation(Graphics2D g2, float x, float y, int size, String label) {
		float zoom = zoomIn ? 3f : 0.3f;
		int screenX = Math.round(x * zoom + camera.x);
		int screenY = Math.round(y * zoom + camera.z);
		g2.fillOval(screenX - size / 2, screenY - size / 2, size, size);
		g2.drawString(label, screenX + size, screenY + size);
	}
	
	private String getBehaviorString(AI ai) {
		if (ai.isEvading()) {
			return "evading";
		} else if (ai.isBlocking()) {
			return "blocking";
		} else if (ai.isOvertaking()) {
			return "overtaking";
		} else {
			return "driving";
		}
	}

	public void animate(Animation anim) {
		repaint();
	}
	
	public void keyPressed(KeyEvent e) {
		float move = e.isShiftDown() ? 30f : 3f;
		if (e.getKeyCode() == KeyEvent.VK_LEFT) { camera.x += move; }
		if (e.getKeyCode() == KeyEvent.VK_RIGHT) { camera.x -= move; }
		if (e.getKeyCode() == KeyEvent.VK_UP) { camera.z += move; }
		if (e.getKeyCode() == KeyEvent.VK_DOWN) { camera.z -= move; }
	}
	
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_S) {
			fastForward = !fastForward;
		} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
			zoomIn = !zoomIn;
		}
	}
	
	public void keyTyped(KeyEvent e) {
		
	}
	
	/**
	 * Non-graphical implementation of the {@code CircuitData} interface.
	 */
	
	private static class TestCircuitData implements CircuitData {
		
		private List<CircuitPoint> points;
		private List<Vector3D> grid;
		
		public TestCircuitData(ResourceFile xml) throws Exception {
			Document document = XMLUtils.parseXML(xml.getStream());
			ContentLoader loader = new ContentLoader();
			points = loader.parseCircuitPoints(document);
			for (CircuitPoint i : points) { i.pointX *= 10f; i.pointY *= 10f; i.altX *= 10f; i.altY *= 10f; } //TODO magic?
			grid = loader.parseCircuitStartGrid(document);
			for (Vector3D i : grid) { i.setVector(i.x * 10f, i.y * 10f, i.z * 10f); } //TODO
		}

		public CircuitPoint getPoint(int index) {
			if ((index < 0) || (index >= points.size())) {
				return points.get(0);
			}
			return points.get(index); 
		}
		
		public String getCircuitName() { return "Test"; }
		public int getNumPoints() { return points.size(); }
		public CircuitPoint getIntermediate(int index) { return points.get(0); }
		public ImmutableVector3D getClosestCamera(ImmutableVector3D v) { return null; }
		public ImmutableVector3D getStartingGridPosition(int index) { return grid.get(index); }
	}
	
	/**
	 * Non-graphical implementation of {@code AbstractCar}.
	 */
	
	private static class TestCar extends AbstractCar {
		
		public TestCar(ResourceFile xml) throws Exception {
			super();
			Document document = XMLUtils.parseXML(xml.getStream());
			setInfo(new ContentLoader().parseCarInfo(document));
		}
		
		public void setPosition(float x, float z) { }
		public void setOrientation(float angle) { }
		public void doGearChange(boolean up) { }
		public void doNextLap() { }
	}
}

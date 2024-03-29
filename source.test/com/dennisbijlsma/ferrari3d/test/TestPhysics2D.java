//-----------------------------------------------------------------------------
// Ferrari3D
// TestPhysics2D
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.dennisbijlsma.ferrari3d.CarPhysics;
import nl.colorize.util.swing.MacSupport;
import nl.colorize.util.swing.Utils2D;
import nl.colorize.util.swing.anim.Animatable;
import nl.colorize.util.swing.anim.Animation;

/**
 * Test class for car physics that uses Java 2D to display the results. While
 * driving some information is printed on-screen to give feedback about how
 * the physics are working.
 */
public class TestPhysics2D extends JPanel implements KeyListener, Animatable {
	
	private CarPhysics physics;
	private boolean accelerate;
	private boolean brake;
	private boolean steerLeft;
	private boolean steerRight;
	
	private BufferedImage backgroundImage;
	private BufferedImage carImage;
	
	private static final Color PANEL_BACKGROUND = new Color(0, 0, 0, 192);
	private static final Color PANEL_FOREGROUND = Color.WHITE;

	public static void main(String[] args) {
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			MacSupport.setSystemProperty(MacSupport.SYSTEM_PROPERTY_QUARTZ, true);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
		
		JFrame frame = new JFrame("TestPhysics2D");
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(new TestPhysics2D(), BorderLayout.CENTER);
		frame.setVisible(true);
	}
	
	public TestPhysics2D() {
		
		super(null);
		
		setOpaque(true);
		setBackground(Color.BLACK);
		setFocusable(true);
		addKeyListener(this);
		requestFocus();
		
		physics = new CarPhysics();
		physics.setOrientation(3.14f);
		
		Animation anim = new Animation("animation", 1f);
		anim.setFrameTime(0.04f);
		anim.setLoopMode(Animation.LoopMode.LOOP);
		anim.addAnimatable(this);
		anim.start();
	}
	
	private BufferedImage createCarImage() {
		BufferedImage image = Utils2D.createImage(30, 15);
		Graphics2D g2 = Utils2D.createGraphics(image, true, false);
		g2.setColor(Color.RED);
		g2.fillRect(0, 0, 30, 15);
		g2.dispose();
		return image;
	}
	
	private BufferedImage createBackgroundImage() {
		BufferedImage image = Utils2D.createImage(getWidth(), getHeight());
		Graphics2D g2 = Utils2D.createGraphics(image, true, false);
		g2.setColor(new Color(100, 100, 100));
		g2.fillRect(0, 0, getWidth(), getHeight());
		g2.setColor(Color.WHITE);
		for (int x = 0; x < getWidth(); x+= 100) {
			for (int y = 0; y < getHeight(); y += 50) {
				g2.fillRect((y % 100 == 0) ? x : x + 50, y, 50, 50);
			}
		}
		g2.dispose();
		return image;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		Graphics2D g2 = Utils2D.createGraphics(g, true, false);
		
		if ((carImage == null) || (backgroundImage == null)) {
			carImage = createCarImage();
			backgroundImage = createBackgroundImage();	
		}
		
		// Update 
		
		physics.setAccelerator(accelerate ? 1f : 0f);
		physics.setBrakes(brake ? 1f : 0f);
		physics.setSteering(steerLeft ? -1f : steerRight ? 1f : 0f);
		physics.setGearChange(0);
		physics.setOnTrack(true);
		physics.updatePhysics(0.04f);
		
		// Paint background
		
		g2.drawImage(backgroundImage, 0, 0, null);
		
		// Paint car
		
		AffineTransform transform = new AffineTransform();
		transform.translate(physics.getPosition().getX(), physics.getPosition().getZ());
		transform.rotate(-physics.getOrientation() + 1.57f, 15, 7.5);
		g2.drawImage(carImage, transform, null);
		
		// Paint HUD
		
		int x = getWidth() - 200;
		
		g2.setColor(PANEL_BACKGROUND);
		g2.fillRoundRect(x, 20, 180, getHeight() - 40, 20, 20);
		
		x += 10;
		
		g2.setColor(PANEL_FOREGROUND);
		g2.drawString("Accelerator: " + physics.getAccelerator(), x, 50);
		g2.drawString("Brakes: " + physics.getBrakes(), x, 70);
		g2.drawString("Steering: " + physics.getSteering(), x, 90);
		g2.drawString("Gear change: " + physics.getGearChange(), x, 110);
		g2.drawString("Speed: " + format(physics.getSpeed()) + " m/s", x, 150);
		g2.drawString("On track: " + physics.isOnTrack(), x, 170);
		g2.drawString("Direction: " + format(physics.getDirection()), x, 210);
		g2.drawString("Orientation: " + format(physics.getOrientation()), x, 230);
		g2.drawString("Angular speed: " + String.format("%.3f", physics.getAngularSpeed()) + " rs", x, 250);
		g2.drawString("Gear: " + physics.getGear(), x, 290);
		g2.drawString("RPM: " + physics.getRPM(), x, 310);
	}
	
	private String format(float n) {
		return String.format("%.2f", n);
	}
	
	public void keyPressed(KeyEvent e) {
		int keycode = e.getKeyCode();
		if (keycode == KeyEvent.VK_UP) { accelerate = true; }
		if (keycode == KeyEvent.VK_DOWN) { brake = true; }
		if (keycode == KeyEvent.VK_LEFT) { steerLeft = true; }
		if (keycode == KeyEvent.VK_RIGHT) { steerRight = true; }
	}

	public void keyReleased(KeyEvent e) {
		int keycode = e.getKeyCode();
		if (keycode == KeyEvent.VK_UP) { accelerate = false; }
		if (keycode == KeyEvent.VK_DOWN) { brake = false; }
		if (keycode == KeyEvent.VK_LEFT) { steerLeft = false; }
		if (keycode == KeyEvent.VK_RIGHT) { steerRight = false; }
	}

	public void keyTyped(KeyEvent e) {
		
	}

	public void animate(Animation anim) {
		repaint();
	}
}

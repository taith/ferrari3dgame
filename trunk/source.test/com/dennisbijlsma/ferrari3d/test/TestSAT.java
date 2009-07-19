package com.dennisbijlsma.ferrari3d.test;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.swing.*;

/**
 * Test the TV graphics ('SAT'). All the different screens can be observed.
 */

public class TestSAT extends JPanel implements KeyListener {
	
	private SAT sat;
	private BufferedImage satImage;
	private int screen;

	
	
	public static void main(String[] args) throws Exception {
		
		Settings.getInstance().init();
		
		TestSAT panel = new TestSAT();
	
		JFrame f = new JFrame();
		f.setTitle("Ferrari3D | Test SAT");
		f.setLayout(new BorderLayout());
		f.setSize(800, 600);
		f.add(panel, BorderLayout.CENTER);
		f.setVisible(true);
		
		f.addKeyListener(panel);
		panel.addKeyListener(panel);
	}
	
	
	
	public TestSAT() {
	
		super(null);
		
		setOpaque(true);
		setBackground(Color.BLACK);
		
		sat = new SAT();
		satImage = new BufferedImage(800, 128, BufferedImage.TYPE_INT_ARGB);
		screen = 0;
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		Color color1 = new Color(0, 0, 100);
		Color color2 = new Color(0, 0, 255);
		g2.setPaint(new GradientPaint(0, 0, color1, 0, getHeight(), color2));
		g2.fillRect(0, 0, getWidth(), getHeight());
				
		if (screen == 0) {
			return;
		}
				
		String driver1 = "Kees Kist";
		String driver2 = "Alwin Distelbrink";
		String car1 = "Ferrari";
		String car2 = "Ferrari";
		LapTime time1 = getRandomLaptime();
		LapTime time2 = getRandomLaptime();
		
		Utils2D.clearGraphics(satImage.createGraphics(), 0, 0, 800, 128);
		sat.setSurface(satImage.createGraphics(), 0, 0, 800, 128);
		
		switch (screen) {
			case 1 : sat.paintDefaultScreen(driver1,car1); break;		
			case 2 : sat.paintTimeScreen(driver1,time1); break;		
			case 3 : sat.paintTimeDiffScreen(driver1,time1,driver2,time2,(int) Math.round(Math.random()*2),false,1); break;
			case 4 : sat.paintTimeDiffScreen(driver1,time1,driver2,time2,(int) Math.round(Math.random()*2),true,2); break;		
			case 5 : sat.paintPosDiffScreen(driver1,1,driver2,2,1234); break;
			case 6 : sat.paintStandingsScreen(getRandomNames(10),getRandomTimes(10)); break;
			case 7 : sat.paintFastestLapScreen(driver1,car1,time1); break;
			default : break;
		}
		
		g2.drawImage(satImage, 0, getHeight() - 128, 800, 128, null);
		g2.setColor(new Color(255, 0, 0, 128));
		g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
	}
	
	
	
	public void keyReleased(KeyEvent e) {
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_1 : setScreen(1); break;
			case KeyEvent.VK_2 : setScreen(2); break;
			case KeyEvent.VK_3 : setScreen(3); break;
			case KeyEvent.VK_4 : setScreen(4); break;
			case KeyEvent.VK_5 : setScreen(5); break;
			case KeyEvent.VK_6 : setScreen(6); break;
			case KeyEvent.VK_7 : setScreen(7); break;
			default : break;
		}
	}
	
	
	
	public void keyPressed(KeyEvent e) { }
	
	public void keyTyped(KeyEvent e) { }
	
	
	
	private void setScreen(int s) {
		screen = s;
		repaint();
	}
	
	
	
	private LapTime getRandomLaptime() {
		
		int sector1 = (int) Math.round(Math.random() * 20000 + 10000);
		int sector2 = (int) Math.round(Math.random() * 20000 + 10000);
		int sector3 = (int) Math.round(Math.random() * 20000 + 10000);

		int intermediate = (int) Math.round(Math.random() * 2);
		if (intermediate < 1) { sector2 = LapTime.TIME_NOT_SET; sector3 = LapTime.TIME_NOT_SET; }
		if (intermediate < 2) { sector3 = LapTime.TIME_NOT_SET; }
		
		LapTime laptime = new LapTime();
		laptime.setCompleted(true);
		if (intermediate >= 0) { laptime.setSectorTime(0, sector1, true); }
		if (intermediate >= 1) { laptime.setSectorTime(1, sector2, true); }
		if (intermediate >= 2) { laptime.setSectorTime(2, sector3, true); }
		
		return laptime;
	}
	
	
	
	private String[] getRandomNames(int n) {
	
		String[] names = new String[n];
		for (int i=0; i<names.length; i++) {
			names[i] = "Kees_" + (i + 1);
		}
		
		return names;
	}
	
	
	
	private LapTime[] getRandomTimes(int n) {
	
		LapTime[] times = new LapTime[n];
		for (int i=0; i<times.length; i++) {
			times[i] = getRandomLaptime();
		}
		
		return times;
	}
}
package com.dennisbijlsma.ferrari3d.test;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.graphics.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.swing.*;



public class TestSAT extends JPanel implements KeyListener {
	
	private SAT sat;
	private BufferedImage satImage;
	private int screen;

	
	
	public static void main(String[] args) throws Exception {
		
		Settings.getInstance().init();
		
		TestSAT panel=new TestSAT();
	
		JFrame f=new JFrame();
		f.setTitle("Ferrari3D | Test SAT");
		f.setLayout(new BorderLayout());
		f.setSize(800,600);
		f.add(panel,BorderLayout.CENTER);
		f.setVisible(true);
		
		f.addKeyListener(panel);
		panel.addKeyListener(panel);
	}
	
	
	
	public TestSAT() {
	
		super(null);
		
		setOpaque(true);
		setBackground(Color.BLACK);
		
		sat=new SAT();
		satImage=new BufferedImage(800,128,BufferedImage.TYPE_INT_ARGB);
		screen=0;
	}
	
	
	
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
		
		Graphics2D g2=(Graphics2D) g;
		
		Color color1=new Color(0,0,100);
		Color color2=new Color(0,0,255);
		g2.setPaint(new GradientPaint(0,0,color1,0,getHeight(),color2));
		g2.fillRect(0,0,getWidth(),getHeight());
				
		if (screen==0) {
			return;
		}
				
		String driver1="Kees Kist";
		String driver2="Alwin Distelbrink";
		String car1="Ferrari";
		String car2="Ferrari";
		Laptime time1=getRandomLaptime();
		Laptime time2=getRandomLaptime();
		
		Utils2D.clearGraphics(satImage.createGraphics(),0,0,800,128);
		sat.setSurface(satImage.createGraphics(),0,0,800,128);
		
		if (screen==1) { sat.paintDefaultScreen(driver1,car1); }		
		if (screen==2) { sat.paintTimeScreen(driver1,time1); }		
		if (screen==3) { sat.paintTimeDiffScreen(driver1,time1,driver2,time2,(int) Math.round(Math.random()*2),false,1); }
		if (screen==4) { sat.paintTimeDiffScreen(driver1,time1,driver2,time2,(int) Math.round(Math.random()*2),true,2); }		
		if (screen==5) { sat.paintPosDiffScreen(driver1,1,driver2,2,1234); }
		if (screen==6) { sat.paintStandingsScreen(getRandomNames(10),getRandomTimes(10)); }
		if (screen==7) { sat.paintFastestLapScreen(driver1,car1,time1); }
		
		g2.drawImage(satImage,0,getHeight()-128,800,128,null);
		
		g2.setColor(new Color(255,0,0,128));
		g2.drawLine(getWidth()/2,0,getWidth()/2,getHeight());
	}
	
	
	
	public void keyReleased(KeyEvent e) {
		
		if (e.getKeyCode()==KeyEvent.VK_1) { setScreen(1); }
		if (e.getKeyCode()==KeyEvent.VK_2) { setScreen(2); }
		if (e.getKeyCode()==KeyEvent.VK_3) { setScreen(3); }
		if (e.getKeyCode()==KeyEvent.VK_4) { setScreen(4); }
		if (e.getKeyCode()==KeyEvent.VK_5) { setScreen(5); }
		if (e.getKeyCode()==KeyEvent.VK_6) { setScreen(6); }
		if (e.getKeyCode()==KeyEvent.VK_7) { setScreen(7); }
	}
	
	
	
	public void keyPressed(KeyEvent e) { }
	public void keyTyped(KeyEvent e) { }
	
	
	
	private void setScreen(int s) {
	
		screen=s;
		repaint();
	}
	
	
	
	private Laptime getRandomLaptime() {
		
		int sector1=(int) Math.round(Math.random()*20000+10000);
		int sector2=(int) Math.round(Math.random()*20000+10000);
		int sector3=(int) Math.round(Math.random()*20000+10000);

		int intermediate=(int) Math.round(Math.random()*2);
		if (intermediate<1) { sector2=Laptime.TIME_NOT_SET; sector3=Laptime.TIME_NOT_SET; }
		if (intermediate<2) { sector3=Laptime.TIME_NOT_SET; }
		
		Laptime laptime=new Laptime();
		laptime.setComplete(true);
		if (intermediate>=0) { laptime.setSector(Laptime.SECTOR_1,sector1,false); }
		if (intermediate>=1) { laptime.setSector(Laptime.SECTOR_2,sector2,false); }
		if (intermediate>=2) { laptime.setSector(Laptime.SECTOR_3,sector3,false); }
		
		return laptime;
	}
	
	
	
	private String[] getRandomNames(int n) {
	
		String[] names=new String[n];
		for (int i=0; i<names.length; i++) {
			names[i]="Kees_"+(i+1);
		}
		
		return names;
	}
	
	
	
	private Laptime[] getRandomTimes(int n) {
	
		Laptime[] times=new Laptime[n];
		for (int i=0; i<times.length; i++) {
			times[i]=getRandomLaptime();
		}
		
		return times;
	}
}
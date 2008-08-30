//--------------------------------------------------------------------------------
// Ferrari3D
// Splashscreen
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.util.*;



public class Splashscreen extends JPanel {
	
	private JFrame frame;
	private JProgressBar progressbar;
	private Image logoImage;
	
	private String message;
	private int progress;
	
	private static final int WINDOW_WIDTH=500;
	private static final int WINDOW_HEIGHT=200;
	private static final Color BACKGROUND_COLOR_1=new Color(0,100,255);
	private static final Color BACKGROUND_COLOR_2=new Color(0,100,200);
	private static final Color BORDER_COLOR=new Color(255,255,255);
	private static final Color MESSAGE_COLOR=new Color(255,255,255);
	private static final Font MESSAGE_FONT=new Font("Verdana",Font.BOLD,12);

	
	
	public Splashscreen() {
	
		super(null);
		
		logoImage=Utils.loadImage("data/graphics/logo.png");
		
		message="";
		progress=0;
	}
	
	
	
	public void paintComponent(Graphics g) {
	
		super.paintComponent(g);
		
		Graphics2D g2=(Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		// Paint background
		
		g2.setPaint(new GradientPaint(0,0,BACKGROUND_COLOR_1,0,getHeight(),BACKGROUND_COLOR_2));
		g2.fillRect(0,0,getWidth(),getHeight());
		g2.drawImage(logoImage,getWidth()/2-logoImage.getWidth(null)/2,20,null);
		g2.setColor(BORDER_COLOR);
		g2.drawRect(0,0,getWidth()-1,getHeight()-1);
		
		// Paint message
		
		g2.setFont(MESSAGE_FONT);
		g2.setColor(MESSAGE_COLOR);
		g2.drawString(message,20,getHeight()-20);
	}
	
	
	
	public void showSplashScreen() {
		
		Dimension screensize=Toolkit.getDefaultToolkit().getScreenSize();
		
		progressbar=new JProgressBar(0,100);
		
		frame=new JFrame();
		frame.setLayout(new BorderLayout());
		frame.setSize(WINDOW_WIDTH,WINDOW_HEIGHT);
		frame.setLocation(screensize.width/2-WINDOW_WIDTH/2,screensize.height/2-WINDOW_HEIGHT/2);
		frame.setResizable(false);
		frame.setUndecorated(true);
		frame.setAlwaysOnTop(true);
		frame.add(this,BorderLayout.CENTER);
		frame.add(progressbar,BorderLayout.SOUTH);
		frame.setVisible(true);
	}
	
	
	
	public void hideSplashScreen() {
		
		frame.dispose();
	}
	
	
	
	public void setMessage(String message) {
		
		this.message=message;
		repaint();
	}
	
	
	
	public void setMessage(String message,int progress) {
	
		setProgress(progress);
		setMessage(message);		
	}
	
	
	
	public String getMessage() {
		
		return message;
	}
	
	
	
	public void setProgress(int progress) {
		
		if (progress<0) { progress=0; }
		if (progress>100) { progress=100; }
	
		this.progress=progress;
		progressbar.setValue(progress);
	}
	
	
	
	public int getProgress() {
	
		return progress;
	}
}
//--------------------------------------------------------------------------------
// Ferrari3D
// Splashscreen
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;

import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.SplashScreen;



public class Splashscreen extends SplashScreen {
	
	private Image logoImage;
	
	private static final int WINDOW_WIDTH=500;
	private static final int WINDOW_HEIGHT=200;
	private static final Color BACKGROUND_COLOR_1=new Color(0,100,255);
	private static final Color BACKGROUND_COLOR_2=new Color(0,100,200);
	private static final Color BORDER_COLOR=new Color(255,255,255);
	private static final Color MESSAGE_COLOR=new Color(255,255,255);
	private static final Font MESSAGE_FONT=new Font("Verdana",Font.BOLD,12);

	
	
	public Splashscreen() {
	
		super(WINDOW_WIDTH,WINDOW_HEIGHT);
		
		logoImage=Utils.loadImage("data/graphics/logo.png");
	}
	
	
	
	@Override
	public void paintComponent(Graphics g) {
	
		super.paintComponent(g);
		
		Graphics2D g2=(Graphics2D) g;
		
		// Paint background
		
		g2.setPaint(new GradientPaint(0,0,BACKGROUND_COLOR_1,0,getHeight(),BACKGROUND_COLOR_2));
		g2.fillRect(0,0,getWidth(),getHeight());
		g2.drawImage(logoImage,getWidth()/2-logoImage.getWidth(null)/2,20,null);
		g2.setColor(BORDER_COLOR);
		g2.drawRect(0,0,getWidth()-1,getHeight()-1);
		
		// Paint message
		
		g2.setFont(MESSAGE_FONT);
		g2.setColor(MESSAGE_COLOR);
		g2.drawString(getMessage(),20,getHeight()-20);
	}
	
	
	
	public void setMessage(String message,int progress) {
	
		setProgress(progress);
		setMessage(message);		
	}
}
//--------------------------------------------------------------------------------
// Ferrari3D
// MenuWidget
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.swing.JPanel;



public abstract class MenuWidget extends JPanel {
	
	public static final Font FONT=new Font("Verdana",Font.BOLD,12);
	public static final Font SMALL_FONT=new Font("Verdana",Font.PLAIN,10);
	public static final Font TITLE_FONT=new Font("Verdana",Font.BOLD,24);
	public static final Color FONT_COLOR=new Color(255,255,255);
	public static final Color BACKGROUND_COLOR_TOP=new Color(100,150,255);
	public static final Color BACKGROUND_COLOR_CENTER=new Color(0,100,255);
	public static final Color BACKGROUND_COLOR_BOTTOM=new Color(0,50,150);
	public static final Color SELECTED_COLOR=new Color(255,255,0);
	public static final Color DISABLED_BACKGROUND_COLOR=new Color(150,150,150);
	public static final Color DISABLED_FOREGROUND_COLOR=new Color(200,200,200);
	public static final Color SHORTKEY_COLOR=new Color(255,255,100);
	public static final Color STROKE_COLOR=new Color(255,255,255);
	public static final Stroke STROKE=new BasicStroke(1f);
	public static final int LINE_HEIGHT=25;
	public static final int LINE_HEIGHT_SMALL=20;
	public static final int BASELINE=8;
	public static final int ARC=10;
	public static final int GLOW=100;
		
	
	
	public MenuWidget() {
		
		super(null);
		
		setOpaque(false);
		setFocusable(false);
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {

		super.paintComponent(g);
		
		Graphics2D g2=(Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
		paintWidget(g2);
	}
	
	
	
	public abstract void paintWidget(Graphics2D g2);
	
	
	
	public abstract Dimension getWidgetSize();
	
	
	
	public Component getRootComponent() {
	
		return getTopLevelAncestor();
	}
	
	
	
	protected void paintBackgroundGradient(Graphics2D g2,int x,int y,int w,int h) {
		
		if (!isEnabled()) {
			g2.setColor(DISABLED_BACKGROUND_COLOR);
			g2.fillRoundRect(x,y,w-1,h-1,ARC,ARC);
			return;
		}
				
		// Java 6 dual-stop gradient
		
		float[] fractals={0f,0.5f,1f};
		Color[] colors={BACKGROUND_COLOR_TOP,BACKGROUND_COLOR_CENTER,BACKGROUND_COLOR_BOTTOM};
		//TODO g2.setPaint(new LinearGradientPaint(0,0,0,h,fractals,colors));
		g2.setPaint(new GradientPaint(0,0,BACKGROUND_COLOR_TOP,0,h,BACKGROUND_COLOR_BOTTOM));
		g2.fillRoundRect(x,y,w-1,h-1,ARC,ARC);
	}
	
	
	
	protected void paintAnimatedBackground(Graphics2D g2,int x,int y,int w,int h,float delta) {
	
		if (!isEnabled()) {
			paintBackgroundGradient(g2,x,y,w,h);
			return;
		}
		
		// Java 6 dual-stop gradient
		
		//TODO
		/*g2.setPaint(new LinearGradientPaint(0,0,0,h,new float[]{0f,0.5f,1f},new Color[]{interpolateColor(
				BACKGROUND_COLOR_TOP,GLOW,delta),interpolateColor(BACKGROUND_COLOR_CENTER,GLOW,delta),
				interpolateColor(BACKGROUND_COLOR_BOTTOM,GLOW,delta)}));*/
		g2.setPaint(new GradientPaint(0,0,interpolateColor(BACKGROUND_COLOR_TOP,GLOW,delta),0,h,
				interpolateColor(BACKGROUND_COLOR_BOTTOM,GLOW,delta)));
		g2.fillRoundRect(x,y,w-1,h-1,ARC,ARC);
	}
	
	
	
	private Color interpolateColor(Color source,int amount,float delta) {
		
		delta=(delta>0.5f) ? 1f-delta : delta;
		amount=Math.round(amount*delta);
		int r=Math.min(source.getRed()+amount,255);
		int g=Math.min(source.getGreen()+amount,255);
		int b=Math.min(source.getBlue()+amount,255);
		
		return new Color(r,g,b);
	}
}
//--------------------------------------------------------------------------------
// Ferrari3D
// UIMenuWidget
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.util.swing.Utils2D;

/**
 * Base class for all menu widgets. All painting is done using Java 2D, and the
 * resulting images are then used as textures.
 */

public abstract class UIMenuWidget {

	private UIWidget widget;
	private BufferedImage image;
	private BufferedImage overImage;
	private BufferedImage downImage;
	private BufferedImage disabledImage;
	
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
	
	/**
	 * Creates a new {@code UIMenuWidget} with the specified dimensions.
	 */
	
	public UIMenuWidget(int width,int height,boolean mouseEnabled) {
		
		if (mouseEnabled) {
			image=new BufferedImage(width,height,BufferedImage.TYPE_4BYTE_ABGR);
			overImage=new BufferedImage(width,height,BufferedImage.TYPE_4BYTE_ABGR);
			downImage=new BufferedImage(width,height,BufferedImage.TYPE_4BYTE_ABGR);
			disabledImage=new BufferedImage(width,height,BufferedImage.TYPE_4BYTE_ABGR);
			
			widget=new UIWidget(0,0,width,height,image,overImage,downImage,disabledImage);
		} else {
			image=new BufferedImage(width,height,BufferedImage.TYPE_4BYTE_ABGR);
			
			widget=new UIWidget(0,0,width,height,image);
		}
		
		widget.setHitArea(getHitArea());
		widget.setMouseEnabled(mouseEnabled);
	}
	
	/**
	 * Returns the {@code UIWidget} that is used to display this widget.<br><br>
	 * If a subclass contains multiple widgets, this method should be overridden
	 * to return a list of all widgets.
	 */
	
	public List<UIWidget> getWidgets() {
		
		return Arrays.asList(widget);
	}
	
	/**
	 * Returns the single value from {@link #getWidgets()}.
	 */
	
	UIWidget getSingleWidget() {
	
		return widget;
	}
	
	/**
	 * Repaints all images by calling the {@code paintXXX} methods. This method
	 * should be called from subclasses at least once.
	 */
	
	public void repaintImages() {
		
		paintImage(image);
		
		if (widget.isMouseEnabled()) {
			paintOverImage(overImage);
			paintDownImage(downImage);
			paintDisabledImage(disabledImage);
		}
		
		widget.forceRepaint();
	}
	
	/**
	 * Paints the default image. 
	 */
	
	protected abstract void paintImage(BufferedImage image);
	
	/**
	 * Paints the image for when the mouse is over this widget. 
	 */
	
	protected void paintOverImage(BufferedImage overImage) {
	
	}
	
	/**
	 * Paints the image for when the mouse is down over this widget.
	 */
	
	protected void paintDownImage(BufferedImage downImage) {
		
	}
	
	/**
	 * Paints the image for when this widget is disabled.
	 */
	
	protected void paintDisabledImage(BufferedImage disabledImage) {
		
	}
	
	/**
	 * Returns the graphics context for the specified image. This method should
	 * be called before painting one of the different images. By default all
	 * painting will be antialiased.
	 */
	
	public Graphics2D getGraphics(BufferedImage image) {
		
		return Utils2D.createGraphics(image,true,false);
	}
	
	/**
	 * Clears the graphics for the specified image. All pixels in the image will
	 * be set to the RGBA value of (0,0,0,0).
	 */
	
	public void clearGraphics(BufferedImage image,Graphics2D g2) {
		
		Utils2D.clearGraphics(g2,0,0,image.getWidth(),image.getHeight());
	}
	
	/**
	 * Paints a background gradient to the specified rectangle. An outline will 
	 * also be painted around the gradient.
	 */
	
	public void paintBackgroundGradient(Graphics2D g2,int x,int y,int width,int height) {
		
		float[] fractals={0f,0.5f,1f};
		Color[] colors={BACKGROUND_COLOR_TOP,BACKGROUND_COLOR_CENTER,BACKGROUND_COLOR_BOTTOM};
		g2.setPaint(new LinearGradientPaint(0,0,0,height,fractals,colors));
		g2.fillRoundRect(x,y,width-1,height-1,ARC,ARC);
	}
	
	/**
	 * Paints a background stroke using the currently set color.
	 */
	
	public void paintBackgroundStroke(Graphics2D g2,int x,int y,int width,int height) {
		
		g2.setStroke(STROKE);
		g2.drawRoundRect(x,y,width-1,height-1,ARC,ARC);
	}
	
	/**
	 * Paints a disabled background gradient. This method should be used instead
	 * of {@link #paintBackgroundGradient(Graphics2D,int,int,int,int)} for disabled
	 * widgets.
	 */
	
	public void paintDisabledGradient(Graphics2D g2,int x,int y,int width,int height) {
		
		g2.setColor(DISABLED_BACKGROUND_COLOR);
		g2.fillRoundRect(x,y,width-1,height-1,ARC,ARC);
		
		g2.setColor(DISABLED_FOREGROUND_COLOR);
		g2.setStroke(STROKE);
		g2.drawRoundRect(x,y,width-1,height-1,ARC,ARC);
	}
	
	/**
	 * Paints text with the specified alignment. The supplied alignment should be
	 * either 'l', 'r' or 'c'.
	 */
	
	public void paintText(Graphics2D g2,String text,int x,int y,char align) {
	
		Utils2D.drawAlignedString(g2,text,x,y,align);
	}
	
	/**
	 * Returns the hit area for this widget. Mouse events will only be accepted
	 * within these bounds.
	 */
	
	public abstract Dimension getHitArea();
	
	/**
	 * Sets the position of this widget to the specified coordinates. This is
	 * convenience version of {@link UIWidget#setPosition(int, int)}.
	 */
	
	public void setPosition(int x,int y) {
	
		getSingleWidget().setPosition(x,y);
	}
}
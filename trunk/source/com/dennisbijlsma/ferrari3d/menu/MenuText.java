//--------------------------------------------------------------------------------
// Ferrari3D
// MenuText
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;

import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.ScalableCanvas;

/**
 * Base component for displaying text and image information. This class uses <code>
 * ScalableCanvas</code> to layout its display contents, which means that it can
 * be resized.
 */

@Deprecated
public class MenuText extends MenuWidget {

	private ScalableCanvas canvas;
	private int width;
	private int height;
	
	/**
	 * Creates a new component with the specified width and height. An image 
	 * buffer is created to store the contents.
	 */
	
	public MenuText(int width,int height) {
	
		super();
		
		this.width=width;
		this.height=height;
		
		canvas=new ScalableCanvas();
	}
	
	/**
	 * Creates a new component with a default width and height.   
	 */
	
	public MenuText() {
		
		this(100,LINE_HEIGHT);
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public void paintWidget(Graphics2D g2) {
	
		g2.setFont(FONT);
		g2.setColor(FONT_COLOR);
		
		canvas.paintContents(g2,getWidth(),getHeight());
	}

	/**
	 * {@inheritDoc}
	 */
	
	@Override
	public Dimension getWidgetSize() {
	 
		return new Dimension(getWidth(),height);
	}
	
	/**
	 * Paints the specified text to the image. The text is painted at the first
	 * available position.
	 */
	
	public void paintText(String text) {
		
		paintTexts(new String[]{text});
	}
	
	/**
	 * Paints the specified texts to the image. The texts are painted with line
	 * breaks in between, and start at the first available position.
	 */
	
	public void paintTexts(String[] texts) {
		
		for (int i=0; i<texts.length; i++) {
			canvas.addText(texts[i],true,0,(i+1)*LINE_HEIGHT_SMALL,'l');
		}
		
		repaint();
	}
	
	/**
	 * Paints a text at the specified coordinates. The text can be aligned left,
	 * right or centered.
	 */
	
	public void paintText(String text,int x,int y,char align) {
		
		canvas.addText(text,true,x,y,align);
		repaint();
	}
	
	/**
	 * Paints a text at the specified relative coordinates. The text can also
	 * be aligned when needed.
	 */
	
	public void paintTextRelative(String text,float x,float y,char align) {
	
		canvas.addText(text,false,x,y,align);
		repaint();
	}
	
	/**
	 * Paints an image at the specified coordinates. The image is painted at its
	 * full size.
	 */
	
	public void paintImage(Image image,int x,int y) {
		
		canvas.addImage(image,true,x,y,'l');
		repaint();
	}
	
	/**
	 * Paints an image at relative coordinates. The image will be aligned from
	 * its top left corner.
	 */
	
	public void paintImageRelative(Image image,float x,float y,char align) {
	
		canvas.addImage(image,false,x,y,align);
		repaint();
	}
	
	/**
	 * Loads an paints an image at the specified coordinates. This method is a
	 * convenience version of <code>paintImage(Image,int,int)</code>.   
	 */
	
	public void paintImage(String location,int x,int y) {
		
		paintImage(Utils.loadImage(location),x,y);
	}
}
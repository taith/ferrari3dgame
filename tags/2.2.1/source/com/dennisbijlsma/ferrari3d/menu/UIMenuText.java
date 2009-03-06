//--------------------------------------------------------------------------------
// Ferrari3D
// UIMenuText
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.ScalableCanvas;

/**
 * Base component for displaying text and image information. This class uses a
 * scalable canvas to layout its display contents, which means that it can be 
 * resized.
 */

public class UIMenuText extends UIMenuWidget {

	private ScalableCanvas canvas;
	private int width;
	private int height;
	
	/**
	 * Creates a new {@code UIMenuText} with the specified dimensions.
	 */
	
	public UIMenuText(int width,int height) {
		
		super(width,height,false);
		
		this.width=width;
		this.height=height;
		canvas=new ScalableCanvas();
		
		repaintImages();
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void paintImage(BufferedImage image) {
		
		Graphics2D g2=getGraphics(image);
		g2.setFont(FONT);
		g2.setColor(FONT_COLOR);
		canvas.paintContents(g2,width,height);
		g2.dispose();
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
		
		repaintImages();
	}
	
	/**
	 * Paints a text at the specified coordinates. The text can be aligned left,
	 * right or centered.
	 */
	
	public void paintText(String text,int x,int y,char align) {
		
		canvas.addText(text,true,x,y,align);
		repaintImages();
	}
	
	/**
	 * Paints a text at the specified relative coordinates. The text can also
	 * be aligned when needed.
	 */
	
	public void paintTextRelative(String text,float x,float y,char align) {
	
		canvas.addText(text,false,x,y,align);
		repaintImages();
	}
	
	/**
	 * Paints an image at the specified coordinates. The image is painted at its
	 * full size.
	 */
	
	public void paintImage(Image image,int x,int y) {
		
		canvas.addImage(image,true,x,y,'l');
		repaintImages();
	}
	
	/**
	 * Paints an image at relative coordinates. The image will be aligned from
	 * its top left corner.
	 */
	
	public void paintImageRelative(Image image,float x,float y,char align) {
	
		canvas.addImage(image,false,x,y,align);
		repaintImages();
	}
	
	/**
	 * Loads an paints an image at the specified coordinates. This method is a
	 * convenience version of <code>paintImage(Image,int,int)</code>.   
	 */
	
	public void paintImage(String location,int x,int y) {
		
		paintImage(Utils.loadImage(location),x,y);
	}
	
	/**
	 * {@inheritDoc}
	 */

	@Override
	public Dimension getHitArea() {
		
		return new Dimension(width,height);
	}
}
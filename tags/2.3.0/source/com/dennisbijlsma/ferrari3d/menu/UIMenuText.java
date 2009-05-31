//--------------------------------------------------------------------------------
// Ferrari3D
// UIMenuText
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;

import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.ScalableCanvas;

/**
 * Base component for displaying text and image information. This class uses a
 * scalable canvas to layout its display contents, which means that it can be 
 * resized.
 */

public class UIMenuText extends UIMenuWidget {

	private ScalableCanvas canvas;
	private int height;
	
	private Font font;
	private Color color;
	
	/**
	 * Creates a new {@code UIMenuText} with the specified dimensions.
	 */
	
	public UIMenuText(int height) {
		
		super(WIDGET_TEXTURE_WIDTH, height < WIDGET_TEXTURE_HEIGHT ? WIDGET_TEXTURE_HEIGHT 
				: height,false);
		
		this.height = height;
		canvas = new ScalableCanvas();
		font = FONT;
		color = FONT_COLOR;
		
		repaint();
	}
	
	/**
	 * Creates a new {@code UIMenuText} with default dimensions.
	 */
	
	public UIMenuText() {
		this(LINE_HEIGHT);
	}
	
	/**
	 * Creates a new {@code UIMenuText with default dimensions and an intial text.
	 */
	
	public UIMenuText(String text) {
		this();
		paintText(text);
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void paintImage(Graphics2D g2) {
		
		g2.setFont(font);
		g2.setColor(color);
		canvas.paintContents(g2, WIDGET_WIDTH, height);
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
	
	private void paintTexts(String... texts) {
		
		for (int i=0; i<texts.length; i++) {
			canvas.addText(texts[i], true, 0, (i + 1) * LINE_HEIGHT_SMALL, 'l');
		}
		
		repaint();
	}
	
	/**
	 * Paints a text at the specified coordinates. The text can be aligned left,
	 * right or centered.
	 */
	
	public void paintText(String text, int x, int y, char align) {
		canvas.addText(text, true, x, y, align);
		repaint();
	}
	
	/**
	 * Paints a text at the specified relative coordinates. The text can also
	 * be aligned when needed.
	 */
	
	public void paintTextRelative(String text, float x, float y, char align) {
		canvas.addText(text, false, x, y, align);
		repaint();
	}
	
	/**
	 * Paints an image at the specified coordinates. The image is painted at its
	 * full size.
	 */
	
	public void paintImage(Image image, int x, int y) {
		canvas.addImage(image, true, x, y, 'l');
		repaint();
	}
	
	/**
	 * Paints an image at relative coordinates. The image will be aligned from
	 * its top left corner.
	 */
	
	public void paintImageRelative(Image image, float x, float y, char align) {
		canvas.addImage(image, false, x, y, align);
		repaint();
	}
	
	/**
	 * Loads an paints an image at the specified coordinates. This method is a
	 * convenience version of <code>paintImage(Image, int, int)</code>.   
	 */
	
	public void paintImage(String location, int x, int y) {
		paintImage(Utils.loadImage(location), x, y);
	}
	
	/**
	 * Sets the font that is used to paint the texts.
	 */
	
	public void setFont(Font font) {
		this.font = font;
	}
	
	/**
	 * Returns the font that is used to paint the texts.
	 */
	
	public Font getFont() {
		return font;
	}
	
	/**
	 * Sets the color that is used to paint the texts.
	 */
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	/**
	 * Returns the color that is used to paint the texts.
	 */
	
	public Color getColor() {
		return color;
	}
}
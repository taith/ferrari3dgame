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
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.core3d.ui.WidgetPainter;
import com.dennisbijlsma.util.swing.Utils2D;

/**
 * Base class for all menu widgets. All painting is done using Java 2D, and the
 * resulting images are then used as textures.
 */

public abstract class UIMenuWidget implements WidgetPainter {

	private UIWidget widget;
	
	public static final Font FONT = new Font("Verdana", Font.BOLD, 12);
	public static final Font SMALL_FONT = new Font("Verdana", Font.BOLD, 10);
	public static final Font TITLE_FONT = new Font("Verdana", Font.BOLD, 24);
	public static final Color FONT_COLOR = new Color(255, 255, 255);
	public static final Color HEADER_COLOR = new Color(255, 255, 0);
	public static final Color BACKGROUND_COLOR_TOP = new Color(100, 150, 255);
	public static final Color BACKGROUND_COLOR_CENTER = new Color(0, 100, 255);
	public static final Color BACKGROUND_COLOR_BOTTOM = new Color(0, 50, 150);
	public static final Color SELECTED_COLOR = new Color(255, 255, 0);
	public static final Color DISABLED_BACKGROUND_COLOR = new Color(150, 150, 150);
	public static final Color DISABLED_FOREGROUND_COLOR = new Color(200, 200, 200);
	public static final Color SHORTKEY_COLOR = new Color(255, 255, 100);
	public static final Color STROKE_COLOR = new Color(255, 255, 255);
	public static final Color SHADOW_COLOR = new Color(0, 0, 0, 128);
	public static final Color PANEL_COLOR = new Color(0, 0, 0, 128);
	public static final Stroke STROKE = new BasicStroke(1f);
	public static final int WIDGET_WIDTH = 512;
	public static final int LINE_HEIGHT = 25;
	public static final int LINE_HEIGHT_SMALL = 20;
	public static final int WIDGET_TEXTURE_WIDTH = 512;
	public static final int WIDGET_TEXTURE_HEIGHT = 32;
	public static final int BASELINE = 8;
	public static final int ARC = 10;
	public static final int GLOW = 100;
	
	/**
	 * Creates a new {@code UIMenuWidget} with the specified dimensions.
	 * @param width The texture width of the widget in pixels.
	 * @param height The texture height of the widget in pixels.
	 * @param mouseEnabled If true, this widget will receive mouse events.
	 */
	
	public UIMenuWidget(int width, int height, boolean mouseEnabled) {		
		widget = new UIWidget(0, 0, width, height);
		widget.setHitArea(getHitArea());
		widget.setMouseEnabled(mouseEnabled);
	}
	
	/**
	 * Returns the {@code UIWidget} that is used to display this widget.
	 */
	
	public UIWidget getWidget() {
		return widget;
	}
	
	/**
	 * Returns any additional widgets used by this one. This method returns an
	 * empty list by default, by can be overridden by subclasses.
	 */
	
	public List<UIMenuWidget> getAdditionalWidgets() {
		return new ArrayList<UIMenuWidget>();
	}
	
	/** {@inheritDoc} */
	
	public void paintWidget(UIWidget widget, BufferedImage image) {
		Graphics2D g2 = Utils2D.createGraphics(image, true, false);
		Utils2D.clearGraphics(g2, 0, 0, image.getWidth(), image.getHeight());
		paintImage(g2);
		g2.dispose();
	}
	
	/**
	 * Repaints this widget's texture images.
	 */
	
	public void repaint() {
		// This method does not direcly call UIWidget.repaint() because subclasses
		// might want to call repaint() at the end of their constructor. By not
		// calling widget.setPainter(this) in the constructor of this class an
		// unneeded paint is avoided.
		widget.setPainter(this);
	}
	
	/**
	 * Paints this widget. Before this method is called the widget's graphics
	 * are cleared.
	 */
	
	protected abstract void paintImage(Graphics2D g2);
	
	/**
	 * Paints a background gradient to the specified rectangle. An outline will 
	 * also be painted around the gradient.
	 */
	
	protected void paintBackgroundGradient(Graphics2D g2, int x, int y, int width, int height) {
		
		float[] fractals = {0f, 0.5f, 1f};
		Color[] colors = {BACKGROUND_COLOR_TOP, BACKGROUND_COLOR_CENTER, BACKGROUND_COLOR_BOTTOM};
		g2.setPaint(new java.awt.LinearGradientPaint(0, 0, 0, height, fractals, colors));
		/* Java 5 gradient g2.setPaint(new java.awt.GradientPaint(0, 0, BACKGROUND_COLOR_TOP, 
		 * 0, height, BACKGROUND_COLOR_BOTTOM, false)); */
		g2.fillRoundRect(x, y, width - 1, height - 1, ARC, ARC);
	}
	
	/**
	 * Paints a background stroke using the currently set color.
	 */
	
	protected void paintBackgroundStroke(Graphics2D g2, int x, int y, int width, int height) {
		g2.setStroke(STROKE);
		g2.drawRoundRect(x, y, width - 1, height - 1, ARC, ARC);
	}
	
	/**
	 * Paints a disabled background gradient. This method should be used instead
	 * of {@link #paintBackgroundGradient(Graphics2D, int, int, int, int)} for 
	 * disabled widgets.
	 */
	
	protected void paintDisabledGradient(Graphics2D g2, int x, int y, int width, int height) {
		
		g2.setColor(DISABLED_BACKGROUND_COLOR);
		g2.fillRoundRect(x, y, width - 1, height - 1, ARC, ARC);
		
		g2.setColor(DISABLED_FOREGROUND_COLOR);
		g2.setStroke(STROKE);
		g2.drawRoundRect(x, y, width - 1, height - 1, ARC, ARC);
	}
	
	/**
	 * Paints text with the specified alignment. The supplied alignment should be
	 * either 'l', 'r' or 'c'.
	 */
	
	protected void paintText(Graphics2D g2, String text, int x, int y, char align) {
		Utils2D.drawAlignedString(g2, text, x, y, align);
	}
	
	/**
	 * Returns the hit area for this widget. Mouse events will only be accepted
	 * within these bounds. The default implementation returns the widget size.
	 */
	
	public Dimension getHitArea() {
		return new Dimension(widget.getWidth(), widget.getHeight());
	}
	
	/**
	 * Sets the position of this widget to the specified coordinates. This is
	 * convenience version of {@link UIWidget#setPosition(int, int)}.
	 */
	
	public void setPosition(int x, int y) {
		widget.setPosition(x, y);
	}
	
	/**
	 * Sets all subwidgets to be enabled or disabled.
	 */
	
	public void setEnabled(boolean enabled) {
		widget.setEnabled(enabled);
	}
	
	/**
	 * Returns if this widget is currently enabled.
	 */
	
	public boolean isEnabled() {
		return widget.isEnabled();
	}
}
//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuButton
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;

import com.dennisbijlsma.core3d.ui.ActionListener;

/**
 * Simple button widget. This class should be extended to provide an implementation
 * for the {@link #widgetClicked()} method, which is invoked when the button is 
 * clicked.
 */
public class UIMenuButton extends UIMenuWidget {

	private String label;
	private char shortkey;
	private ButtonType type;
	private boolean marked;
	
	public static final int NORMAL_WIDTH = 120;
	public static final int OPTION_WIDTH = 30;
	public static final int EDIT_WIDTH = 70;
	public static final int BUTTON_HEIGHT = 25;
	public static final int TEXTURE_WIDTH = 128;
	public static final int TEXTURE_HEIGHT = 32;
	
	public enum ButtonType {
		NORMAL,
		SMALL,
		OPTION,
		EDIT
	}

	/**
	 * Creates a new button with the specified text label.
	 * @param label The text label shown on the button.
	 * @param type The appearance/skin of the button.
	 */
	public UIMenuButton(String label, ButtonType type) {
	
		super(TEXTURE_WIDTH, TEXTURE_HEIGHT, true);
		
		this.label = label;
		this.type = type;
		marked = false;
		
		Dimension hitarea = getHitArea();
		getWidget().setHitArea(hitarea.width, hitarea.height);
		repaint();
	}
	
	/**
	 * Creates a new button with a type of ButtonType.NORMAL.
	 * @param label The text label shown on the button.
	 */
	public UIMenuButton(String label) {
		this(label, ButtonType.NORMAL);
	}
	
	/**
	 * Creates a new button with the specified text label.
	 * @param label The text label shown on the button.
	 * @param type The appearance/skin of the button.
	 * @param shortkey The key that when typed triggers this button.
	 */
	public UIMenuButton(String label, ButtonType type, char shortkey) {
	
		this(label, type);
		
		this.shortkey = shortkey;
	}
	
	/** {@inheritDoc} */
	@Override
	protected void paintImage(Graphics2D g2) {
		
		if (label.length() == 0) {
			// For buttons without text, don't paint at all
			return;
		}

		boolean over = getWidget().isMouseOver();
		boolean down = getWidget().isMouseDown();
		boolean enabled = getWidget().isEnabled();
		paintButton(g2, over, down, !enabled);
	}
	
	/**
	 * Paints a button to the specified graphics context. Depending on the type of
	 * button this method will call one of the {@code paintXXXButton()} methods.
	 */
	private void paintButton(Graphics2D g2, boolean rollover, boolean down, boolean disabled) {
		
		switch (type) {
			case NORMAL : paintNormalButton(g2, rollover, down, disabled, NORMAL_WIDTH); break;
			case SMALL : paintSmallButton(g2, rollover, down, disabled); break;
			case OPTION : paintNormalButton(g2, rollover, down, disabled, OPTION_WIDTH); break;
			case EDIT : paintNormalButton(g2, rollover, down, disabled, EDIT_WIDTH); break;
			default : throw new IllegalStateException("Invalid button type: " + type);
		}
	}
	
	/**
	 * Paints a button of type ButtonType.NORMAL, ButtonType.OPTION or 
	 * ButtonType.EDIT to the specified graphics context. The button will be 
	 * painted with the specified width.
	 */
	private void paintNormalButton(Graphics2D g2, boolean rollover, boolean down, 
			boolean disabled, int width) {
		
		if (!disabled) {
			paintBackgroundGradient(g2, 0, 0, width, BUTTON_HEIGHT);
			g2.setColor((rollover || down) ? SELECTED_COLOR : FONT_COLOR);
			paintBackgroundStroke(g2, 0, 0, width, BUTTON_HEIGHT);
		} else {
			paintDisabledGradient(g2, 0, 0, width, BUTTON_HEIGHT);
		}
		
		g2.setFont(FONT);
		g2.setColor(FONT_COLOR);
		if (rollover || down) { g2.setColor(SELECTED_COLOR); }
		if (disabled) { g2.setColor(DISABLED_FOREGROUND_COLOR); }
		paintText(g2, label, width / 2, BUTTON_HEIGHT - BASELINE, 'c');
	}
	
	/**
	 * Paints a button of type ButtonType.SMALL to the specified graphics context.
	 */
	private void paintSmallButton(Graphics2D g2, boolean rollover, boolean down,
			boolean disabled) {
		
		g2.setColor(marked ? SELECTED_COLOR : FONT_COLOR);
		g2.setStroke(STROKE);
		g2.drawLine(0, BUTTON_HEIGHT - 1, NORMAL_WIDTH - 1, BUTTON_HEIGHT - 1);
		
		g2.setColor((rollover || down) ? SELECTED_COLOR : FONT_COLOR);
		g2.setFont(SMALL_FONT);
		paintText(g2, label, 0, BUTTON_HEIGHT - BASELINE / 2, 'l');
	}

	@Override
	public Dimension getHitArea() {
		
		if (type == null) {
			return new Dimension(0, 0);
		}
		
		switch (type) {
			case NORMAL : return new Dimension(NORMAL_WIDTH, BUTTON_HEIGHT);
			case SMALL : return new Dimension(NORMAL_WIDTH, BUTTON_HEIGHT);
			case OPTION : return new Dimension(OPTION_WIDTH, BUTTON_HEIGHT);
			case EDIT : return new Dimension(EDIT_WIDTH, BUTTON_HEIGHT);
			default : throw new AssertionError();
		}
	}
	
	/**
	 * Shorthand for {@code getSingleWidget().addActionListener(listener)}.
	 */
	public void addActionListener(ActionListener listener) {
		getWidget().addActionListener(listener);
	}
	
	/**
	 * Returns the text label that is displayed on this button.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * Returns the short key for this button. When typed, this key will have the
	 * same effect as clicking the button.
	 * @return The key code for this button's short key, or -1 when none.
	 * @deprecated Short keys are currently not supported by the menu framework,
	 *             both graphically and functionally. They may return in a future
	 *             version.
	 */
	@Deprecated
	public char getShortKey() {
		return shortkey;
	}
	
	public void setMarked(boolean marked) {
		this.marked = marked;
		repaint();
	}
	
	public boolean isMarked() {
		return marked;
	}
}

//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuBackground
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import com.dennisbijlsma.ferrari3d.util.Utils;

/**
 * Widget that displays the background image and the title of the currently active
 * menu. Only one of these widgets should be used by the menu system.
 */
public class UIMenuBackground extends UIMenuWidget {
	
	private BufferedImage backgroundImage;
	private UIMenuTitle titleWidget;
	private String title;
	
	private static final String BACKGROUND_IMAGE_URL = "data/graphics/menu_background.jpg";
	private static final int PANEL_ARC = 20;
	private static final Font TITLE_FONT = new Font("Verdana", Font.BOLD, 24);
	private static final BasicStroke TITLE_STROKE = new BasicStroke(3f);
	private static final int TITLE_HEIGHT = 64;

	/**
	 * Creates a new widget with an initially empty title.
	 */
	public UIMenuBackground() {
	
		super(UIMenu.MENU_TEXTURE_WIDTH, UIMenu.MENU_TEXTURE_HEIGHT, false);

		backgroundImage = Utils.loadImage(BACKGROUND_IMAGE_URL);
		titleWidget = new UIMenuTitle();
		title = "";
		
		repaint();
	}
	
	/** {@inheritDoc} */
	@Override
	protected void paintImage(Graphics2D g2) {
		
		// Background image
		
		g2.drawImage(backgroundImage, 0, 0, null);
		
		// Panels
		
		//TODO make coordinates *slightly* less unclear
		g2.setColor(PANEL_COLOR);
		g2.fillRoundRect(UIMenu.BORDER, UIMenu.HEADER, UIMenu.MENU_WIDTH - UIMenuButton.NORMAL_WIDTH -
				UIMenu.BORDER * 5, UIMenu.MENU_HEIGHT - UIMenu.HEADER - UIMenu.BORDER, PANEL_ARC, 
				PANEL_ARC);
		g2.fillRoundRect(UIMenu.MENU_WIDTH - UIMenuButton.NORMAL_WIDTH - UIMenu.BORDER * 3,
				UIMenu.HEADER, UIMenuButton.NORMAL_WIDTH + UIMenu.BORDER * 2, UIMenu.MENU_HEIGHT -
				UIMenu.HEADER - UIMenu.BORDER, PANEL_ARC, PANEL_ARC);
	}
	
	/**
	 * Changes the title of this widget to the specified value. The widget will 
	 * be repainted to reflect the changes.
	 */
	public void changeTitle(String title) {
		this.title = title;
		titleWidget.repaint();
	}
	
	/**
	 * Changes the title. Convenience version of {@link #changeTitle(String)}.
	 */
	public void setTitle(String title) {
		changeTitle(title);
	}
	
	/** {@inheritDoc} */
	@Override
	public List<UIMenuWidget> getAdditionalWidgets() {
		List<UIMenuWidget> list = super.getAdditionalWidgets();
		list.add(titleWidget);
		return list;
	}
	
	/**
	 * Inner class that contains the title. This class exists simply so that the
	 * entire (large) background doesn't have to be repainted every time the
	 * title is changed.
	 */
	private class UIMenuTitle extends UIMenuWidget {

		public UIMenuTitle() {
			super(UIMenu.MENU_TEXTURE_WIDTH, TITLE_HEIGHT, false);
		}
		
		@Override
		protected void paintImage(Graphics2D g2) {
			g2.setClip(0, 0, UIMenu.MENU_WIDTH, TITLE_HEIGHT);
			g2.setFont(TITLE_FONT);
			g2.setColor(SHADOW_COLOR);
			paintText(g2, title, UIMenu.MENU_WIDTH / 2 + 2, 47, 'c');
			g2.setColor(FONT_COLOR);
			paintText(g2, title, UIMenu.MENU_WIDTH / 2, 45, 'c');
			g2.setStroke(TITLE_STROKE);
			g2.drawArc(-10, 5, UIMenu.MENU_WIDTH + 20, 10, 0, -180);
			g2.drawArc(-10, 54, UIMenu.MENU_WIDTH + 20, 10, 0, 180);
		}
	}
}

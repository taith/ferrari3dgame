//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuFlow
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.List;

/**
 * Widget that allows a selection from a number of thumbnail images. Its primary
 * use is providing a visual way of selecting a car and circuit. However, because
 * there are currently only one car and circuit this widget is 'fake'. If the need
 * arises in a future version its selection behavior shall be implemented.
 */

public class UIMenuFlow extends UIMenuWidget {
	
	private Image[] images;
	private String[] labels;
	
	private static final int THUMBNAIL_WIDTH = 100;
	private static final int THUMBNAIL_HEIGHT = 75;
	private static final int WIDGET_WIDTH = 430;
	private static final int WIDGET_HEIGHT = 100;
	private static final int TEXTURE_WIDTH = 512;
	private static final int TEXTURE_HEIGHT = 128;
	private static final int MAX_THUMBNAILS = 4;
	private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 128);

	/**
	 * Creates a new {@code UIMenuFlow}.
	 * @throws IllegalArgumentException when images and labels arrays are not
	 *         of the same length.
	 */

	public UIMenuFlow(String key, Image[] images, String[] labels) {
		
		super(TEXTURE_WIDTH, TEXTURE_HEIGHT, false);
		
		if (images.length != labels.length) {
			throw new IllegalArgumentException("Images and labels arrays are of different length");
		}
		
		this.images = images;
		this.labels = labels;
		
		repaint();
	}
	
	public UIMenuFlow(String key, Image[] images, List<String> labels) {
		this(key, images, labels.toArray(new String[0]));
	}
	
	@Override
	protected void paintImage(Graphics2D g2) {
		
		int startX = 0;
		
		for (int i = 0; i < MAX_THUMBNAILS; i++) {
			g2.setColor(TRANSPARENT_COLOR);
			g2.fillRect(startX, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);

			if (i < images.length) {
				g2.drawImage(images[i], startX, 0, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, null);
				g2.setFont(FONT);
				g2.setColor(isEnabled() ? FONT_COLOR : DISABLED_FOREGROUND_COLOR);
				paintText(g2, labels[i], startX + THUMBNAIL_WIDTH / 2, THUMBNAIL_HEIGHT + 20, 'c');
			}
			
			if (!isEnabled()) {
				g2.setColor(DISABLED_FOREGROUND_COLOR);
			} else if (i == 0) { //TODO support selections
				g2.setColor(SELECTED_COLOR);
			} else {
				g2.setColor(FONT_COLOR);
			}

			g2.drawRect(startX, 0, THUMBNAIL_WIDTH - 1, THUMBNAIL_HEIGHT - 1);
			
			startX += THUMBNAIL_WIDTH + 10;
		}
	}

	@Override
	public Dimension getHitArea() {
		return new Dimension(WIDGET_WIDTH, WIDGET_HEIGHT);
	}
}

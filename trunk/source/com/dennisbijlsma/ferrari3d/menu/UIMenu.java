//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenu
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UIPanel;
import com.dennisbijlsma.core3d.ui.UIWidget;
import nl.colorize.util.swing.CircularLoader;
import nl.colorize.util.swing.Utils2D;

/**
 * Container class for menu widgets. Each menu screen contains of a number of
 * panels, which in turn contain a number of widgets. Only one panel's widgets can
 * be shown at the same time. Apart from these widgets, the menu itself also 
 * contains two sets of buttons. The first is displayed in the bottom right corner
 * and represent global navigation controls, the second set is for navigating 
 * between the different panels within the menu.
 */
public class UIMenu {

	private UIPanel container;
	private List<UIPanel> panels;
	private List<UIMenuButton> panelButtons;
	private List<UIMenuButton> buttons;
	private List<UIMenuWidget> widgets;
	private UILoaderWidget loaderWidget;
	private String title;
	
	private Map<Integer,Integer> layouts;
	
	public static final int MENU_WIDTH = 800;
	public static final int MENU_HEIGHT = 600;
	public static final int MENU_TEXTURE_WIDTH = 1024;
	public static final int MENU_TEXTURE_HEIGHT = 1024;
	public static final int BORDER = 20;
	public static final int SPACING = 10;
	public static final int ARC = 20;
	public static final int HEADER = 80;
	private static final int LOADER_SIZE = 40;
	private static final int LOADER_TEXTURE_SIZE = 64;
	private static final float LOADER_ROTATION_SPEED = 0.02f;
	private static final String BACKGROUND_IMAGE_URL = "data/graphics/menu_background.jpg";
	
	/**
	 * Creates a new {@code UIMenu} with no panels or widgets.
	 */
	public UIMenu() {
	
		container = new UIPanel();
		panels = new ArrayList<UIPanel>();
		panelButtons = new ArrayList<UIMenuButton>();
		buttons = new ArrayList<UIMenuButton>();
		widgets = new ArrayList<UIMenuWidget>();
		
		loaderWidget = new UILoaderWidget();
		loaderWidget.setPosition(MENU_WIDTH / 2 - LOADER_SIZE / 2,
				MENU_HEIGHT - LOADER_SIZE - BORDER - BORDER / 2);
		loaderWidget.setVisible(false);
		container.addWidget(loaderWidget);
		
		title = "";
		layouts = new HashMap<Integer,Integer>();
	}
	
	/**
	 * Returns the container panel for this menu. This panel should be added to
	 * the scene graph in order for the menu to be displayed.
	 */
	public UIPanel getContainer() {
		return container;
	}
	
	/**
	 * Adds a new panel with the specified title to this menu. A navigation button
	 * for the panel will automatically be added.
	 * @return The index of the added panel.
	 */
	public int addPanel(String title) {
	
		UIPanel panel = new UIPanel();
		container.addSubPanel(panel);
		panels.add(panel);
		
		final int panelIndex = panels.size() - 1;
		layouts.put(panelIndex, HEADER + BORDER);
		
		// for titled panels, add a button		
		
		int buttonX = MENU_WIDTH - BORDER - BORDER - UIMenuButton.NORMAL_WIDTH;
		int buttonY = HEADER + BORDER / 2 + UIMenuButton.BUTTON_HEIGHT * panelButtons.size();
		
		UIMenuButton button = new UIMenuButton(title, UIMenuButton.ButtonType.SMALL);
		button.getWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source, int clickX, int clickY) {
				setSelectedPanel(panelIndex);
			}
		});
		button.setPosition(buttonX, buttonY);
		container.addWidget(button.getWidget());
		panelButtons.add(button);
		
		// For titled panels, add the title as a header
		
		if ((title != null) && (title.length() > 0)) {
			UIMenuText header = new UIMenuText();
			header.setColor(UIMenuWidget.HEADER_COLOR);
			header.paintText(title);
			addWidget(panelIndex, header);
		}
		
		// If this is the first panel, select it
		
		if (panelIndex == 0) {
			setSelectedPanel(panelIndex);
		}
		
		return panelIndex;
	}
	
	/**
	 * Returns the panel with the specified index.
	 * @throws IndexOutOfBoundsException when no panel exists at that index.
	 */
	public UIPanel getPanel(int index) {
		return panels.get(index);
	}
	
	/**
	 * Adds a widget to the panel with the specified index. If a panel does not
	 * exist at the specified index, it is created.
	 */
	public void addWidget(int panelIndex, UIMenuWidget widget) {
		
		if (panels.size() <= panelIndex) {
			int blankPanelIndex = addPanel("");
		}
		
		panels.get(panelIndex).addWidget(widget.getWidget());
		for (UIMenuWidget i : widget.getAdditionalWidgets()) {
			panels.get(panelIndex).addWidget(i.getWidget());
		}
		
		int widgetX = BORDER * 2;
		int widgetY = layouts.get(panelIndex);
		widget.setPosition(widgetX, widgetY);
		
		widgetY += widget.getHitArea().getHeight() + SPACING;
		layouts.put(panelIndex, widgetY);
		widgets.add(widget);
	}
	
	/**
	 * Returns the widget at the specified index. The first added widget is
	 * assumed to be at index 0.
	 * @throws OutOfBoundsException if no widget exists at the index.
	 */
	public UIMenuWidget getWidget(int index) {
		return widgets.get(index);
	}
	
	/**
	 * Adds a button to this menu. The button will be visible regardless of what
	 * panel is selected.
	 */
	public void addButton(UIMenuButton button) {
	
		container.addWidget(button.getWidget());
		buttons.add(button);
		
		int buttonX = MENU_WIDTH-BORDER-BORDER - UIMenuButton.NORMAL_WIDTH;
		int buttonY = MENU_HEIGHT - BORDER - (UIMenuButton.BUTTON_HEIGHT + SPACING) * buttons.size();
		button.getWidget().setPosition(buttonX, buttonY);
	}
	
	/**
	 * Returns the button at the specified index.
	 * @throws IndexOutOfBoundsException when no button exists at that index.
	 */
	public UIMenuButton getButton(int index) {
		return buttons.get(index);
	}
	
	/**
	 * Sets the currently active panel. All other panels will be made invisible.
	 * @throws IndexOutOfBoundsException when no panel exists at that index.
	 */
	public void setSelectedPanel(int panelIndex) {
		
		if ((panelIndex < 0) || (panelIndex >= panelButtons.size())) {
			throw new IndexOutOfBoundsException("Invalid panel index: " + panelIndex);
		}
		
		container.setVisibleSubPanel(panelIndex);
		loaderWidget.setVisible(false);
		for (int i = 0; i < panelButtons.size(); i++) {
			panelButtons.get(i).setMarked(i == panelIndex);
		}
	}
	
	/**
	 * Sets if this menu is currently in loading state. During this state a loading
	 * animation will be displayed.
	 */
	public void setLoading(boolean loading) {
		loaderWidget.setVisible(loading);
	}
	
	/**
	 * Returns if this menu is currently in loading state. During this state a 
	 * loading animation will be displayed.
	 */
	public boolean isLoading() {
		return loaderWidget.isVisible();
	}
	
	/**
	 * Sets the title of this menu.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * Returns the title of this menu.
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Widget that displays an AJAX loader graphic. The widget will be animated
	 * so that it seems as if the graphic is spinning.
	 */
	private static class UILoaderWidget extends UIWidget {
		
		private Image[] frames;
		private int currentFrame;
		private float timeLeft;
		
		public UILoaderWidget() {
			super(0, 0, LOADER_TEXTURE_SIZE, LOADER_TEXTURE_SIZE);
		}
		
		@Override
		protected void updateAnimation(float dt) {
			
			if (frames == null) {
				frames = CircularLoader.createAnimation(LOADER_SIZE, LOADER_SIZE, 
						UIMenuWidget.FONT_COLOR);
			}
			
			if (!isVisible() || !isEnabled()) {
				return;
			}
			
			timeLeft -= dt;
			if (timeLeft <= 0f) {
				timeLeft = 0.08f;
				currentFrame++;
				if (currentFrame >= frames.length) {
					currentFrame = 0;					
				}
				
				Graphics2D g2 = getTextureImage().createGraphics();
				Utils2D.clearGraphics(g2, 0, 0, LOADER_SIZE, LOADER_SIZE);
				g2.drawImage(frames[currentFrame], 0, 0, LOADER_SIZE, LOADER_SIZE, null);
				g2.dispose();
				repaint();
			}
		}
	}
}

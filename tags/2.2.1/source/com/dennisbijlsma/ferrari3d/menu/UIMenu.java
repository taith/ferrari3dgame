//--------------------------------------------------------------------------------
// Ferrari3D
// UIMenu
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UIPanel;
import com.dennisbijlsma.core3d.ui.UIWidget;

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
	private UILoaderWidget loaderWidget;
	
	private Map<Integer,Integer> layouts;
	
	public static final int MENU_WIDTH=800;
	public static final int MENU_HEIGHT=600;
	public static final int BORDER=20;
	public static final int SPACING=10;
	private static final int LOADER_SIZE=40;
	private static final int LOADER_TEXTURE_SIZE=64;
	private static final float LOADER_ROTATION_SPEED=0.02f;
	
	/**
	 * Creates a new {@code UIMenu}.
	 */
	
	public UIMenu() {
	
		container=new UIPanel();
		panels=new ArrayList<UIPanel>();
		panelButtons=new ArrayList<UIMenuButton>();
		buttons=new ArrayList<UIMenuButton>();
		
		loaderWidget=new UILoaderWidget();
		loaderWidget.setPosition(MENU_WIDTH/2-LOADER_SIZE/2,MENU_HEIGHT-LOADER_SIZE*2);
		container.addWidget(loaderWidget);
		
		layouts=new HashMap<Integer,Integer>();
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
	
		UIPanel panel=new UIPanel();
		container.addSubPanel(panel);
		panels.add(panel);
		
		final int panelIndex=panels.size()-1;
		int buttonX=MENU_WIDTH-BORDER-BORDER-UIMenuButton.NORMAL_WIDTH;
		int buttonY=100+UIMenuButton.BUTTON_HEIGHT*panelButtons.size();
		
		UIMenuButton button=new UIMenuButton(title,UIMenuButton.ButtonType.SMALL);
		button.getSingleWidget().addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				setSelectedPanel(panelIndex);
			}
		});
		button.setPosition(buttonX,buttonY);
		container.addWidget(button.getSingleWidget());
		panelButtons.add(button);
		
		layouts.put(panelIndex,BORDER);
		
		if (panelIndex==0) {
			setSelectedPanel(panelIndex);
		}
		
		return panelIndex;
	}
	
	/**
	 * Adds a widget to the panel with the specified index.
	 * @throws IndexOutOfBoundsException when no panel exists at that index.
	 */
	
	public void addWidget(int panelIndex,UIMenuWidget widget) {
		
		panels.get(panelIndex).addWidgets(widget.getWidgets());
		
		int widgetX=BORDER+BORDER;
		int widgetY=layouts.get(panelIndex);
		widget.setPosition(widgetX,widgetY);
		
		widgetY+=widget.getHitArea().getHeight()+SPACING;
		layouts.put(panelIndex,widgetY);
	}
	
	/**
	 * Adds a button to this menu. The button will be visible regardless of what
	 * panel is selected.
	 */
	
	public void addButton(UIMenuButton button) {
	
		container.addWidget(button.getSingleWidget());
		buttons.add(button);
		
		int buttonX=MENU_WIDTH-BORDER-BORDER-UIMenuButton.NORMAL_WIDTH;
		int buttonY=MENU_HEIGHT-BORDER-(UIMenuButton.BUTTON_HEIGHT+SPACING)*buttons.size();
		button.getSingleWidget().setPosition(buttonX,buttonY);
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
		
		if ((panelIndex<0) || (panelIndex>=panelButtons.size())) {
			throw new IndexOutOfBoundsException("Invalid panel index: "+panelIndex);
		}
		
		container.setVisibleSubPanel(panelIndex);
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
	 * Widget that displays an AJAX loader graphic. The widget will be animated
	 * so that it seems as if the graphic is spinning.
	 */
	
	private static class UILoaderWidget extends UIWidget {
		
		public UILoaderWidget() {
			super(0,0,LOADER_TEXTURE_SIZE,LOADER_TEXTURE_SIZE,createLoaderImage());
		}
		
		@Override
		protected void updateAnimation(float dt) {
			getQuad().getTransform().getRotation().z+=LOADER_ROTATION_SPEED;
			getQuad().updateTransform();
		}
		
		private static BufferedImage createLoaderImage() {
			BufferedImage image=new BufferedImage(LOADER_TEXTURE_SIZE,LOADER_TEXTURE_SIZE,
					BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g2=image.createGraphics();
			g2.setColor(java.awt.Color.GREEN);
			g2.fillOval(0,0,LOADER_SIZE,LOADER_SIZE);
			g2.dispose();
			return image;
		}
	}
}
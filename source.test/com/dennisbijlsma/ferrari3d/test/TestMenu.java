package com.dennisbijlsma.ferrari3d.test;

import java.awt.Image;

import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.GameState;
import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.MenuState;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.menu.UIMenu;
import com.dennisbijlsma.ferrari3d.menu.UIMenuButton;
import com.dennisbijlsma.ferrari3d.menu.UIMenuFlow;
import com.dennisbijlsma.ferrari3d.menu.UIMenuOption;
import com.dennisbijlsma.ferrari3d.menu.UIMenuTable;
import com.dennisbijlsma.ferrari3d.menu.UIMenuText;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * Test the menu system and all types of user interface widgets.
 */

public class TestMenu extends GameCore implements GameState {
	
	public static void main(String[] args) {
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	
		Display display = new Display(800, 600, false);
		display.setWindowTitle("Ferrari3D | TestMenu");
		
		TestMenu test = new TestMenu(display);
		test.changeGameState(test);
		test.startGame();
	}
	
	public TestMenu(Display display) {
		super(display);
	}
	
	public void initGameState() {
		
		Loader.createWorld(getContext().getSceneGraph(), getContext().getDisplay());
		getContext().getSceneGraph().getCamera(0).aim(new Vector3D(), new Vector3D(0f, 0f, 10f));
	}
	
	public void updateGameState(float dt) {
		TestMenuState state = new TestMenuState(this);
		changeGameState(state);
	}
	
	public void cleanupGameState() { }
	
	private static class TestMenuState extends MenuState {
		
		public TestMenuState(GameCore game) {
			super(game.getContext());
		}

		protected void initUI() { 
			UIMenu menu = new UIMenu();
			menu.addPanel("Test panel 1");
			menu.addPanel("Test panel 2");
			
			UIMenuButton button = new UIMenuButton("Button A");
			button.addActionListener(new ActionListener() {
				public void actionPerformed(UIWidget source) {
					System.out.println("Button A clicked");
				}
			});
			menu.addButton(button);
			
			UIMenuButton button2 = new UIMenuButton("Button B");
			menu.addButton(button2);
			
			UIMenuText text = new UIMenuText();
			text.paintText("This is a test");
			menu.addWidget(0, text);
	
			UIMenuTable table = new UIMenuTable(128, new String[]{"a", "b"}, new int[]{50, 50});
			table.addRow("Key", "Value");
			menu.addWidget(0, table);
			
			UIMenuOption option = new UIMenuOption("Label: ", "laps", 1, 10, 
					new String[]{"a", "b", "c", "d", "e"});
			menu.addWidget(0, option);
			
			UIMenuFlow flow = new UIMenuFlow("Flow:", new Image[0], new String[0]);
			menu.addWidget(0, flow);
	
			getUISystem().getRootPanel().addSubPanel(menu.getContainer());
			getUISystem().getRootPanel().setVisibleSubPanel(menu.getContainer());
		}
	}
}
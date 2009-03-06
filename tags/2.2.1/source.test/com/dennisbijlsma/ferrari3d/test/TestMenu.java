package com.dennisbijlsma.ferrari3d.test;

import java.awt.Image;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.Display;
import com.dennisbijlsma.core3d.game.GameCore;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UISystem;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.graphics.Loader;
import com.dennisbijlsma.ferrari3d.menu.UIMenu;
import com.dennisbijlsma.ferrari3d.menu.UIMenuButton;
import com.dennisbijlsma.ferrari3d.menu.UIMenuFlow;
import com.dennisbijlsma.ferrari3d.menu.UIMenuOption;
import com.dennisbijlsma.ferrari3d.menu.UIMenuTable;
import com.dennisbijlsma.ferrari3d.menu.UIMenuText;
import com.dennisbijlsma.ferrari3d.util.Settings;



public class TestMenu extends GameCore {

	
	
	public static void main(String[] args) {
		
		try {
			Settings.getInstance().init();
			Settings.getInstance().load();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	
		Display display=new Display(800,600,false);
		display.setWindowTitle("Ferrari3D | TestMenu");
		
		TestMenu test=new TestMenu(display);
		test.startGame();
	}
	
	
	
	public TestMenu(Display display) {
		
		super(display);
	}
	
	
	
	@Override
	public void initGame() {
		
		Loader.createWorld(getSceneGraph(),getDisplay(),false);
		getSceneGraph().getCamera(0).aim(new Vector3D(),new Vector3D(0f,0f,10f));
		
		// UI system
		
		SceneGraphNode uiNode=new SceneGraphNode("uiNode");
		getSceneGraph().getRootNode().addChild(uiNode);
				
		
		UIMenu menu=new UIMenu();
		menu.addPanel("Test panel 1");
		menu.addPanel("Test panel 2");
		
		UIMenuButton button=new UIMenuButton("Button A");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				System.out.println("Button A clicked");
			}
		});
		menu.addButton(button);
		
		UIMenuButton button2=new UIMenuButton("Button B");
		menu.addButton(button2);
		
		UIMenuText text=new UIMenuText(256,64);
		text.paintText("This is a test");
		menu.addWidget(0,text);

		UIMenuTable table=new UIMenuTable(128,128,new String[]{"a","b"},new int[]{50,50});
		table.addRow("Key","Value");
		menu.addWidget(0,table);
		
		UIMenuOption option=new UIMenuOption("Label: ","laps",1,10,new String[]{"a","b","c","d","e"});
		menu.addWidget(0,option);
		
		UIMenuFlow flow=new UIMenuFlow(new Image[0],new String[0]);
		menu.addWidget(0,flow);

		UISystem system=new UISystem(getDisplay(),getController());
		system.getRootPanel().addSubPanel(menu.getContainer());
		system.getRootPanel().setVisibleSubPanel(0);
		system.create(uiNode);
		addGameEntity(system);
	}
	
	
	
	@Override
	protected void updateGame(float dt) {
	
	}
}
//--------------------------------------------------------------------------------
// Ferrari3D
// MenuPanel
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.*;
import java.util.*;
import javax.swing.*;



public class MenuPanel extends JPanel {

	private String title;
	private ArrayList<MenuWidget> widgets;
	private MenuButton button;
	
	private static final int H_GAP=10;
	private static final int V_GAP=5;
	
	private static final Font FONT=new Font("Verdana",Font.BOLD,12);
	
	
	
	public MenuPanel(String panelTitle) {
		
		super(null);
		
		setOpaque(false);

		title=panelTitle;	
		widgets=new ArrayList<MenuWidget>();
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {
	
		super.paintComponent(g);
		
		Graphics2D g2=(Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		int y=0;
		
		// Title
		
		if (getTitle().length()>0) {
			g2.setFont(FONT);
			g2.setColor(MenuWidget.SELECTED_COLOR);
			g2.drawString(getTitle(),0,MenuWidget.LINE_HEIGHT);		
			y+=MenuWidget.LINE_HEIGHT*2+V_GAP*2;
		}

		// Widgets
		
		for (int i=0; i<widgets.size(); i++) {
			int height=widgets.get(i).getWidgetSize().height;
			widgets.get(i).setBounds(0,y,getWidth()-1,(height<=0) ? getHeight()-y : height);
			y+=widgets.get(i).getWidgetSize().height+V_GAP;
		}
	}
	
	
	
	public void addWidget(MenuWidget widget) {
	
		widgets.add(widget);
		
		this.add(widget);
		this.repaint();
	}
	
	
	
	public MenuWidget[] getWidgets() {
	
		return widgets.toArray(new MenuWidget[0]);
	}
	
	
	
	public void setButton(MenuButton button) {
	
		this.button=button;
	}
	
	
	
	public MenuButton getButton() {
	
		return button;
	}
	
	
	
	public String getTitle() {
	
		return title;
	}
}
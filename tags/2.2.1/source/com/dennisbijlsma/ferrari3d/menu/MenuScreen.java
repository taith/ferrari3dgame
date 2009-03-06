//--------------------------------------------------------------------------------
// Ferrari3D
// MenuScreen
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import com.dennisbijlsma.util.swing.AjaxLoader;
import com.dennisbijlsma.util.swing.Utils2D;



@Deprecated
public class MenuScreen extends JPanel {
	
	private JPanel panelContainer;
	private CardLayout panelLayout;
	private List<MenuPanel> panels;
	private List<MenuButton> buttons;
	private int selectedPanel;
	private int selectedButton;
	private String title;
	private AjaxLoader loader;
	
	private static final int BORDER=20;
	private static final int HEADER=60;
	private static final int H_GAP=20;
	private static final int V_GAP=10;
	private static final Color TITLE_COLOR=Color.WHITE;
	private static final Stroke TITLE_STROKE=new BasicStroke(3f);
	private static final Color SHADOW_COLOR=new Color(0,0,0,128);
	private static final Color BOXOUT_COLOR=new Color(0,0,0,128);
	private static final int BUTTON_WIDTH=120;
	private static final int BUTTON_HEIGHT=25;
	
	
	
	public MenuScreen(String menuTitle) {
		
		super(null);
		
		panels=new ArrayList<MenuPanel>();
		buttons=new ArrayList<MenuButton>();
		selectedPanel=0;
		selectedButton=0;
		title=menuTitle;
		loader=null;
		
		setOpaque(false);
		
		panelContainer=new JPanel();
		panelContainer.setOpaque(false);
		add(panelContainer);
		
		panelLayout=new CardLayout();
		panelContainer.setLayout(panelLayout);
	}
	
	
	
	@Override
	protected void paintComponent(Graphics g) {
		
		super.paintComponent(g);
				
		Graphics2D g2=(Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
		// Header
		
		g2.setFont(MenuWidget.TITLE_FONT);
		g2.setColor(SHADOW_COLOR);
		Utils2D.drawAlignedString(g2,title,getWidth()/2+2,HEADER-18,'c');
		g2.setColor(TITLE_COLOR);
		Utils2D.drawAlignedString(g2,title,getWidth()/2,HEADER-20,'c');
			
		g2.setStroke(TITLE_STROKE);
		g2.drawArc(-10,HEADER-10,getWidth()+20,10,0,180);
		g2.drawArc(-10,0,getWidth()+20,10,0,-180);
		
		int y=HEADER+BORDER;
		
		// Boxouts
		
		int panelX=BORDER;
		int panelY=y;
		int panelWidth=getWidth()-BORDER*3-BUTTON_WIDTH-H_GAP*2;
		int panelHeight=getHeight()-y-BORDER;
		int buttonPanelX=panelX+panelWidth+BORDER;
		int buttonPanelWidth=BUTTON_WIDTH+H_GAP*2;
		
		g2.setColor(BOXOUT_COLOR);		
		g2.fillRoundRect(panelX,panelY,panelWidth,panelHeight,20,20);		
		g2.fillRoundRect(buttonPanelX,panelY,buttonPanelWidth,panelHeight,20,20);
				
		// Panels
				
		panelContainer.setBounds(panelX+H_GAP,panelY+V_GAP,panelWidth-H_GAP*2,panelHeight-V_GAP*2);
		
		if ((panels.size()>0) && (getSelectedPanel()!=null)) {
			getSelectedPanel().revalidate();
		}
				
		// Panel buttons
		
		if (panels.size()>1) {
			for (int i=0; i<panels.size(); i++) {
				int buttonX=buttonPanelX+H_GAP;
				int buttonY=panelY+H_GAP+BUTTON_HEIGHT*i;
				panels.get(i).getButton().setBounds(buttonX,buttonY,BUTTON_WIDTH,BUTTON_HEIGHT);
			}
		}
		
		// Buttons
		
		for (int i=0; i<buttons.size(); i++) {
			int buttonX=getWidth()-BUTTON_WIDTH-H_GAP-BORDER;
			int buttonY=getHeight()-BORDER-(BUTTON_HEIGHT+V_GAP)*(i+1);
			buttons.get(i).setBounds(buttonX,buttonY,BUTTON_WIDTH,BUTTON_HEIGHT);
		}
		
		// Loader
		
		if (loader!=null) {
			Dimension d=loader.getPreferredSize();
			loader.setBounds(getWidth()/2-d.width/2,getHeight()-100,d.width,d.height);
		}
	}
	
	
	
	public void addPanel(final MenuPanel panel) {
		
		panel.setButton(new MenuButton(panel.getTitle(),true) {
			public void pressed() {
				setSelectedPanel(panel);
			}
		});
		
		panels.add(panel);
		panelContainer.add(panel,panel.getTitle());
		panelLayout.show(panelContainer,panel.getTitle());
		this.add(panel.getButton());
		
		setSelectedPanel(0);
	}
	
	
	
	public void addPanel(String panelTitle) {
	
		addPanel(new MenuPanel(panelTitle));
	}
	
	
	
	public MenuPanel getPanel(int index) {
	
		if ((index<0) || (index>=panels.size())) {
			return null;
		}
		
		return panels.get(index);
	}
	
	
	
	public MenuPanel[] getPanels() {
	
		return panels.toArray(new MenuPanel[0]);
	}
	
	
	
	public void addWidget(int panel,MenuWidget widget) {
	
		panels.get(panel).addWidget(widget);
	}
	
	
	
	public void addWidget(MenuWidget widget) {
		
		if (panels.size()==0) { 
			addPanel("");
		}
	
		addWidget(0,widget);
	}
	
	
	
	public MenuWidget getWidget(int panel,int widget) {
			
		return panels.get(panel).getWidgets()[widget];
	}
	
	
	
	public void addButton(MenuButton button) {
	
		buttons.add(button);
		add(button);
		repaint();
	}
		
	
	
	public MenuButton getButton(int index) {
	
		return buttons.get(index);
	}
	
	
	
	public MenuButton[] getButtons() {
	
		return buttons.toArray(new MenuButton[0]);
	}
	
	
	
	public void setTitle(String title) {
		
		this.title=title;
		repaint();
	}
	
	
	
	public String getTitle() {
		
		return title;
	}
	
	
	
	public void setLoading(boolean loading) {
		
		if ((loading) && (loader==null)) {
			loader=new AjaxLoader();
			loader.setColor(Color.WHITE);
			add(loader);
			repaint();
		}
		
		if ((!loading) && (loader!=null)) {
			remove(loader);
			loader=null;
			repaint();
		}
	}
	
	
	
	public boolean getLoading() {
		
		return (loader!=null);
	}
		
	
	
	private void setSelectedPanel(int sp) {
		
		selectedPanel=sp;
		panelLayout.show(panelContainer,getSelectedPanel().getTitle());
		
		for (MenuPanel i : panels) {
			i.getButton().setSelected(i==getSelectedPanel());
		}
	}
	
	
	
	private void setSelectedPanel(MenuPanel sp) {
		
		for (int i=0; i<panels.size(); i++) {
			if (panels.get(i)==sp) {
				setSelectedPanel(i);
				break;
			}
		}
	}
	
	
	
	private MenuPanel getSelectedPanel() {
			
		return panels.get(selectedPanel);
	}
}
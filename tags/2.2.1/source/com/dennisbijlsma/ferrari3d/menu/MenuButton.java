//--------------------------------------------------------------------------------
// Ferrari3D
// MenuButton
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

import com.dennisbijlsma.util.swing.Utils2D;
import com.dennisbijlsma.util.swing.anim.Animatable;
import com.dennisbijlsma.util.swing.anim.Animation;



@Deprecated
public class MenuButton extends MenuWidget implements MouseListener,Animatable {
	
	private ActionListener listener;
	
	private String label;
	private boolean toggle;
	private char shortkey;
	
	private boolean selected;
	private boolean rollover;
	private Animation glowAnim;
			
	
	
	public MenuButton(String label,boolean toggle) {
	
		super();
				
		this.label=label;
		this.toggle=toggle;
		
		shortkey=' ';
		
		selected=false;
		rollover=false;
		glowAnim=new Animation("glow",1.5f);
		glowAnim.setLoopMode(Animation.LoopMode.LOOP);
		glowAnim.addAnimatable(this);
						
		addMouseListener(this);
	}
	
	
	
	public MenuButton(String label) {
	
		this(label,false);
	}
	
	
	
	public MenuButton(String label,boolean toggle,char shortkey) {
	
		this(label,toggle);
		
		this.shortkey=shortkey;
	}
	
	
	
	@Override
	public void paintWidget(Graphics2D g2) {
		
		if (toggle) {
			paintToggleButton(g2);
		} else {
			paintNormalButton(g2);
		}
	}
	
	
	
	private void paintNormalButton(Graphics2D g2) {
		
		// Paint background
		
		Color textColor=rollover ? SELECTED_COLOR : FONT_COLOR;
		Color strokeColor=rollover ? SELECTED_COLOR : FONT_COLOR;
		
		if (!isEnabled()) {
			textColor=DISABLED_FOREGROUND_COLOR;
			strokeColor=DISABLED_FOREGROUND_COLOR;
		}
		
		paintAnimatedBackground(g2,0,0,getWidth(),getHeight(),glowAnim.getDelta());
		
		g2.setStroke(STROKE);
		g2.setColor(strokeColor);
		g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,ARC,ARC);
		
		// Paint label
		
		int labelX=getWidth()/2-g2.getFontMetrics(FONT).stringWidth(label)/2;
		int labelY=getHeight()-BASELINE;
		
		g2.setFont(FONT);
		g2.setColor(textColor);
		
		if ((shortkey!=' ') && (isEnabled())) { 
			int index=label.indexOf(shortkey);
			AttributedString as=new AttributedString(label);
			as.addAttribute(TextAttribute.FONT,FONT);
			as.addAttribute(TextAttribute.FOREGROUND,textColor);
			as.addAttribute(TextAttribute.FOREGROUND,SHORTKEY_COLOR,index,index+1);
			g2.drawString(as.getIterator(),labelX,labelY);
		} else {
			g2.drawString(label,labelX,labelY);
		}
	}
	
	
	
	private void paintToggleButton(Graphics2D g2) {
		
		g2.setStroke(STROKE);
		g2.setColor(selected ? SELECTED_COLOR : STROKE_COLOR);
		g2.drawLine(0,getHeight()-1,getWidth()-1,getHeight()-1);
		
		g2.setFont(SMALL_FONT);
		g2.setColor((rollover || selected) ? SELECTED_COLOR : FONT_COLOR);		
		Utils2D.drawAlignedString(g2,label,0,getHeight()-BASELINE/2,'l');
	}
	
	
	
	public void mousePressed(MouseEvent e) {
	
		repaint();
	}
	
	
	
	public void mouseReleased(MouseEvent e) {
		
		if (isEnabled()) {
			pressed();
			repaint();
		}
	}
	
	
	
	public void mouseEntered(MouseEvent e) {
		
		rollover=true;
		glowAnim.start();
	}
	
	
	
	public void mouseExited(MouseEvent e) {				
		
		rollover=false;
		glowAnim.stop();
		repaint();
	}
	
	
	
	public void mouseClicked(MouseEvent e) { 
		
	}
	
	
	
	public void animate(Animation anim,float delta) {
	
		if (anim==glowAnim) {
			repaint();
		}
	}
		
	
	
	public void pressed() {
	
		if (listener!=null) {
			listener.actionPerformed(new ActionEvent(this,ActionEvent.ACTION_PERFORMED,"pressed"));
		}
	}
	
	
	
	public void setLabel(String label) {
	
		this.label=label;
		repaint();
	}
	
	
	
	public String getLabel() {
		
		return label;
	}
	
	
	
	public boolean getToggle() {
	
		return toggle;
	}
	
	
	
	public char getShortKey() {
		
		if (isEnabled()) {
			return shortkey;
		} else {
			return ' ';
		}
	}
	
	
	
	public void setSelected(boolean selected) {
		
		this.selected=selected;
		repaint();
	}
	
	
	
	public boolean getSelected() {
	
		return selected;
	}
	
	
	
	public void setListener(ActionListener listener) {
	
		this.listener=listener;
	}
	
	
	
	public ActionListener getListener() {
		
		return listener;
	}
	
	
	
	@Override
	public Dimension getWidgetSize() {
	
		if (toggle) {
			return new Dimension(getWidth(),LINE_HEIGHT_SMALL);
		} else {
			return new Dimension(getWidth(),LINE_HEIGHT);
		}
	}
}
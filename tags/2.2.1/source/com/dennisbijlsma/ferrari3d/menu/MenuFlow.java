//--------------------------------------------------------------------------------
// Ferrari3D
// MenuFlow
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.swing.Utils2D;



@SuppressWarnings("deprecation")
public class MenuFlow extends MenuWidget implements MouseListener,MouseMotionListener {

	private Image[] images;
	private String[] labels;
	private String label;
	private int selected;
	private int hover;
	
	private Object instance;
	private String field;
		
	private static final int IMAGE_WIDTH=100;
	private static final int IMAGE_HEIGHT=75;
	
	
	
	public MenuFlow(String label,String imageTemplate,String[] labels) {
				
		super();
		
		this.label=label;
		this.images=new Image[labels.length];
		this.labels=labels;
		
		for (int i=0; i<images.length; i++) {
			images[i]=Utils.loadImage(imageTemplate.replaceFirst("____",labels[i]));
		}
		
		selected=0;
		hover=-1;
		
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	
	
	
	public MenuFlow(String label,String imageTemplate,String[] labels,Object instance,String field) {
	
		this(label,imageTemplate,labels);
		
		this.instance=instance;
		this.field=field;
	}
	
	
	
	@Override
	public void paintWidget(Graphics2D g2) {
						
		for (int i=0; i<Math.max(labels.length,5); i++) {
			if ((images.length>i) && (images[i]!=null)) {
				g2.drawImage(images[i],i*(IMAGE_WIDTH+5),0,IMAGE_WIDTH,IMAGE_HEIGHT,null);
			} else {
				g2.setColor(new Color(0,0,0,128));
				g2.fillRect(i*(IMAGE_WIDTH+5),0,IMAGE_WIDTH,IMAGE_HEIGHT);
			}
			
			g2.setStroke(STROKE);
			g2.setColor((selected==i || hover==i) ? SELECTED_COLOR : STROKE_COLOR);
			if (!isEnabled()) { g2.setColor(DISABLED_BACKGROUND_COLOR); }
			g2.drawRect(i*(IMAGE_WIDTH+5),0,IMAGE_WIDTH-1,IMAGE_HEIGHT-1);
						
			if ((labels.length>i) && (labels[i]!=null)) {
				g2.setFont(FONT);
				g2.setColor(FONT_COLOR);
				Utils2D.drawAlignedString(g2,labels[i],i*(IMAGE_WIDTH+5)+IMAGE_WIDTH/2,getHeight()-5,'c');
			}
		}
	}
	
	
	
	public void mouseReleased(MouseEvent e) {
	
		int requested=getImage(e.getX(),e.getY());
		
		if ((selected!=requested) && (requested!=-1)) {
			setSelectedImage(requested);
			changed();
		}
	}
	
	
	
	public void mouseMoved(MouseEvent e) {
	
		int requested=getImage(e.getX(),e.getY());
		
		if (hover!=requested) {
			hover=requested;
			repaint();
		}
	}
	
	
	
	public void mouseExited(MouseEvent e) { 
		
		hover=-1;
		repaint();
	}
	
	
	
	public void mousePressed(MouseEvent e) { }
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) { }
	
	
	
	public void changed() {
		
		if ((instance!=null) && (field!=null)) {
			//TODO
		}
	}
	
	
	
	public void setSelectedImage(int s) {
		
		if (s<0) { s=labels.length-1; }
		if (s>=labels.length) { s=0; }
		
		selected=s;
		repaint();
	}
	
	
	
	public int getSelectedImage() {
	
		return selected;
	}
	
	
	
	public void setSelectedLabel(String s) {
		
		for (int i=0; i<labels.length; i++) {
			if (labels[i].equals(s)) {
				selected=i;
				break;
			}
		}
	}
	
	
	
	public String getSelectedLabel() {
	
		return labels[selected];
	}
	
	
	
	@Override
	public Dimension getWidgetSize() {
		
		return new Dimension(getWidth(),IMAGE_HEIGHT+LINE_HEIGHT);
	}
	
	
	
	private int getImage(int x,int y) {
		
		for (int i=0; i<5; i++) {
			if ((y>0) && (y<IMAGE_HEIGHT)) {
				if ((x>i*(IMAGE_WIDTH+5)) && (x<(i+1)*(IMAGE_WIDTH+5))) {
					return i;				
				}
			}
		}
		
		return -1;
	}
}
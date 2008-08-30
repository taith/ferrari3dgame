//--------------------------------------------------------------------------------
// Ferrari3D
// MenuTable
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.*;
import java.util.*;



public class MenuTable extends MenuWidget {
	
	private String[] th;
	private java.util.List<String[]> td;
	private int[] sizes;
	private int maxrows;
		
	
	
	public MenuTable(String[] th,int[] sizes,int maxrows) {
	
		super();
		
		this.th=th;		
		this.td=Collections.synchronizedList(new ArrayList<String[]>());
		this.sizes=sizes;
		this.maxrows=maxrows;
	}
	
	
	
	public void paintWidget(Graphics2D g2) {
		
		// Calculate column layout
		
		int[] colSizes=new int[sizes.length];
		int x=10;
		for (int i=0; i<colSizes.length; i++) {
			colSizes[i]=getWidth()*sizes[i]/100;
		}
		
		// Table headers
		
		paintBackgroundGradient(g2,0,0,getWidth(),LINE_HEIGHT_SMALL);
		g2.setColor(STROKE_COLOR);
		g2.setStroke(STROKE);
		g2.drawRoundRect(0,0,getWidth()-1,LINE_HEIGHT_SMALL-1,ARC,ARC);
		
		g2.setFont(FONT);
		g2.setColor(FONT_COLOR);
		for (int i=0; i<th.length; i++) {
			g2.drawString(th[i],x,LINE_HEIGHT_SMALL-BASELINE/2);
			x+=colSizes[i];
		}
		
		// Table rows
		
		synchronized (td) {
			for (int i=0; i<td.size(); i++) {			
				x=10;
				int y=(i+1)*LINE_HEIGHT_SMALL;	
				String[] data=td.get(i);
				for (int j=0; j<data.length; j++) {
					g2.drawString(data[j],x,y+LINE_HEIGHT_SMALL-BASELINE/2);				
					x+=colSizes[j];
				}
			}
		}
	}
	
	
	
	public synchronized void addRow(String... data) {
		
		if ((data==null) || (data.length!=th.length)) {
			throw new IllegalArgumentException("Invalid number of row cells");
		}
			
		td.add(data);		
		this.repaint();
	}
	
	
	
	public synchronized void removeRow(String data) {
	
		for (String[] i : td) {
			if ((i.length>0) && (data!=null)) {
				if (i[0].equals(data)) {
					td.remove(i);
					this.repaint();
				}
			}
		}
	}
	
	
	
	public int getNumRows() {
	
		return td.size();
	}
	
	
	
	public void clear() {
	
		td.clear();		
		this.repaint();
	}
	
	
	
	public Dimension getWidgetSize() {
	
		return new Dimension(getWidth(),LINE_HEIGHT_SMALL*(maxrows+1));
	}
}
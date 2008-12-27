//--------------------------------------------------------------------------------
// Ferrari3D
// MenuTable
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollBar;



public class MenuTable extends MenuWidget {
	
	private String[] th;
	private List<String[]> td;
	private int[] sizes;
	private int maxrows;
	
	private JScrollBar scrollbar;
	private int startIndex;
		
	
	
	public MenuTable(String[] th,int[] sizes,int maxrows) {
	
		super();
		
		this.th=th;		
		this.td=Collections.synchronizedList(new ArrayList<String[]>());
		this.sizes=sizes;
		this.maxrows=maxrows;
	}
	
	
	
	@Override
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
				String[] data=getRowData(i+startIndex);
				if (data!=null) {
					for (int j=0; j<data.length; j++) {
						g2.drawString(data[j],x,y+LINE_HEIGHT_SMALL-BASELINE/2);				
						x+=colSizes[j];
					}
				}
			}
		}
		
		// Scrollbar
		
		if (scrollbar!=null) {
			Dimension d=scrollbar.getPreferredSize();
			scrollbar.setBounds(getWidth()-d.width,LINE_HEIGHT_SMALL,d.width,getHeight()-LINE_HEIGHT_SMALL);
		}
	}
	
	
	
	public synchronized void addRow(String... data) {
		
		if ((data==null) || (data.length!=th.length)) {
			throw new IllegalArgumentException("Invalid number of row cells");
		}
			
		td.add(data);	
		
		if (td.size()>maxrows) {
			if (scrollbar==null) {
				scrollbar=new JScrollBar(JScrollBar.VERTICAL);
				scrollbar.addAdjustmentListener(new AdjustmentListener() {
					public void adjustmentValueChanged(AdjustmentEvent e) {
						setStartIndex(scrollbar.getValue());
					}					
				});
				add(scrollbar);
			}
			scrollbar.setMinimum(0);
			scrollbar.setMaximum(td.size());
			scrollbar.setValue(startIndex);
		}
		
		repaint();
	}
	
	
	
	public synchronized void removeRow(String data) {
	
		for (String[] i : td) {
			if ((i.length>0) && (data!=null)) {
				if (i[0].equals(data)) {
					td.remove(i);
					repaint();
				}
			}
		}
	}
	
	
	
	public int getNumRows() {
	
		return td.size();
	}
	
	
	
	private String[] getRowData(int index) {
		
		if ((index>=0) && (index<td.size())) {
			return td.get(index);
		} else {
			return null;
		}
	}
	
	
	
	public void clear() {
	
		td.clear();		
		repaint();
	}
	
	
	
	private void setStartIndex(int startIndex) {
		
		this.startIndex=startIndex;
		repaint();
	}
	
	

	@Override
	public Dimension getWidgetSize() {
	
		return new Dimension(getWidth(),LINE_HEIGHT_SMALL*(maxrows+1));
	}
}
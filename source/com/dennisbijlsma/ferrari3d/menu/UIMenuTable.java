//-------------------------------------------------------------------------------- 
// Ferrari3D
// UIMenuTable
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget that displays data as a number of rows and columns. Column widths are
 * passed as a percentage.
 */

public class UIMenuTable extends UIMenuWidget {
	
	private int width;
	private int height;
	
	private String[] headers;
	private int[] sizes;
	private int columns;
	private List<String[]> data;

	/**
	 * Creates a new {@code UIMenuTable} at the specified location.
	 * @param headers An array of column names.
	 * @param sizes An array of percentages for each column.
	 * @throws IllegalArgumentException when both arrays are of different length.
	 */
	
	public UIMenuTable(int width,int height,String[] headers,int[] sizes) {
		
		super(width,height,false);
		
		if ((headers.length!=sizes.length) || (headers.length==0)) {
			throw new IllegalArgumentException("Invalid table headers");
		}
		
		this.width=width;
		this.height=height;
		this.headers=headers;
		this.sizes=sizes;
		columns=headers.length;
		data=new ArrayList<String[]>();
		
		repaintImages();
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	@Override
	protected void paintImage(BufferedImage image) {
		
		Graphics2D g2=getGraphics(image);
		clearGraphics(image,g2);
		
		// Calculate column widths
		
		int[] colSizes=new int[sizes.length];
		int x=10;
		for (int i=0; i<colSizes.length; i++) {
			colSizes[i]=width*sizes[i]/100;
		}
		
		// Table headers
		
		paintBackgroundGradient(g2,0,0,width,LINE_HEIGHT_SMALL);
		g2.setColor(FONT_COLOR);
		paintBackgroundStroke(g2,0,0,width-1,LINE_HEIGHT_SMALL-1);
		g2.setStroke(STROKE);		
		g2.setFont(FONT);
		for (int i=0; i<headers.length; i++) {
			g2.drawString(headers[i],x,LINE_HEIGHT_SMALL-BASELINE/2);
			x+=colSizes[i];
		}
		
		// Rows
		
		for (int i=0; i<data.size(); i++) {			
			x=10;
			int y=(i+1)*LINE_HEIGHT_SMALL;	
			String[] row=data.get(i);
			for (int j=0; j<row.length; j++) {
				g2.drawString(""+row[j],x,y+LINE_HEIGHT_SMALL-BASELINE/2);				
				x+=colSizes[j];
			}
		}
		
		g2.dispose();
	}
	
	/**
	 * Adds a row to the table, containing the specified column values. The table
	 * will be repainted after adding the data.
	 * @throws IllegalArgumentException when the number of columns is incorrect.
	 */
	
	public void addRow(String... rowdata) {
	
		if ((rowdata==null) || (rowdata.length!=columns)) {
			throw new IllegalArgumentException("Invalid number of columns: "+rowdata.length);
		}
		
		data.add(rowdata);
		repaintImages();
	}
	
	/**
	 * {@inheritDoc}
	 */

	@Override
	public Dimension getHitArea() {
		
		return new Dimension(width,height);
	}
}
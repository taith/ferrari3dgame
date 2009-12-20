//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuTable
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget that displays data as a number of rows and columns. Column widths are
 * passed as a percentage.
 */

public class UIMenuTable extends UIMenuWidget {
	
	private String[] headers;
	private List<String[]> data;
	private int columns;
	private int maxRows;
	private int[] sizes;

	/**
	 * Creates a new {@code UIMenuTable} at the specified location.
	 * @param headers An array of column names.
	 * @param sizes An array of percentages for each column.
	 * @throws IllegalArgumentException when both arrays are of different length.
	 */
	
	public UIMenuTable(int height, String[] headers, int[] sizes) {
		
		super(WIDGET_TEXTURE_WIDTH, height, false);
		
		if ((headers.length != sizes.length) || (headers.length == 0)) {
			throw new IllegalArgumentException("Invalid table headers");
		}

		this.headers = headers;
		this.sizes = sizes;
		columns = headers.length;
		data = new ArrayList<String[]>();
		maxRows = 10;
		
		repaint();
	}
	
	/** {@inheritDoc} */
	
	@Override
	protected void paintImage(Graphics2D g2) {
		
		// Calculate column widths
		
		int[] colSizes = new int[sizes.length];
		int x = 10;
		for (int i = 0; i < colSizes.length; i++) {
			colSizes[i] = WIDGET_WIDTH * sizes[i] / 100;
		}
		
		// Table headers
		
		paintBackgroundGradient(g2, 0, 0, WIDGET_WIDTH, LINE_HEIGHT_SMALL);
		g2.setColor(FONT_COLOR);
		paintBackgroundStroke(g2, 0, 0, WIDGET_WIDTH - 1, LINE_HEIGHT_SMALL - 1);
		g2.setStroke(STROKE);		
		g2.setFont(FONT);
		for (int i=0; i<headers.length; i++) {
			g2.drawString(headers[i], x, LINE_HEIGHT_SMALL - 6);
			x += colSizes[i];
		}
		
		// Rows
		
		for (int i = 0; i < Math.min(data.size(), maxRows); i++) {			
			x = 10;
			int y = (i + 1) * LINE_HEIGHT_SMALL;	
			String[] row = data.get(i);
			for (int j = 0; j < row.length; j++) {
				g2.drawString("" + row[j], x, y + LINE_HEIGHT_SMALL-BASELINE / 2);				
				x += colSizes[j];
			}
		}
	}
	
	/**
	 * Adds a row to the table, containing the specified column values. Note that 
	 * this method does <i>not</i> repaint the table automatically.
	 * @throws IllegalArgumentException when the number of columns is incorrect.
	 */
	
	public void addRow(String... rowdata) {
		if (rowdata.length != columns) {
			throw new IllegalArgumentException("Invalid number of columns: " + rowdata.length);
		}
		data.add(rowdata);
	}
	
	/**
	 * Removes al previously painted rows from this table. Note that this method
	 * does <i>not</i> automatically repaint the table.
	 */
	
	public void clearRows() {
		data.clear();
	}
	
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}
	
	public int getMaxRows() {
		return maxRows;
	}
	
	@Override
	public Dimension getHitArea() {
		Dimension d = super.getHitArea();
		d.height = LINE_HEIGHT_SMALL * (maxRows + 1);
		return d;
	}
}

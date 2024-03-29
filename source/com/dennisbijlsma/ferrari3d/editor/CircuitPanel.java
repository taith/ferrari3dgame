//-----------------------------------------------------------------------------
// Ferrari3D
// CircuitPanel
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;

import com.dennisbijlsma.core3d.Color3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.Popups;

/**
 * Components that allows editing of a model's circuit data. The data contains
 * of a number of points, track camera's and starting positions. These can be
 * edited by using the arrow keys.
 */
public class CircuitPanel extends FormPanel {
	
	private ContentFile file;
	private Preview preview;
	private int mode;
	
	private JComboBox modesList;
	private JComboBox itemsList;
	private JButton editButton;
	
	protected static final int MODE_POINTS = 0;
	protected static final int MODE_GRID = 1;
	protected static final int MODE_CAMERAS = 2;
	
	/**
	 * Creates a new panel that uses the specified preview window to display its
	 * data.
	 */
	public CircuitPanel(Preview preview) {
		
		super();
		
		this.file = null;
		this.preview = preview;
		
		mode = MODE_POINTS;
		
		// Create GUI
		
		Settings settings = Settings.getInstance();
		
		modesList = new JComboBox(new String[]{"Points", "Grid positions", "Cameras"});
		modesList.setPreferredSize(new Dimension(210, 25));
		modesList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setMode(modesList.getSelectedIndex());
			}
		});

		addRow(settings.getText("editor.circuit.selected"), modesList);
				
		itemsList = new JComboBox(new String[]{settings.getText("editor.circuit.notavailable")});
		itemsList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshPreview();
			}
		});
				
		editButton = new JButton(settings.getText("editor.circuit.editbutton"));
		editButton.setToolTipText(settings.getText("editor.tooltip.circuitpanel"));
		editButton.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				edit(e);
			}
		});
		
		addRow(itemsList, editButton);
	}
	
	private void add() {
		
		if (mode == MODE_POINTS) { file.points.add(new CircuitPoint()); }
		if (mode == MODE_GRID) { file.startgrid.add(new Vector3D()); }
		if (mode == MODE_CAMERAS) { file.circuitCameras.add(new Vector3D()); }
				
		setMode(getMode());
		refreshPreview();
	}
	
	private void remove() {
		
		switch (mode) {
			case MODE_POINTS : file.points.remove(getSelectedPoint()); break;
			case MODE_GRID : file.startgrid.remove(getSelectedGrid()); break;
			case MODE_CAMERAS : file.circuitCameras.remove(getSelectedCamera()); break;
			default : break;
		}
				
		setMode(getMode());
		refreshPreview();
	}
	
	private void edit(KeyEvent e) {
		
		// Change selection
		
		int index = itemsList.getSelectedIndex();
		
		if (e.getKeyCode() == KeyEvent.VK_COMMA) { 
			itemsList.setSelectedIndex(index - 1);
			refreshPreview();
			return;
		}
		
		if (e.getKeyCode() == KeyEvent.VK_PERIOD) { 
			itemsList.setSelectedIndex(index + 1);
			refreshPreview();
			return;
		}
		
		// Change position
				
		Vector3D delta = new Vector3D();
		if (e.getKeyCode() == KeyEvent.VK_LEFT) { delta.setX(0.1f); }
		if (e.getKeyCode() == KeyEvent.VK_RIGHT) { delta.setX(-0.1f); }
		if (e.getKeyCode() == KeyEvent.VK_UP) { delta.setZ(0.1f); }
		if (e.getKeyCode() == KeyEvent.VK_DOWN) { delta.setZ(-0.1f); }
		if (e.isShiftDown()) { delta.mul(10f, 1f, 1f); delta.mul(1f, 1f, 10f); }
		
		// Edit selection
	
		switch (mode) {
			case MODE_POINTS : editPoint(index,getSelectedPoint(), delta, e); break;
			case MODE_GRID : editGrid(index,getSelectedGrid(), delta); break;
			case MODE_CAMERAS : editCamera(index,getSelectedCamera(), delta); break;
			default : break;
		}
	}
	
	private void editPoint(int index, CircuitPoint p, Vector3D delta, KeyEvent e) {

		if (p == null) {
			return;
		}
		
		p.pointX += delta.getX();
		p.pointY += delta.getZ();
		
		if (e.getKeyCode() == KeyEvent.VK_I) {
			p.setIntermediate(!p.isIntermediate());
		}
		
		if (e.getKeyCode() == KeyEvent.VK_E) {
			String label = Settings.getInstance().getText("editor.circuit.editpopup");
			String temp = Popups.inputMessage(null, label, "" + p.getSuggestedSpeed());
			try {
				p.setSuggestedSpeed(Float.parseFloat(temp));
			} catch (Exception ex) {
				// Do nothing
			}
		}
		
		preview.setCircuitPoint(index, p);
		refreshPreview();
	}
	
	private void editGrid(int index, Vector3D p, Vector3D delta) {
				
		if (p != null) {
			p.add(delta.getX(), 0f, 0f);
			p.add(0f, 0f, delta.getZ());
			
			preview.setCircuitGrid(index, p);
			refreshPreview();
		}
	}
	
	private void editCamera(int index, Vector3D p, Vector3D delta) {
		if (p!=null) {
			p.add(delta.getX(), 0f, delta.getZ());
			preview.setCircuitCamera(index, p);
			refreshPreview();
		}
	}
	
	/**
	 * Refreshes the preview with the current state of the file.
	 */
	private void refreshPreview() {
		
		float pointerX = 0f;
		float pointerY = 0f;
		Color3D pointerColor = Color3D.WHITE;
		
		if ((mode == MODE_POINTS) && (getSelectedPoint() != null)) {
			pointerX = getSelectedPoint().pointX;
			pointerY = getSelectedPoint().pointY;
			pointerColor = Color3D.RED;
		} else if ((mode == MODE_GRID) && (getSelectedGrid() != null)) {
			pointerX = getSelectedGrid().getX();
			pointerY = getSelectedGrid().getZ();
			pointerColor=new Color3D(150, 150, 150);
		} else if ((mode == MODE_CAMERAS) && (getSelectedCamera() != null)) {
			pointerX = getSelectedCamera().getX();
			pointerY = getSelectedCamera().getZ();
			pointerColor=new Color3D(50, 50, 50);
		}
		
		preview.setPointer(new Vector3D(pointerX, 0f, pointerY), pointerColor);
	}
	
	public void setFile(ContentFile file) {
		this.file = file;
		setMode(mode);		
	}
	
	public ContentFile getFile() {	
		return file;
	}
	
	private void setMode(int mode) {
		
		this.mode = mode;
		
		itemsList.removeAllItems();
		
		if (file == null) {
			return;
		}
		
		String itemName = "";
		int numItems = 0;		
		if (mode == MODE_POINTS) { itemName = "Point"; numItems = file.points.size(); }
		if (mode == MODE_GRID) { itemName = "Gridpos"; numItems = file.startgrid.size(); }
		if (mode == MODE_CAMERAS) { itemName = "Camera"; numItems = file.circuitCameras.size(); }
		
		for (int i=0; i<numItems; i++) {
			itemsList.addItem(itemName + " " + i);
		}
		
		if (numItems > 0) {
			itemsList.setSelectedIndex(0);
		}
	}
	
	private int getMode() {
		return mode;
	}
	
	private CircuitPoint getSelectedPoint() {
		
		int index = itemsList.getSelectedIndex();
		
		if ((index < 0) || (index >= file.points.size())) {
			return null;
		} else {		
			return file.points.get(index);
		}
	}
	
	private Vector3D getSelectedGrid() {
		
		int index = itemsList.getSelectedIndex();
		
		if ((index < 0) || (index >= file.startgrid.size())) {
			return null;
		} else {
			return file.startgrid.get(index);
		}
	}
	
	private Vector3D getSelectedCamera() {
		
		int index = itemsList.getSelectedIndex();
		
		if ((index < 0) || (index >= file.circuitCameras.size())) {
			return null;
		} else {
			return file.circuitCameras.get(index);
		}
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		modesList.setEnabled(enabled);
		itemsList.setEnabled(enabled);
		editButton.setEnabled(enabled);
	}
}

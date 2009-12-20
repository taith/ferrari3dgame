//-----------------------------------------------------------------------------
// Ferrari3D
// Preview
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.dennisbijlsma.core3d.Color3D;
import com.dennisbijlsma.core3d.Controller;
import com.dennisbijlsma.core3d.SwingGame;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.LightSource;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;

/**
 * Component that previews the model currently open in the editor.
 */

public class Preview extends JPanel {
	
	private ContentFile file;
	private SceneGraphNode root;
	private SceneGraphNode rootNode;
	private boolean changed;
	
	private Primitive[] points;
	private Primitive[] grid;
	private Primitive[] cameras;
	private Primitive pointer;
	private Vector3D pointerPos;
	private Color3D pointerColor;
	private float viewAngleX;
	private float viewAngleY;
	private boolean spinning;
	
	private static final float SCROLL_SPEED = 0.1f;
	private static final float SPIN_SPEED = 0.002f;
	private static final Vector3D POINT_SIZE = new Vector3D(0.1f, 0.1f, 0.1f);
	
	/**
	 * Creates a new {@code Preview}. The 3D system will not be created until 
	 * {@link #init()} is called.
	 */
	
	public Preview() {
	
		super(new BorderLayout());
		
		setOpaque(true);
		setBackground(java.awt.Color.BLACK);
		
		pointer = null;
		pointerPos = new Vector3D();
		pointerColor = Color3D.RED;
		viewAngleX = 0f;
		viewAngleY = 0f;
		spinning = false;
		changed = false;
		
		// Create view options GUI
		
		Settings settings = Settings.getInstance();
		
		final JSlider sViewAngleX=new JSlider(JSlider.HORIZONTAL, 0, 628, 0);
		sViewAngleX.setPreferredSize(new Dimension(150, sViewAngleX.getPreferredSize().height));
		sViewAngleX.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				viewAngleX = sViewAngleX.getValue() / 100f;
			}
		});
		
		final JSlider sViewAngleY=new JSlider(JSlider.HORIZONTAL, 0, 628, 0);
		sViewAngleY.setPreferredSize(new Dimension(150, sViewAngleY.getPreferredSize().height));
		sViewAngleY.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				viewAngleY = sViewAngleY.getValue() / 100f;
			}
		});
		
		final JCheckBox cSpin = new JCheckBox(settings.getText("editor.preview.spin") + "          ");
		cSpin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSpinning();
			}
		});
		
		JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		sliderPanel.add(new JLabel(settings.getText("editor.preview.viewangle") + "    "));
		sliderPanel.add(new JLabel("X:"));
		sliderPanel.add(sViewAngleX);
		sliderPanel.add(new JLabel("Y:"));
		sliderPanel.add(sViewAngleY);
		
		JPanel viewOptionsPanel = new JPanel(new BorderLayout());
		viewOptionsPanel.setOpaque(true);
		viewOptionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		viewOptionsPanel.add(sliderPanel, BorderLayout.WEST);
		viewOptionsPanel.add(cSpin, BorderLayout.EAST);
		add(viewOptionsPanel, BorderLayout.SOUTH);
	}
	
	/**
	 * Initializes the 3D system used by this preview.
	 */
	
	protected void init() {
		
		final SwingGame game = new SwingGame(500, 500, 25) {
			public void initGameState() {
				root = new SceneGraphNode("__preview");
				getSceneGraph().getRootNode().addChild(root);	
				
				Camera camera = new Camera(getDisplay());			
				camera.aim(new Vector3D(0f, 10f, -100f), new Vector3D());
				getSceneGraph().addCamera(camera);
				
				LightSource light = LightSource.createDirectionalLight(new Vector3D(0f, -1f, 0.5f));
				getSceneGraph().addLight(light);
			}
						
			public void updateGameState(float dt) {			
				if (rootNode != null) {
					updateControls(getController(), getSceneGraph().getCamera(0));
					
					rootNode.getTransform().getRotation().x = viewAngleX;
					rootNode.getTransform().getRotation().y = viewAngleY;
					if (spinning) { 
						rootNode.getTransform().getRotation().y += SPIN_SPEED; 
					}
					
					if (changed) { 
						setCircuitPoints();
						setCircuitGrid();
						setCircuitCameras();
						changed = false; 
					}										
					
					updatePointer();
				}
			}
		};

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JPanel viewport = game.getDisplay();
				add(viewport, BorderLayout.CENTER);
				revalidate();
			}
		});
	}
	
	private void updateControls(Controller controller, Camera camera) {
				
		float delta = SCROLL_SPEED;
		if (controller.isKeyPressed(Controller.KEY_SHIFT)) { 
			delta *= 10f; 
		}
		
		if ((controller.isMouseButton(0)) && (controller.getMouseDeltaX() != 0f)) {
			camera.move(new Vector3D(delta * controller.getMouseDeltaX(), 0f, 0f), true);
		}
		
		if ((controller.isMouseButton(0)) && (controller.getMouseDeltaY() != 0f)) {
			camera.move(new Vector3D(0f, delta * -controller.getMouseDeltaY(), 0f), true);
		}
		
		if (controller.getMouseWheel() != 0) {
			camera.move(new Vector3D(0f, 0f, delta * controller.getMouseWheel()), true);
		}
	}
	
	private void updatePointer() {
		if (pointer != null) {
			pointer.getTransform().setPosition(pointerPos);
			pointer.applyColor(pointerColor);
		}
	}
	
	public void setFile(ContentFile file) {
		
		this.file = file;
			
		if (rootNode != null) {
			root.clearChildren();
			rootNode = null;
		}
		
		if (file == null) {
			return;
		}
		
		rootNode = file.getNode();
		root.addChild(rootNode);
		
		changed = true;
	}
	
	public ContentFile getFile() {
		return file;
	}
	
	protected void setCircuitPoints() {
		
		points = new Primitive[file.points.size()];
		for (int i = 0; i < points.length; i++) {
			CircuitPoint p = file.points.get(i);
			points[i] = Primitive.createBox(0.1f, 0.1f, 0.1f);
			points[i].setName("__point_" + i);
			points[i].getTransform().setPosition(p.pointX, 0f, p.pointY);
			points[i].applyColor(getPointColor(p));
			rootNode.addChild(points[i]);
		}
			
		pointer = Primitive.createBox(0.2f, 0.2f, 0.2f);
		pointer.setName("__pointer");
		pointer.applyColor(Color3D.WHITE);
		rootNode.addChild(pointer);
	}
	
	protected void setCircuitPoint(int index, CircuitPoint p) {
		Primitive node = (Primitive) rootNode.getChild("__point_" + index);
		if (node != null) {
			node.getTransform().setPosition(p.pointX, 0f, p.pointY);
			node.applyColor(getPointColor(p));
		}
	}
	
	protected void setCircuitGrid() {
		grid = new Primitive[file.startgrid.size()];
		for (int i = 0; i < grid.length; i++) {
			grid[i] = Primitive.createBox(0.1f, 0.1f, 0.1f);
			grid[i].setName("__gridpos_" + i);
			grid[i].getTransform().setPosition(file.startgrid.get(i));
			grid[i].applyColor(new Color3D(150, 150, 150));
			rootNode.addChild(grid[i]);
		}
	}
	
	protected void setCircuitGrid(int index, Vector3D p) {
		SceneGraphNode node = rootNode.getChild("__gridpos_" + index);
		if (node != null) {
			node.getTransform().setPosition(p);
		}
	}
	
	protected void setCircuitCameras() {
		cameras = new Primitive[file.circuitCameras.size()];
		for (int i = 0; i < cameras.length; i++) {
			cameras[i] = Primitive.createBox(0.1f, 0.1f, 0.1f);
			cameras[i].setName("__ccam_" + i);
			cameras[i].getTransform().setPosition(file.circuitCameras.get(i));
			cameras[i].applyColor(new Color3D(50, 50, 50));
			rootNode.addChild(cameras[i]);
		}
	}
	
	protected void setCircuitCamera(int index, Vector3D p) {
		SceneGraphNode node = rootNode.getChild("__ccam_" + index);
		if (node != null) {
			node.getTransform().setPosition(p);
		}
	}
	
	public void setPointer(Vector3D pointerPos, Color3D pointerColor) {
		this.pointerPos = pointerPos;
		this.pointerColor = pointerColor;
	}
	
	
	
	private void setSpinning() {
		if ((spinning) || (rootNode == null)) {
			spinning = false;
		} else {
			spinning = true;
			rootNode.getTransform().getRotation().y = 0f;
		}	
	}
	
	private void setWireframe() {	
		for (SceneGraphNode i : rootNode.getChildren()) {
			if (i instanceof Model) {
				((Model) i).applyColor(new Color3D(0, 0, 200));
				((Model) i).applyWireframe(1f);
			}
		}
	}
	
	private Color3D getPointColor(CircuitPoint p) {
		return p.isSuggestedSpeed() ? Color3D.CYAN : Color3D.RED;
	}
}

//--------------------------------------------------------------------------------
// Ferrari3D
// Preview
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.game.InputController;
import com.dennisbijlsma.core3d.game.SwingGame;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.LightSource;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraph;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;
import com.dennisbijlsma.ferrari3d.util.Settings;



public class Preview extends JPanel {
	
	private F3dFile file;
	private SceneGraphNode root;
	private SceneGraphNode rootNode;
	private boolean changed;
	
	private Primitive[] points;
	private Primitive[] grid;
	private Primitive[] cameras;
	private Primitive pointer;
	private Vector3D pointerPos;
	private Color pointerColor;
	private float viewAngle;
	private boolean spinning;
	
	private static final int VIEWPORT_WIDTH=580;
	private static final int VIEWPORT_HEIGHT=560;
	private static final float SCROLL_SPEED=0.1f;
	private static final float SPIN_SPEED=0.002f;
	private static final Vector3D POINT_SIZE=new Vector3D(0.1f,0.1f,0.1f);
	
	
	
	public Preview() {
	
		super(new BorderLayout());
		
		setOpaque(true);
		
		pointer=null;
		pointerPos=new Vector3D();
		pointerColor=Color.RED;
		viewAngle=0f;
		spinning=false;

		changed=false;
		
		// Create view options GUI
		
		Settings settings=Settings.getInstance();
		
		final JSlider sViewAngle=new JSlider(JSlider.HORIZONTAL,0,628,0);
		sViewAngle.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				viewAngle=sViewAngle.getValue()/100f;
			}
		});
		
		final JCheckBox cSpin=new JCheckBox(settings.getText("editor.preview.spin")+"          ");
		cSpin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSpinning();
			}
		});
		
		JPanel sliderPanel=new JPanel(new BorderLayout(10,0));
		sliderPanel.add(new JLabel(settings.getText("editor.preview.viewangle")),BorderLayout.WEST);
		sliderPanel.add(sViewAngle,BorderLayout.CENTER);
		
		JPanel viewOptionsPanel=new JPanel(new BorderLayout());
		viewOptionsPanel.setOpaque(true);
		viewOptionsPanel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
		viewOptionsPanel.add(sliderPanel,BorderLayout.WEST);
		viewOptionsPanel.add(cSpin,BorderLayout.EAST);
		this.add(viewOptionsPanel,BorderLayout.SOUTH);
	}
	
	
	
	protected void init() {
		
		SceneGraph scene=new SceneGraph();
		InputController controller=new InputController();
	
		SwingGame game=new SwingGame(scene,controller,VIEWPORT_WIDTH,VIEWPORT_HEIGHT) {
			protected void initGame() {
				Camera camera=new Camera(getViewport());				
				getSceneGraph().addCamera(camera);
				camera.aim(new Vector3D(0f,10f,-50f),new Vector3D(0f,0f,0f));
				
				LightSource light=new LightSource(LightSource.Type.DIRECTIONAL,new Vector3D(0f,-1f,-0.2f));
				getSceneGraph().addLight(light);
				
				root=new SceneGraphNode("__preview");
				getSceneGraph().getRootNode().addChild(root);	
			}
						
			protected void updateGame(float dt) {			
				if (rootNode!=null) {
					updateControls(getController(),getSceneGraph().getCamera());
					
					rootNode.getTransform().getRotation().x=viewAngle;
					
					if (spinning) { 
						rootNode.getTransform().getRotation().y+=SPIN_SPEED; 
					}
					
					if (changed) { 
						setCircuitPoints();
						setCircuitGrid();
						setCircuitCameras();
						changed=false; 
					}										
					
					updatePointer();
				}
			}
		};
		game.startGame();

		JComponent viewport=game.getViewportComponent();
		viewport.setPreferredSize(new Dimension(VIEWPORT_WIDTH,VIEWPORT_HEIGHT));
		viewport.setBackground(Color.BLACK);
		viewport.setBorder(BorderFactory.createLineBorder(Color.BLACK,1));
		this.add(viewport,BorderLayout.CENTER);
		revalidate();
	}
	
	
	
	private void updateControls(InputController controller,Camera camera) {
				
		float delta=SCROLL_SPEED;
		if (controller.isKeyPressed(InputController.KEY_LSHIFT)) { delta*=10f; }
		if (controller.isKeyPressed(InputController.KEY_RSHIFT)) { delta*=10f; }
		
		if ((controller.isMouseButton(InputController.MOUSE_BUTTON_LEFT)) && (controller.getMouseDeltaX()!=0f)) {
			camera.move(new Vector3D(delta*controller.getMouseDeltaX(),0f,0f),true);
		}
		
		if ((controller.isMouseButton(InputController.MOUSE_BUTTON_LEFT)) && (controller.getMouseDeltaY()!=0f)) {
			camera.move(new Vector3D(0f,delta*controller.getMouseDeltaY(),0f),true);
		}
		
		if (controller.getMouseWheel()!=0) {
			camera.move(new Vector3D(0f,0f,delta*controller.getMouseWheel()),true);
		}
	}
	
	
	
	private void updatePointer() {
	
		if (pointer!=null) {
			pointer.getTransform().setPosition(pointerPos);
			pointer.applyColor(pointerColor);
		}
	}
	
	
	
	public void setFile(F3dFile file) {
		
		this.file=file;
			
		if (rootNode!=null) {
			root.removeChildren();
			rootNode=null;
		}
		
		if (file==null) {
			return;
		}
		
		rootNode=file.getNode();
		root.addChild(rootNode);
		
		changed=true;
	}
	
	
	
	public F3dFile getFile() { 
	
		return file;
	}
	
	
	
	protected void setCircuitPoints() {
		
		points=new Primitive[file.points.size()];
		for (int i=0; i<points.length; i++) {
			CircuitPoint p=file.points.get(i);
			points[i]=new Primitive("__point_"+i,Primitive.ShapeType.BOX,new Vector3D(.1f,.1f,.1f));
			points[i].getTransform().setPosition(p.pointX,0f,p.pointY);
			points[i].applyColor(getPointColor(p));
			rootNode.addChild(points[i]);
		}
			
		pointer=new Primitive("__pointer",Primitive.ShapeType.BOX,new Vector3D(.2f,.2f,.2f));
		pointer.applyColor(Color.WHITE);
		rootNode.addChild(pointer);
	}
	
	
	
	protected void setCircuitPoint(int index,CircuitPoint p) {
	
		Primitive node=(Primitive) rootNode.getChild("__point_"+index);
		if (node!=null) {
			node.getTransform().setPosition(p.pointX,0f,p.pointY);
			node.applyColor(getPointColor(p));
		}
	}
	
	
	
	protected void setCircuitGrid() {
	
		grid=new Primitive[file.startgrid.size()];
		for (int i=0; i<grid.length; i++) {
			grid[i]=new Primitive("__gridpos_"+i,Primitive.ShapeType.BOX,new Vector3D(.1f,.1f,.1f));
			grid[i].getTransform().setPosition(file.startgrid.get(i));
			grid[i].applyColor(new Color(150,150,150));
			rootNode.addChild(grid[i]);
		}
	}
	
	
	
	protected void setCircuitGrid(int index,Vector3D p) {
	
		SceneGraphNode node=rootNode.getChild("__gridpos_"+index);
		if (node!=null) {
			node.getTransform().setPosition(p);
		}
	}
	
	
	
	protected void setCircuitCameras() {
		
		cameras=new Primitive[file.circuitCameras.size()];
		for (int i=0; i<cameras.length; i++) {
			cameras[i]=new Primitive("__ccam_"+i,Primitive.ShapeType.BOX,new Vector3D(.1f,.1f,.1f));
			cameras[i].getTransform().setPosition(file.circuitCameras.get(i));
			cameras[i].applyColor(new Color(50,50,50));
			rootNode.addChild(cameras[i]);
		}
	}
	
	
	
	protected void setCircuitCamera(int index,Vector3D p) {
	
		SceneGraphNode node=rootNode.getChild("__ccam_"+index);
		if (node!=null) {
			node.getTransform().setPosition(p);
		}
	}
	
	
	
	public void setPointer(Vector3D pointerPos,Color pointerColor) {
		
		this.pointerPos=pointerPos;
		this.pointerColor=pointerColor;
	}
	
	
	
	private void setSpinning() {
		
		if ((spinning) || (rootNode==null)) {
			spinning=false;
		} else {
			spinning=true;
			rootNode.getTransform().getRotation().y=0f;
		}	
	}
	
	
	
	private void setWireframe() {
		
		for (SceneGraphNode i : rootNode.getChildren()) {
			if (i instanceof Model) {
				((Model) i).applyColor(new Color(0,0,200));
				((Model) i).applyWireframe(1f);
			}
		}
	}
	
	
	
	private Color getPointColor(CircuitPoint p) {
		
		return p.isSuggestedSpeed() ? Color.CYAN : Color.RED;
	}
}
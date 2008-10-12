//--------------------------------------------------------------------------------
// Ferrari3D
// EditorGUI
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import com.dennisbijlsma.core3d.data.*;
import com.dennisbijlsma.core3d.scene.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;
import com.dennisbijlsma.util.swing.mac.*;



public class EditorGUI extends FormPanel {
	
	private F3dFile file;
	private Preview preview;
	
	private SceneGraphNode selectedNode;
	private JButton addNodeButton;
	private JButton removeNodeButton;
	
	private JLabel titleLabel;
	private JList nodesList;
	
	private JLabel nodeLabel;
	private JComboBox tNodeType;
	private JTextField[] tNodeTransform;
	private JButton transformButton;
	
	private CircuitPanel circuitPanel;
	private PropertyPanel carInfoList;
		
	
	
	public EditorGUI(Preview preview) {
		
		super(240,-1,5,5);
		
		this.file=null;
		this.preview=preview;
		
		Settings settings=Settings.getInstance();
		
		// Create file section
		
		titleLabel=new JLabel(settings.getText("editor.gui.nofileselected"));
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));		
		addRow(titleLabel);
		
		nodesList=new JList(new String[0]);
		nodesList.putClientProperty(OSXHandler.QUAQUA_LIST,"striped");
		nodesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				setSelectedNode(file.getNode().getChild((String) nodesList.getSelectedValue()));
			}
		});
		JScrollPane nodesListPane=new JScrollPane(nodesList);
		nodesListPane.setPreferredSize(new Dimension(230,120));
		nodesListPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		addRow(nodesListPane);
		
		addNodeButton=new JButton(" + ");
		addNodeButton.setToolTipText(settings.getText("editor.gui.addnode"));
		addNodeButton.putClientProperty(OSXHandler.QUAQUA_BUTTON,"square");
		addNodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addChildNode();
			}
		});
		
		removeNodeButton=new JButton(" - ");
		removeNodeButton.setToolTipText(settings.getText("editor.gui.removenode"));
		removeNodeButton.putClientProperty(OSXHandler.QUAQUA_BUTTON,"square");
		removeNodeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeChildNode();
			}
		});
					
		addRow("",createSwitchButtonPanel("",addNodeButton,removeNodeButton));
				
		// Create node section
		
		nodeLabel=new JLabel(settings.getText("editor.gui.nonodeselected"));
		nodeLabel.setFont(nodeLabel.getFont().deriveFont(Font.BOLD));
		addRow(nodeLabel);
				
		tNodeType=new JComboBox(new String[]{"SceneGraphNode","Model","Primitive","Billboard"});
		tNodeType.setEnabled(false);
		addRow(settings.getText("editor.gui.type"),tNodeType);
		
		tNodeTransform=new JTextField[7];
		for (int i=0; i<tNodeTransform.length; i++) {
			tNodeTransform[i]=new JTextField("0");		}
		addRow(settings.getText("editor.gui.position"),createXYZPanel(tNodeTransform[0],tNodeTransform[1],tNodeTransform[2]));
		addRow(settings.getText("editor.gui.rotation"),createXYZPanel(tNodeTransform[3],tNodeTransform[4],tNodeTransform[5]));
		addRow(settings.getText("editor.gui.scale"),tNodeTransform[6]);
		
		transformButton=new JButton(settings.getText("editor.gui.transform"));
		transformButton.setToolTipText(settings.getInstance().getText("editor.gui.transformbutton"));
		transformButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				transformNode();
			}			
		});
		transformButton.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				transformNode(e);
			}
		});
		addRow("",transformButton);
		
		addRow("",new JLabel(""));
		
		// Create data section
		
		JTabbedPane dataPane=new JTabbedPane();
		dataPane.setPreferredSize(new Dimension(230,120));
		addRow(dataPane);
				
		circuitPanel=new CircuitPanel(preview);
		dataPane.add(settings.getText("editor.gui.circuitdata"),circuitPanel);
		
		JPanel carPanel=new JPanel(new BorderLayout());
		dataPane.add(settings.getText("editor.gui.cardata"),carPanel);
		
		carInfoList=new PropertyPanel(new Properties());
		carInfoList.putClientProperty(OSXHandler.QUAQUA_LIST,"striped");
		JScrollPane infoPane=PropertyPanel.createScrollPane(carInfoList,100,100);
		infoPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		carPanel.add(infoPane,BorderLayout.CENTER);
		
		// Refresh GUI
		
		refreshFileGUI();
		refreshNodeGUI();
		refreshDataGUI();
	}
	
	
	
	private void refreshFileGUI() {
			
		if (file!=null) {
			titleLabel.setText(file.getName());
			ArrayList<String> nodeNames=new ArrayList<String>();
			for (SceneGraphNode i : file.getNode().getChildren()) {
				if (!i.getName().startsWith("__")) {
					nodeNames.add(i.getName());
				}
			}
			nodesList.setListData(nodeNames.toArray(new String[0]));
		} else {
			titleLabel.setText(Settings.getInstance().getText("editor.gui.nofileselected"));
			nodesList.setListData(new String[0]);
		}
		
		nodesList.setEnabled(file!=null);
		addNodeButton.setEnabled(file!=null);
		removeNodeButton.setEnabled(file!=null);
	}
	
	
	
	private void refreshNodeGUI() {
				
		if (selectedNode!=null) {
			nodeLabel.setText(selectedNode.getName());
			tNodeType.setSelectedIndex(0);
			if (selectedNode instanceof Model) { tNodeType.setSelectedIndex(1); }
			if (selectedNode instanceof Primitive) { tNodeType.setSelectedIndex(2); }
			if (selectedNode instanceof Billboard) { tNodeType.setSelectedIndex(3); }
			Transform3D transform=new Transform3D(selectedNode.getTransform());
			tNodeTransform[0].setText(""+transform.getPosition().x);
			tNodeTransform[1].setText(""+transform.getPosition().y);
			tNodeTransform[2].setText(""+transform.getPosition().z);
			tNodeTransform[3].setText(""+transform.getRotation().x);
			tNodeTransform[4].setText(""+transform.getRotation().y);
			tNodeTransform[5].setText(""+transform.getRotation().z);
			tNodeTransform[6].setText(""+transform.getScale().x);
		} else {
			nodeLabel.setText(Settings.getInstance().getText("editor.gui.nonodeselected"));
			tNodeType.setSelectedIndex(0);
			for (JTextField i : tNodeTransform) { 
				i.setText("0"); 
			}
		}
		
		for (JTextField i : tNodeTransform) { 
			i.setEnabled(selectedNode!=null); 
		}
		transformButton.setEnabled(selectedNode!=null);
	}
	
	
	
	private void refreshDataGUI() {
		
		circuitPanel.setFile(file);
		circuitPanel.setEnabled(file!=null);
		
		carInfoList.setEnabled(file!=null);		
		carInfoList.setProperties(new Properties());
		
		if (file!=null) {
			carInfoList.setProperties(file.carInfo);
		}
	}
	
	
	
	private void addChildNode() {
		
		// Create node properties dialog
	
		JTextField tName=new JTextField();
		JComboBox tType=new JComboBox(new String[]{"Model","Billboard"});
		FormPanel formPanel=new FormPanel(200,-1,0,5);
		formPanel.setPreferredSize(new Dimension(200,65));
		formPanel.addRow(Settings.getInstance().getText("editor.gui.newnodename"),tName);
		formPanel.addRow(Settings.getInstance().getText("editor.gui.newnodetype"),tType);
		
		String[] buttons={"OK","Cancel"};
		if (PopUp.confirm(null,Settings.getInstance().getText("editor.gui.addnode"),formPanel,buttons)!=0) {
			return;
		}
		
		String nodeName=tName.getText();
		String nodeType=(String) tType.getSelectedItem();
		
		// Create resource file dialog
		
		FileChooser dialog=new FileChooser();
		dialog.setTitle(Settings.getInstance().getText("editor.gui.openmodel"));
		if (nodeType.equals("Model")) { dialog.addFilter("jme,obj,3ds,dae","3D models"); }
		if (nodeType.equals("Billboard")) { dialog.addFilter("png,jpg,gif","Textures"); }		
		File url=dialog.showOpenDialog(null);
		
		if ((nodeName.length()==0) || (file.getNode().getChild(nodeName)!=null) || (file==null) || (!url.exists())) {
			return;		
		}
		
		// Create child node
		
		if (nodeType.equals("Model")) {
			Model model=MediaLoader.loadModel(url);
			model.setName(nodeName);
			file.getNode().addChild(model);
		}
		
		if (nodeType.equals("Billboard")) {
			Image texture=Utils.loadImage(url.getAbsolutePath());
			Primitive quad=new Primitive("billboardQuad",Primitive.ShapeType.QUAD,new Vector3D(1f,1f,0f));
			quad.applyTexture(texture);
			Billboard billboard=new Billboard(nodeName,quad);
			file.getNode().addChild(billboard);
		}
		
		// Refresh
		
		refreshFileGUI();
		refreshNodeGUI();
	}
	
	
	
	private void removeChildNode() {
		
		if (getSelectedNode()==null) {
			PopUp.message(this,Settings.getInstance().getText("editor.gui.nonodeselected")+".");
			return;
		}
		
		if (!PopUp.confirm(this,Settings.getInstance().getText("editor.gui.remove")+" '"+
				getSelectedNode().getName()+"'?")) {
			return;
		}
		
		file.getNode().removeChild(getSelectedNode());
		
		setSelectedNode(null);
		refreshFileGUI();
		refreshNodeGUI();
	}
	
	
	
	private void transformNode() {
	
		if (getSelectedNode()==null) {
			return;
		}
		
		Transform3D transform=getSelectedNode().getTransform();
		transform.getPosition().x=getTextF(tNodeTransform[0]);
		transform.getPosition().y=getTextF(tNodeTransform[1]);
		transform.getPosition().z=getTextF(tNodeTransform[2]);
		transform.getRotation().x=getTextF(tNodeTransform[3]);
		transform.getRotation().y=getTextF(tNodeTransform[4]);
		transform.getRotation().z=getTextF(tNodeTransform[5]);
		transform.setScale(getTextF(tNodeTransform[6]));
	}
	
	
	
	private void transformNode(KeyEvent e) {
		
		if (getSelectedNode()==null) {
			return;
		}
		
		float deltaX=0f;
		float deltaZ=0f;
		float rot=0f;
		
		if (e.getKeyCode()==KeyEvent.VK_LEFT) { deltaX=0.2f; }
		if (e.getKeyCode()==KeyEvent.VK_RIGHT) { deltaX=-0.2f; }
		if (e.getKeyCode()==KeyEvent.VK_UP) { deltaZ=0.2f; }
		if (e.getKeyCode()==KeyEvent.VK_DOWN) { deltaZ=-0.2f; }
		if (e.getKeyCode()==KeyEvent.VK_COMMA) { rot=-0.01f; }
		if (e.getKeyCode()==KeyEvent.VK_PERIOD) { rot=0.01f; }
		if (e.isShiftDown()) { deltaX*=10f; deltaZ*=10f; rot*=10f; }
		
		getSelectedNode().getTransform().getPosition().x+=deltaX;
		getSelectedNode().getTransform().getPosition().z+=deltaZ;
		getSelectedNode().getTransform().getRotation().y+=rot;
		
		refreshNodeGUI();
	}
	
	
	
	protected void cloneNode(SceneGraphNode node) {
	
		if (node==null) {
			PopUp.message(this,Settings.getInstance().getText("editor.gui.nonodeselected")+".");
			return;
		}
		
		String suggestedName=Settings.getInstance().getText("editor.gui.cloneof")+" "+node.getName();
		String name=PopUp.input(this,Settings.getInstance().getText("editor.gui.nodename"),suggestedName);
		
		if ((name==null) || (name.length()==0) || (file.getNode().getChild(name)!=null)) {
			PopUp.message(this,Settings.getInstance().getText("editor.gui.clonenameinuse"));
			return;
		}
		
		// Clone node
		
		SceneGraphNode clone=null;
		if (node instanceof Model) { 
			clone=MediaLoader.loadModel(DataLoader.toURL(((Model) node).getGeometryResource()));
			clone.setName(node.getName());
		}
		if (node instanceof Billboard) {
			Billboard bbNode=(Billboard) node;
			clone=new Billboard(node.getName(),bbNode.getModel());
		}
		clone.getTransform().setTransform(node.getTransform());
		clone.setVisible(node.getVisible());
		file.getNode().addChild(clone);
		
		// Refresh
		
		setSelectedNode(clone);
		refreshFileGUI();
		refreshNodeGUI();
	}
	
	
	
	public void setFile(F3dFile file) {
	
		this.file=file;	
		this.selectedNode=null;
		
		refreshFileGUI();
		refreshNodeGUI();
		refreshDataGUI();
	}
	
	
	
	public F3dFile getFile() {
	
		return file;
	}
	
	
	
	public void setSelectedNode(SceneGraphNode sn) {
		
		selectedNode=sn;
		refreshNodeGUI();
	}
	
	
	
	public SceneGraphNode getSelectedNode() {
	
		return selectedNode;
	}
		
	
	
	private JPanel createSwitchButtonPanel(String label,JButton plusButton,JButton minButton) {
		
		JLabel spacer=new JLabel("");
		spacer.setPreferredSize(new Dimension(2,plusButton.getPreferredSize().height));
		
		JPanel panel=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
		panel.add(plusButton);
		panel.add(spacer);
		panel.add(minButton);
		
		return panel;
	}
	
	
	
	private JPanel createXYZPanel(JTextField tX,JTextField tY,JTextField tZ) {
				
		JPanel panel=new JPanel(new GridLayout(1,3,2,0));
		panel.add(tX);
		panel.add(tY);
		panel.add(tZ);
		
		return panel;
	}
	
	
	
	private float getTextF(JTextField t) {
	
		try {
			return Float.parseFloat(t.getText());
		} catch (NumberFormatException e) {
			return 0f;
		}
	}
}
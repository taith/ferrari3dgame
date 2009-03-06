//--------------------------------------------------------------------------------
// Ferrari3D
// ModelEditor
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.dennisbijlsma.core3d.data.Vector3D;
import com.dennisbijlsma.core3d.scene.Billboard;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.core3d.scene.Quad;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.ferrari3d.Ferrari3D;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import com.dennisbijlsma.util.ResourceFile;
import com.dennisbijlsma.util.TextFormatter;
import com.dennisbijlsma.util.swing.DualFileDialog;
import com.dennisbijlsma.util.swing.FormPanel;
import com.dennisbijlsma.util.swing.Popups;
import com.dennisbijlsma.util.swing.PropertyTextField;
import com.dennisbijlsma.util.swing.PropertyPanel;
import com.dennisbijlsma.util.swing.mac.MacHandler;

/**
 * A desktop application that can open, save and edit model files from and to XML
 * files. All child nodes are shown in a list, and can be selected. When selected
 * their transforms can be changed visually. While this is taking place the model
 * is live previewed within the editor. 
 */

public final class ModelEditor {
	
	private ContentLoader loader;
	private ContentFile content;
	private Settings settings;
	
	private JFrame frame;
	private Map<String,JMenuItem> menuitems;
	private JLabel fileLabel;
	private JList nodesList;
	private JButton addNodeButton;
	private JButton removeNodeButton;
	private JButton copyNodeButton;
	private JButton renameNodeButton;
	private NodePanel nodePanel;
	private CircuitPanel circuitPanel;
	private PropertyPanel carInfoList;
	private Preview preview;
	
	private static final int EDITOR_WIDTH=1000;
	private static final int EDITOR_HEIGHT=700;
	private static final int SIDEBAR_WIDTH=300;
	private static final int SIDEBAR_HEIGHT=700;
	private static final String[] NODE_TYPES={"","SceneGraphNode","Model","Primitive","Billboard"};
	private static final float MOVE_AMOUNT=0.2f;
	private static final float ROTATE_AMOUNT=0.01f;
	
	/**
	 * Starts a new editor. The main application window will appear, but no content
	 * files will initially be opened.
	 */
	
	public ModelEditor() {
		
		loader=new ContentLoader();
		settings=Settings.getInstance();
		
		// Create frame
		
		frame=new JFrame(settings.getText("editor.title"));
		frame.setSize(EDITOR_WIDTH,EDITOR_HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Settings.EDITOR_ICON);
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
		
		// Create GUI
		
		JPanel contentPanel=new JPanel(new BorderLayout(10,10));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		frame.add(contentPanel,BorderLayout.CENTER);
		
		FormPanel form=new FormPanel();
		form.setPreferredSize(new Dimension(SIDEBAR_WIDTH,SIDEBAR_HEIGHT));
		contentPanel.add(form,BorderLayout.WEST);
		
		fileLabel=new JLabel();
		fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD));
		form.addRow(fileLabel);
		
		nodesList=new JList();
		nodesList.putClientProperty(MacHandler.QUAQUA_LIST,MacHandler.QUAQUA_VALUE_STRIPED);
		nodesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				SceneGraphNode selected=content.getNode().getChild((String) nodesList.getSelectedValue());
				nodePanel.setNode(selected);
			}
		});
		JScrollPane nodesListPane=new JScrollPane(nodesList);
		nodesListPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH,150));
		form.addRow(nodesListPane);
		
		addNodeButton=new JButton("+");
		addNodeButton.setToolTipText(settings.getText("editor.tooltip.add"));
		addNodeButton.putClientProperty(MacHandler.QUAQUA_BUTTON,MacHandler.QUAQUA_VALUE_SQUARE);
		delegate(addNodeButton,"addNodeClicked");

		removeNodeButton=new JButton("-");
		removeNodeButton.setToolTipText(settings.getText("editor.tooltip.remove"));
		removeNodeButton.putClientProperty(MacHandler.QUAQUA_BUTTON,MacHandler.QUAQUA_VALUE_SQUARE);
		delegate(removeNodeButton,"removeNodeClicked");
		
		copyNodeButton=new JButton(settings.getText("editor.gui.copy"));
		copyNodeButton.setToolTipText(settings.getText("editor.tooltip.copy"));
		copyNodeButton.putClientProperty(MacHandler.QUAQUA_BUTTON,MacHandler.QUAQUA_VALUE_SQUARE);
		delegate(copyNodeButton,"copyNodeClicked");
		
		renameNodeButton=new JButton(settings.getText("editor.gui.rename"));
		renameNodeButton.setToolTipText(settings.getText("editor.tooltip.rename"));
		renameNodeButton.putClientProperty(MacHandler.QUAQUA_BUTTON,MacHandler.QUAQUA_VALUE_SQUARE);
		delegate(renameNodeButton,"renameNodeClicked");
		
		JPanel leftButtonPanel=new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
		leftButtonPanel.add(addNodeButton);
		leftButtonPanel.add(removeNodeButton);
		JPanel rightButtonPanel=new JPanel(new FlowLayout(FlowLayout.RIGHT,5,0));
		rightButtonPanel.add(copyNodeButton);
		rightButtonPanel.add(renameNodeButton);
		JPanel buttonPanel=new JPanel(new BorderLayout());
		buttonPanel.add(leftButtonPanel,BorderLayout.WEST);
		buttonPanel.add(rightButtonPanel,BorderLayout.EAST);
		form.addRow(buttonPanel);
		
		form.addRow();
		
		nodePanel=new NodePanel(form);
		
		form.addRow();
		
		preview=new Preview();
		contentPanel.add(preview,BorderLayout.CENTER);
		preview.init();
				
		circuitPanel=new CircuitPanel(preview);
		circuitPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		JPanel carPanel=new JPanel(new BorderLayout());		
		carPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		carInfoList=new PropertyPanel(new Properties());
		carInfoList.putClientProperty(MacHandler.QUAQUA_LIST,MacHandler.QUAQUA_VALUE_STRIPED);
		JScrollPane infoPane=PropertyPanel.createScrollPane(carInfoList,100,100);
		infoPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		carPanel.add(infoPane,BorderLayout.CENTER);
		
		JTabbedPane dataPane=new JTabbedPane();
		dataPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH,150));
		dataPane.add(settings.getText("editor.gui.circuitdata"),circuitPanel);
		dataPane.add(settings.getText("editor.gui.cardata"),carPanel);
		form.addRow(dataPane);
		
		// Create menu
		
		menuitems=new HashMap<String,JMenuItem>();
		
		JMenuBar menubar=new JMenuBar();
		frame.setJMenuBar(menubar);
		
		JMenu fileMenu=new JMenu(settings.getText("editor.menu.file"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.new"),KeyEvent.VK_N,"newItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.open"),KeyEvent.VK_O,"openItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.import"),KeyEvent.VK_I,"importItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.save"),KeyEvent.VK_S,"saveItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.saveas"),-1,"saveAsItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.close"),KeyEvent.VK_W,"closeItemClicked"));
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem(settings.getText("editor.menu.exit"),-1,"exitItemClicked"));
		menubar.add(fileMenu);
		
		JMenu toolsMenu=new JMenu(settings.getText("editor.menu.tools"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.meta"),KeyEvent.VK_M,"metaItemClicked"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.checkcar"),-1,"checkCarItemClicked"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.checkcircuit"),-1,"checkCircuitItemClicked"));
		toolsMenu.addSeparator();
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.texts"),-1,"textsItemClicked"));
		menubar.add(toolsMenu);
		
		JMenu helpMenu=new JMenu(settings.getText("editor.menu.help")+" "); // Add space for bug on Mac
		helpMenu.add(createMenuItem(settings.getText("editor.menu.help"),-1,"helpItemClicked"));
		helpMenu.addSeparator();
		helpMenu.add(createMenuItem(settings.getText("editor.menu.about"),-1,"aboutItemClicked"));
		menubar.add(helpMenu);
		
		// Start
		
		setContentFile(null);
		
		frame.setVisible(true);
	}
	
	/**
	 * Creates an {@link ActionListener} which calls the specified delegate event
	 * handler method. This allows the same type of GUI event handling as used in
	 * Flash ActionScript.
	 */
	
	private void delegate(AbstractButton b,final String methodName) {
		
		// Obviously it's far from ideal to set event listeners for menu items
		// like this. However, the alternatives of having 1000 anonymous
		// ActionListeners or switching over all menu items is far from great
		// either, and might in fact be worse.
		final Object targetObject=this;
		final Class<?> targetClass=getClass();
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Method method=targetClass.getMethod(methodName);
					method.invoke(targetObject);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}
	
	/**
	 * Creates a new menu item with the specified title and shortkey. When clicked,
	 * the method with the specified name will be executed through reflection.
	 */
	
	private JMenuItem createMenuItem(String label,int shortkey,final String methodName) {
		
		JMenuItem menuitem=new JMenuItem(label);
		menuitems.put(label,menuitem);
		delegate(menuitem,methodName);
		
		if (shortkey!=-1) {
			int mask=Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			KeyStroke keystroke=KeyStroke.getKeyStroke(shortkey,mask);
			menuitem.setAccelerator(keystroke);
		}
		
		return menuitem;
	}
	
	/**
	 * Sets the currently opened content file.
	 */
	
	protected void setContentFile(ContentFile content) {
		
		this.content=content;
		
		updateMenuState();
		fileLabel.setText(content!=null ? content.getName() : "");
		nodesList.setEnabled(content!=null);
		nodesList.setListData(content!=null ? getChildNodeNames() : new String[0]);
		addNodeButton.setEnabled(content!=null);
		removeNodeButton.setEnabled(content!=null);
		copyNodeButton.setEnabled(content!=null);
		renameNodeButton.setEnabled(content!=null);
		nodePanel.setNode(null);
		circuitPanel.setEnabled(content!=null);
		circuitPanel.setFile(content);
		carInfoList.setEnabled(content!=null);
		carInfoList.setProperties(content!=null ? content.carInfo : new Properties());
		preview.setFile(content);
	}
	
	/**
	 * Returns the currently opened content file.
	 */
	
	protected ContentFile getContentFile() {
		
		return content;	
	}
	
	/**
	 * Updates the state of menu items that are affected by if a file is currently
	 * opened or not.
	 */
	
	private void updateMenuState() {
		
		//TODO find some more robust way of doing this
		
		menuitems.get(settings.getText("editor.menu.new")).setEnabled(content==null);
		menuitems.get(settings.getText("editor.menu.open")).setEnabled(content==null);
		menuitems.get(settings.getText("editor.menu.import")).setEnabled(content==null);
		
		menuitems.get(settings.getText("editor.menu.save")).setEnabled(content!=null);
		menuitems.get(settings.getText("editor.menu.saveas")).setEnabled(content!=null);
		menuitems.get(settings.getText("editor.menu.close")).setEnabled(content!=null);
		menuitems.get(settings.getText("editor.menu.meta")).setEnabled(content!=null);
		menuitems.get(settings.getText("editor.menu.checkcar")).setEnabled(content!=null);
		menuitems.get(settings.getText("editor.menu.checkcircuit")).setEnabled(content!=null);
		
		menuitems.get(settings.getText("editor.menu.help")).setEnabled(false);
	}
	
	/**
	 * Returns an array of all child node names for the currently opened content
	 * file. The array will be in alphabetical order.
	 */
	
	private String[] getChildNodeNames() {
	
		List<String> childNodeNames=new ArrayList<String>();
		for (SceneGraphNode i : content.getNode().getChildren()) {
			if ((i.getName()!=null) && (!i.getName().startsWith("_"))) {
				childNodeNames.add(i.getName());
			}
		}
		
		String[] names=childNodeNames.toArray(new String[0]);
		Arrays.sort(names);
		return names;
	}
	
	/**
	 * Shows a file dialog for opening or saving Ferrari3D model files (XML).
	 * @param save If true, shows a save dialog, else a open dialog.
	 * @return The file that was selected, or {@code null} when cancelled.
	 */
	
	private File showFileDialog(boolean save) {
		
		DualFileDialog dialog=new DualFileDialog();
		dialog.setTitle(save ? settings.getText("editor.savemodel") : settings.getText("editor.openmodel"));
		dialog.addFilter(settings.getText("editor.filedescription"),"xml");
		
		if (save) {
			return dialog.showSaveDialog(frame,"xml");
		} else {
			return dialog.showOpenDialog(frame);
		}
	}
	
	/**
	 * Returns an array of all content files that are used by the game. This list
	 * includes both cars and circuits. No assumptions should be made about the 
	 * ordering of the array.
	 */
	
	private String[] getAllContent() {
	
		List<String> list=new ArrayList<String>();
		for (String i : settings.cars) {
			list.add("cars/"+i+"/"+i+".xml");
		}
		for (String i : settings.circuits) {
			list.add("circuits/"+i+"/"+i+".xml");
		}
		
		return list.toArray(new String[0]);
	}
	
	// GUI event handler methods
	
	public void newItemClicked() { 
		File file=showFileDialog(true);
		if (file!=null) {
			ContentFile cf=loader.newFile(file);
			cf.setMeta(ContentFile.META_AUTHOR,"Ferrari3D");
			cf.setMeta(ContentFile.META_DATE,TextFormatter.dateFormat(false));
			cf.setMeta(ContentFile.META_VERSION,Ferrari3D.VERSION);
			cf.setMeta(ContentFile.META_PREVIEW,"preview.jpg");
			setContentFile(cf);
			
			// Save newly created file
			saveItemClicked();
		}
	}
	
	public void openItemClicked() { 
		File file=showFileDialog(false);
		if (file!=null) {
			try {
				ContentFile cf=loader.load(file.getName(),new ResourceFile(file),false);
				setContentFile(cf);
			} catch (Exception e) {
				settings.getLogger().warning("Could not open file: "+file.getAbsolutePath(),e);
				Popups.errorMessage(frame,settings.getText("editor.error.open",file.getName()));
			}
		}
	}
	
	public void importItemClicked() {
		JList list=new JList(getAllContent());
		list.putClientProperty(MacHandler.QUAQUA_LIST,MacHandler.QUAQUA_VALUE_STRIPED);
		JScrollPane listPane=new JScrollPane(list);
		listPane.setPreferredSize(new Dimension(400,100));
		if (Popups.confirmMessage(frame,"editor.import",listPane)) {
			String selected=(String) list.getSelectedValue();
			if (selected!=null) {
				File file=new File(selected);
				try {
					ContentFile cf=loader.load(file.getName(),new ResourceFile(file),false);
					setContentFile(cf);
				} catch (Exception e) {
					settings.getLogger().warning("Could not open file: "+file.getAbsolutePath(),e);
					Popups.errorMessage(frame,settings.getText("editor.error.open",file.getName()));
				}
			}
		}
	}
	
	public void saveItemClicked() {
		File file=content.getResource().getLocalFile();
		try {
			content.setMeta(ContentFile.META_DATE,TextFormatter.dateFormat(false));
			content.setMeta(ContentFile.META_VERSION,Ferrari3D.VERSION);
			loader.save(content,file);
			Popups.message(frame,settings.getText("editor.savedas",file.getName()));
		} catch (Exception e) {
			settings.getLogger().warning("Could not save file: "+file.getAbsolutePath(),e);
			Popups.errorMessage(frame,settings.getText("editor.error.save",file.getName()));
		}
	}
	
	public void saveAsItemClicked() { 
		File file=showFileDialog(true);
		if (file!=null) {
			try {
				loader.save(content,file);
				Popups.message(frame,settings.getText("editor.savedas",file.getName()));
			} catch (Exception e) {
				settings.getLogger().warning("Could not save file: "+file.getAbsolutePath(),e);
				Popups.errorMessage(frame,settings.getText("editor.error.save",file.getName()));
			}
			
			setContentFile(null);
			openItemClicked();
		}
	}
	
	public void closeItemClicked() { 
		setContentFile(null);
	}
	
	public void exitItemClicked() {
		if (Popups.confirmMessage(frame,settings.getText("editor.exit"))) {
			System.exit(0);
		}
	}
	
	public void metaItemClicked() { 
		FormPanel form=new FormPanel();
		form.setPreferredSize(new Dimension(400,200));
		form.addRow(settings.getText("editor.meta.name"),new JLabel(content.getName()));
		form.addRow(settings.getText("editor.meta.author"),new JLabel(content.getMeta(ContentFile.META_AUTHOR)));
		form.addRow(settings.getText("editor.meta.date"),new JLabel(content.getMeta(ContentFile.META_DATE)));
		form.addRow(settings.getText("editor.meta.version"),new JLabel(content.getMeta(ContentFile.META_VERSION)));
		Popups.message(frame,settings.getText("editor.meta.title"),form);
	}
	
	public void checkCarItemClicked() { 
		boolean check=content.checkCarModel();
		Popups.message(null,settings.getText("editor.message.checkcar"+check));
	}
	
	public void checkCircuitItemClicked() { 
		boolean check=content.checkCircuitModel();
		Popups.message(null,settings.getText("editor.message.checkcircuit"+check));
	}
	
	public void textsItemClicked() { 
		PropertyPanel textsPanel=new PropertyPanel(settings.getAllTexts());
		textsPanel.setPreferredSize(new Dimension(600,textsPanel.getPreferredSize().height));
		textsPanel.putClientProperty(MacHandler.QUAQUA_LIST,"striped");
		JScrollPane textsPane=PropertyPanel.createScrollPane(textsPanel,600,200);
		Popups.message(frame,settings.getText("editor.menu.texts"),textsPane);
	}
	
	public void helpItemClicked() { }
	
	public void aboutItemClicked() { 
		Popups.message(null,settings.getText("editor.title")+"\n"+settings.getText("menu.copyright"));
	}
	
	public void addNodeClicked() {
		String name=Popups.inputMessage(frame,settings.getText("editor.gui.nodename"),"");
		if (name==null) {
			return;
		}
		if ((name.isEmpty()) || (content.getNode().getChild(name)!=null)) {
			Popups.message(frame,settings.getText("editor.gui.invalidnodename"));
		}
		
		String type=Popups.selectMessage(frame,settings.getText("editor.gui.nodetype"),
				new String[]{"SceneGraphNode","Model","Billboard"},"SceneGraphNode");
		if (type==null) {
			return;
		}
		
		SceneGraphNode node=null;
		if (type.equals("SceneGraphNode")) {
			node=new SceneGraphNode(name);
		}
		if (type.equals("Model")) {
			String modelFile=Popups.inputMessage(frame,settings.getText("editor.gui.modellocation"),"");
			if (modelFile==null) {
				return;
			}
			if ((modelFile.isEmpty()) || (!content.getSubResource(modelFile).exists())) {
				Popups.message(frame,settings.getText("editor.gui.modelinvalid",modelFile));
				return;
			}
			
			ResourceFile modelResource=content.getSubResource(modelFile);
			node=new Model(name,modelResource.getURL(),modelResource.getURL());
		}
		if (type.equals("Billboard")) {
			String billboardFile=Popups.inputMessage(frame,settings.getText("editor.gui.billboardlocation"),"");
			if (billboardFile==null) {
				return;
			}
			if ((billboardFile.isEmpty()) || (!content.getSubResource(billboardFile).exists())) {
				Popups.message(frame,settings.getText("editor.gui.billboardinvalid"));
				return;
			}
			
			ResourceFile billboardResource=content.getSubResource(billboardFile);
			Quad quad=new Quad(name,1f,1f);
			quad.setTexture(Utils.loadImage(billboardResource));
			quad.setTextureURL(billboardResource.getName());
			node=new Billboard(name,quad);
		}
		content.getNode().addChild(node);
		
		nodesList.setSelectedValue(null,false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(node.getName(),false);
	}
	
	public void removeNodeClicked() {
		if (nodePanel.getNode()==null) {
			Popups.message(frame,settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		if (!Popups.confirmMessage(frame,settings.getText("editor.gui.removesure"))) {
			return;
		}
		
		content.getNode().removeChild(nodePanel.getNode());
		
		nodePanel.setNode(null);
		nodesList.setSelectedValue(null,false);
		nodesList.setListData(getChildNodeNames());
	}
	
	public void copyNodeClicked() {
		if (nodePanel.getNode()==null) {
			Popups.message(frame,settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		SceneGraphNode node=nodePanel.getNode();
		SceneGraphNode copy=null;
		String copyName=settings.getText("editor.gui.copyof")+" "+node.getName();
		
		if (node.getClass()==SceneGraphNode.class) {
			copy=new SceneGraphNode(copyName);
		}
		
		if (node.getClass()==Model.class) {
			Popups.message(frame,settings.getText("editor.gui.copynotsupported"));
			return;
		}
		
		if (node.getClass()==Primitive.class) {
			Primitive prim=(Primitive) node;
			copy=new Primitive(copyName,prim.getType(),prim.getDimensions());
		}
		
		if (node.getClass()==Billboard.class) {
			Popups.message(frame,settings.getText("editor.gui.copynotsupported"));
			return;
			//TODO
		}
		
		copy.getTransform().setTransform(node.getTransform());
		copy.setVisible(node.getVisible());
		content.getNode().addChild(copy);
		
		nodesList.setSelectedValue(null,false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(copyName,false);
	}
	
	public void renameNodeClicked() {
		if (nodePanel.getNode()==null) {
			Popups.message(frame,settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		SceneGraphNode node=nodePanel.getNode();
		String name=Popups.inputMessage(frame,settings.getText("editor.gui.renamenode"),node.getName());
		if ((name==null) || (name.isEmpty())) {
			return;
		}
		
		node.setName(name);
		nodesList.setSelectedValue(null,false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(name,false);
	}
	
	/**
	 * Wrapper class around the form fields for the selected node. Creating this
	 * class will also add all fields to the specified form.
	 */

	private static class NodePanel {
		
		private SceneGraphNode node=null;
		private Settings settings=Settings.getInstance();
		
		private JLabel nodeLabel;
		private JComboBox nodeTypeList;
		private PropertyTextField[] posFields;
		private PropertyTextField[] rotFields;
		private PropertyTextField scaleField;
		private JButton transformButton;
		
		public NodePanel(FormPanel form) {
			nodeLabel=new JLabel("");
			nodeLabel.setFont(nodeLabel.getFont().deriveFont(Font.BOLD));
			form.addRow(nodeLabel);
			
			nodeTypeList=new JComboBox(NODE_TYPES);
			nodeTypeList.setEnabled(false);
			form.addRow(settings.getText("editor.gui.type"),nodeTypeList);
			
			posFields=new PropertyTextField[3];
			initField(posFields[0]=new PropertyTextField());
			initField(posFields[1]=new PropertyTextField());
			initField(posFields[2]=new PropertyTextField());
			form.addRow(settings.getText("editor.gui.position"),posFields[0],posFields[1],posFields[2]);
			
			rotFields=new PropertyTextField[3];
			initField(rotFields[0]=new PropertyTextField());
			initField(rotFields[1]=new PropertyTextField());
			initField(rotFields[2]=new PropertyTextField());
			form.addRow(settings.getText("editor.gui.rotation"),rotFields[0],rotFields[1],rotFields[2]);
			
			initField(scaleField=new PropertyTextField());
			form.addRow(settings.getText("editor.gui.scale"),scaleField);
			
			transformButton=new JButton(settings.getText("editor.gui.transform"));
			transformButton.setToolTipText(settings.getText("editor.tooltip.transform"));
			transformButton.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					freeTransform(e);
				}
			});
			form.addRow("",transformButton);
		}
		
		private void initField(PropertyTextField field) {
			field.addTextListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent e) {
					if (e.getPropertyName().equals(PropertyTextField.PROPERTY_TEXT)) {
						updateTransform();
					}
				}
			});
		}
		
		public void setNode(SceneGraphNode node) {
			this.node=node;
			setEnabled(node!=null);
			nodeLabel.setText(node!=null ? node.getName() : "");
			nodeTypeList.setSelectedItem(node!=null ? node.getClass().getSimpleName() : "");
			if (node!=null) {
				sync();
			}
		}
		
		public SceneGraphNode getNode() {
			return node;
		}
		
		public void sync() {
			Vector3D position=node.getTransform().getPosition();
			posFields[0].setText(position.x,1);
			posFields[1].setText(position.y,1);
			posFields[2].setText(position.z,1);
			
			Vector3D rotation=node.getTransform().getRotation();
			rotFields[0].setText(rotation.x,1);
			rotFields[1].setText(rotation.y,1);
			rotFields[2].setText(rotation.z,1);
			
			Vector3D scale=node.getTransform().getScale();
			scaleField.setText(scale.x,3);
		}
		
		private void setEnabled(boolean enabled) {
			posFields[0].setEnabled(enabled);
			posFields[1].setEnabled(enabled);
			posFields[2].setEnabled(enabled);
			rotFields[0].setEnabled(enabled);
			rotFields[1].setEnabled(enabled);
			rotFields[2].setEnabled(enabled);
			scaleField.setEnabled(enabled);
			transformButton.setEnabled(enabled);
		}
		
		private void updateTransform() {
			Vector3D position=node.getTransform().getPosition();
			position.setVector(posFields[0].getTextFloat(),posFields[1].getTextFloat(),
					posFields[2].getTextFloat());
			Vector3D rotation=node.getTransform().getRotation();
			rotation.setVector(rotFields[0].getTextFloat(),rotFields[1].getTextFloat(),
					rotFields[2].getTextFloat());
			Vector3D scale=node.getTransform().getScale();
			scale.setVector(scaleField.getTextFloat(),scaleField.getTextFloat(),scaleField.getTextFloat());
		}
		
		private void freeTransform(KeyEvent e) {
			float deltaX=0f;
			float deltaZ=0f;
			float rot=0f;
			
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT : deltaX=MOVE_AMOUNT; break;
				case KeyEvent.VK_RIGHT : deltaX=-MOVE_AMOUNT; break;
				case KeyEvent.VK_UP : deltaZ=MOVE_AMOUNT; break;
				case KeyEvent.VK_DOWN : deltaZ=-MOVE_AMOUNT; break;
				case KeyEvent.VK_COMMA : rot=-ROTATE_AMOUNT; break;
				case KeyEvent.VK_PERIOD : rot=ROTATE_AMOUNT; break;
				default : break;
			}
			
			if (e.isShiftDown()) {
				deltaX*=10;
				deltaZ*=10;
				rot*=10;
			}
			
			if (e.getKeyCode()==KeyEvent.VK_LEFT) { deltaX=0.2f; }
			if (e.getKeyCode()==KeyEvent.VK_RIGHT) { deltaX=-0.2f; }
			if (e.getKeyCode()==KeyEvent.VK_UP) { deltaZ=0.2f; }
			if (e.getKeyCode()==KeyEvent.VK_DOWN) { deltaZ=-0.2f; }
			if (e.getKeyCode()==KeyEvent.VK_COMMA) { rot=-0.01f; }
			if (e.getKeyCode()==KeyEvent.VK_PERIOD) { rot=0.01f; }
			if (e.isShiftDown()) { deltaX*=10f; deltaZ*=10f; rot*=10f; }
			
			node.getTransform().getPosition().x+=deltaX;
			node.getTransform().getPosition().z+=deltaZ;
			node.getTransform().getRotation().y+=rot;
			
			sync();
		}
	}
}
//-----------------------------------------------------------------------------
// Ferrari3D
// Editor
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
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

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.scene.Billboard;
import com.dennisbijlsma.core3d.scene.BufferedImageTexture;
import com.dennisbijlsma.core3d.scene.InvalidTextureException;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.Primitive;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import com.dennisbijlsma.ferrari3d.Ferrari3D;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.ferrari3d.util.Utils;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.FormatUtils;
import nl.colorize.util.swing.Action;
import nl.colorize.util.swing.ActionForwarder;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.Popups;
import nl.colorize.util.swing.PropertyTextField;
import nl.colorize.util.swing.PropertyPanel;
import nl.colorize.util.swing.MacSupport;

/**
 * A desktop application that can open, save and edit model files from and to XML
 * files. All child nodes are shown in a list, and can be selected. When selected
 * their transforms can be changed visually. While this is taking place the model
 * is live previewed within the editor. 
 */
public final class Editor {
	
	private ContentLoader loader;
	private ContentFile content;
	private Settings settings;
	
	private ActionForwarder forwarder;
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
	
	private static final int EDITOR_WIDTH = 1000;
	private static final int EDITOR_HEIGHT = 700;
	private static final int SIDEBAR_WIDTH = 300;
	private static final int SIDEBAR_HEIGHT = 700;
	private static final String[] NODE_TYPES = {"", "SceneGraphNode", "Model", "Primitive", "Billboard"};
	private static final float MOVE_AMOUNT = 0.2f;
	private static final float ROTATE_AMOUNT = 0.01f;
	
	/**
	 * Starts a new editor. The main application window will appear, but no content
	 * files will initially be opened.
	 */
	public Editor() {
		
		loader = new ContentLoader();
		settings = Settings.getInstance();
				
		// Create frame
		
		frame = new JFrame(settings.getText("editor.title"));
		frame.setSize(EDITOR_WIDTH, EDITOR_HEIGHT);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Settings.EDITOR_ICON);
		frame.setResizable(false);
		frame.setLayout(new BorderLayout());
		
		// Create GUI
		
		forwarder = new ActionForwarder(this);
		
		JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		frame.add(contentPanel, BorderLayout.CENTER);
		
		FormPanel form = new FormPanel();
		form.setPreferredSize(new Dimension(SIDEBAR_WIDTH, SIDEBAR_HEIGHT));
		contentPanel.add(form, BorderLayout.WEST);
		
		fileLabel = new JLabel();
		fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD));
		form.addRow(fileLabel);
		
		nodesList = new JList();
		nodesList.putClientProperty(MacSupport.QUAQUA_LIST, MacSupport.QUAQUA_VALUE_STRIPED);
		nodesList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				SceneGraphNode selected = content.getNode().getChild((String) nodesList.getSelectedValue());
				nodePanel.setNode(selected);
			}
		});
		JScrollPane nodesListPane = new JScrollPane(nodesList);
		nodesListPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 150));
		form.addRow(nodesListPane);
		
		addNodeButton = new JButton("+");
		addNodeButton.setToolTipText(settings.getText("editor.tooltip.add"));
		addNodeButton.putClientProperty(MacSupport.QUAQUA_BUTTON, MacSupport.QUAQUA_VALUE_SQUARE);
		delegate(addNodeButton, "addNodeClicked");

		removeNodeButton = new JButton("-");
		removeNodeButton.setToolTipText(settings.getText("editor.tooltip.remove"));
		removeNodeButton.putClientProperty(MacSupport.QUAQUA_BUTTON, MacSupport.QUAQUA_VALUE_SQUARE);
		delegate(removeNodeButton, "removeNodeClicked");
		
		copyNodeButton = new JButton(settings.getText("editor.gui.copy"));
		copyNodeButton.setToolTipText(settings.getText("editor.tooltip.copy"));
		copyNodeButton.putClientProperty(MacSupport.QUAQUA_BUTTON, MacSupport.QUAQUA_VALUE_SQUARE);
		delegate(copyNodeButton, "copyNodeClicked");
		
		renameNodeButton = new JButton(settings.getText("editor.gui.rename"));
		renameNodeButton.setToolTipText(settings.getText("editor.tooltip.rename"));
		renameNodeButton.putClientProperty(MacSupport.QUAQUA_BUTTON, MacSupport.QUAQUA_VALUE_SQUARE);
		delegate(renameNodeButton, "renameNodeClicked");
		
		JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		leftButtonPanel.add(addNodeButton);
		leftButtonPanel.add(removeNodeButton);
		JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		rightButtonPanel.add(copyNodeButton);
		rightButtonPanel.add(renameNodeButton);
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(leftButtonPanel, BorderLayout.WEST);
		buttonPanel.add(rightButtonPanel, BorderLayout.EAST);
		form.addRow(buttonPanel);
		
		form.addRow();
		
		nodePanel = new NodePanel(form);
		
		form.addRow();
		
		preview = new Preview();
		contentPanel.add(preview, BorderLayout.CENTER);
		preview.init();
				
		circuitPanel = new CircuitPanel(preview);
		circuitPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		JPanel carPanel = new JPanel(new BorderLayout());		
		carPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		carInfoList = new PropertyPanel(new Properties());
		carInfoList.putClientProperty(MacSupport.QUAQUA_LIST, MacSupport.QUAQUA_VALUE_STRIPED);
		JScrollPane infoPane = new JScrollPane(carInfoList);
		infoPane.setPreferredSize(new Dimension(100, 100));
		infoPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		carPanel.add(infoPane, BorderLayout.CENTER);
		
		JTabbedPane dataPane = new JTabbedPane();
		dataPane.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 150));
		dataPane.add(settings.getText("editor.gui.circuitdata"), circuitPanel);
		dataPane.add(settings.getText("editor.gui.cardata"), carPanel);
		form.addRow(dataPane);
		
		// Create menu
		
		menuitems = new HashMap<String,JMenuItem>();
		
		JMenuBar menubar = new JMenuBar();
		frame.setJMenuBar(menubar);
		
		JMenu fileMenu = new JMenu(settings.getText("editor.menu.file"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.new"), KeyEvent.VK_N, "newItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.open"), KeyEvent.VK_O, "openItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.import"), KeyEvent.VK_I, "importItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.save"), KeyEvent.VK_S, "saveItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.saveas"), -1, "saveAsItemClicked"));
		fileMenu.add(createMenuItem(settings.getText("editor.menu.close"), KeyEvent.VK_W, "closeItemClicked"));
		fileMenu.addSeparator();
		fileMenu.add(createMenuItem(settings.getText("editor.menu.exit"), -1, "exitItemClicked"));
		menubar.add(fileMenu);
		
		JMenu toolsMenu = new JMenu(settings.getText("editor.menu.tools"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.meta"), KeyEvent.VK_M, "metaItemClicked"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.checkcar"), -1, "checkCarItemClicked"));
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.checkcircuit"), -1, "checkCircuitItemClicked"));
		toolsMenu.addSeparator();
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.convert"), -1, "convertModel"));
		toolsMenu.addSeparator();
		toolsMenu.add(createMenuItem(settings.getText("editor.menu.texts"), -1, "textsItemClicked"));
		menubar.add(toolsMenu);
		
		JMenu helpMenu=new JMenu(settings.getText("editor.menu.help") + " "); // Add space for bug on Mac
		helpMenu.add(createMenuItem(settings.getText("editor.menu.help"), -1, "helpItemClicked"));
		helpMenu.addSeparator();
		helpMenu.add(createMenuItem(settings.getText("editor.menu.about"), -1, "aboutItemClicked"));
		menubar.add(helpMenu);
		
		// Start
		
		setContentFile(null);
		
		frame.setVisible(true);
	}
	
	/**
	 * Shorthand version of {@link ActionForwarder.delegate(AbstractButton, String)}.
	 */
	private void delegate(AbstractButton b, final String methodName) {
		forwarder.delegate(b, methodName);
	}
	
	/**
	 * Creates a new menu item with the specified title and shortkey. When clicked,
	 * the method with the specified name will be executed through reflection.
	 */
	private JMenuItem createMenuItem(String label, int shortkey, String methodName) {
		
		JMenuItem menuitem = new JMenuItem(label);
		menuitems.put(label, menuitem);
		delegate(menuitem, methodName);
		
		if (shortkey != -1) {
			int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
			menuitem.setAccelerator(KeyStroke.getKeyStroke(shortkey, mask));
		}
		
		return menuitem;
	}
	
	/**
	 * Sets the currently opened content file.
	 */
	protected void setContentFile(ContentFile content) {
		
		this.content = content;
		
		updateMenuState();
		fileLabel.setText(content != null ? content.getName() : "");
		nodesList.setEnabled(content != null);
		nodesList.setListData(content != null ? getChildNodeNames() : new String[0]);
		addNodeButton.setEnabled(content != null);
		removeNodeButton.setEnabled(content != null);
		copyNodeButton.setEnabled(content != null);
		renameNodeButton.setEnabled(content != null);
		nodePanel.setNode(null);
		circuitPanel.setEnabled(content != null);
		circuitPanel.setFile(content);
		carInfoList.setEnabled(content != null);
		carInfoList.setProperties(content != null ? content.carInfo : new Properties());
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
		
		menuitems.get(settings.getText("editor.menu.new")).setEnabled(content == null);
		menuitems.get(settings.getText("editor.menu.open")).setEnabled(content == null);
		menuitems.get(settings.getText("editor.menu.import")).setEnabled(content == null);
		
		menuitems.get(settings.getText("editor.menu.save")).setEnabled(content != null);
		menuitems.get(settings.getText("editor.menu.saveas")).setEnabled(content != null);
		menuitems.get(settings.getText("editor.menu.close")).setEnabled(content != null);
		menuitems.get(settings.getText("editor.menu.meta")).setEnabled(content != null);
		menuitems.get(settings.getText("editor.menu.checkcar")).setEnabled(content != null);
		menuitems.get(settings.getText("editor.menu.checkcircuit")).setEnabled(content != null);
		
		menuitems.get(settings.getText("editor.menu.help")).setEnabled(false);
	}
	
	/**
	 * Returns an array of all child node names for the currently opened content
	 * file. The array will be in alphabetical order.
	 */
	private String[] getChildNodeNames() {
	
		List<String> childNodeNames = new ArrayList<String>();
		for (SceneGraphNode i : content.getNode().getChildren()) {
			if ((i.getName() != null) && (!i.getName().startsWith("_"))) {
				childNodeNames.add(i.getName());
			}
		}
		
		String[] names = childNodeNames.toArray(new String[0]);
		Arrays.sort(names);
		return names;
	}
	
	/**
	 * Shows a file dialog for opening or saving Ferrari3D model files (XML).
	 * @param save If true, shows a save dialog, else a open dialog.
	 * @return The file that was selected, or {@code null} when cancelled.
	 */
	private File showFileDialog(boolean save) {
		
		ComboFileDialog dialog = new ComboFileDialog();
		dialog.setTitle(save ? settings.getText("editor.savemodel") : settings.getText("editor.openmodel"));
		dialog.addFilter(settings.getText("editor.filedescription"), "xml");
		
		if (save) {
			return dialog.showSaveDialog(frame, "xml");
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
	
		List<String> list = new ArrayList<String>();
		for (String i : settings.cars) {
			list.add("cars/" + i + "/" + i + ".xml");
		}
		for (String i : settings.circuits) {
			list.add("circuits/" + i + "/" + i + ".xml");
		}
		
		return list.toArray(new String[0]);
	}
	
	/**
	 * Returns the name for a copy of the specified node.
	 */
	private String getCopyName(SceneGraphNode node) {
		
		String originalName = node.getName();
		
		// Check if the last digit is a number. If so increment by one. If not,
		// add a prefix to the name.
		try {
			int number = Integer.parseInt(originalName.substring(originalName.length() - 1));
			return originalName.replaceAll("" + number, "" + (number + 1));
		} catch (NumberFormatException e) {
			return settings.getText("editor.gui.copyof") + " " + originalName;
		}
	}
	
	// GUI event handler methods
	
	@Action
	public void newItemClicked() { 
		File file = showFileDialog(true);
		if (file != null) {
			if (file.exists()) {
				Popups.message(frame, settings.getText("editor.message.fileAlreadyExists"), file.getName());
				return;
			}
			
			ContentFile cf = loader.newFile(file);
			cf.setMeta(ContentFile.META_AUTHOR, "Ferrari3D");
			cf.setMeta(ContentFile.META_DATE, FormatUtils.datetimeFormat("dd-MM-yyyy"));
			cf.setMeta(ContentFile.META_VERSION, Ferrari3D.VERSION.toString());
			cf.setMeta(ContentFile.META_PREVIEW, "preview.jpg");
			setContentFile(cf);
			// Save newly created file
			saveItemClicked();
		}
	}
	
	@Action
	public void openItemClicked() { 
		File file = showFileDialog(false);
		if (file != null) {
			try {
				ContentFile cf = loader.load(file.getName(), new ResourceFile(file), false);
				setContentFile(cf);
			} catch (Exception e) {
				settings.getLogger().warning("Could not open file: " + file.getAbsolutePath(), e);
				Popups.errorMessage(settings.getText("editor.error.open", file.getName()));
			}
		}
	}
	
	@Action
	public void importItemClicked() {
		JList list = new JList(getAllContent());
		list.putClientProperty(MacSupport.QUAQUA_LIST, MacSupport.QUAQUA_VALUE_STRIPED);
		JScrollPane listPane = new JScrollPane(list);
		listPane.setPreferredSize(new Dimension(400, 100));
		if (Popups.confirmMessage(frame, "editor.import", listPane)) {
			String selected = (String) list.getSelectedValue();
			if (selected != null) {
				File file = new File(selected);
				try {
					ContentFile cf = loader.load(file.getName(), new ResourceFile(file), false);
					setContentFile(cf);
				} catch (Exception e) {
					settings.getLogger().warning("Could not open file: " + file.getAbsolutePath(), e);
					Popups.errorMessage(settings.getText("editor.error.open", file.getName()));
				}
			}
		}
	}
	
	@Action
	public void saveItemClicked() {
		File file = content.getResource().toLocalFile();
		try {
			content.setMeta(ContentFile.META_DATE, FormatUtils.datetimeFormat("dd-MM-yyyy"));
			content.setMeta(ContentFile.META_VERSION, Ferrari3D.VERSION.toString());
			loader.save(content, file);
			Popups.message(frame, settings.getText("editor.savedas", file.getName()));
		} catch (Exception e) {
			settings.getLogger().warning("Could not save file: " + file.getAbsolutePath(), e);
			Popups.errorMessage(settings.getText("editor.error.save", file.getName()));
		}
	}
	
	@Action
	public void saveAsItemClicked() { 
		File file = showFileDialog(true);
		if (file != null) {
			try {
				loader.save(content, file);
				Popups.message(frame, settings.getText("editor.savedas", file.getName()));
			} catch (Exception e) {
				settings.getLogger().warning("Could not save file: " + file.getAbsolutePath(), e);
				Popups.errorMessage(settings.getText("editor.error.save", file.getName()));
			}
			
			setContentFile(null);
			openItemClicked();
		}
	}
	
	@Action
	public void closeItemClicked() { 
		setContentFile(null);
	}
	
	@Action
	public void exitItemClicked() {
		if (Popups.confirmMessage(frame, settings.getText("editor.exit"))) {
			System.exit(0);
		}
	}
	
	@Action
	public void metaItemClicked() { 
		FormPanel form = new FormPanel();
		form.setPreferredSize(new Dimension(400, 200));
		form.addRow(settings.getText("editor.meta.name"), content.getName());
		form.addRow(settings.getText("editor.meta.author"), content.getMeta(ContentFile.META_AUTHOR));
		form.addRow(settings.getText("editor.meta.date"), content.getMeta(ContentFile.META_DATE));
		form.addRow(settings.getText("editor.meta.version"), content.getMeta(ContentFile.META_VERSION));
		Popups.message(frame,settings.getText("editor.meta.title"), form);
	}
	
	@Action
	public void checkCarItemClicked() { 
		Popups.message(null, settings.getText("editor.message.checkcar" + content.checkCarModel()));
	}
	
	@Action
	public void checkCircuitItemClicked() { 
		Popups.message(null, settings.getText("editor.message.checkcircuit" + content.checkCircuitModel()));
	}
	
	@Action
	public void convertModel() {
		
		ComboFileDialog inputDialog = new ComboFileDialog();
		inputDialog.setTitle(settings.getText("editor.convertModel.selectInput"));
		inputDialog.addFilter(settings.getText("editor.convertModel.files"), "obj", "3ds");
		
		File inputFile = inputDialog.showOpenDialog(frame);
		if (inputFile == null) {
			return;
		}
		
		ComboFileDialog outputDialog = new ComboFileDialog();
		outputDialog.setTitle(settings.getText("editor.convertModel.selectOutput"));
		outputDialog.addFilter(settings.getText("editor.convertModel.files"), "jme");
		outputDialog.setStartDirectory(inputFile.getParentFile());
		
		File outputFile = outputDialog.showSaveDialog(frame, "jme");
		if (outputFile == null) {
			return;
		}
		
		try {
			com.dennisbijlsma.core3d.internal.ModelLoader.convert(inputFile, outputFile);
			Popups.message(frame, settings.getText("editor.convertModel.success", inputFile.getName()));
		} catch (Exception e) {
			Popups.errorMessage(settings.getText("editor.error.convertModel", inputFile.getName()));
			e.printStackTrace();
		}
	}
	
	@Action
	public void textsItemClicked() { 
		PropertyPanel textsPanel = new PropertyPanel(settings.getAllTexts());
		textsPanel.setPreferredSize(new Dimension(600, textsPanel.getPreferredSize().height));
		textsPanel.putClientProperty(MacSupport.QUAQUA_LIST, MacSupport.QUAQUA_VALUE_STRIPED);
		JScrollPane textsPane = new JScrollPane(textsPanel);
		textsPane.setPreferredSize(new Dimension(600, 200));
		Popups.message(frame, settings.getText("editor.menu.texts"), textsPane);
	}
	
	@Action
	public void helpItemClicked() { 
		
	}
	
	@Action
	public void aboutItemClicked() { 
		Popups.message(null, settings.getText("editor.title") + "\n" + settings.getText("menu.copyright"));
	}
	
	@Action
	public void addNodeClicked() {
		String name = Popups.inputMessage(frame, settings.getText("editor.gui.nodename"), "");
		if (name == null) {
			return;
		}
		if ((name.length() == 0) || (content.getNode().getChild(name) != null)) {
			Popups.message(frame,settings.getText("editor.gui.invalidnodename"));
		}
		
		String type = Popups.selectMessage(frame, settings.getText("editor.gui.nodetype"),
				new String[]{"SceneGraphNode", "Model", "Billboard"}, "SceneGraphNode");
		if (type == null) {
			return;
		}
		
		SceneGraphNode node = null;
		if (type.equals("SceneGraphNode")) {
			node = new SceneGraphNode(name);
		}
		if (type.equals("Model")) {
			String modelFile = Popups.inputMessage(frame, settings.getText("editor.gui.modellocation"), "");
			if (modelFile == null) {
				return;
			}
			if ((modelFile.length() == 0) || (!content.getSubResource(modelFile).exists())) {
				Popups.message(frame, settings.getText("editor.gui.modelinvalid", modelFile));
				return;
			}
			
			ResourceFile modelResource = content.getSubResource(modelFile);
			node = new Model(name, modelResource.getURL(), modelResource.getURL());
		}
		if (type.equals("Billboard")) {
			String billboardFile = Popups.inputMessage(frame, settings.getText("editor.gui.billboardlocation"), "");
			if (billboardFile == null) {
				return;
			}
			if ((billboardFile.length() == 0) || (!content.getSubResource(billboardFile).exists())) {
				Popups.message(frame, settings.getText("editor.gui.billboardinvalid"));
				return;
			}
			
			ResourceFile billboardResource = content.getSubResource(billboardFile);
			BufferedImage texture = Utils.loadImage(billboardResource);
			
			try {
				new BufferedImageTexture(texture);
			} catch (InvalidTextureException e) {
				Popups.message(frame, settings.getText("editor.message.invalidtexture", billboardFile));
				return;
			}
			
			//TODO support other billboard dimensions
			node = new Billboard(name, 1f, 1f, new BufferedImageTexture(texture)); 
			((Billboard) node).setTextureURL(billboardFile);
		}
		content.getNode().addChild(node);
		
		nodesList.setSelectedValue(null, false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(node.getName(), false);
	}
	
	@Action
	public void removeNodeClicked() {
		if (nodePanel.getNode() == null) {
			Popups.message(frame, settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		if (!Popups.confirmMessage(frame, settings.getText("editor.gui.removesure"))) {
			return;
		}
		
		content.getNode().removeChild(nodePanel.getNode());
		
		nodePanel.setNode(null);
		nodesList.setSelectedValue(null, false);
		nodesList.setListData(getChildNodeNames());
	}
	
	@Action
	@SuppressWarnings("deprecation")
	public void copyNodeClicked() {
		if (nodePanel.getNode() == null) {
			Popups.message(frame, settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		SceneGraphNode node = nodePanel.getNode();
		SceneGraphNode copy = null;
		String copyName = getCopyName(node);
		
		if (node.getClass() == SceneGraphNode.class) {
			copy = new SceneGraphNode(copyName);
		}
		
		if (node.getClass() == Model.class) {
			Popups.message(frame, settings.getText("editor.gui.copynotsupported"));
			return;
		}
		
		if (node.getClass() == Primitive.class) {
			Primitive prim = (Primitive) node;
			ImmutableVector3D dimensions = prim.getDimensions();
			switch (prim.getType()) {
				case 1 : copy = Primitive.createQuad(dimensions.getX(), dimensions.getY()); break;
				case 2 : copy = Primitive.createBox(dimensions); break;
				case 3 : copy = Primitive.createSphere(dimensions.getX(), 18); break;
				case 4 : copy = Primitive.createCylinder(dimensions.getX(), dimensions.getY(), 5, 5); break;
				default : throw new IllegalStateException("Unknown shape type: " + prim.getType());
			}
		}
		
		if (node.getClass() == Billboard.class) {
			Billboard bb = (Billboard) node;
			copy = new Billboard(copyName, bb.getWidth(), bb.getHeight(), bb.getTexture());
			((Billboard) copy).setTextureURL(bb.getTextureURL());
		}
		
		copy.getTransform().setTransform(node.getTransform());
		copy.setVisible(node.isVisible());
		content.getNode().addChild(copy);
		
		nodesList.setSelectedValue(null, false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(copyName, false);
	}
	
	@Action
	public void renameNodeClicked() {
		if (nodePanel.getNode() == null) {
			Popups.message(frame, settings.getText("editor.gui.nonodeselected"));
			return;
		}
		
		SceneGraphNode node = nodePanel.getNode();
		String name = Popups.inputMessage(frame, settings.getText("editor.gui.renamenode"), node.getName());
		if ((name == null) || (name.length() == 0)) {
			return;
		}
		
		node.setName(name);
		nodesList.setSelectedValue(null, false);
		nodesList.setListData(getChildNodeNames());
		nodesList.setSelectedValue(name, false);
	}
	
	/**
	 * Wrapper class around the form fields for the selected node. Creating this
	 * class will also add all fields to the specified form.
	 */
	private static class NodePanel {
		
		private SceneGraphNode node = null;
		private Settings settings = Settings.getInstance();
		
		private JLabel nodeLabel;
		private JComboBox nodeTypeList;
		private PropertyTextField[] posFields;
		private PropertyTextField[] rotFields;
		private PropertyTextField scaleField;
		private JButton transformButton;
		
		public NodePanel(FormPanel form) {
			nodeLabel = new JLabel("");
			nodeLabel.setFont(nodeLabel.getFont().deriveFont(Font.BOLD));
			form.addRow(nodeLabel);
			
			nodeTypeList = new JComboBox(NODE_TYPES);
			nodeTypeList.setEnabled(false);
			form.addRow(settings.getText("editor.gui.type"), nodeTypeList);
			
			posFields = new PropertyTextField[3];
			initField(posFields[0] = new PropertyTextField());
			initField(posFields[1] = new PropertyTextField());
			initField(posFields[2] = new PropertyTextField());
			form.addRow(settings.getText("editor.gui.position"), posFields[0], posFields[1], posFields[2]);
			
			rotFields = new PropertyTextField[3];
			initField(rotFields[0] = new PropertyTextField());
			initField(rotFields[1] = new PropertyTextField());
			initField(rotFields[2] = new PropertyTextField());
			form.addRow(settings.getText("editor.gui.rotation"), rotFields[0], rotFields[1], rotFields[2]);
			
			initField(scaleField = new PropertyTextField());
			form.addRow(settings.getText("editor.gui.scale"), scaleField);
			
			transformButton = new JButton(settings.getText("editor.gui.transform"));
			transformButton.setToolTipText(settings.getText("editor.tooltip.transform"));
			transformButton.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent e) {
					freeTransform(e);
				}
			});
			form.addRow("", transformButton);
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
			this.node = node;
			setEnabled(node != null);
			nodeLabel.setText(node != null ? node.getName() : "");
			nodeTypeList.setSelectedItem(node != null ? node.getClass().getSimpleName() : "");
			if (node != null) {
				sync();
			}
		}
		
		public SceneGraphNode getNode() {
			return node;
		}
		
		public void sync() {
			Vector3D position = node.getTransform().getPosition();
			posFields[0].setText(position.getX(), 1);
			posFields[1].setText(position.getY(), 1);
			posFields[2].setText(position.getZ(), 1);
			
			Vector3D rotation = node.getTransform().getRotation();
			rotFields[0].setText(rotation.getX(), 1);
			rotFields[1].setText(rotation.getY(), 1);
			rotFields[2].setText(rotation.getZ(), 1);
			
			Vector3D scale = node.getTransform().getScale();
			scaleField.setText(scale.getX(), 3);
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
			Vector3D position = node.getTransform().getPosition();
			position.setVector(posFields[0].getTextFloat(), posFields[1].getTextFloat(),
					posFields[2].getTextFloat());
			Vector3D rotation = node.getTransform().getRotation();
			rotation.setVector(rotFields[0].getTextFloat(), rotFields[1].getTextFloat(),
					rotFields[2].getTextFloat());
			Vector3D scale = node.getTransform().getScale();
			scale.setVector(scaleField.getTextFloat(), scaleField.getTextFloat(), scaleField.getTextFloat());
		}
		
		private void freeTransform(KeyEvent e) {
			float deltaX = 0f;
			float deltaZ = 0f;
			float rot = 0f;
			
			switch (e.getKeyCode()) {
				case KeyEvent.VK_LEFT : deltaX = MOVE_AMOUNT; break;
				case KeyEvent.VK_RIGHT : deltaX = -MOVE_AMOUNT; break;
				case KeyEvent.VK_UP : deltaZ = MOVE_AMOUNT; break;
				case KeyEvent.VK_DOWN : deltaZ = -MOVE_AMOUNT; break;
				case KeyEvent.VK_COMMA : rot = -ROTATE_AMOUNT; break;
				case KeyEvent.VK_PERIOD : rot = ROTATE_AMOUNT; break;
				default : break;
			}
			
			if (e.isShiftDown()) {
				deltaX *= 10;
				deltaZ *= 10;
				rot *= 10;
			}
			
			node.getTransform().getPosition().add(deltaX, 0f, deltaZ);
			node.getTransform().getRotation().add(0f, rot, 0f);
			
			sync();
		}
	}
}

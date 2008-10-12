//--------------------------------------------------------------------------------
// Ferrari3D 
// Editor
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.editor;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.util.data.*;
import com.dennisbijlsma.util.swing.*;
import com.dennisbijlsma.util.swing.mac.*;



public class Editor extends JFrame implements ActionListener {
	
	public static final String VERSION="2.3";
		
	private F3dFile file;
	private EditorGUI sidebar;
	private Preview preview;
	
	private JMenuItem newItem,openItem,importItem,saveItem,saveAsItem,closeItem,exitItem;
	private JMenuItem cloneItem,checkCarItem,checkCircuitItem,convertItem,metaItem,textsItem,editMenuItem;
	private JMenuItem helpItem,aboutItem;
		
	
	
	public Editor() {
		
		super();
		
		setTitle(Settings.getInstance().getText("editor.title"));
		setSize(Settings.EDITOR_SIZE);	
		setLocationRelativeTo(null);		
		setIconImage(Settings.EDITOR_ICON);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		setLayout(new BorderLayout());
		
		// Create Menu
		
		Settings settings=Settings.getInstance();
			
		JMenuBar menubar=new JMenuBar();
		this.setJMenuBar(menubar);
		
		JMenu fileMenu=new JMenu(settings.getText("editor.menu.file"));
		menubar.add(fileMenu);
		JMenu toolsMenu=new JMenu(settings.getText("editor.menu.tools"));
		menubar.add(toolsMenu);
		JMenu helpMenu=new JMenu(settings.getText("editor.menu.help"));
		menubar.add(helpMenu);
		
		setMenuItem(newItem=new JMenuItem(),settings.getText("editor.menu.new"),fileMenu,KeyEvent.VK_N,false);
		setMenuItem(openItem=new JMenuItem(),settings.getText("editor.menu.open"),fileMenu,KeyEvent.VK_O,false);
		setMenuItem(importItem=new JMenuItem(),settings.getText("editor.menu.import"),fileMenu,KeyEvent.VK_O,true);
		setMenuItem(saveItem=new JMenuItem(),settings.getText("editor.menu.save"),fileMenu,KeyEvent.VK_S,false);
		setMenuItem(saveAsItem=new JMenuItem(),settings.getText("editor.menu.saveas"),fileMenu,KeyEvent.VK_S,true);
		setMenuItem(closeItem=new JMenuItem(),settings.getText("editor.menu.close"),fileMenu,KeyEvent.VK_W,false);
		fileMenu.addSeparator();
		setMenuItem(exitItem=new JMenuItem(),settings.getText("editor.menu.exit"),fileMenu);
		
		setMenuItem(convertItem=new JMenuItem(),settings.getText("editor.menu.convert"),toolsMenu);
		setMenuItem(cloneItem=new JMenuItem(),settings.getText("editor.menu.clone"),toolsMenu);
		setMenuItem(checkCarItem=new JMenuItem(),settings.getText("editor.menu.checkcar"),toolsMenu);
		setMenuItem(checkCircuitItem=new JMenuItem(),settings.getText("editor.menu.checkcircuit"),toolsMenu);
		setMenuItem(metaItem=new JMenuItem(),settings.getText("editor.menu.meta"),toolsMenu,KeyEvent.VK_M,true);
		toolsMenu.addSeparator();
		setMenuItem(textsItem=new JMenuItem(),settings.getText("editor.menu.texts"),toolsMenu);
		setMenuItem(editMenuItem=new JMenuItem(),settings.getText("editor.menu.editmenu"),toolsMenu);
		
		setMenuItem(helpItem=new JMenuItem(),settings.getText("editor.menu.help"),helpMenu);
		helpMenu.addSeparator();
		setMenuItem(aboutItem=new JMenuItem(),settings.getText("editor.menu.about"),helpMenu);
		
		setMenuState();
		
		// Create GUI
		
		JPanel contentPanel=new JPanel(new BorderLayout(10,10));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		this.add(contentPanel,BorderLayout.CENTER);

		preview=new Preview();
		contentPanel.add(preview,BorderLayout.CENTER);
		
		sidebar=new EditorGUI(preview);
		sidebar.setPreferredSize(new Dimension(250,getHeight()));
		contentPanel.add(sidebar,BorderLayout.WEST);
	
		Thread t=new Thread("Ferrari3D-Editor-Preview") {
			public void run() {
				preview.init();
			}
		};
		t.start();
	}
	
	
	
	public void actionPerformed(ActionEvent e) {
	
		Object o=e.getSource();
	
		if (o==newItem) {
			File url=showFileDialog(true);
			if (url!=null) {
				createFile(url);
				openFile(url);
			}
		}
		
		if (o==openItem) { 
			File url=showFileDialog(false);
			if (url!=null) {
				openFile(url);
			}
		}
		
		if (o==importItem) { 
			File url=showImportDialog();
			if ((url!=null) && (url.exists())) {
				openFile(url);
			}
		}
		
		if (o==saveItem) { 
			saveFile(file.getSourceFile()); 
		}
		
		if (o==saveAsItem) { 
			File url=showFileDialog(true);
			if (url!=null) {
				file.setName(url.getName());
				file.setSourceFile(url);				
				saveFile(url);
			}
		}
		
		if (o==closeItem) { 
			String text=Settings.getInstance().getText("editor.close");
			if (PopUp.confirm(this,text+" '"+file.getName()+"'?")) {
				closeFile(); 
			}
		}
		
		if (o==exitItem) { exit(); }		
		if (o==cloneItem) { sidebar.cloneNode(sidebar.getSelectedNode()); }
		if (o==convertItem) { convertModel(); }	
		if (o==checkCarItem) { 
			if (file.isCarModel()) {
				PopUp.message(null,Settings.getInstance().getText("editor.message.checkcartrue"));
			} else {
				PopUp.message(null,Settings.getInstance().getText("editor.message.checkcarfalse"));
			}
		}
		if (o==checkCircuitItem) { 
			if (file.isCircuitModel()) {
				PopUp.message(null,Settings.getInstance().getText("editor.message.checkcircuittrue"));
			} else {
				PopUp.message(null,Settings.getInstance().getText("editor.message.checkcircuitfalse"));
			}
		}
		if (o==metaItem) { showMeta(); }
		if (o==textsItem) { editTexts(); }
		if (o==editMenuItem) { editMenu(); }
		if (o==helpItem) { }	
		if (o==aboutItem) {
			Settings settings=Settings.getInstance();
			PopUp.message(null,settings.getText("editor.title")+"\n"+settings.getText("menu.copyright")+
					"\n"+settings.getText("menu.version")+" "+VERSION);
		}
	}
		
	
	
	private void exit() {
		
		if (PopUp.confirm(this,Settings.getInstance().getText("editor.exit"))) {
			System.exit(0);
		}		
	}
	
	
	
	protected void createFile(File fileURL) {

		F3dFile file=new F3dFile(fileURL.getName(),fileURL);
		file.setMeta(F3dFile.META_AUTHOR,System.getProperty("user.name"));
		file.setMeta(F3dFile.META_DATE,Utils.getCurrentDate(false));
		file.setMeta(F3dFile.META_VERSION,VERSION);
		file.setMeta(F3dFile.META_PREVIEW,"preview.jpg");
		
		try {
			F3dLoader.getInstance().save(file);
		} catch (Exception e) {
			e.printStackTrace();
			PopUp.errorMessage(this,Settings.getInstance().getText("editor.error.new"));
			System.exit(1);
		}
	}
	
	
	
	protected void openFile(File url) {
		
		try {
			file=F3dLoader.getInstance().load(url.getAbsolutePath(),true,false);
		} catch (Exception e) {
			e.printStackTrace();
			PopUp.errorMessage(this,Settings.getInstance().getText("editor.error.open")+" '"+url+"'.");
			System.exit(1);
		}

		sidebar.setFile(file);
		preview.setFile(file);
		setMenuState();
	}
	
	
	
	protected void saveFile(File url) {
		
		file.setMeta(F3dFile.META_DATE,Utils.getCurrentDate(false));
		file.setMeta(F3dFile.META_VERSION,VERSION);
	
		try {
			F3dLoader.getInstance().save(file);
			PopUp.message(this,Settings.getInstance().getText("editor.savedas")+" '"+url.getName()+"'.");
		} catch (Exception ex) {
			ex.printStackTrace();
			PopUp.errorMessage(this,Settings.getInstance().getText("editor.error.save"));
			System.exit(1);
		}
	}
	
	
	
	protected void closeFile() {
						
		file=null;		
		sidebar.setFile(null);
		preview.setFile(null);
		setMenuState();
	}
	
	
	
	private File showFileDialog(boolean saveMode) {
		
		Settings s=Settings.getInstance();
		
		FileChooser dialog=new FileChooser();
		dialog.setTitle(saveMode ? s.getText("editor.savemodel") : s.getText("editor.openmodel"));
		dialog.setStartLocation(".");
		dialog.addFilter("f3d","Ferrari3D models");
		dialog.addIcon("f3d",new ImageIcon(Settings.getInstance().ICON));
		
		if (saveMode) {
			return dialog.showSaveDialog(null);
		} else {
			return dialog.showOpenDialog(null);
		}
	}
	
	
	
	private File showImportDialog() {
	
		String[] cars=Settings.getInstance().cars.toArray(new String[0]);
		String[] circuits=Settings.getInstance().circuits.toArray(new String[0]);
		String[] files=new String[cars.length+circuits.length];
		int index=0;
		
		for (String i : cars) { files[index]="cars/"+i+"/"+i+".f3d"; index++; }
		for (String i : circuits) { files[index]="circuits/"+i+"/"+i+".f3d";; index++; }
		
		JList importList=new JList(files);
		importList.putClientProperty(OSXHandler.QUAQUA_LIST,"striped");
		JScrollPane importPane=new JScrollPane(importList);
		importPane.setPreferredSize(new Dimension(300,150));
		importPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
				
		JPanel panel=new JPanel(new BorderLayout());
		panel.add(importPane,BorderLayout.CENTER);
		
		PopUp.message(this,Settings.getInstance().getText("editor.import"),panel);		
		
		if (importList.getSelectedValue()!=null) {
			return new File((String) importList.getSelectedValue());
		} else {
			return null;
		}
	}
	
	
	
	private void editTexts() {
		
		Map<String,String> texts=Settings.getInstance().getAllTexts();
		PropertyPanel textsPanel=new PropertyPanel(texts);
		textsPanel.setPreferredSize(new Dimension(600,textsPanel.getPreferredSize().height));
		textsPanel.putClientProperty(OSXHandler.QUAQUA_LIST,"striped");
		JScrollPane textsPane=PropertyPanel.createScrollPane(textsPanel,600,200);
		
		PopUp.message(this,Settings.getInstance().getText("editor.menu.texts"),textsPane);
	}
	
	
	
	private void editMenu() {
	
		FileChooser dialog=new FileChooser();
		dialog.setTitle(Settings.getInstance().getText("editor.openmenufile"));
		dialog.setStartLocation("data/menu");
		dialog.addFilter("bsh","BeanShell files");
		File source=dialog.showOpenDialog(null);
		
		if ((source==null) || (!source.exists())) {
			return;
		}
		
		String contents=DataLoader.loadTextFile(source,"UTF-8");
		
		if ((contents==null) || (contents.length()==0)) {
			PopUp.errorMessage(this,Settings.getInstance().getText("editor.error.editmenu"));
			return;
		}
		
		JTextArea textarea=new JTextArea(contents);
		JScrollPane textpane=new JScrollPane(textarea);
		textpane.setPreferredSize(new Dimension(600,400));
		textpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		textpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		PopUp.message(this,source.getName(),textpane);
	}
	
	
	
	private void convertModel() {
				
		FileChooser sourceDialog=new FileChooser();
		sourceDialog.setTitle(Settings.getInstance().getText("editor.convertsource"));
		sourceDialog.addFilter("obj,3ds,dae","3D models");
		File source=sourceDialog.showOpenDialog(null);
		
		FileChooser targetDialog=new FileChooser();
		targetDialog.setTitle(Settings.getInstance().getText("editor.converttarget"));
		targetDialog.addFilter("jme","3D models");
		targetDialog.setStartLocation(source!=null ? source.getParent() : "");
		File target=targetDialog.showSaveDialog(null);
		
		if ((source==null) || (target==null) || (target.exists())) {
			return;
		}
		
		try {
			F3dLoader.getInstance().convertModel(source,target);
			PopUp.message(this,Settings.getInstance().getText("editor.convertsuccess"));
		} catch (Exception e) {
			PopUp.errorMessage(this,Settings.getInstance().getText("editor.error.convert"));
			e.printStackTrace();
		}
	}
	
	
	
	private void showMeta() {
		
		Settings settings=Settings.getInstance();
		
		FormPanel form=new FormPanel(400);
		form.setPreferredSize(new Dimension(400,200));
		form.addRow(settings.getText("editor.meta.name"),new JLabel(file.getName()));
		form.addRow(settings.getText("editor.meta.author"),new JLabel(file.getMeta(F3dFile.META_AUTHOR)));
		form.addRow(settings.getText("editor.meta.date"),new JLabel(file.getMeta(F3dFile.META_DATE)));
		form.addRow(settings.getText("editor.meta.version"),new JLabel(file.getMeta(F3dFile.META_VERSION)));
		
		PopUp.message(null,settings.getText("editor.meta.title"),form);
	}
	
	
	
	private void setMenuItem(JMenuItem item,String label,JMenu menu,int key,boolean shift) {
		
		item.setText(label);
		item.addActionListener(this);
		menu.add(item);
		
		if (key!=-1) {
			item.setAccelerator(OSXHandler.getMenuKeyStroke(key,shift));
		}
	}
	
	
	
	private void setMenuItem(JMenuItem item,String label,JMenu menu) {
		
		setMenuItem(item,label,menu,-1,false);
	}
	
	
	
	private void setMenuState() {
		
		newItem.setEnabled(file==null);
		openItem.setEnabled(file==null);
		saveItem.setEnabled(file!=null);
		saveAsItem.setEnabled(file!=null);
		closeItem.setEnabled(file!=null);
		cloneItem.setEnabled(file!=null);
		checkCarItem.setEnabled(file!=null);
		checkCircuitItem.setEnabled(file!=null);
		metaItem.setEnabled(file!=null);
		helpItem.setEnabled(false);
	}
}
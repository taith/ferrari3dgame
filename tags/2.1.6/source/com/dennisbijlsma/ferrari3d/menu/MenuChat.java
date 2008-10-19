//--------------------------------------------------------------------------------
// Ferrari3D
// MenuChat
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.xmlserver.*;



public class MenuChat extends MenuWidget implements ConnectionListener {
	
	private ConnectionManager manager;
	
	private JScrollPane textpane;
	private JTextArea textarea;
	private JTextField input;
	
	private static final char[] ILLEGAL_CHARS={'\\','/','<','>','\'','"'};
	
	
	
	public MenuChat(ConnectionManager manager) {
		
		super();
		
		this.manager=manager;
		this.manager.addConnectionListener(this);
				
		textarea=new JTextArea();
		textarea.setWrapStyleWord(true);
		textarea.setEditable(false);
		textarea.setOpaque(false);
		textarea.setFont(FONT);
		textarea.setForeground(FONT_COLOR);
		textpane=new JScrollPane(textarea);
		textpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		textpane.setOpaque(false);
		textpane.setBorder(BorderFactory.createLineBorder(Color.WHITE,1));
		textpane.getViewport().setOpaque(false);
		this.add(textpane);
		
		input=new JTextField();
		input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (input.getText().length()>0) {
					sendMessage(Settings.getInstance().name,input.getText());
					input.setText("");
				}
			}
		});
		this.add(input);
	}
	
	
	
	@Override
	public void paintWidget(Graphics2D g2) {

		textpane.setBounds(0,0,getWidth(),getHeight()-35);
		//textarea.setBounds(0,0,getWidth(),getHeight()-35);		
		input.setBounds(0,getHeight()-25,getWidth(),25);
	}
	
	
	
	@Override
	public Dimension getWidgetSize() {
	
		return new Dimension(getWidth(),100);
	}



	@Override
	public void messageReceived(Connection connection,Message message) {
		
		if (message.getType().equals("chat")) {
			receiveMessage(message.getParameter("from"),message.getParameter("message"));
		}
	}
	
	
	
	@Override public void connected(Connection connection) { }
	@Override public void disconnected(Connection connection) { }
	@Override public void messageSent(Connection connection,Message message) { }

	
	
	public void sendMessage(String from,String text) {
		
		for (char i : ILLEGAL_CHARS) {
			text=text.replace(i,' ');
		}
	
		receiveMessage(from,text);
		
		Message message=new XMLMessage();
		message.setType("chat");
		message.setParameter("from",from);
		message.setParameter("message",text);
				
		manager.sendAll(message);
	}
	
	
	
	public void receiveMessage(String sender,String text) {
						
		textarea.append(sender+": "+text+"\n");
		textarea.moveCaretPosition(textarea.getText().length()-1);
	}
}
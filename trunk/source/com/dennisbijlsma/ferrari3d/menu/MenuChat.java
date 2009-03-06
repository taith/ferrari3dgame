//--------------------------------------------------------------------------------
// Ferrari3D
// MenuChat
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Server;
import com.dennisbijlsma.xmlserver.Message;



@SuppressWarnings("deprecation")
public class MenuChat extends MenuWidget implements ConnectionListener {
	
	private Server server;
	
	private JScrollPane textpane;
	private JTextArea textarea;
	private JTextField input;
	
	private static final char[] ILLEGAL_CHARS={'\\','/','<','>','\'','"'};
	
	
	
	public MenuChat(Server server) {
		
		super();
		
		this.server=server;
		server.addConnectionListener(this);
				
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



	public void messageReceived(Connection connection,Message message) {
		
		if (message.getType().equals("chat")) {
			receiveMessage(message.getParameter("from"),message.getParameter("message"));
		}
	}
	
	
	
	public void connected(Connection connection) { }
	public void disconnected(Connection connection) { }
	public void messageSent(Connection connection,Message message) { }

	
	
	public void sendMessage(String from,String text) {
		
		for (char i : ILLEGAL_CHARS) {
			text=text.replace(i,' ');
		}
	
		receiveMessage(from,text);
		
		Message message=new Message();
		message.setType("chat");
		message.setParameter("from",from);
		message.setParameter("message",text);
		server.send(message);
	}
	
	
	
	public void receiveMessage(String sender,String text) {
						
		textarea.append(sender+": "+text+"\n");
		textarea.moveCaretPosition(textarea.getText().length()-1);
	}
}
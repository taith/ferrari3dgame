//--------------------------------------------------------------------------------
// Ferrari3D
// UIChatWidget
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Message;
import com.dennisbijlsma.xmlserver.Server;

/**
 * Special type of widget that can send and receive chat messages. The widget will
 * listen to any incoming messages itself. Incoming messages are appended at the 
 * top of the chat window. Entering text is done by clicking on the component and
 * entering the message in a popup window. This is rather poor usability compared
 * to the old Swing version of the menu system, but considering the importance of
 * the chat functionality it is adequate for now.
 */

public class UIChatWidget extends UIMenuWidget implements ConnectionListener {
	
	private Server server;
	
	private static final int WIDGET_WIDTH=512;
	private static final int WIDGET_HEIGHT=128;
	private static final char[] ILLEGAL_CHARS={'\\','/','<','>','\'','"'};

	
	
	public UIChatWidget(Server server) {
	
		super(WIDGET_WIDTH,WIDGET_HEIGHT,false);
		
		this.server=server;
		server.addConnectionListener(this);
		
		repaintImages();
	}
	
	
	
	@Override
	protected void paintImage(BufferedImage image) {
		
		Graphics2D g2=getGraphics(image);
		clearGraphics(image,g2);
		g2.dispose();
	}



	@Override
	public Dimension getHitArea() {
	
		return new Dimension(WIDGET_WIDTH,WIDGET_HEIGHT);
	}
	
	
	
	public void messageReceived(Connection connection, Message message) {
		
		if (message.getType().equals("chat")) {
			receiveMessage(message.getParameter("from"),message.getParameter("message"));
		}
	}

	public void connected(Connection connection) { }
	public void disconnected(Connection connection) { }
	public void messageSent(Connection connection, Message message) { }
	
	/**
	 * Sends a chat message to all connected clients. The message is sent with
	 * the specified sender name.
	 */
	
	private void sendMessage(String from,String text) {
		
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
	
	/**
	 * Appends a message with the specified details to the list of received 
	 * messages.
	 */
	
	private void receiveMessage(String sender,String text) {
						
		//TODO
	}
}
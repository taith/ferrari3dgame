//-----------------------------------------------------------------------------
// Ferrari3D
// UIMenuChat
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.menu;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import com.dennisbijlsma.core3d.ui.ActionListener;
import com.dennisbijlsma.core3d.ui.UIWidget;
import com.dennisbijlsma.ferrari3d.MessageListener;
import com.dennisbijlsma.ferrari3d.Multiplayer;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.messaging.Message;
import nl.colorize.util.swing.Popups;

/**
 * Widget that displays incoming and outgoing chat messages. Messages can also
 * be sent by entering them in a dialog window.
 */
public class UIMenuChat extends UIMenuWidget implements MessageListener {
	
	private Multiplayer multiplayer;
	private List<String> messages;
	private UIMenuButton chatButton;
	
	private static final int COMPONENT_HEIGHT = 128;
	private static final int DISPLAYED = 6;
	
	/**
	 * Creates a new chat component that will use the specified multiplayer
	 * session.
	 */
	public UIMenuChat(Multiplayer multiplayer) {
		
		super(WIDGET_TEXTURE_WIDTH, COMPONENT_HEIGHT, false);
		
		this.multiplayer = multiplayer;
		this.multiplayer.addMessageListener(this);
		messages = new ArrayList<String>();
		
		chatButton = new UIMenuButton(Settings.getInstance().getText("menu.lobby.chat"));
		chatButton.addActionListener(new ActionListener() {
			public void actionPerformed(UIWidget source) {
				showChatWindow();
			}
		});
		
		repaint();
	}
	
	@Override
	protected void paintImage(Graphics2D g2) {
		
		g2.setFont(FONT);		
		g2.setColor(FONT_COLOR);
		
		int startIndex = Math.max(messages.size() - 6, 0);
		int y = LINE_HEIGHT_SMALL;
		for (int i = startIndex; i < messages.size(); i++) {
			g2.drawString(messages.get(i), 10, y);
			y += LINE_HEIGHT_SMALL;
		}

		g2.drawRect(0, 0, WIDGET_TEXTURE_WIDTH - 1, COMPONENT_HEIGHT - 1);
	}
	
	@Override
	public void repaint() {
		super.repaint();
		for (UIMenuWidget i : getAdditionalWidgets()) {
			i.repaint();
		}
	}
	
	@Override
	public List<UIMenuWidget> getAdditionalWidgets() {
		List<UIMenuWidget> list = super.getAdditionalWidgets();
		list.add(chatButton);
		return list;
	}
	
	@Override
	public void setPosition(int x, int y) {
		super.setPosition(x, y);
		chatButton.setPosition(x, y + COMPONENT_HEIGHT + 10);
	}
	
	public void addMessage(String from, String message) {
		messages.add(from + ": " + message);
		repaint();
	}
	
	private void showChatWindow() {
		String message = Popups.inputMessage(null, Settings.getInstance().getText("game.chatmessage"), "");
		if ((message != null) && (message.length() > 0)) {
			if (multiplayer.getNumParticipants() > 1) {
				multiplayer.sendChatMessage(Settings.getInstance().name, message);
			}
			addMessage(Settings.getInstance().name, message);
		}
	}

	public void messageReceived(Message message) {
		if (message.getType().equals(Multiplayer.MESSAGE_CHAT)) {
			addMessage(message.getParameter("from"), message.getParameter("message"));
		}
	}
}

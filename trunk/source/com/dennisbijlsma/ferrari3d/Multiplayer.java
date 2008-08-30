//--------------------------------------------------------------------------------
// Ferrari3D
// Multiplayer
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.*;

import com.dennisbijlsma.core3d.*;
import com.dennisbijlsma.ferrari3d.util.*;
import com.dennisbijlsma.xmlserver.*;

/**
 * Controls the multiplayer connections and allows for sending of messages. This
 * class uses <code>XML Server</code> initially. This allows all XML-based 
 * messages to be sent over a number of protocols.<br><br>
 * Sending should be done by calling one of the methods in this class, which will
 * convert the parameters to an XML messages and send it. Receiving messages can
 * however be done by any class implementing the <code>MPReceiver</code> interface.
 * These can be registered to listen to all messages for a specific ID, or to 
 * receive all generic messages.
 */

public class Multiplayer implements ConnectionListener {
	
	private XMLServer server;
	private ConnectionManager manager;
	private MessageQueue receiveQueue;
	
	public static final String MESSAGE_INIT="init";
	public static final String MESSAGE_DISCONNECT="disconnect";
	public static final String MESSAGE_CONNECT="connect";
	public static final String MESSAGE_UPDATE="update";
	public static final String MESSAGE_START="start";
	public static final String MESSAGE_STOP="stop";
	public static final String MESSAGE_START_SESSION="startSession";
	public static final String MESSAGE_STOP_SESSION="stopSession";
	public static final String MESSAGE_PAUSE="pause";
	public static final String MESSAGE_CHAT="chat";
	
	private static final Multiplayer INSTANCE=new Multiplayer();
		
	/**
	 * Private constructor as this class should not be initialized.
	 */
	
	private Multiplayer() {
		
		super();
		
		manager=new SocketManager(Settings.getInstance().MULTIPLAYER_PORT);
		manager.addConnectionListener(this);
		server=new XMLServer(manager);
		
		receiveQueue=new MessageQueue();		
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static Multiplayer getInstance() {
	
		return INSTANCE;
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void connected(Connection connection) {
		
		Message message=new XMLMessage();
		message.setType(MESSAGE_INIT);
		message.setParameter("id",connection.getMeta("id"));
		
		for (ConnectionListener i : manager.getConnectionListeners()) {
			i.messageReceived(connection,message);
		}
		
		print("Connected: "+connection.getHost());
	}
	
	/**
	 * {@inheritDoc}
	 */
	
	public void disconnected(Connection connection) { 
		
		Message message=new XMLMessage();
		message.setType(MESSAGE_DISCONNECT);
		message.setParameter("id",connection.getMeta("id"));
		
		for (ConnectionListener i : manager.getConnectionListeners()) {
			i.messageReceived(connection,message);
		}
		
		print("Disconnected: "+connection.getHost());
	}
	
	/**
	 * {@inheritDoc} 
	 */
	
	public void messageReceived(Connection connection,Message message) {
		
		receiveQueue.addMessage(message);
		print("<<< "+message);
	}
	
	/**
	 * {@inheritDoc}
	 */	
	
	public void messageSent(Connection connection,Message message) { 
		
		print(">>> "+message);
	}
	
	/**
	 * Sends the specified message. This method is the core of sending all 
	 * multiplayer messages. Numerous convenience methods are available in this
	 * class for creating and sending certain types of messages directly.
	 */
	
	private void sendMessage(Message message) {
	
		if (message instanceof XMLMessage) {
			((XMLMessage) message).setUseHeader(false);
		}
		
		manager.sendAll(message);
	}
	
	/**
	 * Sends a message with position and lap data for the specified contestant.
	 * This message should be sent every frame. When the game is running as a
	 * server, it should also send update data for all AI cars.
	 */	
	
	public void sendUpdateMessage(Contestant contestant) {
		
		Vector3D position=contestant.getPosition();
		
		Message message=new XMLMessage();
		message.setType(MESSAGE_UPDATE);
		message.setParameter("id",contestant.getID());
		message.setParameter("position",position.x+","+position.y+","+position.z);
		message.setParameter("orientation",""+contestant.getOrientation());
		message.setParameter("direction",""+contestant.getDirection());
		message.setParameter("lap",""+contestant.getLap());
		message.setParameter("intermediate",""+contestant.getIntermediate());
		message.setParameter("point",""+contestant.getPoint());
		message.setParameter("laptime",""+contestant.getCurrentLaptime().getCurrentTime());
		message.setParameter("speed",""+contestant.getSpeed(false));
		message.setParameter("angular",""+contestant.getAngular());
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the game has been started. This should be sent by the
	 * server when the menu is terminated and loading of the game is beginning. 
	 */
	
	public void sendStartMessage() {
		
		Message message=new XMLMessage();
		message.setType(MESSAGE_START);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the game has been stopped. This should be sent by the
	 * server when the game is terminated and the menu is recreated.
	 */
	
	public void sendStopMessage() {
	
		Message message=new XMLMessage();
		message.setType(MESSAGE_STOP);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the session is started. This method should be sent by
	 * the server, either directly after loading or when the green lights have been
	 * shown (in race mode).
	 */
	
	public void sendStartSessionMessage() {
	
		Message message=new XMLMessage();
		message.setType(MESSAGE_START_SESSION);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the session is stopped. This method can be sent by 
	 * both client and server, when they signal that one of the contestants has
	 * met the requirements needed to finish the session.
	 */
	
	public void sendStopSessionMessage() {
		
		Message message=new XMLMessage();
		message.setType(MESSAGE_STOP_SESSION);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the paused state has changed. This can be sent by both
	 * the client and server, when the game is paused or depaused by one of them.
	 */
	
	public void sendPauseMessage(boolean state) {
	
		Message message=new XMLMessage();
		message.setType(MESSAGE_PAUSE);
		message.setParameter("state",""+state);
		sendMessage(message);
	}
	
	/**
	 * Sends a chat message. This method can be sent both in the menu and in-game.
	 * The sender field for the message can be manually specified.
	 */
	
	public void sendChatMessage(String from,String chatMessage) {
	
		Message message=new XMLMessage();
		message.setType(MESSAGE_STOP);
		message.setParameter("from",from);
		message.setParameter("message",chatMessage);
		sendMessage(message);
	}
	
	/**
	 * Flushes all received messages since the last time this method was called.
	 * The messages will be returned to whoever called the method.<br><br>
	 * To prevent synchronization problems, this method must be called only
	 * during the game loop.
	 */
	
	public synchronized Message[] flushReceivedMessages() {
		
		return receiveQueue.flushAll();
	}
	
	/**
	 * Creates a new server connection with this class.
	 */
	
	public void connectAsServer() {
		
		server.connectServer();
	}
	
	/**
	 * Creates a new client connection with this class.
	 */
	
	public boolean connectAsClient() {
		
		return server.connectClient(Settings.getInstance().server);
	}
	
	/**
	 * Disconnects all connections. When this class is running as a server, it 
	 * will end the current session.
	 */
	
	public void disconnect() {
	
		server.disconnect();
	}
	
	/**
	 * Resets all variables to their original state. This method should be called
	 * between sessions.
	 */
	
	public void reset() {
	
		receiveQueue.clear();
	}
	
	/**
	 * Returns true if this class is currently connected. This method is a 
	 * shorthand for <code>XMLServer.isConnected()</code>.
	 */
	
	public boolean isConnected() {
	
		return server.isConnected();
	}
	
	/**
	 * Returns true if this class is currently running as a server. This method
	 * is a shorthand for <code>XMLServer.isServer()</code>. 
	 */
	
	public boolean isServer() {
		
		return server.isServer();
	}
	
	/**
	 * Returns the <code>XMLServer</code> that is used by this class.
	 */
	
	public XMLServer getXMLServer() {
		
		return server;
	}
	
	/**
	 * Returns the <code>ConnectionManager</code> that is used by this class.
	 */
	
	public ConnectionManager getConnectionManager() {
	
		return manager;
	}
	
	/**
	 * Method for controlling debug output. This output could be written to
	 * the console, to the log file, or not at all.
	 */
	
	private void print(String message) {
	
		if ((Ferrari3D.DEBUG_MODE) || (Settings.getInstance().debug)) {
			System.out.println("Multiplayer  "+System.currentTimeMillis()+"   "+message);
		}
	}
}
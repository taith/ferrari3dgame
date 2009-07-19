//--------------------------------------------------------------------------------
// Ferrari3D
// Multiplayer
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Message;
import com.dennisbijlsma.xmlserver.Server;

/**
 * Handles network multiplayer. This class uses an <code>XML Server</code> 
 * internally, which is used to send messages over the network. To send messages
 * to all connected players, use one of the <code>sendXXXMessage</code> methods.
 * As this class is a singleton, messages can be sent directly from any class.
 * Receiving messages can be done by any class, all received messages are injected
 * into the game loop through the <code>GameEventListener</code> interface. As
 * long as this mechanism is used, this class is thread safe.
 */

public class Multiplayer extends Server implements ConnectionListener {
	
	private List<Message> receiveQueue;
	
	public static final String MESSAGE_INIT = "init";
	public static final String MESSAGE_DISCONNECT = "disconnect";
	public static final String MESSAGE_CONNECT = "connect";
	public static final String MESSAGE_UPDATE = "update";
	public static final String MESSAGE_START = "start";
	public static final String MESSAGE_STOP = "stop";
	public static final String MESSAGE_START_SESSION = "startSession";
	public static final String MESSAGE_STOP_SESSION = "stopSession";
	public static final String MESSAGE_PAUSE = "pause";
	public static final String MESSAGE_CHAT = "chat";
	
	private static final MessageProtocol PROTOCOL = MessageProtocol.SERIALIZATION;
	private static final Multiplayer INSTANCE = new Multiplayer();
		
	/**
	 * Private constructor as this class should not be initialized.
	 */
	
	private Multiplayer() {
		
		super(PROTOCOL, Settings.MULTIPLAYER_PORT);
		
		receiveQueue = Collections.synchronizedList(new ArrayList<Message>());
		addConnectionListener(this);
	}
	
	/** {@inheritDoc} */
	
	@Override
	public void stop() {
		super.stop();
		receiveQueue.clear();
	}
	
	/** {@inheritDoc} */
	
	public void connected(Connection connection) {
		
		Message message = new Message();
		message.setType(MESSAGE_INIT);
		message.setParameter("id", connection.getMeta("id"));
		
		fireMessageReceived(connection, message);
		print("Connected: " + connection.getHost());
	}
	
	/** {@inheritDoc} */
	
	public void disconnected(Connection connection) { 
		
		Message message = new Message();
		message.setType(MESSAGE_DISCONNECT);
		message.setParameter("id", connection.getMeta("id"));
		
		fireMessageReceived(connection, message);
		print("Disconnected: " + connection.getHost());
	}
	
	/** {@inheritDoc} */
	
	public void messageReceived(Connection connection, Message message) {
		receiveQueue.add(message);
		print("<<< " + message);
	}
	
	/** {@inheritDoc} */	
	
	public void messageSent(Connection connection, Message message) { 
		print(">>> " + message);
	}
	
	/**
	 * Sends the specified message. This method is the core of sending all 
	 * multiplayer messages. Numerous convenience methods are available in this
	 * class for creating and sending certain types of messages directly.
	 */
	
	private void sendMessage(Message message) {
		send(message);
	}
	
	/**
	 * Sends a message with position and lap data for the specified contestant.
	 * This message should be sent every frame. When the game is running as a
	 * server, it should also send update data for all AI cars.
	 */	
	
	public void sendUpdateMessage(Contestant contestant) {
		
		Message message = new Message();
		message.setType(MESSAGE_UPDATE);
		message.setParameter("id", contestant.getName());
		message.setParameter("position", vectorToString(contestant.getPosition()));
		message.setParameter("orientation", "" + contestant.getOrientation());
		message.setParameter("direction"," " + contestant.getDirection());
		message.setParameter("lap", "" + contestant.getLap());
		message.setParameter("intermediate", "" + contestant.getIntermediate());
		message.setParameter("point", "" + contestant.getPoint());
		message.setParameter("laptime", "" + contestant.getCurrentLaptime().getTime());
		message.setParameter("speed", "" + contestant.getSpeed());
		message.setParameter("angularSpeed", "" + contestant.getAngularSpeed());
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the game has been started. This should be sent by the
	 * server when the menu is terminated and loading of the game is beginning. 
	 */
	
	public void sendStartMessage() {
		
		Message message = new Message();
		message.setType(MESSAGE_START);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the game has been stopped. This should be sent by the
	 * server when the game is terminated and the menu is recreated.
	 */
	
	public void sendStopMessage() {
	
		Message message = new Message();
		message.setType(MESSAGE_STOP);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the session is started. This method should be sent by
	 * the server, either directly after loading or when the green lights have been
	 * shown (in race mode).
	 */
	
	public void sendStartSessionMessage() {
	
		Message message = new Message();
		message.setType(MESSAGE_START_SESSION);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the session is stopped. This method can be sent by 
	 * both client and server, when they signal that one of the contestants has
	 * met the requirements needed to finish the session.
	 */
	
	public void sendStopSessionMessage() {
		
		Message message = new Message();
		message.setType(MESSAGE_STOP_SESSION);
		sendMessage(message);
	}
	
	/**
	 * Sends a message that the paused state has changed. This can be sent by both
	 * the client and server, when the game is paused or depaused by one of them.
	 */
	
	public void sendPauseMessage(boolean state) {
	
		Message message = new Message();
		message.setType(MESSAGE_PAUSE);
		message.setParameter("state", "" + state);
		sendMessage(message);
	}
	
	/**
	 * Sends a chat message. This method can be sent both in the menu and in-game.
	 * The sender field for the message can be manually specified.
	 */
	
	public void sendChatMessage(String from, String chatMessage) {
	
		Message message = new Message();
		message.setType(MESSAGE_STOP);
		message.setParameter("from", from);
		message.setParameter("message", chatMessage);
		sendMessage(message);
	}
	
	/**
	 * Flushes all received messages since the last time this method was called.
	 * The messages will be returned to whoever called the method.<br><br>
	 * To prevent synchronization problems, this method must be called only
	 * during the game loop.
	 */
	
	public Message[] flushReceivedMessages() {
		Message[] messages = receiveQueue.toArray(new Message[0]);
		receiveQueue.clear();
		return messages;
	}
	
	/**
	 * Method for controlling debug output. This output could be written to
	 * the console, to the log file, or not at all.
	 */
	
	private void print(String message) {
		if (Settings.getInstance().debug) {
			System.out.println("Multiplayer  " + "   " + message);
		}
	}
	
	/**
	 * Returns the specified vector as a string, so that it can be sent as a
	 * parameter in messages.
	 */
	
	private String vectorToString(ImmutableVector3D v) {
		return v.getX() + "," + v.getY() + "," + v.getZ();
	}
	
	/**
	 * Returns the only existing instance of this class.
	 */
	
	public static Multiplayer getInstance() {
		return INSTANCE;
	}
}
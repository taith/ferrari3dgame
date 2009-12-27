//-----------------------------------------------------------------------------
// Ferrari3D
// Multiplayer
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.ferrari3d.util.Settings;
import com.dennisbijlsma.messaging.ConnectionListener;
import com.dennisbijlsma.messaging.JoinSessionException;
import com.dennisbijlsma.messaging.Message;
import com.dennisbijlsma.messaging.MessageQueue;
import com.dennisbijlsma.messaging.MessagingClient;
import com.dennisbijlsma.messaging.Participant;
import com.dennisbijlsma.messaging.Server;
import com.dennisbijlsma.messaging.ServletMessagingClient;
import com.dennisbijlsma.messaging.SocketMessagingClient;

/**
 * Handles all communication for a multiplayer session. This class can change
 * between a number of networking implementations, allowing for both local 
 * network play as well as internet multiplayer. All received messages are 
 * placed in a queue, which is flushes during the game loop. Any class that wants
 * to be notified of incoming messages can register itself by implementing the 
 * {@code MessageListener} interface. 
 */
public class Multiplayer implements ConnectionListener {
	
	private Server server;
	private MessagingClient client;
	private MessageQueue receiveQueue;
	private List<MessageListener> messageListeners;
	private boolean isServer;
	private float updateInterval;
	
	private static Map<Session,Multiplayer> instances = new ConcurrentHashMap<Session,Multiplayer>();
	private static Settings settings = Settings.getInstance();
	
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
	
	private static final float UPDATE_INTERVAL_NETWORK = 0.04f;
	private static final float UPDATE_INTERVAL_INTERNET = 2f;
	private static final long POLL_TIME = 3000;
		
	/**
	 * Private constructor. Use the static factory method 
	 * {@link #getInstanceForSession(Session)} to obtain an instance.
	 */
	private Multiplayer() {
		receiveQueue = new MessageQueue();
		messageListeners = new CopyOnWriteArrayList<MessageListener>();
	}
	
	/**
	 * Starts a server so that other players can join the session. Calling this
	 * method will automatically create a client that will join the local server.
	 * @throws IOException if the creation of a server fails.
	 * @throws JoinSessionException see {@link #startAsClient(boolean)}.
	 */
	public void startAsServer() throws IOException, JoinSessionException {
		
		isServer = true;
		
		if (settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL) {
			server = new Server(Settings.LOCAL_MULTIPLAYER_PORT);
			startAsClient(true);
		} else if (settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET) {
			// Internet multiplayer doesn't require a local server, so just start 
			// the client and join the session.
			startAsClient(true);
		} else {
			throw new AssertionError();
		}
	}
	
	/**
	 * Joins the session on the specified server.
	 * @param sessionOwner When true, this participant owns the session. 
	 * @throws IOException if a connection to the server cannot be created.
	 * @throws JoinSessionException if a participant with the same name already
	 *         exists, or if the session is full. 
	 */
	public void startAsClient(boolean sessionOwner) throws IOException, JoinSessionException {
	
		isServer = sessionOwner;
		
		if (settings.multiplayerMode == Settings.MULTIPLAYER_LOCAL) {
			client = new SocketMessagingClient(settings.multiplayerServer, 
					Settings.LOCAL_MULTIPLAYER_PORT);
			updateInterval = UPDATE_INTERVAL_NETWORK;
		} else if (settings.multiplayerMode == Settings.MULTIPLAYER_INTERNET) {
			client = new ServletMessagingClient(Settings.INTERNET_MULTIPLAYER_SERVER, POLL_TIME);
			updateInterval = UPDATE_INTERVAL_INTERNET;
		} else {
			throw new AssertionError();
		}

		client.addConnectionListener(this);
		
		if (sessionOwner) {
			client.createSession(settings.multiplayerSession, settings.name);
		} else {
			client.joinSession(settings.multiplayerSession, settings.name);	
		}
	}
	
	public void stop() {
		if (server != null) {
			server.disconnect();
		}
		client.disconnect();
		messageListeners.clear();
	}
	
	public boolean isServer() {
		return isServer;
	}
	
	/**
	 * Returns the participant with the specified name, or {@code null} if no
	 * such participant exists in the current multiplayer session.
	 */
	public Participant getParticipant(String name) {
		return client.currentSession().getParticipant(name);
	}
	
	/**
	 * Returns the set of all participants currently in the multiplayer session.
	 * Note that this set might also contain local participants. 
	 */
	public Set<Participant> getParticipants() {
		Set<Participant> copy = new HashSet<Participant>();
		copy.addAll(client.currentSession().getParticipants());
		return copy;
	}
	
	public int getNumParticipants() {
		if (client == null) {
			return 0;
		}
		return client.currentSession().getNumParticipants();
	}
	
	/**
	 * Invoked whenever one of the other clients connects to the current
	 * session. Connects will be sent to all listeners as 'fake' messages.
	 */
	public void connected(Participant participant) {
		
		Message message = new Message();
		message.setType(MESSAGE_INIT);
		message.setParameter("id", participant.getName());
		receiveQueue.add(message);
		
		if (settings.debug) {
			settings.getLogger().info("Connected: " + participant.getName());
		}
	}
	
	/**
	 * Invoked whenever one of the other clients disconnects from the current
	 * session. Disconnects will be sent to all listeners as 'fake' messages.
	 */
	public void disconnected(Participant participant) { 
		
		Message message = new Message();
		message.setType(MESSAGE_DISCONNECT);
		message.setParameter("id", participant.getName());
		receiveQueue.add(message);
		
		if (settings.debug) {
			settings.getLogger().info("Disconnected: " + participant.getName());
		}
	}
	
	/**
	 * Invoked when a message has been received from one of the other clients.
	 * The message will be added to the receive queue, which will be flushed
	 * during the next frame.
	 */
	public void messageReceived(Message message) {
		receiveQueue.add(message);
		if (settings.debug) {
			settings.getLogger().info("<<< " + message.toString());
		}
	}
	
	/**
	 * Sends the specified message. This method is the core of sending all 
	 * multiplayer messages. Numerous convenience methods are available in this
	 * class for creating and sending certain types of messages directly.
	 */
	private void sendMessage(Message message) {
		client.send(message);
		if (settings.debug) {
			settings.getLogger().info(">>> " + message.toString());
		}
	}
	
	/**
	 * Sends the initial connection message, containing basic information such
	 * as the driver name, car, etc.
	 */
	public void sendConnectMessage() {
		Message message = new Message();
		message.setType(MESSAGE_CONNECT);
		message.setParameter("id", settings.name);
		message.setParameter("car", settings.car);
		message.setParameter("circuit", settings.circuit);
		message.setParameter("mode", "" + settings.mode);
		message.setParameter("aiActive", "" + settings.aiActive);
		message.setParameter("aiLevel", "" + settings.aiLevel);
		message.setParameter("laps", "" + settings.laps);
		message.setParameter("ip", Server.getLocalHost());
		message.setParameter("version", Ferrari3D.VERSION.toString());
		sendMessage(message);
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
		message.setParameter("direction", "" + contestant.getDirection());
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
		message.setType(MESSAGE_CHAT);
		message.setParameter("from", from);
		message.setParameter("message", chatMessage);
		sendMessage(message);
	}
	
	private String vectorToString(ImmutableVector3D v) {
		return v.getX() + "x" + v.getY() + "x" + v.getZ();
	}
	
	/**
	 * Flushes the queue of all received messages since the last time this method 
	 * was called. All listeners will be notified for each received message.
	 */
	public void flushReceivedMessages() {
		Message[] flushed = receiveQueue.flush();
		for (Message i : flushed) {
			for (MessageListener j : messageListeners) {
				j.messageReceived(i);
			}
		}
	}
	
	public void addMessageListener(MessageListener ml) {
		if (!messageListeners.contains(ml)) {
			messageListeners.add(ml);
		}
	}
	
	public void removeMessageListener(MessageListener ml) {
		messageListeners.remove(ml);
	}
	
	public float getUpdateInterval() {
		return updateInterval;
	}
	
	/**
	 * Returns the {@code Multiplayer} instance for the specified session. 
	 * Multiple invocations of this method for the same session will keep
	 * returning the same instance.  
	 */
	public static Multiplayer getInstanceForSession(Session session) {
		if (instances.containsKey(session)) {
			return instances.get(session);
		}
		Multiplayer instance = new Multiplayer();
		instances.put(session, instance);
		return instance;
	}
}

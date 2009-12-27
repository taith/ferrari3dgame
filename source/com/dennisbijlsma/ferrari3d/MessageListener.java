//-----------------------------------------------------------------------------
// Ferrari3D
// MessageListener
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.messaging.Message;

/**
 * Implementations of this interface can register themselves with the 
 * {@code Multiplayer} class to be notified of any incoming messages. The 
 * callback methods in this class are always called from the game loop.
 */
public interface MessageListener {

	/**
	 * Invoked when a message has been received. This method is always invoked
	 * from inside the game thread.
	 * @param message The message that has been received.
	 */
	public void messageReceived(Message message);
}

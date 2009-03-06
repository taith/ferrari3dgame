package com.dennisbijlsma.ferrari3d.test;

import com.dennisbijlsma.ferrari3d.Multiplayer;
import com.dennisbijlsma.xmlserver.Connection;
import com.dennisbijlsma.xmlserver.ConnectionListener;
import com.dennisbijlsma.xmlserver.Message;



public class TestMultiplayer implements ConnectionListener {

	
	
	public static void main(String[] args) {
	
		try {
			new TestMultiplayer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	public TestMultiplayer() throws Exception {
		
		Multiplayer multiplayer=Multiplayer.getInstance();
		multiplayer.addConnectionListener(this);
		multiplayer.startServer();
	}



	public void connected(Connection connection) {
		System.out.println("connected to "+connection.getHost());
	}

	public void disconnected(Connection connection) {
		System.out.println("disconnected from "+connection.getHost());
	}

	public void messageReceived(Connection connection,Message message) {
		System.out.println("received "+message);
	}

	public void messageSent(Connection connection,Message message) {
		System.out.println("sent "+message);
	}
}
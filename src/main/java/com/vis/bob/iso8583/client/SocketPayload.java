package com.vis.bob.iso8583.client;

import java.net.Socket;

public class SocketPayload {

	private byte[] data;
	private Socket socket;
	
	public SocketPayload(byte[] data, Socket socket) {
		this.data = data;
		this.socket = socket;
	}

	public byte[] getData() {
		return data;
	}

	public Socket getSocket() {
		return socket;
	}
}

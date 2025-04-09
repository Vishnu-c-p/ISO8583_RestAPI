package com.vis.bob.iso8583.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;

import com.vis.bob.iso8583.exception.ConnectionException;
import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.vo.MessageVO;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ISOConnection {

	private final static int SLEEP_TIME = 50;
	
	private boolean connected = false;
	private boolean used = false;
	private long lastAction = 0;
	
	private HashMap<String, CallbackAction> callbackMap = new HashMap<String, CallbackAction>();

	private final PayloadQueue payloadQueue = new PayloadQueue();
	private HashMap<String, Sender> senderMap = new HashMap<String, ISOConnection.Sender>();
	
	private String host;
	private int port;
	private int timeout;
	
	private ISOClient isoClient;
	
	public ISOConnection(String host, int port, int timeout) {
		this.host = host;
		this.port = port;
		this.timeout = (timeout * 1000);
	}
	
	public void connect(Iso8583Config isoConfig, String threadName) throws IOException, ConnectionException {
		if (isoConfig != null && callbackMap.containsKey(threadName)) {
			if(isoConfig.getXmlFilePath()!=null){
				registerActionTimeMilis();
				
				this.isoClient = new ISOClient(host, port, isoConfig, payloadQueue);
	
				registerSender(threadName);
			}
			else{
				throw new ConnectionException("ISOConfig is missing");
			}
		}
		else {
			throw new ConnectionException("ISOConfig or Callback is missing");
		}
	}
	
	public void registerSender(String threadName) {
		connected = true;
		if (!senderMap.containsKey(threadName)) {
			senderMap.put(threadName, new Sender(threadName));
			senderMap.get(threadName).start();
		}
	}
	
//	public boolean isConnected() {
//		return connected;
//	}
	
	public void endConnection(String threadName) {
		connected = false;
		lastAction = 0;
		
		if (isoClient != null) isoClient.closeConnection();

		isoClient = null;
	}
	
	private void waitRequest(String threadName, int keepaliveTimeout) throws InterruptedException {
		log.debug("Waiting for Response from CBS");
		while (connected && !payloadQueue.hasMorePayloadIn()) {
			Thread.sleep(SLEEP_TIME);
			
			if (keepaliveTimeout > 0 && (System.currentTimeMillis() - startOfWaitNextRequest) > (keepaliveTimeout * 1000)) {
				startOfWaitNextRequest = System.currentTimeMillis();
				callbackMap.get(threadName).keepalive();
			}
			
		}
	}

	private long startOfWaitNextRequest = 0;
	public synchronized MessageVO processNextPayload(Iso8583Config isoConfig, String threadName, boolean waitIfThereIsNothingAtQueue, int keepaliveTimeout) throws ParseException, InterruptedException {
		
		if (waitIfThereIsNothingAtQueue) {
			startOfWaitNextRequest = System.currentTimeMillis();
			waitRequest(threadName, keepaliveTimeout);
		}
		
		if (payloadQueue.hasMorePayloadIn()) {
			log.debug("Parsing bytes for Thread ["+threadName+"]...");
			callbackMap.get(threadName).dataReceived(isoConfig, payloadQueue.getNextPayloadIn());
		}
		
		return new MessageVO();
	}
	
	public void putCallback(String threadName, CallbackAction callback) {
		callbackMap.put(threadName, callback);
	}

	public void send(SocketPayload payload) throws IOException, ParseException, InterruptedException {
		payloadQueue.addPayloadOut(payload);
	}
	
	public void registerActionTimeMilis() {
		lastAction = System.currentTimeMillis();
	}
	
	public String getHost() {
		return host;
	}
	
	public int getPort() {
		return port;
	}
	
	public Socket getClientSocket() {
		Socket socket = null;
		if (isoClient != null)
			socket = isoClient.getSocket();
		return socket;
	}
	
	private class Sender extends Thread {
		
		private String threadName;
		
		Sender(String threadName) {
			setName("Sender");
			this.threadName = threadName;
		}
		
		public void run() {
			try {
				while (connected) {
					try {
						if (payloadQueue.hasMorePayloadOut()) {
							send(payloadQueue.getNextPayloadOut());
						}

						sleep(SLEEP_TIME);
					}
					catch (SocketException se) {
						log.debug("Client disconnected...");
					}
					
					if (timeout > 0 && timeout < (System.currentTimeMillis() - lastAction)) {
						log.debug("Connection timeout.");
						connected = false;
						lastAction = 0;
					}
				}
			}
			catch (Exception x) {
				if (connected) {
					x.printStackTrace();
					log.error("ISOConnection - ERROR "+ x.getMessage());
				}
			}
			finally {
				log.debug("Connection closed.");
				endConnection(threadName);
			}
		}
		
		private void send(SocketPayload payload) throws IOException {
			synchronized(this) {
				registerActionTimeMilis();
				
				if (payload == null) {
					log.warn("ISOConnection - Null payload");
				}
				else if (payload.getSocket() != null && !payload.getSocket().isClosed()) {
					OutputStream out = payload.getSocket().getOutputStream();
					
					log.info("ISOConnection - Sending data...");
					
					out.write(payload.getData());
			        out.flush();
				}
				else {
					log.error("ISOConnection - Impossible to send the payload, the socket is closed!");
				}
			}
		}

	}
	
	public void setIsoConfig(Iso8583Config isoConfig) {
		this.isoClient.setIsoConfig(isoConfig);
	}
}
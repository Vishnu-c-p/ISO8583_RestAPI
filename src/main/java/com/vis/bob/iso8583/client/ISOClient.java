package com.vis.bob.iso8583.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.vis.bob.iso8583.exception.InvalidPayloadException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.util.ISOUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ISOClient extends Thread {

	private PayloadQueue payloadQueue;

	private Socket socket;
	private InputStream input;
	private List<Byte> bytes;
	
	Iso8583Config isoConfig;

	private boolean isConnected;

	public ISOClient(Socket cliSocket, Iso8583Config isoConfig, PayloadQueue payloadQueue) {
		this.socket = cliSocket;
		this.isConnected = true;
		this.payloadQueue = payloadQueue;
		this.isoConfig = isoConfig;

		setName("Client-"+ cliSocket.getInetAddress().getHostAddress() + "-" + cliSocket.getPort());

		start();
	}

	public ISOClient(String host, int port, Iso8583Config isoConfig, PayloadQueue payloadQueue) throws UnknownHostException, IOException {
		this(new Socket(host, port), isoConfig, payloadQueue);
	}

	public void run() {
		String clientName = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
		long parsingTime = 0;
		int ix;
		int ibt;
		boolean l;
		l=true;
		log.info("ISOClient - Client connected ["+ clientName +"]");

		try {
			while (isConnected) {

				input = socket.getInputStream();

				try {
					bytes = new ArrayList<Byte>();
					ibt=0;
					byte bRead;
					parsingTime = 0;

					while (this.isConnected) {
						ix=input.read();
						if(ix>=0){
							bRead = Byte.valueOf((byte)ix);
							if(isoConfig.getStxEtx() && ibt<4)
								l=false;
							else
								l=true;
							
							if(l)
								bytes.add(Byte.valueOf(bRead));

							if (isoConfig.getDelimiter().isPayloadComplete(bytes, isoConfig)) {
								//Message complete.
								break;
							}

							if (parsingTime == 0) parsingTime = System.currentTimeMillis();
							ibt++;
						}
						else {
							//TODO: verify if it is really necessary
							bytes.clear();
						}
					}

					log.trace("Bytes Recieved ["+ bytes.toString() +"]");

					if (this.isConnected) {
						byte[] data = isoConfig.getDelimiter().clearPayload(ISOUtils.listToArray(bytes), isoConfig);

						if(data.length!=65537) {
							payloadQueue.addPayloadIn(new SocketPayload(data, socket));
						}
						String hexResponse = ISOUtils.bytesToHex(data);
						log.trace("Response Payload HEX - ["+ hexResponse +"]");
						String ascciiResponse =  ISOUtils.hexToAsccii(hexResponse);
						log.debug("Response Payload ASCII - ["+ ascciiResponse +"]");

						String hexMTI = hexResponse.substring(0,8);
						log.trace("MTI HEX - ["+ hexMTI +"]");
						String hexBitMap = hexResponse.substring(8,40);
						log.trace ("Bitmap HEX - ["+ hexBitMap +"]");
						String hexData = hexResponse.substring(40);
						log.trace("Data HEX - ["+ hexData +"]");
						String binaryBitMap = ISOUtils.hexToBin(hexBitMap);
						log.trace("Bitmap Binary - ["+ binaryBitMap +"]");
						String convertedASCIIPayload = ISOUtils.hexToAsccii(hexMTI)+binaryBitMap+ISOUtils.hexToAsccii(hexData);
						log.trace("ASCCII Payload Recieved - ["+ convertedASCIIPayload +"]");
						data = convertedASCIIPayload.getBytes();
					}
				}
				catch (InvalidPayloadException e) {
					log.error("Invalid Payload ("+ e.getMessage() +")");
					StringWriter errors = new StringWriter();
					e.printStackTrace(new PrintWriter(errors));
					log.error("ISOClient - " + errors.toString());
				}
				catch (Exception ex) {
					log.trace("Socket Closed ("+ ex.getMessage() +")");
				}
			}
		}
		catch (Exception x) {
			log.trace("Catch Error "+ x.getMessage());
			log.trace("IsConnected = "+ isConnected);
		}
		finally {
			try {
				socket.close();
			}
			catch (Exception x) {
				log.trace(x.getMessage());
				StringWriter errors = new StringWriter();
				x.printStackTrace(new PrintWriter(errors));
				log.trace(errors.toString());
			}
			finally {
				log.trace("2finally");
				socket = null;
			}
		}

		log.debug("Client disconnected ["+ clientName +"]");
	}

	public Socket getSocket() {
		return socket;
	}

	public void closeConnection() {
		this.isConnected = false;
		try {
			socket.close();
		}
		catch (Exception x) {
			log.trace(x.getMessage());
		}
		finally {
			socket = null;
		}
	}

	public void setIsoConfig(Iso8583Config isoConfig) {
		this.isoConfig = isoConfig;
	}
	
}

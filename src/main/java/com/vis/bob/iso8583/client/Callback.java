package com.vis.bob.iso8583.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.helper.PayloadMessageConfig;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Callback extends CallbackAction {
	
	private Socket socketToRespnd;
	
	public Callback(PayloadMessageConfig payloadMessageConfig) {
		this.payloadMessageConfig = payloadMessageConfig;
	}
	
	//@Autowired
	PayloadMessageConfig payloadMessageConfig;
	
	public void dataReceived(Iso8583Config isoConfig, SocketPayload payload) throws ParseException {
		
		log.debug("Callback - Response Recieved from Server");	
		if(payload.getData().length != 65537) {
			try {payloadMessageConfig.setMessage(payloadMessageConfig, isoConfig, "1210");
				payloadMessageConfig.updateFromPayload(isoConfig, payload.getData());
				socketToRespnd = payload.getSocket();
			}
			catch(Exception ex) {
				log.error(Instant.now()+" - dataReceived - payload.size={"+payload.getData().length+"}. Error parsin data received. "+ex.getMessage());
				ex.printStackTrace();
				
				StringWriter errors = new StringWriter();
				ex.printStackTrace(new PrintWriter(errors));
				log.error("callback parse error -> " + errors.toString());
			}
		}
		else {
			log.error("dataReceived - dirty message");
		}
	}
	
	public Socket getSocketToRespond() {
		return socketToRespnd;
	}
}
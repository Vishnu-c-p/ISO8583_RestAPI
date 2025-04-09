package com.vis.bob.iso8583.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.vis.bob.iso8583.client.Callback;
import com.vis.bob.iso8583.client.ISOConnection;
import com.vis.bob.iso8583.client.SocketPayload;
import com.vis.bob.iso8583.exception.ConnectionException;
import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.helper.PayloadMessageConfig;
import com.vis.bob.iso8583.pojo.StandardResponse;
import com.vis.bob.iso8583.pojo.StopCheque;
import com.vis.bob.iso8583.pojo.StopChequeResponse;
import com.vis.bob.iso8583.pojo.StandardChequeRequest;
import com.vis.bob.iso8583.protocol.ISOMessage;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@Slf4j
public class StopChequeAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${stopcheque.configfile.path}")
	private String configFile;
	@Value("${stopcheque.maxbytes}")
	private int maxBytes;

	@Value("${stopcheque.field.processingcode}")
	private String processingCode;
	@Value("${stopcheque.field.functioncode}")
	private String functionCode;
	@Value("${stopcheque.field.instid}")
	private String instId;
	@Value("${stopcheque.field.fees}")
	private String fees;
	@Value("${stopcheque.field.currency}")
	private String curr;
	@Value("${stopcheque.field.reason}")
	private String stopReason;
	@Value("${stopcheque.field.numofleaves}")
	private String numOfLeaves;
	@Value("${stopcheque.field.accountid.1}")
	private String accountId1;
	@Value("${stopcheque.field.accountid.2}")
	private String accountId2;
	@Value("${stopcheque.field.dccid}")
	private String dccId;
	
	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/stopCheque")
	public StopChequeResponse stopCheque(@RequestBody StandardChequeRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		StopChequeResponse response = new StopChequeResponse();

		int clientCount = -1;
		boolean newClient = false;
		
		for (int i=0; i <= isoConnClient.size(); i++) {
			if (i == isoConnClient.size()) {
				clientCount = i;
				newClient = true;
			} else if (isoConnClient.get(i) !=  null && isoConnClient.get(i).isConnected() && !isoConnClient.get(i).isUsed()) {
				clientCount = i;
				newClient = false;
				isoConnClient.get(i).setUsed(true);
				break;
			} else if (isoConnClient.get(i) == null || !isoConnClient.get(i).isConnected()){
				clientCount = i;
				newClient = true;
				break;
			}
		}
		
		if (newClient) {
			isoConfig = new Iso8583Config();

			isoConfig.openFile(configFile);
			isoConfig.parseXmlToConfig();

			isoConfig.setMaxBytes(maxBytes);

			if (clientCount < isoConnClient.size())
				isoConnClient.set(clientCount, new ISOConnection(cbsHost, cbsPort, cbsTimeout));
			else
				isoConnClient.add(clientCount, new ISOConnection(cbsHost, cbsPort, cbsTimeout));
			
			isoConnClient.get(clientCount).putCallback(String.valueOf(Thread.currentThread().getId()), callback);
			isoConnClient.get(clientCount).connect(isoConfig,String.valueOf(Thread.currentThread().getId()));
			isoConnClient.get(clientCount).setUsed(true);
			log.info("Client connected.");
		}

		String systemTraceNumReq = ISOUtils.getCbsRrn();
		LocalDateTime ldt = LocalDateTime.now();

		for (int i=0; i<isoConfig.getConfigTreeNode().size();i++) {
			MessageVO messageVOreq = isoConfig.getConfigTreeNode().get(i);
			if (messageVOreq.getType().equals("1200")) {

				for(int j=0; j<messageVOreq.getFieldList().size();j++) {
					FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

					switch (fieldVOreq.getName()) {
					case "PAN":
//						fieldVOreq.setValue(ISOUtils.pad(chequeStatusFieldPan, "0", 16, 0));
						fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum(), "0", 19, 1));
						break;
					case "ProcessingCode":
						fieldVOreq.setValue(processingCode);
						break;
					case "SystemTraceAuditNumber":
						fieldVOreq.setValue(systemTraceNumReq);
						break;
					case "DateTime":
						fieldVOreq.setValue(DateTimeFormatter.ofPattern("yyyyMMddHHmmss",Locale.ENGLISH).format(ldt));
						break;
					case "Date":
						fieldVOreq.setValue(DateTimeFormatter.ofPattern("yyyyMMdd",Locale.ENGLISH).format(ldt));
						break;
					case "FunctionCode":
						fieldVOreq.setValue(functionCode);
						break;
					case "Fees":
						if (fees == null)
							fees = "";
						fieldVOreq.setValue(fees);
						break;
					case "AcquiringInstId":
						fieldVOreq.setValue(instId);
						break;
					case "Currency":
						fieldVOreq.setValue(curr);
						break;
					case "ChequeDetails":
						fieldVOreq.setValue(ISOUtils.pad(request.getChequeNum(), "0", 16, 0) + 
								ISOUtils.pad(stopReason, "0", 4, 0) + 
								ISOUtils.pad(numOfLeaves, "0", 3, 0));
						break;
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(accountId1, " ", 11, 1) + 
								ISOUtils.pad(accountId2, " ", 8, 1) + 
								ISOUtils.pad(request.getAccountNum(), " ", 19, 1));
						
						break;
					case "DCCID":
						fieldVOreq.setValue(dccId);
						break;
					default:
						break;
					}
				}
				break;
			}
		}

		payloadMessageConfig.setMessage(payloadMessageConfig, isoConfig, "1200");
		payloadMessageConfig.updateFromMessageVO(isoConfig);
		ISOMessage requestMessage = payloadMessageConfig.getIsoMessage();
		byte[] preparedPayload = isoConfig.getDelimiter().preparePayload(requestMessage, isoConfig);
		String strPayload = new String(preparedPayload, StandardCharsets.UTF_8);
		log.trace("Payload send ["+strPayload +"]");
		String binBitmap = strPayload.substring(4, 132);
		log.trace("Binary BitMap ["+binBitmap+"]");
		String ascciiBitMap = ISOUtils.binToAscii(binBitmap);		
		log.trace("ISOMessage()","ASCCII BitMap ["+ascciiBitMap+"]");		
		strPayload = strPayload.replace(binBitmap,ascciiBitMap);		
		log.debug("Hex BitMap ["+ISOUtils.binToHexLog(binBitmap)+"]");		
		log.info("Final Payload ["+strPayload+"]");		
		String hexFinal = ISOUtils.ascciiToHex(strPayload);		
		log.info("Final Payload Hex ["+ hexFinal.toUpperCase().trim() +"]");		
		preparedPayload = strPayload.getBytes(StandardCharsets.ISO_8859_1);		
		log.info("Bytes Payload in Hex ["+ ISOUtils.bytesToHex(preparedPayload) +"]");

		isoConfig.setDirtyPayload(false);
		isoConnClient.get(clientCount).send(new SocketPayload(preparedPayload, isoConnClient.get(clientCount).getClientSocket()));		
		isoConnClient.get(clientCount).putCallback(String.valueOf(Thread.currentThread().getId()), callback);		
		isoConnClient.get(clientCount).processNextPayload(isoConfig, String.valueOf(Thread.currentThread().getId()), true, 30);

		String systemTraceNumRes = "";
		String actionCode = "";
		String chequeDetails = "";
		String reservedField1 = "";
		String reservedField3 = "";

		MessageVO messageVOreq = payloadMessageConfig.getMessageVO();
		if (messageVOreq.getType().equals("1210")) {

			for(int j=0; j<messageVOreq.getFieldList().size();j++) {
				FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

				switch (fieldVOreq.getName()) {
				case "SystemTraceAuditNumber":
					systemTraceNumRes = fieldVOreq.getValue();
					break;
				case "ActionCode":
					actionCode = fieldVOreq.getValue();
					break;
				case "ChequeDetails":
					chequeDetails = fieldVOreq.getValue();
					break;
				case "ReservedField-1":
					reservedField1 = fieldVOreq.getValue();
					break;
				case "ReservedField-3":
					reservedField3 = fieldVOreq.getValue();
					break;
				default:
					break;						
				}
			}
		}

		if (systemTraceNumReq.equals(systemTraceNumRes)) {
			StandardResponse stdResponse = new StandardResponse();
			stdResponse.setRequestId(request.getRequestId());
			stdResponse.setChannel(request.getChannel());
			stdResponse.setAccountNum(request.getAccountNum());
			stdResponse.setResponseCode(actionCode);
			response.setResponse(stdResponse);

			if (isoConfig.isDirtyPayload())
				isoConnClient.get(clientCount).endConnection(String.valueOf(Thread.currentThread().getId()));
			
			response.setStopCheque(getStopCheque(chequeDetails, reservedField1, reservedField3));
		}
		isoConnClient.get(clientCount).setUsed(false);
		return response;
	}
	
	private static StopCheque getStopCheque(String chequeDetails, String field1, String fields3) {
		StopCheque stopCheque = new StopCheque();

		stopCheque.setChequeNum(chequeDetails.substring(0,16).replaceFirst("^0+(?!$)", ""));

		stopCheque.setStopChequeResponse(field1);
		stopCheque.setResponseDesc(fields3);


		return stopCheque;
	}
}


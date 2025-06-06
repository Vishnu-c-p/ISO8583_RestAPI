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
import com.vis.bob.iso8583.pojo.StandardChequeRequest;
import com.vis.bob.iso8583.pojo.ChequeStatus;
import com.vis.bob.iso8583.pojo.ChequeStatusResponse;
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
public class GetChequeStatusAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${chequestatus.configfile.path}")
	private String configFile;
	@Value("${chequestatus.maxbytes}")
	private int maxBytes;

	@Value("${chequestatus.field.processingcode}")
	private String fieldProcessingCode;
	@Value("${chequestatus.field.functioncode}")
	private String fieldFunctionCode;
	@Value("${chequestatus.field.instid}")
	private String fieldInstId;
	@Value("${chequestatus.field.currency}")
	private String fieldCurr;
	@Value("${chequestatus.field.numofleaves}")
	private String fieldNumOfLeaves;
	@Value("${chequestatus.field.accountid.1}")
	private String fieldAccountId1;
	@Value("${chequestatus.field.accountid.2}")
	private String fieldAccountId2;
	@Value("${chequestatus.field.dccid}")
	private String fieldDccId;
	
	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/getChequeStatus")
	public ChequeStatusResponse getChequeStatus(@RequestBody StandardChequeRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		ChequeStatusResponse response = new ChequeStatusResponse();

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
						fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum(), "0", 19, 1));
						break;
					case "ProcessingCode":
						fieldVOreq.setValue(fieldProcessingCode);
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
						fieldVOreq.setValue(fieldFunctionCode);
						break;
					case "AcquiringInstId":
						fieldVOreq.setValue(fieldInstId);
						break;
					case "Currency":
						fieldVOreq.setValue(fieldCurr);
						break;
					case "ChequeDetails":
						fieldVOreq.setValue(ISOUtils.pad(request.getChequeNum(), "0", 16, 0) +
								ISOUtils.pad(fieldNumOfLeaves, "0", 3, 0));
						break;
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(fieldAccountId1, " ", 11, 1) + 
								ISOUtils.pad(fieldAccountId2, " ", 8, 1) + 
								ISOUtils.pad(request.getAccountNum(), " ", 19, 1));
						
						break;
					case "DCCID":
						fieldVOreq.setValue(fieldDccId);
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
			
			response.setChequeStatus(getChequeStatus(chequeDetails, reservedField1, reservedField3));
		}
		isoConnClient.get(clientCount).setUsed(false);
		return response;
	}
	
	private static ChequeStatus getChequeStatus(String chequeDetails, String field1, String fields3) {
		ChequeStatus chequeStatus = new ChequeStatus();

		chequeStatus.setChequeNum(chequeDetails.substring(0,16).replaceFirst("^0+(?!$)", ""));

		chequeStatus.setChequeStatusResponse(field1);
		chequeStatus.setResponseDesc(fields3);


		return chequeStatus;
	}
}


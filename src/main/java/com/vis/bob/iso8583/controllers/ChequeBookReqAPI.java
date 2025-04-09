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
import com.vis.bob.iso8583.pojo.ChequeBookReq;
import com.vis.bob.iso8583.pojo.ChequeBookReqRequest;
import com.vis.bob.iso8583.pojo.ChequeBookReqResponse;
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
public class ChequeBookReqAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${chequebookreq.configfile.path}")
	private String configFile;
	@Value("${chequebookreq.maxbytes}")
	private int maxBytes;

	@Value("${chequebookreq.field.processingcode}")
	private String processingCode;
	@Value("${chequebookreq.field.amount}")
	private String amount;
	@Value("${chequebookreq.field.functioncode}")
	private String functionCode;
	@Value("${chequebookreq.field.instid}")
	private String instId;
	@Value("${chequebookreq.field.currency}")
	private String curr;
	@Value("${chequebookreq.field.accountid.1}")
	private String fieldAccountId1;
	@Value("${chequebookreq.field.accountid.2}")
	private String fieldAccountId2;
	@Value("${chequebookreq.field.dccid}")
	private String dccId;
	
	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/chequeBookReq")
	public ChequeBookReqResponse chequeBookReq(@RequestBody ChequeBookReqRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		ChequeBookReqResponse response = new ChequeBookReqResponse();

		log.info("Recieved Cheque Book Request - CHANNEL ["+ request.getChannel() +"], REQUEST ID ["+ 
				request.getRequestId() +"], ACCOUNT_NUM ["+ request.getAccountNum() +"], MOBILE_NUM ["+ request.getMobileNum() +"]");
		
		int clientCount = -1;
		boolean newClient = false;
		
		for (int i=0; i <= isoConnClient.size(); i++) {
			if (i == isoConnClient.size()) {
				clientCount = i;
				newClient = true;
				log.info("No Free CBS Client Available. Create New Client, ClientNum["+ i +"]");
			} else if (isoConnClient.get(i) !=  null && isoConnClient.get(i).isConnected() && !isoConnClient.get(i).isUsed()) {
				clientCount = i;
				newClient = false;
				isoConnClient.get(i).setUsed(true);
				log.info("CBS Client, ClientNum["+ i +"] is Available. Use ClientNum["+ i +"]");
				log.debug("ClientNum ["+ i +"] Marked as USED");
				break;
			} else if (isoConnClient.get(i) == null || !isoConnClient.get(i).isConnected()){
				clientCount = i;
				newClient = true;
				log.info("CBS Client, ClientNum["+ i +"] is Available, But is disconnected. Re-connect ClientNum["+ i +"]");
				break;
			}
		}
		
		if (newClient) {
			isoConfig = new Iso8583Config();

			isoConfig.openFile(configFile);
			log.debug("Using Config File ["+ configFile +"]");
			isoConfig.parseXmlToConfig();

			log.trace("Using Response MaxBytes as ["+ maxBytes +"]");
			isoConfig.setMaxBytes(maxBytes);

			if (clientCount < isoConnClient.size())
				isoConnClient.set(clientCount, new ISOConnection(cbsHost, cbsPort, cbsTimeout));
			else
				isoConnClient.add(clientCount, new ISOConnection(cbsHost, cbsPort, cbsTimeout));
			
			isoConnClient.get(clientCount).putCallback(String.valueOf(Thread.currentThread().getId()), callback);
			isoConnClient.get(clientCount).connect(isoConfig,String.valueOf(Thread.currentThread().getId()));
			isoConnClient.get(clientCount).setUsed(true);
			log.info("ClientNum["+ clientCount +"] Connected.");
			log.debug("ClientNum["+ clientCount +"] Marked as USED");
		}

		String systemTraceNumReq = ISOUtils.getCbsRrn();
		log.debug("Generated SystemTraceNum ["+ systemTraceNumReq +"]");
		LocalDateTime ldt = LocalDateTime.now();
		
		log.info("CBS Request Parameters - SystemTraceAuditNumber ["+ systemTraceNumReq +"], AccountNumber ["+ request.getAccountNum() +"]");

		for (int i=0; i<isoConfig.getConfigTreeNode().size();i++) {
			MessageVO messageVOreq = isoConfig.getConfigTreeNode().get(i);
			if (messageVOreq.getType().equals("1200")) {

				log.debug("CBS Request Building -");
				for(int j=0; j<messageVOreq.getFieldList().size();j++) {
					FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

					switch (fieldVOreq.getName()) {
					case "PAN":
						fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum(), "0", 19, 1));
						log.debug(" 	PAN ["+ ISOUtils.pad(request.getAccountNum(), "0", 19, 1) +"]");
						break;
					case "ProcessingCode":
						fieldVOreq.setValue(processingCode);
						log.debug(" 	ProcessingCode ["+ processingCode +"]");
						break;
					case "TransactionAmount":
						fieldVOreq.setValue(ISOUtils.pad(amount, "0", 16, 0));
						log.debug(" 	TransactionAmount ["+ ISOUtils.pad(amount, "0", 16, 0) +"]");
						break;
					case "SystemTraceAuditNumber":
						fieldVOreq.setValue(systemTraceNumReq);
						log.debug(" 	SystemTraceAuditNumber ["+ systemTraceNumReq +"]");
						break;
					case "DateTime":
						fieldVOreq.setValue(DateTimeFormatter.ofPattern("yyyyMMddHHmmss",Locale.ENGLISH).format(ldt));
						log.debug(" 	DateTime ["+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss",Locale.ENGLISH).format(ldt) +"]");
						break;
					case "Date":
						fieldVOreq.setValue(DateTimeFormatter.ofPattern("yyyyMMdd",Locale.ENGLISH).format(ldt));
						log.debug(" 	Date ["+ DateTimeFormatter.ofPattern("yyyyMMdd",Locale.ENGLISH).format(ldt) +"]");
						break;
					case "FunctionCode":
						fieldVOreq.setValue(functionCode);
						log.debug(" 	FunctionCode ["+ functionCode +"]");
						break;
					case "AcquiringInstId":
						fieldVOreq.setValue(instId);
						log.debug(" 	AcquiringInstId ["+ instId +"]");
						break;
					case "RetrevalRefNum":
						fieldVOreq.setValue(systemTraceNumReq);
						log.debug(" 	RetrevalRefNum ["+ systemTraceNumReq +"]");
						break;
					case "Currency":
						fieldVOreq.setValue(curr);
						log.debug(" 	Currency ["+ curr +"]");
						break;
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(fieldAccountId1, " ", 11, 1) + 
								ISOUtils.pad(fieldAccountId2, " ", 8, 1) + 
								ISOUtils.pad(request.getAccountNum(), " ", 14, 1));
						log.debug(" 	AccountID-1 ["+ ISOUtils.pad(fieldAccountId1, " ", 11, 1) + 
								ISOUtils.pad(fieldAccountId2, " ", 8, 1) + 
								ISOUtils.pad(request.getAccountNum(), " ", 14, 1) +"]");
						break;
					case "DCCID":
						fieldVOreq.setValue(dccId);
						log.debug(" 	DCCID ["+ dccId +"]");
						break;
					case "ReservedField-1":
						fieldVOreq.setValue("CHQ" + "|" + ISOUtils.pad(request.getAccountNum(), " ", 14, 0) + 
								"|I|C|" + request.getMobileNum() + "|X|1");
						log.debug(" 	ReservedField-1 [CHQ" + "|" + ISOUtils.pad(request.getAccountNum(), " ", 14, 0) + 
								"|I|C|" + request.getMobileNum() + "|X|1]");
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
		log.trace("Raw Payload ["+strPayload +"]");
		String binBitmap = strPayload.substring(4, 132);
		log.trace("Binary BitMap ["+binBitmap+"]");
		String ascciiBitMap = ISOUtils.binToAscii(binBitmap);		
		log.trace("ASCCII BitMap ["+ascciiBitMap+"]");			
		strPayload = strPayload.replace(binBitmap,ascciiBitMap);		
		log.trace("Hex BitMap ["+ISOUtils.binToHexLog(binBitmap)+"]");		
		log.info("Payload to CBS ["+strPayload+"]");	
		String hexFinal = ISOUtils.ascciiToHex(strPayload);		
		log.info("Final Payload Hex ["+ hexFinal.toUpperCase().trim() +"]");		
		preparedPayload = strPayload.getBytes(StandardCharsets.ISO_8859_1);		
		log.debug("Payload to CBS in HEX ["+ ISOUtils.bytesToHex(preparedPayload) +"]");

		isoConfig.setDirtyPayload(false);
		isoConnClient.get(clientCount).send(new SocketPayload(preparedPayload, isoConnClient.get(clientCount).getClientSocket()));		
		isoConnClient.get(clientCount).putCallback(String.valueOf(Thread.currentThread().getId()), callback);		
		isoConnClient.get(clientCount).processNextPayload(isoConfig, String.valueOf(Thread.currentThread().getId()), true, 30);

		String systemTraceNumRes = "";
		String actionCode = "";
		String reservedField1 = "";
		
		MessageVO messageVOreq = payloadMessageConfig.getMessageVO();
		if (messageVOreq.getType().equals("1210")) {

			log.info("Retreiving required parameters from CBS Response -");	
			for(int j=0; j<messageVOreq.getFieldList().size();j++) {
				FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

				switch (fieldVOreq.getName()) {
				case "SystemTraceAuditNumber":
					systemTraceNumRes = fieldVOreq.getValue();
					log.info(" 		SystemTraceAuditNumber ["+systemTraceNumRes+"]");
					break;
				case "ActionCode":
					actionCode = fieldVOreq.getValue();
					log.info(" 		ActionCode ["+actionCode+"]");
					break;
				case "ReservedField-1":
					reservedField1 = fieldVOreq.getValue();
					log.info(" 		ReservedField-1 ["+reservedField1+"]");
					break;
				default:
					break;						
				}
			}
		}

		if (systemTraceNumReq.equals(systemTraceNumRes)) {
			log.trace("Building API Response -");
			StandardResponse stdResponse = new StandardResponse();
			stdResponse.setRequestId(request.getRequestId());
			stdResponse.setChannel(request.getChannel());
			stdResponse.setAccountNum(request.getAccountNum());
			stdResponse.setResponseCode(actionCode);
			response.setResponse(stdResponse);
			log.trace(" 		response [requesttId ["+ request.getRequestId() +"], channel ["+ request.getChannel() +"], "
					+ "accountNum ["+ request.getAccountNum() +"], responseCode ["+ actionCode +"]]");
			
			if (isoConfig.isDirtyPayload()) {
				log.error("Dirty Payload or Incomplete Payload detected. Closing Socket ClientNum["+ clientCount +"]");
				isoConnClient.get(clientCount).endConnection(String.valueOf(Thread.currentThread().getId()));
			}
			
			response.setChequeBookReq(getChequeBookReq(reservedField1));
		} else
			log.error("Recieved Response of Wrong Request. Request SystemTraceAuditNumber ["+systemTraceNumReq+"], "
					+ "Response SystemTraceAuditNumber ["+ systemTraceNumRes +"]");
		
		isoConnClient.get(clientCount).setUsed(false);
		log.debug("ClientNum["+ clientCount +"] Marked as UN-USED");
		log.info("Response back to API -");
		log.info(" 		response [requesttId ["+ response.getResponse().getRequestId() +"], channel ["+ response.getResponse().getChannel() +"], "
				+ "accountNum ["+ response.getResponse().getAccountNum() +"], responseCode ["+ response.getResponse().getResponseCode() +"]]");
		log.info(" 		chequeBookReq [chequeBookReqResponse ["+ response.getChequeBookReq().getChequeBookReqResponse() +"], responseDesc ["+ response.getChequeBookReq().getResponseDesc() +"], "
				+ "refNum ["+ response.getChequeBookReq().getRefNum() +"]]");
		log.info("Completed Cheque Book Request with RequestID ["+ request.getRequestId() +"].");
		return response;
	}
	
	private static ChequeBookReq getChequeBookReq(String field1) {
		ChequeBookReq chequeBookReq = new ChequeBookReq();
		
		String[] fields = field1.split("\\|",-1);
		
		if (fields.length >= 1)
			chequeBookReq.setChequeBookReqResponse(fields[0]);
		if (fields.length >= 2)
			chequeBookReq.setResponseDesc(fields[1]);
		if (fields.length >= 3)
			chequeBookReq.setRefNum(fields[2]);
		
		log.trace(" 		chequeBookReq [chequeBookReqResponse ["+ chequeBookReq.getChequeBookReqResponse() +"], responseDesc ["+ chequeBookReq.getResponseDesc() +"], "
				+ "refNum ["+ chequeBookReq.getRefNum() +"]]");
		
		return chequeBookReq;
	}
}


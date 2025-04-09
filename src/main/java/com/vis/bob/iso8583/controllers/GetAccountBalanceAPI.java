package com.vis.bob.iso8583.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.vis.bob.iso8583.client.Callback;
import com.vis.bob.iso8583.client.ISOConnection;
import com.vis.bob.iso8583.client.SocketPayload;
import com.vis.bob.iso8583.exception.ConnectionException;
import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.helper.PayloadMessageConfig;
import com.vis.bob.iso8583.pojo.AccountBalanceResponse;
import com.vis.bob.iso8583.pojo.Balances;
import com.vis.bob.iso8583.pojo.StandardRequest;
import com.vis.bob.iso8583.pojo.StandardResponse;
import com.vis.bob.iso8583.protocol.ISOMessage;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class GetAccountBalanceAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${balanceinquiry.configfile.path}")
	private String configFile;
	@Value("${balanceinquiry.maxbytes}")
	private int maxBytes;

	@Value("${balanceinquiry.field.pan}")
	private String pan;
	@Value("${balanceinquiry.field.processingcode}")
	private String processingCode;
	@Value("${balanceinquiry.field.amount}")
	private String amount;
	@Value("${balanceinquiry.field.functioncode}")
	private String functionCode;
	@Value("${balanceinquiry.field.instid}")
	private String instId;
	@Value("${balanceinquiry.field.cardacceptor.termid}")
	private String cardTermId;
	@Value("${balanceinquiry.field.cardacceptor.name}")
	private String cardName;
	@Value("${balanceinquiry.field.currency}")
	private String curr;
	@Value("${balanceinquiry.field.accountid.1}")
	private String accountId1;
	@Value("${balanceinquiry.field.accountid.2}")
	private String accountId2;
	@Value("${balanceinquiry.field.dccid}")
	private String dccId;
	@Value("${balanceinquiry.field.termtype}")
	private String termType;

	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();
	
	private static String configXmlText= "";
	
	private Map<String, Map<String, String>> rspCache = new ConcurrentHashMap<>();

//	@Autowired
//	Callback callback;
//	@Autowired
//	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/getAccountBalance")
	public AccountBalanceResponse getAccountBalance(@RequestBody StandardRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		AccountBalanceResponse response = new AccountBalanceResponse();
		Iso8583Config isoConfig = new Iso8583Config();
		
		if(configXmlText == "") {		
			log.info("Config file xml text not available, creating");
			configXmlText = ISOUtils.readConfigFile(configFile);
			if(configXmlText != "") {
				isoConfig.setXmlFilePath(configFile);
			}else {
				isoConfig.setXmlFilePath(null);
			}
		}else {
			isoConfig.setXmlFilePath(configFile);
			log.info("Config file xml text available");
		}
		isoConfig.setXmlText(configXmlText);
		
		
//		isoConfig.openFile(configFile);
		
		log.debug("Using Config File ["+ configFile +"]");
		isoConfig.parseXmlToConfig();

		log.trace("Using Response MaxBytes as ["+ maxBytes +"]");
		isoConfig.setMaxBytes(maxBytes);
		
		log.info("Recieved Balance Inquiry Request - CHANNEL ["+ request.getChannel() +"], REQUEST ID ["+ 
				request.getRequestId() +"], ACCOUNT_NUM ["+ request.getAccountNum() +"]");

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
				isoConnClient.get(i).setIsoConfig(isoConfig);
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
		PayloadMessageConfig payloadMessageConfig = new PayloadMessageConfig();
		Callback callback = new Callback(payloadMessageConfig);
		
		if (newClient) {
//			isoConfig = new Iso8583Config();
//			isoConfig.openFile(configFile);
//			log.debug("Using Config File ["+ configFile +"]");
//			isoConfig.parseXmlToConfig();
//
//			log.trace("Using Response MaxBytes as ["+ maxBytes +"]");
//			isoConfig.setMaxBytes(maxBytes);

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
						fieldVOreq.setValue(ISOUtils.pad(pan, "0", 16, 0));
						log.debug(" 	PAN ["+ ISOUtils.pad(pan, "0", 16, 0) +"]");
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
					case "CardAcceptorTerminalID":
						fieldVOreq.setValue(ISOUtils.pad(cardTermId, " " , 16, 1));
						log.debug(" 	CardAcceptorTerminalID ["+ ISOUtils.pad(cardTermId, " " , 16, 1) +"]");
						break;
					case "CardAceptorName":
						fieldVOreq.setValue(cardName);
						log.debug(" 	CardAceptorName ["+ cardName +"]");
						break;
					case "Currency":
						fieldVOreq.setValue(curr);
						log.debug(" 	Currency ["+ curr +"]");
						break;
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(accountId1, " ", 11, 1) + 
								ISOUtils.pad(accountId2, " ", 8, 1) + ISOUtils.pad(request.getAccountNum()," ",14,1));
						log.debug(" 	AccountID-1 ["+ ISOUtils.pad(accountId1, " ", 11, 1) + 
								ISOUtils.pad(accountId2, " ", 8, 1) + ISOUtils.pad(request.getAccountNum()," ",14,1) +"]");
						break;
					case "DCCID":
						fieldVOreq.setValue(dccId);
						log.debug(" 	DCCID ["+ dccId +"]");
						break;
					case "TerminalType":
						fieldVOreq.setValue(termType);
						log.debug(" 	TerminalType ["+ termType +"]");
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
		preparedPayload = strPayload.getBytes(StandardCharsets.ISO_8859_1);		
		log.debug("Payload to CBS in HEX ["+ ISOUtils.bytesToHex(preparedPayload) +"]");

		isoConfig.setDirtyPayload(false);
		isoConnClient.get(clientCount).send(new SocketPayload(preparedPayload, isoConnClient.get(clientCount).getClientSocket()));		
		isoConnClient.get(clientCount).putCallback(String.valueOf(Thread.currentThread().getId()), callback);		
		isoConnClient.get(clientCount).processNextPayload(isoConfig, String.valueOf(Thread.currentThread().getId()), true, 30);

		

//		String systemTraceNumRes = "";
//		String actionCode = "";
//		String additionalData = "";
//
//		MessageVO messageVOreq = payloadMessageConfig.getMessageVO();
//		if (messageVOreq.getType().equals("1210")) {
//
//			log.info("Retreiving required parameters from CBS Response -");	
//			for(int j=0; j<messageVOreq.getFieldList().size();j++) {
//				FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);
//
//				switch (fieldVOreq.getName()) {
//				case "SystemTraceAuditNumber":
//					systemTraceNumRes = fieldVOreq.getValue();
//					log.info(" 		SystemTraceAuditNumber ["+systemTraceNumRes+"]");
//					break;
//				case "ActionCode":
//					actionCode = fieldVOreq.getValue();
//					log.info(" 		ActionCode ["+actionCode+"]");
//					break;
//				case "AdditionalData":
//					additionalData = fieldVOreq.getValue();
//					log.info(" 		AdditionalData ["+additionalData+"]");
//					break;
//				default:
//					break;						
//				}
//			}
//		}
		
		
		MessageVO messageVOreq = payloadMessageConfig.getMessageVO();
		
		if (messageVOreq.getType().equals("1210")) {
			
			String systemTraceNumRes = "";
			String actionCode = "";
			String additionalData = "";

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
				case "AdditionalData":
					additionalData = fieldVOreq.getValue();
					log.info(" 		AdditionalData ["+additionalData+"]");
					break;
				default:
					break;						
				}
				
			}
			Map<String, String> responseData = new HashMap<>();
			responseData.put("actionCode", actionCode);
			responseData.put("additionalData", additionalData);
			rspCache.put(systemTraceNumRes, responseData);
			
			log.info("Response added to hashmap, systemTraceNumReq:{}, systemTraceNumRes: {} ", systemTraceNumReq, systemTraceNumRes);
			
		}
		
		
		log.info("Response hash map size: "+ rspCache.size());
		
		String actionCode = "";
		String additionalData = "";
		String systemTraceNumRes = "";
		boolean isTraceAvailable = false;
		int counter = 0;
		
		while(additionalData == ""  && counter<=10) {
			log.info("SystemTraceAuditNumber: {} --> Before fetching data for Trace num, counter: {}", systemTraceNumReq, counter);
			if(rspCache.containsKey(systemTraceNumReq)) {
				log.info("SystemTraceAuditNumber: {} --> Found data from hashmap, counter: {}", systemTraceNumReq, counter);
				Map<String, String> data = rspCache.get(systemTraceNumReq);
				isTraceAvailable= true;
				actionCode= data.get("actionCode");
				additionalData= data.get("additionalData");
				log.info("SystemTraceAuditNumber: {} --> removed from hashmap", systemTraceNumReq);
				rspCache.remove(systemTraceNumReq);
				break;
			}
			log.info("SystemTraceAuditNumber: {} --> Not Found data from hashmap, going for next fetch, counter: {}", systemTraceNumReq, counter);
			Thread.sleep(100);
			counter++;
		}

		
		if (isTraceAvailable) {
			log.trace("Building API Response -");
			StandardResponse stdResponse = new StandardResponse();
			stdResponse.setRequestId(request.getRequestId());
			stdResponse.setChannel(request.getChannel());
			stdResponse.setAccountNum(request.getAccountNum());
			stdResponse.setResponseCode(actionCode);
			response.setResponse(stdResponse);
			log.trace(" 		response [requesttId ["+ request.getRequestId() +"], channel ["+ request.getChannel() +"], "
					+ "accountNum ["+ request.getAccountNum() +"], responseCode ["+ actionCode +"]]");

			if (actionCode.equals("000") && additionalData.length() >= 88) {

				response.setBalances(getBalances(additionalData));
			}
			else if (isoConfig.isDirtyPayload()) {
				log.error("Dirty Payload or Incomplete Payload detected. Closing Socket ClientNum["+ clientCount +"]");
				isoConnClient.get(clientCount).endConnection(String.valueOf(Thread.currentThread().getId()));
			}
		} else
			log.error("Recieved Response of Wrong Request. Request SystemTraceAuditNumber ["+systemTraceNumReq+"], "
					+ "Response SystemTraceAuditNumber ["+ systemTraceNumRes +"]");

		isoConnClient.get(clientCount).setUsed(false);
		log.debug("ClientNum["+ clientCount +"] Marked as UN-USED");
		log.info("Response back to API -");
		log.info(" 		response [requesttId ["+ response.getResponse().getRequestId() +"], channel ["+ response.getResponse().getChannel() +"], "
				+ "accountNum ["+ response.getResponse().getAccountNum() +"], responseCode ["+ response.getResponse().getResponseCode() +"]]");
		if (actionCode.equals("000") && additionalData.length() >= 88) {
			log.info(" 		balances [ledgerBalance ["+ response.getBalances().getLedgerBalance() +"], availableBalance ["+ response.getBalances().getAvailableBalance() +"], "
					+ "floatBalance ["+ response.getBalances().getFloatBalance() +"], ffdBalance ["+ response.getBalances().getFfdBalance() +"], "
					+ "userDefinedBalance ["+ response.getBalances().getUserDefinedBalance() +"], balanceCurrency ["+ response.getBalances().getBalanceCurrency() +"]]");
		}
		log.info("Completed Balance Inquiry Request with RequestID ["+ request.getRequestId() +"].");
		return response;
	}

	private Balances getBalances(String balanceStr) {
		Balances balances = new Balances();

		balances.setLedgerBalance(ISOUtils.getCbsCurrencyVal(balanceStr.substring(0,17)));
		balances.setAvailableBalance(ISOUtils.getCbsCurrencyVal(balanceStr.substring(17,34)));
		balances.setFloatBalance(ISOUtils.getCbsCurrencyVal(balanceStr.substring(34,51)));
		balances.setFfdBalance(ISOUtils.getCbsCurrencyVal(balanceStr.substring(51,68)));
		balances.setUserDefinedBalance(ISOUtils.getCbsCurrencyVal(balanceStr.substring(68,85)));
		balances.setBalanceCurrency(balanceStr.substring(85,88));
		log.trace(" 		balances [ledgerBalance ["+ balances.getLedgerBalance() +"], availableBalance ["+ balances.getAvailableBalance() +"], "
				+ "floatBalance ["+ balances.getFloatBalance() +"], ffdBalance ["+ balances.getFfdBalance() +"], "
				+ "userDefinedBalance ["+ balances.getUserDefinedBalance() +"], balanceCurrency ["+ balances.getBalanceCurrency() +"]]");

		return balances;
	}

}


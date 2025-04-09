package com.vis.bob.iso8583.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.vis.bob.iso8583.client.Callback;
import com.vis.bob.iso8583.client.ISOConnection;
import com.vis.bob.iso8583.client.SocketPayload;
import com.vis.bob.iso8583.exception.ConnectionException;
import com.vis.bob.iso8583.exception.ParseException;
import com.vis.bob.iso8583.helper.Iso8583Config;
import com.vis.bob.iso8583.helper.PayloadMessageConfig;
import com.vis.bob.iso8583.pojo.StandardRequest;
import com.vis.bob.iso8583.pojo.StandardResponse;
import com.vis.bob.iso8583.pojo.Tran;
import com.vis.bob.iso8583.pojo.TranList;
import com.vis.bob.iso8583.pojo.Balances;
import com.vis.bob.iso8583.pojo.Last5TransResponse;
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
public class GetLast5TransactionsAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${last5trans.configfile.path}")
	private String configFile;
	@Value("${last5trans.maxbytes}")
	private int maxBytes;

	@Value("${last5trans.field.processingcode}")
	private String fieldProcessingCode;
	@Value("${last5trans.field.amount}")
	private String fieldAmount;
	@Value("${last5trans.field.functioncode}")
	private String fieldFunctionCode;
	@Value("${last5trans.field.instid}")
	private String fieldInstId;
	@Value("${last5trans.field.currency}")
	private String fieldCurr;
	@Value("${last5trans.field.accountid.1}")
	private String fieldAccountId1;
	@Value("${last5trans.field.accountid.2}")
	private String fieldAccountId2;
	@Value("${last5trans.field.dccid}")
	private String fieldDccId;
	
	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/getLast5Trans")
	public Last5TransResponse getLast5Trans(@RequestBody StandardRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		Last5TransResponse response = new Last5TransResponse();

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
						fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum()," ",19,1));
						break;
					case "ProcessingCode":
						fieldVOreq.setValue(fieldProcessingCode);
						break;
					case "TransactionAmount":
						fieldVOreq.setValue(ISOUtils.pad(fieldAmount,"0",16,0));
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
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(fieldAccountId1, " ", 11, 1) + 
								ISOUtils.pad(fieldAccountId2, " ", 8, 1) + ISOUtils.pad(request.getAccountNum()," ",19,1));
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
		String additionalData = "";
		String reserveField = "";

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
				case "AdditionalData":
					additionalData = fieldVOreq.getValue();
					break;
				case "ReservedField-1":
					reserveField = fieldVOreq.getValue();
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

			if (additionalData.length() >= 88 && reserveField.length() >= 88) {
				response.setBalances(getBalances(additionalData));
				response.setTransactions(getTransactions(reserveField));
			}

			else if (isoConfig.isDirtyPayload())
				isoConnClient.get(clientCount).endConnection(String.valueOf(Thread.currentThread().getId()));
		}
		isoConnClient.get(clientCount).setUsed(false);
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

		return balances;
	}
	
	private TranList getTransactions(String transactionsStr) {
		TranList tranList = new TranList();

		int count = 0;
		List<Tran> trans = new ArrayList<Tran>();

		for (int i=0; i < transactionsStr.length(); i=i+87) {
			String tranStr = transactionsStr.substring(i);
			if (tranStr.length() >= 87) {
				Tran tran = new Tran();
				tran.setDate(tranStr.substring(0,8).trim());
				tran.setCategory(tranStr.substring(24,26).trim());
				tran.setDesc(tranStr.substring(29,69).trim());
				tran.setType(tranStr.substring(69,70).trim());
				tran.setAmount(tranStr.substring(70,87).trim());

				count++;
				trans.add(tran);
			} else
				break;			
		}
		tranList.setCount(count);
		tranList.setTransaction(trans);
		return tranList;
	}
	
}


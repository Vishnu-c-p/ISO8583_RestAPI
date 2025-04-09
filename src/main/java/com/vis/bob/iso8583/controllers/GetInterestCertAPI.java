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
import com.vis.bob.iso8583.pojo.Address;
import com.vis.bob.iso8583.pojo.DepositInterest;
import com.vis.bob.iso8583.pojo.InterestCert;
import com.vis.bob.iso8583.pojo.InterestCertRequest;
import com.vis.bob.iso8583.pojo.InterestCertResponse;
import com.vis.bob.iso8583.pojo.InterestRecord;
import com.vis.bob.iso8583.pojo.LoanInterest;
import com.vis.bob.iso8583.protocol.ISOMessage;
import com.vis.bob.iso8583.util.ISOUtils;
import com.vis.bob.iso8583.vo.FieldVO;
import com.vis.bob.iso8583.vo.MessageVO;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
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
public class GetInterestCertAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${interestcert.configfile.path}")
	private String configFile;
	@Value("${interestcert.maxbytes}")
	private int maxBytes;

	@Value("${interestcert.field.processingcode}")
	private String processingCode;
	@Value("${interestcert.field.amount}")
	private String amount;
	@Value("${interestcert.field.functioncode}")
	private String functionCode;
	@Value("${interestcert.field.instid}")
	private String instId;
	@Value("${interestcert.field.currency}")
	private String curr;
	@Value("${interestcert.field.accountid.1}")
	private String accountId1;
	@Value("${interestcert.field.accountid.2}")
	private String accountId2;
	@Value("${interestcert.field.dccid}")
	private String dccId;

	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/getInterestCert")
	public InterestCertResponse getInterestCert(@RequestBody InterestCertRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		InterestCertResponse response = new InterestCertResponse();

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
						fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum(), " ", 19, 1));
						break;
					case "ProcessingCode":
						fieldVOreq.setValue(processingCode);
						break;
					case "TransactionAmount":
						fieldVOreq.setValue(ISOUtils.pad(amount, "0", 16, 0));
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
					case "AcquiringInstId":
						fieldVOreq.setValue(instId);
						break;
					case "RetrevalRefNum":
						fieldVOreq.setValue(systemTraceNumReq);
						break;
					case "Currency":
						fieldVOreq.setValue(curr);
						break;
					case "AccountID-1":
						fieldVOreq.setValue(ISOUtils.pad(accountId1, " ", 11, 1) + 
								ISOUtils.pad(accountId2, " ", 8, 1) + 
								ISOUtils.pad(request.getAccountNum(), " ", 14, 1));

						break;
					case "DCCID":
						fieldVOreq.setValue(dccId);
						break;
					case "ReservedField-1":

						fieldVOreq.setValue("INTCERT" + "|" + ISOUtils.pad(request.getAccountNum(), " ", 14, 1) + 
								"|" + "01-04-" + request.getStartYear() +
								"|" + "31-03-" + request.getEndYear());
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
		String reservedField1 = "";
		String reservedField2 = "";
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
				case "ReservedField-1":
					reservedField1 = fieldVOreq.getValue();
					break;
				case "ReservedField-2":
					reservedField2 = fieldVOreq.getValue();
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

			response.setInterestCert(getInterestCert(reservedField1, reservedField2, reservedField3));
		}
		isoConnClient.get(clientCount).setUsed(false);
		return response;
	}

	private static InterestCert getInterestCert(String field1, String field2, String field3) {
		InterestCert interestCert = new InterestCert();
		DecimalFormat amountFormat = new DecimalFormat("0.00");
		
		if (field2.startsWith("F|")) {
			interestCert.setStatus("F");
			field2 += "  ";
			interestCert.setCbsMssg(field2.substring(2).trim());
		}
		else {
			String [] cbsResponse = field1.split("\\|",-1);
			if (cbsResponse.length >= 2) {
				interestCert.setStatus(cbsResponse[0]);
				interestCert.setStatement(cbsResponse[1]);
			}

			
			if (field3.startsWith("L|")) {
				String[] interestDetails = field2.split("\\|",-1);
				
				List<InterestRecord> interestRecords = new ArrayList<InterestRecord>();
				
				for (int i=0; i < interestDetails.length; i=i+6) {
					if (i+6 <= interestDetails.length) {
						InterestRecord interestRecord = new InterestRecord();
						interestRecord.setSlNum(interestDetails[i+0]);
						interestRecord.setTransactionDate(interestDetails[i+1]);
						if (!interestDetails[i+2].trim().equals(""))
							interestRecord.setTransactionAmount(amountFormat.format(Float.parseFloat(interestDetails[i+2].trim())));
						interestRecord.setInterestFlag(interestDetails[i+3]);
						interestRecord.setNarration(interestDetails[i+4]);
						interestRecord.setTdsAccured(interestDetails[i+5]);
						interestRecords.add(interestRecord);
					}
					
				}

				interestCert.setInterestRecords(interestRecords);
				
				String[] loanDetails = field3.split("\\|",-1);
				
				if (loanDetails.length >= 12) {
					LoanInterest loanInterest = new LoanInterest();
					loanInterest.setLoanType(loanDetails[1]);
					loanInterest.setSolId(loanDetails[2]);
					loanInterest.setAccountOpenDate(loanDetails[3]);
					loanInterest.setTotalCreditAmount(amountFormat.format(Float.parseFloat(loanDetails[4].trim())));
					loanInterest.setTotalTaxAmount(amountFormat.format(Float.parseFloat(loanDetails[5].trim())));
					loanInterest.setTotalInterestAmount(amountFormat.format(Float.parseFloat(loanDetails[6].trim())));
					loanInterest.setTotalPrincipalAmount(amountFormat.format(Float.parseFloat(loanDetails[7].trim())));
					
					loanInterest.setCustName(loanDetails[8]);
					
					Address address = new Address();
					String[] custAddress = loanDetails[9].split("~",-1);
					if (custAddress.length >= 7) {
						address.setAddressLine1(custAddress[0]);
						address.setAddressLine2(custAddress[1]);
						address.setAddressLine3(custAddress[2]);
						address.setCity(custAddress[3]);
						address.setState(custAddress[4]);
						address.setPincode(custAddress[5]);
						address.setCountry(custAddress[6]);
						loanInterest.setAddress(address);
					}
					
					String[] jointHolderDetails = loanDetails[10].split("~",-1);
					List<String> jointHolders = new ArrayList<String>();					
					for (int i=0; i < jointHolderDetails.length; i++)
						jointHolders.add(jointHolderDetails[i]);
					loanInterest.setJointHolders(jointHolders);
					
					loanInterest.setOfferFlag(loanDetails[11]);
					interestCert.setLoanInterest(loanInterest);
				}
			} else {
				String[] interestDetails = field2.split("\\|",-1);
				if (interestDetails.length >= 3) {
					if (!interestDetails[interestDetails.length-3].contains("~"))
						interestCert.setCustEmail(interestDetails[interestDetails.length-3]);
					if (!interestDetails[interestDetails.length-2].contains("~"))
						interestCert.setSolId(interestDetails[interestDetails.length-2]);
				}
				if (interestDetails.length > 0) {
					List<InterestRecord> interestRecords = new ArrayList<InterestRecord>();
					
					for (int i=0; i < interestDetails.length; i++) {
						String[] interestRow = interestDetails[i].split("~",-1);
						if (interestRow.length >= 6) {
							InterestRecord interestRecord = new InterestRecord();
							interestRecord.setSlNum(interestRow[0]);
							interestRecord.setTransactionDate(interestRow[1]);
							if (!interestRow[2].trim().equals(""))
								interestRecord.setTransactionAmount(amountFormat.format(Float.parseFloat(interestRow[2].trim())));
							interestRecord.setInterestFlag(interestRow[3]);
							interestRecord.setNarration(interestRow[4]);
							interestRecord.setTdsAccured(interestRow[5]);
							interestRecords.add(interestRecord);
						}
						
					}

					interestCert.setInterestRecords(interestRecords);
				}
				
				String[] depositDetails = field3.split("\\|",-1);
				if (depositDetails.length >= 4) {
					DepositInterest depositInterest = new DepositInterest();
					depositInterest.setCustId(depositDetails[0]);
					depositInterest.setCustName(depositDetails[1]);
					depositInterest.setCustAddress(depositDetails[2]);
					depositInterest.setCustEmail(depositDetails[3]);
					interestCert.setDepositInterest(depositInterest);
				}
			}
		}

		return interestCert;
	}
}


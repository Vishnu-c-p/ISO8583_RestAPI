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
import com.vis.bob.iso8583.pojo.TdsCert;
import com.vis.bob.iso8583.pojo.TdsCertRequest;
import com.vis.bob.iso8583.pojo.TdsCertResponse;
import com.vis.bob.iso8583.pojo.TdsRecord;
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
public class GetTdsCertAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${tdscert.configfile.path}")
	private String configFile;
	@Value("${tdscert.maxbytes}")
	private int maxBytes;

	@Value("${tdscert.field.pan}")
	private String pan;
	@Value("${tdscert.field.processingcode}")
	private String processingCode;
	@Value("${tdscert.field.amount}")
	private String amount;
	@Value("${tdscert.field.functioncode}")
	private String functionCode;
	@Value("${tdscert.field.instid}")
	private String instId;
	@Value("${tdscert.field.currency}")
	private String curr;
	@Value("${tdscert.field.accountid.1}")
	private String accountId1;
	@Value("${tdscert.field.accountid.2}")
	private String accountId2;
	@Value("${tdscert.field.dccid}")
	private String dccId;
	
	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/geTdsCert")
	public TdsCertResponse getTdsCert(@RequestBody TdsCertRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		TdsCertResponse response = new TdsCertResponse();

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
		
		TdsCert tdsCert = new TdsCert();
		boolean isFirstPage = true;
		String moreRecords = "Y";
		int recordStartCount = 1;
		int recordEndCount = 11;
		
		while (moreRecords.equals("Y")) {
			moreRecords = "N";
			log.info("Pending Pages to be retrieved");
			for (int i=0; i<isoConfig.getConfigTreeNode().size();i++) {
				MessageVO messageVOreq = isoConfig.getConfigTreeNode().get(i);
				if (messageVOreq.getType().equals("1200")) {

					for(int j=0; j<messageVOreq.getFieldList().size();j++) {
						FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

						switch (fieldVOreq.getName()) {
						case "PAN":
							fieldVOreq.setValue(ISOUtils.pad(pan, "0", 19, 0));
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
									ISOUtils.pad("0", "0", 14, 0));
							
							break;
						case "DCCID":
							fieldVOreq.setValue(dccId);
							break;
						case "ReservedField-1":
							fieldVOreq.setValue("CUSTCERT" + "|" + request.getCustId() + 
									"|" + "01-04-" + request.getStartYear() +
									"|" + "31-03-" + request.getEndYear() +
									"|" + recordStartCount + 
									"|" + recordEndCount + "|");
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
				stdResponse.setAccountNum(request.getCustId());
				stdResponse.setResponseCode(actionCode);
				response.setResponse(stdResponse);

				if (isoConfig.isDirtyPayload())
					isoConnClient.get(clientCount).endConnection(String.valueOf(Thread.currentThread().getId()));
				
				tdsCert = getTdsCert(isFirstPage, tdsCert,reservedField1, reservedField2, reservedField3);
				response.setTdsCert(tdsCert);
				isFirstPage =  false;
				
				if (tdsCert.getStatus().equals("F")) 
					moreRecords = "N";
				
				if (reservedField3.startsWith("Y|")) {
					moreRecords = "Y";
					recordStartCount += 11;
					recordEndCount += 11;
				} else 
					moreRecords = "N";
			}
		}
		
		isoConnClient.get(clientCount).setUsed(false);
		return response;
	}
	
	private static TdsCert getTdsCert(boolean isFirstPage,TdsCert tdsCert, String field1, String field2, String field3) {
				
		if (field1.startsWith("F|")) {
			field1 += " ";
			tdsCert.setStatus("F");
			tdsCert.setCbsMssg(field1.substring(2).trim());
		} else {
			tdsCert.setStatus("S");
			
			if (field2.length()>0)
				field1 += field2;
			
			if (isFirstPage) {
				String [] cbsOut = field1.split("#",-1);
				tdsCert.setStatement(cbsOut[0]);
				if (cbsOut.length > 1) {
					String tdsDetails = cbsOut[1];
					
					String[] records = tdsDetails.split("\\|",-1);
					tdsCert.setTdsRecords(getTdsRecords(isFirstPage, tdsCert, records));
				}
				
				String[] custDetails = field3.split("\\|",-1);
				if (custDetails.length >= 5) {
					tdsCert.setCounterFlag(custDetails[0]);
					tdsCert.setCustName(custDetails[2]);
					tdsCert.setCustAddress(custDetails[3]);
					tdsCert.setCustEmail(custDetails[4]);
				}
			} else {
				String[] records = field1.split("\\|",-1);
				tdsCert.setTdsRecords(getTdsRecords(isFirstPage, tdsCert, records));
			}
		}
		
		return tdsCert;
	}
	
	private static List<TdsRecord> getTdsRecords(boolean isFirstPage, TdsCert tdsCert, String[] records) {
		
		List<TdsRecord> tdsRecords;
		DecimalFormat amountFormat = new DecimalFormat("0.00");
		 
		if (isFirstPage)
			tdsRecords = new ArrayList<TdsRecord>();
		else
			tdsRecords = tdsCert.getTdsRecords();
		
		for (int i = 0; i < records.length; i++) {
			TdsRecord record = new TdsRecord();
			String[] columns = records[i].split("~",-1);
			if (columns.length >= 9) {
				record.setSlNum(columns[0]);
				record.setAccountNum(columns[1]);
				record.setSchemeCode(columns[2]);
				record.setTransactionDate(columns[3]);
				if (!columns[4].trim().equals(""))
					record.setTransactionAmountCollected(amountFormat.format(Float.parseFloat(columns[4].trim())));
				if (!columns[5].trim().equals(""))
					record.setTransactionAmountPaid(amountFormat.format(Float.parseFloat(columns[5].trim())));
				if (!columns[6].trim().equals(""))
					record.setGrossAmount(amountFormat.format(Float.parseFloat(columns[6].trim())));
				if (!columns[7].trim().equals(""))
					record.setTdsAmount(amountFormat.format(Float.parseFloat(columns[7].trim())));
				record.setTdsFlag(columns[8]);
				tdsRecords.add(record);
			}
		}
		return tdsRecords;
	}
}


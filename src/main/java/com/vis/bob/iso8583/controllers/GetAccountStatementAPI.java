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
import com.vis.bob.iso8583.pojo.TransactionRecord;
import com.vis.bob.iso8583.pojo.AccountStatement;
import com.vis.bob.iso8583.pojo.AccountStatementRequest;
import com.vis.bob.iso8583.pojo.AccountStatementResponse;
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
import java.time.YearMonth;
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
public class GetAccountStatementAPI {

	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;
	@Value("${cbs.server.timeout}")
	private int cbsTimeout;

	@Value("${accountstmt.configfile.path}")
	private String configFile;
	@Value("${accountstmt.maxbytes}")
	private int maxBytes;

	@Value("${accountstmt.field.processingcode}")
	private String processingCode;
	@Value("${accountstmt.field.amount}")
	private String amount;
	@Value("${accountstmt.field.functioncode}")
	private String functionCode;
	@Value("${accountstmt.field.instid}")
	private String instId;
	@Value("${accountstmt.field.currency}")
	private String curr;
	@Value("${accountstmt.field.accountid.1}")
	private String accountId1;
	@Value("${accountstmt.field.accountid.2}")
	private String accountId2;
	@Value("${accountstmt.field.dccid}")
	private String dccId;

	@Getter
	private static List<ISOConnection> isoConnClient = new ArrayList<ISOConnection>();

	private Iso8583Config isoConfig;

	@Autowired
	Callback callback;
	@Autowired
	PayloadMessageConfig payloadMessageConfig;

	@PostMapping("/getAccountStatement")
	public AccountStatementResponse getAccountStatement(@RequestBody AccountStatementRequest request) throws NumberFormatException, ParseException, IOException, InterruptedException, ConnectionException {
		AccountStatementResponse response = new AccountStatementResponse();
		log.info("Recieved Account Statement Request - CHANNEL ["+ request.getChannel() +"], REQUEST ID ["+ 
				request.getRequestId() +"], ACCOUNT_NUM ["+ request.getAccountNum() +"], FROM_DATE ["+ request.getFromDate() +"], TO_DATE ["+ request.getToDate() +"]");

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

		AccountStatement accountStatement = new AccountStatement();
		boolean isFirstPage = true;
		String moreRecords = "Y";
		String balanceForNextRecord = "";
		int recordStartCount = 1;
		int recordEndCount = 11;

		while (moreRecords.equals("Y")) {
			moreRecords = "N";
			log.info("Pending Pages to be retrieved");
			for (int i=0; i<isoConfig.getConfigTreeNode().size();i++) {
				MessageVO messageVOreq = isoConfig.getConfigTreeNode().get(i);
				if (messageVOreq.getType().equals("1200")) {

					log.debug("CBS Request Building -");
					for(int j=0; j<messageVOreq.getFieldList().size();j++) {
						FieldVO fieldVOreq = messageVOreq.getFieldList().get(j);

						switch (fieldVOreq.getName()) {
						case "PAN":
							fieldVOreq.setValue(ISOUtils.pad(request.getAccountNum(), " ", 19, 1));
							log.debug(" 	PAN ["+ ISOUtils.pad(request.getAccountNum(), " ", 19, 1) +"]");
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
							fieldVOreq.setValue(ISOUtils.pad(accountId1, " ", 11, 1) + 
									ISOUtils.pad(accountId2, " ", 8, 1) + 
									ISOUtils.pad(request.getAccountNum(), " ", 14, 1));
							log.debug(" 	AccountID-1 ["+ ISOUtils.pad(accountId1, " ", 11, 1) + 
									ISOUtils.pad(accountId2, " ", 8, 1) + 
									ISOUtils.pad(request.getAccountNum(), " ", 14, 1) +"]");
							break;
						case "DCCID":
							fieldVOreq.setValue(dccId);
							log.debug(" 	DCCID ["+ dccId +"]");
							break;
						case "ReservedField-1":
							String fromDate = request.getFromDate();
							fromDate = "01-" + fromDate.substring(0,2) + "-20" + fromDate.substring(2);

							String toDate = request.getToDate();
							String toMM = toDate.substring(0,2);
							String toYYYY = "20" + toDate.substring(2);
							YearMonth yearMonth = YearMonth.of(Integer.parseInt(toYYYY), Integer.parseInt(toMM));
							String toDD = "" + yearMonth.lengthOfMonth(); 
							toDate = toDD + "-" + toMM + "-" + toYYYY;

							fieldVOreq.setValue("ACC3M" + "|" + request.getAccountNum() + 
									"|" + fromDate +
									"|" + toDate +
									"|" + recordStartCount +
									"|" + recordEndCount + 
									"|A|" + balanceForNextRecord + "|");
							log.debug(" 	ReservedField-1 [ACC3M" + "|" + request.getAccountNum() + 
									"|" + fromDate +
									"|" + toDate +
									"|" + recordStartCount +
									"|" + recordEndCount + 
									"|A|" + balanceForNextRecord + "|");
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
			String reservedField2 = "";
			String reservedField3 = "";

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
					case "ReservedField-2":
						reservedField2 = fieldVOreq.getValue();
						log.info(" 		ReservedField-2 ["+reservedField2+"]");
						break;
					case "ReservedField-3":
						reservedField3 = fieldVOreq.getValue();
						log.info(" 		ReservedField-3 ["+reservedField3+"]");
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

				accountStatement = getAccountStatement(isFirstPage, accountStatement, reservedField1, reservedField2);
				response.setAccountStatement(accountStatement);
				isFirstPage = false;

				if (accountStatement.getStatus().equals("F")) 
					moreRecords = "N";

				String[] nextPageDetails = reservedField3.split("\\|",-1);

				if (nextPageDetails.length >= 2) {
					moreRecords = nextPageDetails[0];
					balanceForNextRecord = nextPageDetails[1];
					recordStartCount += 11;
					recordEndCount += 11;
				} else 
					moreRecords = "N";

			} else
				log.error("Recieved Response of Wrong Request. Request SystemTraceAuditNumber ["+systemTraceNumReq+"], "
						+ "Response SystemTraceAuditNumber ["+ systemTraceNumRes +"]");
		}

		isoConnClient.get(clientCount).setUsed(false);

		log.debug("ClientNum["+ clientCount +"] Marked as UN-USED");
		log.info("Response back to API -");
		log.info(" 		response [requesttId ["+ response.getResponse().getRequestId() +"], channel ["+ response.getResponse().getChannel() +"], "
				+ "accountNum ["+ response.getResponse().getAccountNum() +"], responseCode ["+ response.getResponse().getResponseCode() +"]]");
		log.info(" 		Account Statement [status ["+ response.getAccountStatement().getStatus() +"], cbsMssg ["+ response.getAccountStatement().getCbsMssg() +"], "
				+ "custName ["+ response.getAccountStatement().getCustName() +"], accountNum ["+ response.getAccountStatement().getAccountNum() +"] "
				+ "schemeType ["+ response.getAccountStatement().getSchemeType() +"], availaleAmount ["+ response.getAccountStatement().getAvailaleAmount() +"], "
				+ "unclearBalance ["+ response.getAccountStatement().getUnclearBalance() +"], lienAmount ["+ response.getAccountStatement().getLienAmount() +"], "
				+ "clearBalance ["+ response.getAccountStatement().getClearBalance() +"], effectiveAvailableAmount ["+ response.getAccountStatement().getEffectiveAvailableAmount() +"], "
				+ "accountCurrency ["+ response.getAccountStatement().getAccountCurrency() +"], accountOpenDate ["+ response.getAccountStatement().getAccountOpenDate() +"], "
				+ "primarySecondary ["+ response.getAccountStatement().getPrimarySecondary() +"], name ["+ response.getAccountStatement().getName() +"], "
				+ "initialBalance ["+ response.getAccountStatement().getInitialBalance() +"], custId ["+ response.getAccountStatement().getCustId() +"], "
				+ "custAddress ["+ response.getAccountStatement().getCustAddress() +"], custEmail ["+ response.getAccountStatement().getCustEmail() +"], "
				+ "phoneNum ["+ response.getAccountStatement().getPhoneNum() +"], branchAddress ["+ response.getAccountStatement().getBranchAddress() +"]]");
		log.info("Completed Cheque Book Request with RequestID ["+ request.getRequestId() +"].");

		return response;
	}

	private static AccountStatement getAccountStatement(boolean isFirstPage, AccountStatement accountStatement, String field1, String field2) {

		if (field1.startsWith("F|")) {
			field1 += " ";
			accountStatement.setStatus("F");
			accountStatement.setCbsMssg(field1.substring(2).trim());
		} else {
			accountStatement.setStatus("S");

			if (field2.length()>0)
				field1 += field2;

			if (isFirstPage) {
				String[] cbsOut = field1.split("##",-1);

				if (cbsOut.length >= 2) {

					String[] custDetails = cbsOut[0].split("\\|",-1);

					if (custDetails.length >= 18) {
						accountStatement.setCustName(custDetails[0].trim());
						accountStatement.setAccountNum(custDetails[1].trim());
						accountStatement.setSchemeType(custDetails[2].trim());
						accountStatement.setAvailaleAmount(custDetails[3].trim());
						accountStatement.setUnclearBalance(custDetails[4].trim());
						accountStatement.setLienAmount(custDetails[5].trim());
						accountStatement.setClearBalance(custDetails[6].trim());
						accountStatement.setEffectiveAvailableAmount(custDetails[7].trim());
						accountStatement.setAccountCurrency(custDetails[8].trim());
						accountStatement.setAccountOpenDate(custDetails[9].trim());
						accountStatement.setPrimarySecondary(custDetails[10].trim());
						accountStatement.setName(custDetails[11].trim());
						accountStatement.setInitialBalance(custDetails[12].trim());
						accountStatement.setCustId(custDetails[13].trim());
						accountStatement.setCustAddress(custDetails[14].trim());
						accountStatement.setCustEmail(custDetails[15].trim());
						accountStatement.setPhoneNum(custDetails[16].trim());
						accountStatement.setBranchAddress(custDetails[17].trim());
					}

					String[] transDetails = cbsOut[1].split("!",-1); 

					accountStatement.setTransactionRecords(getTransactions(isFirstPage, accountStatement, transDetails));

				}
			} else {
				String[] transDetails = field1.split("!",-1); 				

				accountStatement.setTransactionRecords(getTransactions(isFirstPage, accountStatement, transDetails));
			}
		}

		return accountStatement;
	}

	private static List<TransactionRecord> getTransactions(boolean isFirstPage, AccountStatement accountStatement, String[] transDetails) {
		DecimalFormat amountFormat = new DecimalFormat("0.00");
		List<TransactionRecord> transactionRecords;

		if (isFirstPage)
			transactionRecords = new ArrayList<TransactionRecord>();
		else
			transactionRecords = accountStatement.getTransactionRecords();

		for (int i=0; i < transDetails.length; i++) {
			String[] transactionRecordArry = transDetails[i].split("~",-1);
			if (transactionRecordArry.length >= 7) {
				TransactionRecord transactionRecord = new TransactionRecord();
				transactionRecord.setTransactionDate(transactionRecordArry[0].trim());
				transactionRecord.setNarration(transactionRecordArry[1].trim());
				transactionRecord.setTransactionId(transactionRecordArry[2].trim());
				if (transactionRecordArry[3].trim().equals(""))
					transactionRecord.setDebitAmount("");
				else
					transactionRecord.setDebitAmount(amountFormat.format(Float.parseFloat(transactionRecordArry[3].trim())));
				if (transactionRecordArry[4].trim().equals(""))
					transactionRecord.setCreditAmount("");
				else
					transactionRecord.setCreditAmount(amountFormat.format(Float.parseFloat(transactionRecordArry[4].trim())));
				if (transactionRecordArry[5].trim().equals(""))
					transactionRecord.setBalanceAfterTransaction("");
				else
					transactionRecord.setBalanceAfterTransaction(amountFormat.format(Float.parseFloat(transactionRecordArry[5].trim())));
				transactionRecord.setValueDate(transactionRecordArry[6].trim());
				transactionRecords.add(transactionRecord);
			}

		}
		return transactionRecords;
	}
}


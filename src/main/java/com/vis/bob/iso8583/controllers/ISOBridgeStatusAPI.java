package com.vis.bob.iso8583.controllers;

import org.springframework.web.bind.annotation.RestController;

import com.vis.bob.iso8583.client.ISOConnection;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class ISOBridgeStatusAPI {
	
	@Value("${cbs.server.host}")
	private String cbsHost;
	@Value("${cbs.server.port}")
	private int cbsPort;

	@GetMapping("/statusISOBridge")
	public String getAppStatus() {
		String returnStr = "<p>&nbsp</p><h1><b>ISO-8583 Bridge Server Status (Started)</b></h1><p>&nbsp</p>";
		List<ISOConnection> isoConnClient;
		isoConnClient = GetAccountBalanceAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Balance Inquiry - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Balance Inquiry - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetAccountBalanceAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetAccountBalanceAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}
		isoConnClient = GetLast5TransactionsAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Last 5 Transactions - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Last 5 Transactions - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetLast5TransactionsAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetLast5TransactionsAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = GetChequeStatusAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Cheque Status - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Cheque Status - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetChequeStatusAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetChequeStatusAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = GetAccountStatementAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Account Statement - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Account Statement - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetAccountStatementAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetAccountStatementAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = ChequeBookReqAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Cheque Book Request - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Cheque Book Request - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+ChequeBookReqAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							ChequeBookReqAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = GetInterestCertAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Interest Certificate - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Interest Certificate - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetInterestCertAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetInterestCertAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = SmsAlertActivationAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">SMS Alert Activation - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">SMS Alert Activation - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+SmsAlertActivationAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							SmsAlertActivationAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = StopChequeAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">Stop Cheque - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">Stop Cheque - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+StopChequeAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							StopChequeAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		isoConnClient = GetTdsCertAPI.getIsoConnClient();
		if (isoConnClient.size() <= 0)
			returnStr += "<h3 style=\"color:red\">TDS Certificate - Not Connected to ISO-8583 Server</h3>";
		else {
			returnStr += "<h3 style=\"color:green\">TDS Certificate - Connected to ISO-8583 Server ("+ cbsHost +":"+ cbsPort +")</h3>";
			for (int i=0; i < isoConnClient.size(); i++) {
				int j = i+1;
				if (isoConnClient.get(i) != null && isoConnClient.get(i).isConnected())
					returnStr += "<p style=\"color:green\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspActive Client - "+ 
							(""+GetTdsCertAPI.getIsoConnClient().get(i).getClientSocket().getLocalAddress()).substring(1) +":"+ 
							GetTdsCertAPI.getIsoConnClient().get(i).getClientSocket().getLocalPort() +"</p>";
				else
					returnStr += "<p style=\"color:grey\">&nbsp&nbsp"+j+")&nbsp&nbsp&nbsp&nbsp&nbspInactive Client - Disconnected</p>";
			}
		}

		return returnStr;
	}
}


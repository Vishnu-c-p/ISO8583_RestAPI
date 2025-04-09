package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class AccountStatement {

	private String status;
	private String cbsMssg;
	private String custName;
	private String accountNum;
	private String schemeType;
	private String availaleAmount;
	private String unclearBalance;
	private String lienAmount;
	private String clearBalance;
	private String effectiveAvailableAmount;
	private String accountCurrency;
	private String accountOpenDate;
	private String primarySecondary;
	private String name;
	private String initialBalance;
	private String custId;
	private String custAddress;
	private String custEmail;
	private String phoneNum;
	private String branchAddress;
	private List<TransactionRecord> transactionRecords;
}

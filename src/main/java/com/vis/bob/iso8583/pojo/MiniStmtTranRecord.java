package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class MiniStmtTranRecord {

	private String openingBalance;
	private String transactionDate;
	private String transactionParticulars;
	private String debitAmount;
	private String creditAmount;
	private String balanceAfterTransaction;
}

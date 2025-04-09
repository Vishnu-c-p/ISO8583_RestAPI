package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class TransactionRecord {

	private String transactionDate;
	private String narration;
	private String transactionId;
	private String debitAmount;
	private String creditAmount;
	private String balanceAfterTransaction;
	private String valueDate;
}

package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class TdsRecord {

	private String slNum;
	private String accountNum;
	private String schemeCode;
	private String transactionDate;
	private String transactionAmountCollected;
	private String transactionAmountPaid;
	private String grossAmount;
	private String tdsAmount;
	private String tdsFlag;
}

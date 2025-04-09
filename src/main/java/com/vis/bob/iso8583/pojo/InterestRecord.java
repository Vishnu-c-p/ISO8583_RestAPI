package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class InterestRecord {

	private String slNum;
	private String transactionDate;
	private String transactionAmount;
	private String interestFlag;
	private String narration;
	private String tdsAccured;
	
}

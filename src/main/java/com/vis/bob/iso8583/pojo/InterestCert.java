package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class InterestCert {
	
	private String status;
	private String cbsMssg;
	private String statement;
	private List<InterestRecord> interestRecords;
	private String custEmail;
	private String solId;
	private DepositInterest depositInterest;
	private LoanInterest loanInterest;
}

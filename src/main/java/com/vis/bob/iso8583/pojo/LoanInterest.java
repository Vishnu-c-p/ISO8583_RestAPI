package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class LoanInterest {

	private String loanType;
	private String solId;
	private String accountOpenDate;
	private String totalCreditAmount;
	private String totalTaxAmount;
	private String totalInterestAmount;
	private String totalPrincipalAmount;
	private String custName;
	private Address address;
	private List<String> jointHolders;
	private String offerFlag;
}

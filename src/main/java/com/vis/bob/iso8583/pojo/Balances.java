package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class Balances {

	private String ledgerBalance;
	private String availableBalance;
	private String floatBalance;
	private String ffdBalance;
	private String userDefinedBalance;
	private String balanceCurrency;
}

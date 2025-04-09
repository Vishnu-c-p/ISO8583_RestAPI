package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class AccountBalanceResponse {

	private StandardResponse response;
	private Balances balances;
		
}

package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class Last5TransResponse {

	private StandardResponse response;
	private Balances balances;
	private TranList transactions;
		
}

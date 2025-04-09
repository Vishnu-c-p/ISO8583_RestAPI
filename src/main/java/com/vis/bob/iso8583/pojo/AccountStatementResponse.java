package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class AccountStatementResponse {

	private StandardResponse response;
	private AccountStatement accountStatement;
		
}

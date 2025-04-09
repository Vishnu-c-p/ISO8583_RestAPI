package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class AccountStatementRequest {

	private String requestId;
	private String channel;
	private String accountNum;
	private String fromDate;
	private String toDate;
}

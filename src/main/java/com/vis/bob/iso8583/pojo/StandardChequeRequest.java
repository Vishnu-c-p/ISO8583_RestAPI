package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class StandardChequeRequest {

	private String requestId;
	private String channel;
	private String accountNum;
	private String chequeNum;
	private String mobileNum;
		
}

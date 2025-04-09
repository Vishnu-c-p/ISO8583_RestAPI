package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class StandardResponse {

	private String responseCode;
	private String requestId;
	private String channel;
	private String accountNum;		
}

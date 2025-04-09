package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class StandardRequest {

	private String requestId;
	private String channel;
	private String accountNum;
		
}

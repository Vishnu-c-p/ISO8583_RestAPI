package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class SmsAlertActRequest {

	private String requestId;
	private String channel;
	private String accountNum;
	private String action;
}

package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class SmsAlertActResponse {

	private StandardResponse response;
	private SmsAlertAct smsAlertAct;
		
}

package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class ChequeStatusResponse {

	private StandardResponse response;
	private ChequeStatus chequeStatus;
		
}

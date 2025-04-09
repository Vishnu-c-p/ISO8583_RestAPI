package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class ChequeBookReqResponse {

	private StandardResponse response;
	private ChequeBookReq chequeBookReq;
		
}

package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class ChequeStatus {

	private String chequeStatusResponse;
	private String chequeNum;
	private String responseDesc;
}

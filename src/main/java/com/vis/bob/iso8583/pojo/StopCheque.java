package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class StopCheque {

	private String stopChequeResponse;
	private String chequeNum;
	private String responseDesc;
}

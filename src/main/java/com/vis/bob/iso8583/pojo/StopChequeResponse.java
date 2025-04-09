package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class StopChequeResponse {

	private StandardResponse response;
	private StopCheque stopCheque;
		
}

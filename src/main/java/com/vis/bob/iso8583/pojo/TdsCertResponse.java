package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class TdsCertResponse {

	private StandardResponse response;
	private TdsCert tdsCert;
		
}

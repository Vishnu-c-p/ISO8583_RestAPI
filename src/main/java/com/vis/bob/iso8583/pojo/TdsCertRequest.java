package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class TdsCertRequest {

	private String requestId;
	private String channel;
	private String custId;
	private String startYear;
	private String endYear;
}

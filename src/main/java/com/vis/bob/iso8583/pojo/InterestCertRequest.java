package com.vis.bob.iso8583.pojo;

import lombok.Data;

@Data
public class InterestCertRequest {

	private String requestId;
	private String channel;
	private String accountNum;
	private String startYear;
	private String endYear;
}

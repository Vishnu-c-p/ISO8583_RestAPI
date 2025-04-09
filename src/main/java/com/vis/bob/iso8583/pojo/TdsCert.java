package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class TdsCert {

	private String status;
	private String cbsMssg;
	private String statement;
	private List<TdsRecord> tdsRecords;
	private String counterFlag;
	private String custName;
	private String custAddress;
	private String custEmail;
}

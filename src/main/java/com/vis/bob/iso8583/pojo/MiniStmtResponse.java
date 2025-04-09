package com.vis.bob.iso8583.pojo;

import java.util.List;

import lombok.Data;

@Data
public class MiniStmtResponse {

	private StandardResponse response;
	private List<MiniStmtTranRecord> transactions;
		
}
